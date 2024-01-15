package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import org.jboss.resteasy.reactive.RestForm;
import validator.DateInPast;

import java.util.Date;

@Entity
public class Invoice extends PanacheEntity {

    @RestForm
    @NotBlank
    public String numFacture;

    @RestForm
    @DateInPast(message = "La date doit être dans le passé")
    public Date dateFacturation;
}
