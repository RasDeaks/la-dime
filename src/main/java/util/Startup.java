package util;

import java.util.Date;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.transaction.Transactional;
import model.Invoice;
import model.User;
import model.UserStatus;

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
            System.err.println("Adding user fromage");
            User stef = new User();
            stef.email = "fromage@example.com";
            stef.firstName = "Stef";
            stef.lastName = "Epardaud";
            stef.userName = "fromage";
            stef.password = BcryptUtil.bcryptHash("1q2w3e4r");
            stef.status = UserStatus.REGISTERED;
            stef.isAdmin = true;
            stef.persist();

            System.err.println("Adding user dix");
            User dix = new User();
            dix.email = "dix@example.com";
            dix.firstName = "Jean";
            dix.lastName = "JEAN";
            dix.userName = "dix";
            dix.password = BcryptUtil.bcryptHash("P455W0RD");
            dix.status = UserStatus.REGISTERED;
            dix.isAdmin = true;
            dix.persist();


            Invoice invoiceDev1 = new Invoice();
            invoiceDev1.numFacture="NUM-FACTURE-0001";
            invoiceDev1.dateFacturation = new Date();
            invoiceDev1.user = stef;
            invoiceDev1.persist();

            Invoice invoiceDev2 = new Invoice();
            invoiceDev2.numFacture="NUM-FACTURE-0002";
            invoiceDev2.dateFacturation = new Date();
            invoiceDev2.user = stef;
            invoiceDev2.persist();


        }
    }
}
