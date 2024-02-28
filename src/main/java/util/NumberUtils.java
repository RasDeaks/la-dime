package util;

import io.quarkus.logging.Log;

import java.math.BigDecimal;
import java.util.Optional;

public class NumberUtils {

    // utils class...
    private NumberUtils(){}


    /**
     * Safely parse a String to an Optional<BigDecimal>
     *
     * @param value The String to parse
     * @return An Optional of a bigDecimal representation of the String OR an Optional.empty() if an error occurs
     */
    public static Optional<BigDecimal> tryStringToBigDecimal(String value){
        try {
            return Optional.of(new BigDecimal(value));
        }catch (NullPointerException | NumberFormatException e){
            Log.warn(String.format("Unable to validate String as a number, String value=[%s]", value));
            return Optional.empty();
        }
    }

    public static BigDecimal parseStringToBigDecimal(String value){
        Optional<BigDecimal> bigDecimal = tryStringToBigDecimal(value);
        return bigDecimal.orElseGet(() -> BigDecimal.valueOf(0));
    }

    /**
     * Validate the length of the integer part of a BigDecimal against a limit
     *
     * @param value The BigDecimal to validate
     * @param sizeLimit The limit used to validate
     * @return true if the number is valid, else false.
     */
    public static boolean isIntLengthValid(BigDecimal value, int sizeLimit){
        boolean b = value.toBigInteger().toString().length() <= sizeLimit;
        Log.debug(String.format("isIntLengthValid:  value=[%s], limit=[%s], result=%s", value.toPlainString(), sizeLimit, b ? "OK" : "KO"));
        return b;
    }

    /**
     * Validate the length of the decimal part of a BigDecimal against a limit
     *
     * @param value The BigDecimal to validate
     * @param sizeLimit The limit used to validate
     * @return true if the number is valid, else false.
     */
    public static boolean isDecimalLengthValid(BigDecimal value, int sizeLimit){
        boolean b = getLengthAfterLastSeparator(value.toPlainString()) <= sizeLimit;
        Log.debug(String.format("isDecimalLengthValid:  value=[%s], limit=[%s], result=%s", value.toPlainString(), sizeLimit, b ? "OK" : "KO"));
        return b;
    }

    /**
     * Find decimal length of a number as String using substring of last separator (".")
     *
     * @param numberAsString The string containing a number that might have a separator
     * @return The length after the last separator OR ZERO if no separator found
     */
    public static int getLengthAfterLastSeparator(String numberAsString) {
        int result;
        int lastPointPos = numberAsString.lastIndexOf(".");
        if (lastPointPos == -1){
            result= 0;
        }else{
            result = numberAsString.length() - (lastPointPos + 1);
        }
        return result;
    }

}
