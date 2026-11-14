package vn.edu.davs.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mail sender configuration properties ({@code davs.mail}).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "davs.mail")
public class MailProperties {

    private String from = "noreply@davs.local";
}
