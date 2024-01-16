package service;

import jakarta.enterprise.context.ApplicationScoped;
import model.Invoice;
import model.User;

@ApplicationScoped
public class ControlFunc {


    public boolean isNumFactureUnique(String numFacture, User user){
        // case insensitive  PER USER (should change to company)
        return Invoice.find("lower(numFacture) = lower(?1) AND user.id=?2", numFacture, user.id)
                .count() > 0;
    }
}
