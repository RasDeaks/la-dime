package util;

import io.quarkus.qute.TemplateExtension;

/**
 * Add your custom Qute extension methods here.
 */
@TemplateExtension
public class JavaExtensions {
    /**
     * This registers the String.capitalise extension method
     *
     * It Turns the first letter of a String to upperCase, nicely*
     */
    public static String capitalise(String string) {
        StringBuilder sb = new StringBuilder();
        for (String part : string.split("\\s+")) {
            if(sb.length() > 0) {
                sb.append(" ");
            }
            if(part.length() > 0) {
            sb.append(part.substring(0, 1).toUpperCase());
            sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    /**
     * toUpperCase extension
     */
    public static String toUpperCase(String s){
        return s.toUpperCase();
    }
}
