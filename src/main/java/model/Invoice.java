package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Date;

@Entity
public class Invoice extends PanacheEntity {

    @RestForm
    @NotBlank
    public String numFacture;

    @RestForm
    public Date dateFacturation;
}
