package rest;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.logging.Log;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import model.User;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestForm;
import service.RenardeAppleClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Authenticated
@Blocking
public class Users extends ControllerWithUser<User> {

    @Inject
    RenardeSecurity security;

    @Inject
    @IdToken
    JsonWebToken jwt;

    @Inject
    AccessTokenCredential accessToken;


    @ConfigProperty(name = "quarkus.oidc.apple.client-id")
    String appleClientId;

    @ConfigProperty(name = "quarkus.oidc.apple.client-secret")
    String appleClientSecret;


    @RestClient
    RenardeAppleClient renardeAppleClient;




    @CheckedTemplate
    public static class Templates{

        public static native TemplateInstance user(User user);

        public static native TemplateInstance users(List<User> users);

    }


    @Path("/users")
    @RolesAllowed("admin")
    public TemplateInstance users(){
        return Templates.users(User.listAll());
    }


    @Path(("/account"))
    public TemplateInstance user(){
        return Templates.user(getUser());
    }


    @Path("/user")
    @POST
    public void updateUser(@RestForm String firstName,@RestForm String lastName){
        Log.info("Incomming User update p = " + firstName + " | " + lastName);
        User user = getUser();
        user.firstName = firstName;
        user.lastName = lastName;
        user.persist();
        user();
    }

    @Path("/revokeApple")
    public void revokeApple(){
        User user = getUser();
        Log.info(String.format("Revoke apple for [%s] %n\tclient_id=%s%n\tclient_secret=%s%n\ttoken=%s",
                user.userName,
                appleClientId,
                appleClientSecret,
                accessToken.getToken()));
        //fixme: currently not working | no details about the error (might be the clientID/secret)
        //see https://developer.apple.com/documentation/accountorganizationaldatasharing/revoke-tokens
        try {
            renardeAppleClient.revokeAppleUser(appleClientId, appleClientSecret, accessToken.getToken(), "access_token");
        } catch (ClientWebApplicationException e) {
            flash("backendError", "Error on /revoke : " + e.getMessage());
            user();
        }
        // rm user : NOT IMPL
        // logout
        try {
            security.makeLogoutResponse(new URI("/login"));
        } catch (URISyntaxException e) {
            //can't happen
            throw new RuntimeException(e);
        }
    }

}
