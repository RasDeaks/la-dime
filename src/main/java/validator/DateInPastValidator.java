package validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Date;

public class DateInPastValidator implements ConstraintValidator<DateInPast, Date> {

    boolean required;


    @Override
    public void initialize(DateInPast constraintAnnotation) {
        required = constraintAnnotation.required();
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Date value, ConstraintValidatorContext context) {
        if(value == null) return !required;
        return new Date().after(value);
    }
}
