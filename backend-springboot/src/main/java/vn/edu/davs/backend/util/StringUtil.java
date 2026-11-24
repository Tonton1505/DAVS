package vn.edu.davs.backend.util;

/**
 * Technical helper utility for string checks and sensitive data masking.
 */
public final class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence cs) {
        return !isEmpty(cs);
    }

    public static boolean isBlank(CharSequence cs) {
        if (cs == null) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    public static String maskSensitive(String input, int startVisible, int endVisible) {
        if (input == null) {
            return null;
        }
        int len = input.length();
        if (len <= (startVisible + endVisible)) {
            return "*".repeat(len);
        }
        return input.substring(0, startVisible)
                + "*".repeat(len - startVisible - endVisible)
                + input.substring(len - endVisible);
    }
}
