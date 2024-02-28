package validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import util.NumberUtils;

public class NumberFormatValidator implements ConstraintValidator<NumberFormat, String> {

    boolean required;

    int maxIntPartSize;

    int maxDecimalDepth;


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // validate only if required OR given
        if (required || !StringUtils.isBlank(value)) {
            // try to read number, then check max int size and decimal depth
            return NumberUtils.tryStringToBigDecimal(value).map(bigDecimal ->
                            NumberUtils.isIntLengthValid(bigDecimal, maxIntPartSize) &&
                                    NumberUtils.isDecimalLengthValid(bigDecimal, maxDecimalDepth))
                    .orElse(!required);

        }
        return true;
    }


    @Override
    public void initialize(NumberFormat arg0) {
        required = arg0.required();
        maxIntPartSize = arg0.maxIntPartSize();
        maxDecimalDepth = arg0.maxDecimalDepth();
    }


}
