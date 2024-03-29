package rest;

import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import model.Invoice;
import model.User;
import org.jboss.resteasy.reactive.RestPath;
import service.ControlFunc;

import java.util.List;

@Authenticated
@Blocking
public class Invoices extends ControllerWithUser<User> {

    @Inject
    ControlFunc controlFunc;

    @CheckedTemplate
    public static class Templates{
        public static native TemplateInstance invoices(List<Invoice> invoices);
        public static native TemplateInstance invoice(Invoice invoice);
    }

    @Path("/invoices")
    public TemplateInstance invoices(){
        User user = getUser();
        checkUserStatus(user);
        Log.info("All invoices for user : " + user.userName + ", registered: " + user.registered());
        return Templates.invoices(Invoice.all(user));
    }


    @Path("/invoices")
    @POST
    public void initInvoice(@Valid Invoice invoice){
        User user = getUser();
        checkUserStatus(user);
        // VALIDATION
        // hibernate validation : set by annotation, displayed on view with "#error"
        if (validationFailed()){
            invoices();
        }
        if (controlFunc.isNumFactureUnique(invoice.numFacture, user)){
            // add a renarde "flash" message to the view
            flash("backendError",
                    String.format("La facture [%s] existe déja, édite là", invoice.numFacture));
            // and redirect to invoices home
            invoices();
        }
        // valid? let's persist
        invoice.user = user;
        invoice.persist();
        // redirect to Invoices list
        invoices();
    }

    @Path(("/invoice"))
    @POST
    public void updateInvoice(@RestPath Long invoiceId, @Valid Invoice invoice){
        checkUserStatus(getUser());
        invoice.persist();
        displayInvoice(invoiceId);
    }

    // Invoice Detail : secured by user
    @Path(("/invoice"))
    public TemplateInstance displayInvoice(@RestPath Long id){
        Invoice byId = Invoice.findById(id);
        if (byId == null){
            notFound(String.format("Facture [%s] does not exist",id));
        }
        notFoundIfNull(byId);
        if (!byId.user.id.equals(getUser().id)) {
            forbidden("Not your Facture");
        }
        return Templates.invoice(byId);
    }

    @POST
    public void delete(@RestPath long id){
        checkUserStatus(getUser());
        PanacheEntityBase byId = Invoice.findById(id);
        notFoundIfNull(byId);
        byId.delete();
        invoices();
    }

    // dirty fix for CONFIRMATION_REQUIRED user (getUser will filter status, they are null)
    private void checkUserStatus(User user){
        if (user == null){
            Log.info("USER null, CONFIRMATION REQUIRED");
            throw new RedirectException(Response.seeOther(Router.getURI(Login::logout)).build());
        }
    }

}
