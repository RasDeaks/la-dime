package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Todo  extends PanacheEntity {

    public String task;
    public Date completed;

}