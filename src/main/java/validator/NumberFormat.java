package validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NumberFormatValidator.class)
@Documented
public @interface NumberFormat {

    String message() ;

    /**
     * @return maximum integer length (number of char of the int part of the number)
     */
    int maxIntPartSize() default Integer.MAX_VALUE;

    /**
     * @return maximum number of char after separator
     */
    int maxDecimalDepth() default Integer.MAX_VALUE;

    boolean required() default true;

    Class<?>[] groups() default {};


    Class<? extends Payload>[] payload() default {};

}
