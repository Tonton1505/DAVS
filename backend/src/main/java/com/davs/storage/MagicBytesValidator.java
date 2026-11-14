package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates file header magic bytes against the declared file extension to reject
 * spoofed or executable payloads (ERR-PRJ-08).
 */
@Component
public class MagicBytesValidator {

    private static final Set<String> TEXT_EXTENSIONS = Set.of("csv", "tsv", "txt", "json", "xml", "twb");
    private static final Set<String> ZIP_BASED_EXTENSIONS = Set.of("zip", "xlsx", "twbx", "davs");

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF_MAGIC = {'G', 'I', 'F', '8'};
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] ZIP_MAGIC_1 = {'P', 'K', 0x03, 0x04};
    private static final byte[] ZIP_MAGIC_2 = {'P', 'K', 0x05, 0x06};
    private static final byte[] OLE2_MAGIC = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] PARQUET_MAGIC = {'P', 'A', 'R', '1'};
    private static final byte[] EXE_MZ_MAGIC = {'M', 'Z'};
    private static final byte[] ELF_MAGIC = {0x7F, 'E', 'L', 'F'};
    private static final byte[] CLASS_OR_MACHO_MAGIC = {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

    /**
     * Validates that {@code headerBytes} matches the declared extension of {@code fileName}.
     *
     * @throws DavsException with {@link ErrorCode#ERR_PRJ_08} if the file is empty, unsupported, or spoofed
     */
    public void validate(String fileName, byte[] headerBytes) {
        if (headerBytes == null || headerBytes.length == 0) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }

        String ext = extractExtension(fileName);
        if (ext.isEmpty()) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }

        if (ZIP_BASED_EXTENSIONS.contains(ext)) {
            if (!startsWith(headerBytes, ZIP_MAGIC_1) && !startsWith(headerBytes, ZIP_MAGIC_2)) {
                throw new DavsException(ErrorCode.ERR_PRJ_08);
            }
            return;
        }

        switch (ext) {
            case "png" -> requireMagic(headerBytes, PNG_MAGIC);
            case "jpg", "jpeg" -> requireMagic(headerBytes, JPEG_MAGIC);
            case "gif" -> requireMagic(headerBytes, GIF_MAGIC);
            case "pdf" -> requireMagic(headerBytes, PDF_MAGIC);
            case "xls" -> requireMagic(headerBytes, OLE2_MAGIC);
            case "parquet" -> requireMagic(headerBytes, PARQUET_MAGIC);
            default -> {
                if (TEXT_EXTENSIONS.contains(ext)) {
                    validateTextPayload(ext, headerBytes);
                } else {
                    throw new DavsException(ErrorCode.ERR_PRJ_08);
                }
            }
        }
    }

    private static void validateTextPayload(String ext, byte[] headerBytes) {
        if (startsWith(headerBytes, EXE_MZ_MAGIC)
                || startsWith(headerBytes, ELF_MAGIC)
                || startsWith(headerBytes, CLASS_OR_MACHO_MAGIC)
                || startsWith(headerBytes, ZIP_MAGIC_1)
                || startsWith(headerBytes, PDF_MAGIC)
                || startsWith(headerBytes, PNG_MAGIC)
                || startsWith(headerBytes, JPEG_MAGIC)
                || startsWith(headerBytes, GIF_MAGIC)
                || startsWith(headerBytes, OLE2_MAGIC)) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }

        for (byte b : headerBytes) {
            if (b == 0x00) {
                throw new DavsException(ErrorCode.ERR_PRJ_08);
            }
        }

        String text = new String(headerBytes, StandardCharsets.UTF_8).stripLeading();
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1).stripLeading();
        }
        if ("json".equals(ext) && (text.isEmpty() || (text.charAt(0) != '{' && text.charAt(0) != '['))) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }
        if (("xml".equals(ext) || "twb".equals(ext)) && (text.isEmpty() || text.charAt(0) != '<')) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }
    }

    private static void requireMagic(byte[] headerBytes, byte[] magic) {
        if (!startsWith(headerBytes, magic)) {
            throw new DavsException(ErrorCode.ERR_PRJ_08);
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }
}
