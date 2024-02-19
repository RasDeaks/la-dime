package rest;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.security.RenardeSecurity;
import io.quarkus.logging.Log;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.User;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import util.DixUtils;

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
    public Response revokeApple(User user){
        Log.info("Incomming Revoke apple user update p = " + user.userName);
        //TODO revoke
        // rm user
        // logout
        try {
            return security.makeLogoutResponse(new URI("/renarde"));
        } catch (URISyntaxException e) {
            //can't happen
            throw new RuntimeException(e);
        }
    }

}
