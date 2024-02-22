package rest;


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
            flash("message", "SIREN non valide");
            Log.info("Validation of PUT /entreprises Failed !");
            entreprises();
        }
        // siren must be unique
        if (Entreprise.count("siren", entreprise.siren) > 0){
            Log.info(String.format("Siren %s already known", entreprise.siren));
            flash("message", "Le siren lé innocent !");
            entreprises();
        }
        entreprise.persist();
        entreprises();
    }


    @Path("/testSirene")
    @POST
    public TemplateInstance testSirene(
            @RestForm
            @Length(min = 9, max = 9, message = "Siren 9 char")
            @Valid
            String sirene){
        if (validationFailed()){
            entreprises();
        }
        Log.info("ENTER check siren, p=" + sirene);
        Entreprise testLunatech = new Entreprise();
        testLunatech.siren = sirene;
        String resp = sirenService.verifySirenByApi(testLunatech);
        flash("message", resp);
        return entreprises();
    }

}
