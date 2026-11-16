package vn.edu.davs.backend.dto;

public record ExportLimitsDto(
        long maxRows,
        long maxBytes,
        int timeoutSeconds
) {
    public static ExportLimitsDto standard() {
        return new ExportLimitsDto(500_000L, 100L * 1024 * 1024, 30);
    }
}
