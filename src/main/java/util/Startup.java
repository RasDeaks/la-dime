package util;

import java.util.Date;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.transaction.Transactional;
import model.Invoice;

@ApplicationScoped
@Blocking //idk, fromage made it (Transactionnal on method should be enough)
public class Startup {
    /**
     * This method is executed at the start of your application
     *
     * @Transactional cause it publish to DB
     */
    @Transactional
    public void start(@Observes StartupEvent evt) {
        // in DEV mode we seed some data
        if(LaunchMode.current() == LaunchMode.DEVELOPMENT) {
            Invoice invoiceDev1 = new Invoice();
            invoiceDev1.numFacture="NUM-FACTURE-0001";
            invoiceDev1.dateFacturation = new Date();
            invoiceDev1.persist();

            Invoice invoiceDev2 = new Invoice();
            invoiceDev2.numFacture="NUM-FACTURE-0002";
            invoiceDev2.dateFacturation = new Date();
            invoiceDev2.persist();


        }
    }
}
