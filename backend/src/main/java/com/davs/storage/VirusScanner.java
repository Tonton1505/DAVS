package com.davs.storage;

/**
 * Abstraction for scanning uploaded files for malware before storing in MinIO (ADR D8, NFR-SEC-09).
 */
public interface VirusScanner {

    /**
     * Scans the file header/content and returns whether the file is safe.
     *
     * @param fileName      sanitized file name
     * @param headerContent initial bytes of the file
     * @param totalSize     total file size in bytes
     * @return scan result indicating whether the file passed inspection
     */
    ScanResult scan(String fileName, byte[] headerContent, long totalSize);

    record ScanResult(boolean clean, String reason) {

        public static ScanResult ok() {
            return new ScanResult(true, "OK");
        }

        public static ScanResult rejected(String reason) {
            return new ScanResult(false, reason);
        }
    }
}
