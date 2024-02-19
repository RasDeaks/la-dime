package util;

import java.util.Date;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.transaction.Transactional;
import model.Entreprise;
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

            System.err.println("Adding entreprise lunatech");
            Entreprise lunatech = new Entreprise();
            lunatech.raisonSocial = "Lunatech France S.A.S.";
            lunatech.siren = "818618977";
            lunatech.persist();

            System.err.println("Adding entreprise red hat");
            Entreprise redHat = new Entreprise();
            redHat.raisonSocial = "Red Hat France";
            redHat.siren = "421199464";
            redHat.persist();

            System.err.println("Adding user fromage");
            User stef = new User();
            stef.email = "fromage@example.com";
            stef.firstName = "Stef";
            stef.lastName = "Epardaud";
            stef.userName = "fromage";
            stef.password = BcryptUtil.bcryptHash("1q2w3e4r");
            stef.status = UserStatus.REGISTERED;
            stef.isAdmin = true;
            stef.entreprise = redHat;
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
            dix.entreprise = lunatech;
            dix.persist();

            System.err.println("Adding facture 001");
            // Fromage de RedHat facture un PC a lunatech
            Invoice invoiceFromage = new Invoice();
            invoiceFromage.numFacture="NUM-FACTURE-0001";
            invoiceFromage.dateFacturation = new Date();
            invoiceFromage.user = stef;
            invoiceFromage.acheteur = lunatech;
            invoiceFromage.vendeur = redHat;
            invoiceFromage.persist();

            System.err.println("Adding facture 002");
            // Dix de Lunatech facture un conseil à RedHat
            Invoice invoiceDix = new Invoice();
            invoiceDix.numFacture="NUM-FACTURE-0002";
            invoiceDix.dateFacturation = new Date();
            invoiceDix.user = dix;
            invoiceDix.acheteur = redHat;
            invoiceDix.vendeur = lunatech;
            invoiceDix.persist();


        }
    }
}
