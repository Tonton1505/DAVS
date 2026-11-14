package vn.edu.davs.backend.config;

import vn.edu.davs.backend.service.NoopVirusScanner;
import vn.edu.davs.backend.service.VirusScanner;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the {@link MinioClient} bean for object storage operations (T-009).
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(VirusScanner.class)
    public VirusScanner noopVirusScanner() {
        return new NoopVirusScanner();
    }
}
