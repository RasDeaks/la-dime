package rest;

import java.util.Date;
import java.util.List;

import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import jakarta.ws.rs.core.MediaType;
import model.Invoice;
import org.jboss.resteasy.reactive.RestForm;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.CheckedTemplate;
import io.quarkiverse.renarde.Controller;
import model.Todo;
import org.jboss.resteasy.reactive.RestQuery;

import javax.xml.crypto.Data;

/**
 * This defines a REST controller, each method will be available under the "Classname/method" URI by convention
 */
@Blocking
public class Todos extends Controller {
    
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
        /**
         * This specifies that the Todos/todos.html template takes a todos parameter of type List&lt;Todo&gt;
         */
        public static native TemplateInstance todos(List<Todo> todos);
    }



    // This overrides the convention and makes this method available at "/renarde"
    @Path("/renarde")
    public TemplateInstance index() {
        // renders the Todos/index.html template
        return Templates.index();
    }

    @Transactional
    public TemplateInstance todos() {
        // renders the Todos/todos.html template with our list of Todo entities
        return Templates.todos(Todo.listAll());
    }
    
    // Creates a POST action at Todos/add taking a form element named task
    @POST
    public void add(@RestForm @NotBlank String task) {
        // If validation fails, redirect to the todos page (with errors propagated)
        if(validationFailed()) {
            // redirect to the todos page by just calling the method: it does not return!
            todos();
        }
        // save the new Todo
        Todo todo = new Todo();
        todo.task = task;
        todo.persist();
        // redirect to the todos page
        todos();
    }


    public TemplateInstance invoices(){
        return Templates.invoices(Invoice.listAll());
    }


    @Path("/invoices")
    @POST
    public void initInvoice(@RestForm @NotBlank String numFacture,
                            @RestForm Date dateFacturation){
        if (validationFailed()){
            invoices();
        }
        //System.out.println(numFacture + " | " + dateFacturation);
        Invoice invoice = new Invoice();
        invoice.numFacture = numFacture;
        invoice.dateFacturation = dateFacturation;
        invoice.persist();
        invoices();
    }


}