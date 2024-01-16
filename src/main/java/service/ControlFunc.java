package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Invoice;

@ApplicationScoped
public class ControlFunc {


    public boolean isNumFactureUnique(String numFacture){
        // case insensitive uniqueness
        return Invoice.find("lower(numFacture) like concat('%', lower(?1), '%')", numFacture)
                .count() > 0;
    }
}
