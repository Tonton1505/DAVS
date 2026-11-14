package com.davs.job;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for background job polling and retries ({@code davs.jobs}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "davs.jobs")
public class JobProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 2000L;
    private long retryDelaySeconds = 5L;
    private int maxAttempts = 3;
}
