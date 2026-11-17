package vn.edu.davs.backend.dto;

/**
 * Request payload for toggling Row-Level Security (RLS) on a data source (T-101).
 */
public record ToggleDatasourceRlsRequest(boolean enabled) {
}
