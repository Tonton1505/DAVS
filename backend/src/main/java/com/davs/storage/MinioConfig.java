package com.davs.storage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the MinIO client bean using properties from {@code application.yml}.
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${davs.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${davs.minio.access-key:davs_minio_admin}") String accessKey,
            @Value("${davs.minio.secret-key:davs_minio_secret_change_me}") String secretKey
    ) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
