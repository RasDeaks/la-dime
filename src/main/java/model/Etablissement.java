package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "establishment", uniqueConstraints = @UniqueConstraint(columnNames = {"siret"}))
public class Etablissement extends PanacheEntity {

    @ManyToOne
    public Entreprise entreprise;

    public String siret;

    public String denominationUniteLegale;

    public String numVoie;

    public String typeVoie;

    public String libelleVoie;

    public String codePostal;

    public String libelleCommune;

}
