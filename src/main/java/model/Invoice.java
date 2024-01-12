package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.util.Date;

@Entity
public class Invoice extends PanacheEntity {

    public String numFacture;
    public Date dateFacturation;
}
