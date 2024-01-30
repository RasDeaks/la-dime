package rest;

import email.Emails;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkiverse.renarde.util.StringUtils;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.webauthn.WebAuthnLoginResponse;
import io.quarkus.security.webauthn.WebAuthnRegisterResponse;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.smallrye.common.annotation.Blocking;
import io.vertx.ext.auth.webauthn.Authenticator;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import model.User;
import model.UserStatus;
import model.WebAuthnCredential;
import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

@Blocking
public class Login extends ControllerWithUser<User>{
    @Context HttpHeaders headers;

    @Inject
    RenardeSecurity security;

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @CheckedTemplate
    static class Templates {
        public static native TemplateInstance login();
        public static native TemplateInstance register(String email);
        public static native TemplateInstance confirm(User newUser);
        public static native TemplateInstance logoutFirst();
        public static native TemplateInstance welcome();
    }

    /**
     * Login page
     * Check is user already logged and q_auth cookie relic
     */
    @Path("/login")
    public TemplateInstance login(){
        // early exit : can't access page if already logged
        if (getUser() !=null){
            flash.flash("message","You are already logged, please logout to reconnect");
            throw new RedirectException(Response.seeOther(Router.getURI(Application::index)).build());
        }

        // tricks to proper Logout (q_auth failure fix) : clear auth cookie
        boolean gAuthCookieFound = headers.getCookies().keySet().stream()
                .anyMatch(cookieName -> cookieName.startsWith("q_auth"));
        if (gAuthCookieFound){
            Log.info("auth cookie found at login ! redirect to /clearAuthCookie");
            clearAuthCookie();
        }
        // end tricks

        // not logged and not on oidc registration -> let him create an account
        return Templates.login();
    }

    @Path("/clearAuthCookie")
    public Response clearAuthCookie() {
        // DIX try to fix INVALID user on oidc failure
        // need to clean q_auth_* cookie as user was never saved with tenantID when it fails
        // ex: q_auh_goole_UUID_XX_XXXX cookie will look for userName with tenant google, which is saved on oidcSuccess
        // see RenardeSecurity makeLogoutResponse
        List<NewCookie> cookiesToRemove = headers.getCookies().keySet().stream()
                .filter(cookie -> cookie.startsWith("q_auth"))
                .map(s -> new NewCookie.Builder(s)
                        .sameSite(NewCookie.SameSite.LAX)
                        .maxAge(0)
                        .build())
                .toList();
        Log.info("clear " + cookiesToRemove.size() + " auth cookie(s)");
        return Response.seeOther(Router.getURI(Login::login)).cookie(cookiesToRemove.toArray(new NewCookie[0])).build();
    }

    /**
     * Welcome page at the end of registration
     */
    @Authenticated
    public TemplateInstance welcome() {
        return Templates.welcome();
    }

    /**
     * Manual login form
     */
    @POST
    public Response manualLogin(@NotBlank @RestForm String userName,
                                @RestForm String password,
                                @BeanParam WebAuthnLoginResponse webAuthnResponse,
                                RoutingContext ctx) {
        if(webAuthnResponse.isSet()) {
            validation.required("webAuthnId", webAuthnResponse.webAuthnId);
            validation.required("webAuthnRawId", webAuthnResponse.webAuthnRawId);
            validation.required("webAuthnResponseClientDataJSON", webAuthnResponse.webAuthnResponseClientDataJSON);
            validation.required("webAuthnResponseAuthenticatorData", webAuthnResponse.webAuthnResponseAuthenticatorData);
            validation.required("webAuthnResponseSignature", webAuthnResponse.webAuthnResponseSignature);
            // UserHandle not required
            validation.required("webAuthnType", webAuthnResponse.webAuthnType);
        } else {
            validation.required("password", password);
        }
        if(validationFailed()) {
            login();
        }
        User user = User.findRegisteredByUserName(userName);
        if(user == null) {
            validation.addError("userName", "Invalid username/pasword");
            prepareForErrorRedirect();
            login();
        }
        if(!webAuthnResponse.isSet()) {
            if(!BcryptUtil.matches(password, user.password)) {
                validation.addError("userName", "Invalid username/pasword");
                prepareForErrorRedirect();
                login();
            }
        } else {
            // This is sync anyway
            Authenticator authenticator = this.webAuthnSecurity.login(webAuthnResponse, ctx)
                    .await().indefinitely();
            // bump the auth counter
            user.webAuthnCredential.counter = authenticator.getCounter();
        }
        NewCookie cookie = security.makeUserCookie(user);
        return Response.seeOther(Router.getURI(Application::index)).cookie(cookie).build();
    }

    /**
     * Manual registration form, sends confirmation email
     */
    @POST
    public TemplateInstance register(@RestForm @NotBlank @Email String email) {
        // early exit on email pattern
        if(validationFailed()) {
            login();
        }
        // find user in DB by mail in case he already tried
        // or create a new user (regular case)
        User newUser = User.findUnconfirmedByEmail(email);
        if(newUser == null) {
            newUser = new User();
            newUser.email = email;
            newUser.status = UserStatus.CONFIRMATION_REQUIRED;
            newUser.confirmationCode = UUID.randomUUID().toString();
            newUser.persist();
        }
        // send the confirmation code
        Emails.confirm(newUser);
        return Templates.register(email);
    }


    /**
     * Confirmation form, once email is verified, to add user info
     */
    public TemplateInstance confirm(@RestQuery String confirmationCode){
        checkLogoutFirst();
        User newUser = checkConfirmationCode(confirmationCode);
        return Templates.confirm(newUser);
    }

    @Path("/logout")
    public Response logout(){
        // might have an issue here. Some auth cookie might need to be cleaned
        // see RenardeSecurityController.logout()
        try {
            return security.makeLogoutResponse(new URI("/renarde"));
        } catch (URISyntaxException e) {
            //can't happen
            throw new RuntimeException(e);
        }
    }

    private void checkLogoutFirst() {
        if(getUser() != null) {
            logoutFirst();
        }
    }

    /**
     * Link to logout page
     */
    public TemplateInstance logoutFirst() {
        return Templates.logoutFirst();
    }

    private User checkConfirmationCode(String confirmationCode) {
        // can't use error reporting as those are query parameters and not form fields
        if(StringUtils.isEmpty(confirmationCode)){
            flash("message", "Missing confirmation code");
            flash("messageType", "error");
            redirect(Application.class).index();
        }
        User user = User.findForContirmation(confirmationCode);
        if(user == null){
            flash("message", "Invalid confirmation code");
            flash("messageType", "error");
            redirect(Application.class).index();
        }
        return user;
    }

    @POST
    public Response complete(@RestQuery String confirmationCode,
                             @RestForm @NotBlank String userName,
                             @RestForm @Length(min = 8) String password,
                             @RestForm @Length(min = 8) String password2,
                             @BeanParam WebAuthnRegisterResponse webAuthnResponse,
                             @RestForm @NotBlank String firstName,
                             @RestForm @NotBlank String lastName,
                             RoutingContext ctx) {
        checkLogoutFirst();
        User user = checkConfirmationCode(confirmationCode);

        if(validationFailed())
            confirm(confirmationCode);

        // is it OIDC?
        if(!user.isOidc()) {
            if(!webAuthnResponse.isSet()) {
                validation.required("password", password);
                validation.required("password2", password2);
                validation.equals("password", password, password2);
            } else {
                validation.required("webAuthnId", webAuthnResponse.webAuthnId);
                validation.required("webAuthnRawId", webAuthnResponse.webAuthnRawId);
                validation.required("webAuthnResponseAttestationObject", webAuthnResponse.webAuthnResponseAttestationObject);
                validation.required("webAuthnResponseClientDataJSON", webAuthnResponse.webAuthnResponseClientDataJSON);
                validation.required("webAuthnType", webAuthnResponse.webAuthnType);
            }
        }

        if(User.findRegisteredByUserName(userName) != null)
            validation.addError("userName", "User name already taken");
        if(validationFailed())
            confirm(confirmationCode);

        if(!user.isOidc()) {
            if(!webAuthnResponse.isSet()) {
                user.password = BcryptUtil.bcryptHash(password);
            } else {
                // this is sync
                Authenticator authenticator = webAuthnSecurity.register(webAuthnResponse, ctx).await().indefinitely();
                WebAuthnCredential creds = new WebAuthnCredential(authenticator, user);
                creds.persist();
            }
        }
        user.userName = userName;
        user.firstName = firstName;
        user.lastName = lastName;
        user.confirmationCode = null;
        user.status = UserStatus.REGISTERED;

        ResponseBuilder responseBuilder = Response.seeOther(Router.getURI(Login::welcome));
        if(!user.isOidc()) {
            NewCookie cookie = security.makeUserCookie(user);
            responseBuilder.cookie(cookie);
        }
        return responseBuilder.build();
    }
}
