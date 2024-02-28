package model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import org.jboss.resteasy.reactive.RestForm;

import java.math.BigDecimal;

@Entity
@Table(name = "line_product")
public class Line extends PanacheEntity {

    @ManyToOne
    public Invoice invoice;

    @RestForm
    public String description;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer=3, fraction=2)
    public BigDecimal qtty;

    @RestForm
    @Pattern(regexp = "^(-)?(([1-9]{1}\\d*)|([0]{1}))?$")
    public String unitPrice;

    @RestForm
    @Pattern(regexp = "^(-)?(([1-9]{1}\\d*)|([0]{1}))?$")
    public String taxRate;

}
