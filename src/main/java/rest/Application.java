package rest;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import model.User;
import service.ControlFunc;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */

@Blocking
public class Application extends ControllerWithUser<User> {

    @Inject
    ControlFunc controlFunc;
    
    /**
     * This defines templates available in src/main/resources/templates/Classname/method.html by convention
     */
    @CheckedTemplate
    public static class Templates {
        /**
         * This specifies that the Application/index.html template does not take any parameter
         */
        public static native TemplateInstance index();

    }



    // This overrides the convention and makes this method available at "/renarde"
    @Path("/renarde")
    public TemplateInstance index() {
        // renders the Todos/index.html template
        return Templates.index();
    }




}