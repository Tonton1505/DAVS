package vn.edu.davs.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties bound to {@code davs.minio.*}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "davs.minio")
public class StorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "davs_minio_admin";
    private String secretKey = "davs_minio_secret_change_me";
    private boolean virusScanEnabled = false;
    private int presignExpiryMinutes = 15;
}
