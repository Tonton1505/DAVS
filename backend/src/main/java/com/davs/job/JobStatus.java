package com.davs.job;

/**
 * Lifecycle status of a background job in the {@code jobs} table.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
