package vn.edu.davs.backend.util;

import java.util.regex.Pattern;

/**
 * Technical helper utility for file name validation, extension extraction, and sanitization.
 */
public final class FileUtil {

    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    private FileUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean checkFileNameSpecialChar(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        return SAFE_FILENAME_PATTERN.matcher(fileName.trim()).matches();
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase();
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String clean = fileName.replaceAll("[\\\\/]+", "_")
                .replaceAll("\\.\\.+", ".")
                .trim();
        return clean.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
}
