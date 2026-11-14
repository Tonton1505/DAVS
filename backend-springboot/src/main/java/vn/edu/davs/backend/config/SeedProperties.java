package vn.edu.davs.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for initial bootstrap seeding ({@code davs.seed}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "davs.seed")
public class SeedProperties {

    private boolean enabled = true;
    private String adminEmail = "admin@davs.local";
    private String adminInitialPassword = "Admin@123456!ChangeMe";
}
