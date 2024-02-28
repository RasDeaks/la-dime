package rest;

import error.DimeError;
import error.DimeWsException;
import io.quarkiverse.renarde.router.Router;
import io.quarkiverse.renarde.util.RedirectException;
import io.quarkus.logging.Log;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import model.Entreprise;
import model.Invoice;
import model.Line;
import model.User;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import service.ControlFunc;
import service.SirenService;
import validator.NumberFormat;

import java.math.BigDecimal;
import java.util.List;

@Authenticated
@Blocking
public class Invoices extends HxControllerWithUser<User> {

    @Inject
    ControlFunc controlFunc;

    @Inject
    SirenService sirenService;

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates{
        public static native TemplateInstance invoices(List<Invoice> invoices);

        //https://docs.quarkiverse.io/quarkus-renarde/dev/advanced.html#htmx
        public static native TemplateInstance invoices$invoiceRow(Invoice invoice);

        public static native TemplateInstance invoice(Invoice invoice, List<Line> lines, Line newLine);

        public static native TemplateInstance invoice$lineRow(Invoice invoice, Line line);

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
        invoice.vendeur =(Entreprise) Entreprise.find("siren", 818618977).firstResult();
        invoice.persist();
        // redirect to Invoices list
        invoices();
    }

    @Path(("/invoice"))
    @POST
    public void updateInvoice(@RestPath Long invoiceId, Invoice dto){
        checkUserStatus(getUser());

        // find Facture or 404
        Invoice byId = Invoice.findById(invoiceId);
        notFoundIfNull(byId);

        // update value and save
        byId.vendeur = (Entreprise) Entreprise.find("siren", dto.vendeur.siren).firstResult();
        byId.persist();
        displayInvoice(invoiceId);
    }

    // Invoice Detail : secured by user
    @Path(("/invoice"))
    public TemplateInstance displayInvoice(@RestPath Long id){
        Invoice byId = Invoice.findById(id);
        if (byId == null){
            notFound(String.format("Facture [%s] does not exist",id));
        }
        notFoundIfNull(byId); // redondant...
        if (!byId.user.id.equals(getUser().id)) {
            forbidden("Not your Facture");
        }
        List<Line> lines = Line.list("invoice", byId).stream()
                .map(Line.class::cast)
                .toList();
        return Templates.invoice(byId, lines, new Line());
    }

    @POST
    public TemplateInstance delete(@RestPath long id){
        checkUserStatus(getUser());
        Invoice byId = (Invoice) Invoice.findById(id);
        notFoundIfNull(byId);
        byId.delete();
        if (isHxRequest()) {
            return Templates.invoices$invoiceRow(byId);
        }else {
            return invoices();
        }
    }


    @POST
    public TemplateInstance attachNewVendeur(@RestPath Long id, @RestForm String sirene){
        Log.info("ENTER attach by siren, p=" + sirene);
        Invoice byId = Invoice.findById(id);
        if (byId == null){
            notFound(String.format("Facture [%s] does not exist",id));
        }
        Entreprise testLunatech = new Entreprise();
        testLunatech.siren = sirene;
        try {
            String resp = sirenService.verifyAndSaveCompany(testLunatech);
            flash("message", resp);
            byId.vendeur = Entreprise.find("siren", sirene).firstResult();
            byId.persist();
        } catch (DimeWsException | DimeError e) {
            flash("backendError", e.getMessage());
            return displayInvoice(id);
        }
        return displayInvoice(id);
    }

    @POST
    public void addLine(@RestPath Long id, @RestForm String description,
                         @NumberFormat(message = "Format nombre incorrect", maxIntPartSize = 2, maxDecimalDepth = 3, required = true)
                         @RestForm String qttyDto,
                         @RestForm String unitPrice,
                         @RestForm String taxRate){
        Log.info(String.format("ENTER add product, invoice=%s, desc=%s, qtty=%s", id, description, qttyDto));
        Invoice relatedInvoice =(Invoice) Invoice.findById(id);
        notFoundIfNull(relatedInvoice);

        if (validationFailed()){
            Log.error("Validation failled for new product line");
            displayInvoice(id);
        }

        Line line = new Line();
        line.description = description;
        line.unitPrice = unitPrice;
        line.qtty = new BigDecimal(qttyDto);
        line.taxRate = taxRate;
        line.invoice = relatedInvoice;
        line.persist();

        displayInvoice(id);
    }

    @POST
    public TemplateInstance updateDescription(@RestPath Long id, @RestForm String description){
        Log.info("ENTER update description, line=" + id + " desc="+description);
        Line byId = (Line) Line.findById(id);
        notFoundIfNull(byId);

        byId.description = description;
        byId.persist();
        return Templates.invoice$lineRow(byId.invoice,byId);
    }

    @POST
    public TemplateInstance updateQtty(@RestPath Long id, @NumberFormat(message = "Format nombre incorrect", maxIntPartSize = 2, maxDecimalDepth = 3, required = true) @RestForm String qttyDto){
        Log.info("ENTER update qtty, line=" + id + " qtty="+qttyDto);
        Line byId = (Line) Line.findById(id);
        notFoundIfNull(byId);

        if (isHxRequest() && validation.hasErrors()){
            this.hx(HxResponseHeader.REFRESH, "true");
            return Templates.invoice$lineRow(byId.invoice,byId);
        } else if (validationFailed()){
            displayInvoice(id);
        }

        byId.qtty = new BigDecimal(qttyDto);
        byId.persist();
        return Templates.invoice$lineRow(byId.invoice,byId);
    }

    @POST
    public TemplateInstance updateUnitPrice(@RestPath Long id, @RestForm String unitPrice){
        Log.info("ENTER update unitPrice, line=" + id + " price="+unitPrice);
        Line byId = (Line) Line.findById(id);
        notFoundIfNull(byId);

        byId.unitPrice = unitPrice;
        byId.persist();
        return Templates.invoice$lineRow(byId.invoice,byId);
    }

    @POST
    public TemplateInstance updateTaxRate(@RestPath Long id, @RestForm String taxRate){
        Log.info("ENTER update taxRate, line=" + id + " rate="+taxRate);
        Line byId = (Line) Line.findById(id);
        notFoundIfNull(byId);

        byId.taxRate = taxRate;
        byId.persist();
        return Templates.invoice$lineRow(byId.invoice,byId);
    }


    @POST
    public TemplateInstance removeLine(@RestPath Long id){
        Log.info("ENTER rm product, p=" + id);
        Line byId = (Line) Line.findById(id);
        notFoundIfNull(byId);
        Invoice relatedInvoice =(Invoice) Invoice.findById(byId.invoice.id);
        byId.delete();
        return Templates.invoice$lineRow(relatedInvoice, byId);

    }

    // dirty fix for CONFIRMATION_REQUIRED user (getUser will filter status, they are null)
    private void checkUserStatus(User user){
        if (user == null){
            Log.info("USER null, CONFIRMATION REQUIRED");
            throw new RedirectException(Response.seeOther(Router.getURI(Login::logout)).build());
        }
    }

}
