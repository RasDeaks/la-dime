package rest;


import error.DimeError;
import error.DimeWsException;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.Entreprise;
import model.User;
import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestForm;
import service.SirenService;

import java.util.List;

@Authenticated
@Blocking
public class Entreprises extends ControllerWithUser<User> {

    @Inject
    SirenService sirenService;

    @CheckedTemplate
    public static class Templates{

        public static native TemplateInstance entreprises(List<Entreprise> entreprises);

    }


    @Path("/entreprises")
    public TemplateInstance entreprises(){
        Log.info("ENTER All entreprise for logged user  : " + getUser().userName);
        return Templates.entreprises(Entreprise.listAll());
    }


    @Path("/entreprises")
    @POST
    public void createEntreprise(@Valid Entreprise entreprise){
        Log.info("ENTER POST /entreprises");
        User user = getUser();
        // make sure user registered fixme : add role and avoid this boiler plate
        if (user == null){
            Log.info("USER null, CONFIRMATION REQUIRED");
            throw new RedirectException(Response.seeOther(Router.getURI(Login::logout)).build());
        }
        // basic hibernate validation (size, pattern etc..)
        if (validationFailed()){
            flash("backendError", "SIREN non valide");
            Log.info("Validation of PUT /entreprises Failed !");
            entreprises();
        }
        try {
            String resp = sirenService.verifyAndSaveCompany(entreprise);
            flash("message", resp);
        } catch (DimeWsException | DimeError e) {
            flash("backendError", e.getMessage());
        }
        entreprises();
    }


    @Path("/testSirene")
    @POST
    public void testSirene(
            @RestForm
            @Length(min = 9, max = 9, message = "Siren 9 char")
            @Valid
            String sirene){
        // early exit on Validation failure
        if (validationFailed()){
            entreprises();
        }
        Log.info("ENTER check siren, p=" + sirene);
        Entreprise testLunatech = new Entreprise();
        testLunatech.siren = sirene;

        try {
            String resp = sirenService.verifyAndSaveCompany(testLunatech);
            flash("message", resp);
        } catch (DimeWsException | DimeError e) {
            flash("backendError", e.getMessage());
        }
        entreprises();
    }

}
