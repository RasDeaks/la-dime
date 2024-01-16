package rest;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import model.Invoice;
import org.jboss.resteasy.reactive.RestPath;
import service.ControlFunc;

import java.util.List;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Blocking
public class Application extends Controller {

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
        public static native TemplateInstance invoices(List<Invoice> invoices);
        public static native TemplateInstance invoice(Invoice invoice);
    }



    // This overrides the convention and makes this method available at "/renarde"
    @Path("/renarde")
    public TemplateInstance index() {
        // renders the Todos/index.html template
        return Templates.index();
    }


    @Path("/invoices")
    public TemplateInstance invoices(){
        return Templates.invoices(Invoice.listAll());
    }


    @Path("/invoices")
    @POST
    public void initInvoice(@Valid Invoice invoice){
        // VALIDATION
        // hibernate validation : set by annotation, displayed on view with "#error"
        if (validationFailed()){
            invoices();
        }
        if (controlFunc.isNumFactureUnique(invoice.numFacture)){
            // add a renarde "flash" message to the view
            flash("backendError",
                    String.format("NOPE la facture [%s] existe déja", invoice.numFacture));
            // and redirect to invoices home
            invoices();
        }
        // ALL GOOD, let's persist and redirect
        invoice.persist();
        invoices();
    }

    @Path(("/invoice"))
    public TemplateInstance displayInvoice(@RestPath Long id){
        Invoice byId = Invoice.findById(id);
        notFoundIfNull(byId);
        return Templates.invoice(byId);
    }


}