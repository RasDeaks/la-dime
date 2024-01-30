package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import org.hibernate.validator.constraints.Length;

@Entity
public class Entreprise extends PanacheEntity {

    public String raisonSocial;

    @Length(message = "Le siren doit faire 9 caractères", min = 9, max = 9)
    public String siren;

    //List<Etablissement> -> List<CodeRoutage>

}
