package rest;

import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import model.Invoice;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Blocking
public class Application extends Controller {
    
    /**
     * This defines templates available in src/main/resources/templates/Classname/method.html by convention
     */
    @CheckedTemplate
    public static class Templates {
        /**
         * This specifies that the Todos/index.html template does not take any parameter
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


    // Creates a POST action at Todos/initInvoice taking a form element named invoice
    @Path("/invoices")
    @POST
    public void initInvoice(@Valid Invoice invoice){
        if (validationFailed()){
            invoices();
        }
        //System.out.println("DIXX: " + invoice.numFacture + " | " + invoice.dateFacturation);
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