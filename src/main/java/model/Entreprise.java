package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import org.hibernate.validator.constraints.Length;
import org.jboss.resteasy.reactive.RestForm;

@Entity
public class Entreprise extends PanacheEntity {

    @RestForm
    public String raisonSocial;

    @RestForm
    @Length(message = "Le siren doit faire 9 caractères", min = 9, max = 9)
    public String siren;

    //List<Etablissement> -> List<CodeRoutage>

}
