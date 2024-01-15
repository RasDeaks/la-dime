package validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateInPastValidator.class)
@Documented
public @interface DateInPast {

    // custom message for front
    String message() ;

    // Accept null value ?
    boolean required() default false;


    // idk lol
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
