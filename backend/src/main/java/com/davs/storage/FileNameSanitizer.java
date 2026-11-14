package com.davs.storage;

import java.util.regex.Pattern;

/**
 * Sanitizes user-supplied filenames to prevent path traversal ({@code ../../x}),
 * control-character injection, and invalid object-key characters.
 */
public final class FileNameSanitizer {

    private static final int MAX_FILE_NAME_LENGTH = 180;
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final Pattern MULTI_DOTS = Pattern.compile("\\.{2,}");

    private FileNameSanitizer() {
    }

    public static String sanitize(String rawFileName) {
        if (rawFileName == null || rawFileName.isBlank()) {
            return "file";
        }
        String cleaned = rawFileName.replace("\0", "").trim();
        int lastSlash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            cleaned = cleaned.substring(lastSlash + 1);
        }

        cleaned = UNSAFE_CHARS.matcher(cleaned).replaceAll("_");
        cleaned = MULTI_DOTS.matcher(cleaned).replaceAll(".");

        while (cleaned.startsWith(".") || cleaned.startsWith("_") || cleaned.startsWith("-")) {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.isBlank()) {
            return "file";
        }

        if (cleaned.length() > MAX_FILE_NAME_LENGTH) {
            int dotIdx = cleaned.lastIndexOf('.');
            if (dotIdx > 0 && cleaned.length() - dotIdx <= 16) {
                String ext = cleaned.substring(dotIdx);
                String base = cleaned.substring(0, MAX_FILE_NAME_LENGTH - ext.length());
                return base + ext;
            }
            return cleaned.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return cleaned;
    }
}
