package vn.edu.davs.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@link JobProperties} and Spring scheduling when {@code davs.jobs.enabled=true}.
 */
@Configuration
@EnableConfigurationProperties(JobProperties.class)
public class JobSchedulingConfig {

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "davs.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class SchedulingActivation {
    }
}
