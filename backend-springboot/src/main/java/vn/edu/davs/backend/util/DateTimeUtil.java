package vn.edu.davs.backend.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Technical helper utility for date/time formatting, parsing, and epoch conversions.
 */
public final class DateTimeUtil {

    public static final String DEFAULT_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    public static final String ISO_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private DateTimeUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return "";
        }
        String p = (pattern != null && !pattern.isEmpty()) ? pattern : DEFAULT_DATETIME_PATTERN;
        return dateTime.format(DateTimeFormatter.ofPattern(p));
    }

    public static LocalDateTime parse(String text, String pattern) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String p = (pattern != null && !pattern.isEmpty()) ? pattern : DEFAULT_DATETIME_PATTERN;
        return LocalDateTime.parse(text.trim(), DateTimeFormatter.ofPattern(p));
    }

    public static long toEpochMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static LocalDateTime fromEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
