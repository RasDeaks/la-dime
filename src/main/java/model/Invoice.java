package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import org.jboss.resteasy.reactive.RestForm;
import validator.DateInPast;

import java.util.Date;
import java.util.List;

@Entity
public class Invoice extends PanacheEntity {

    @ManyToOne
    public User user;

    @RestForm
    @NotBlank(message = "Le numéro de facture ne peut etre vide")
    public String numFacture;

    @RestForm
    @DateInPast(message = "La date doit être dans le passé")
    public Date dateFacturation;

    public static List<Invoice> all(User user){
        return list("user", Sort.by("user"), user);
    }
}
