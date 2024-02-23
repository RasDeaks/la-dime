package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestForm;

@Entity
@Table(name = "company", uniqueConstraints = @UniqueConstraint(columnNames = {"siren"}))
public class Entreprise extends PanacheEntity {

    @RestForm
    public String raisonSocial;

    /**
     * The French unique identifier of a company
     */
    @RestForm
    @Length(message = "Le siren doit faire 9 caractères", min = 9, max = 9)
    public String siren;


}
