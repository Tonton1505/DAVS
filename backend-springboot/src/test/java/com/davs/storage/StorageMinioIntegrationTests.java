package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.service.SettingKey;
import com.davs.common.service.SettingsService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class StorageMinioIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    @Container
    static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("pgsty/silo:latest").asCompatibleSubstituteFor("minio/minio"))
            .withUserName("davs_test_minio")
            .withPassword("davs_test_minio_secret");

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("davs.minio.endpoint", MINIO::getS3URL);
        registry.add("davs.minio.access-key", MINIO::getUserName);
        registry.add("davs.minio.secret-key", MINIO::getPassword);
    }

    @Autowired
    private StorageService storageService;

    @Autowired
    private SettingsService settingsService;

    @Test
    void T009_putGetExistsPresignDeleteAndSanitizationWorkAgainstMinio() throws Exception {
        byte[] csvBytes = "region,sales\nNorth,12500\n".getBytes(StandardCharsets.UTF_8);

        StoredObject stored = storageService.put(
                Bucket.DATASETS,
                "../../tenant-1",
                "../../x.csv",
                "text/csv",
                new ByteArrayInputStream(csvBytes),
                csvBytes.length
        );

        assertThat(stored.sanitizedFileName()).isEqualTo("x.csv");
        assertThat(stored.objectKey()).doesNotContain("..");
        assertThat(stored.objectKey()).endsWith("-x.csv");
        assertThat(storageService.exists(Bucket.DATASETS, stored.objectKey())).isTrue();

        try (InputStream downloaded = storageService.get(Bucket.DATASETS, stored.objectKey())) {
            assertThat(downloaded.readAllBytes()).isEqualTo(csvBytes);
        }

        String presignedUrl = storageService.presignGet(Bucket.DATASETS, stored.objectKey(), Duration.ofMinutes(5));
        HttpResponse<byte[]> httpResponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(presignedUrl)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()
        );
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).isEqualTo(csvBytes);

        storageService.delete(Bucket.DATASETS, stored.objectKey());
        assertThat(storageService.exists(Bucket.DATASETS, stored.objectKey())).isFalse();
    }

    @Test
    void T009_spoofedExtensionAndOversizedUploadAreRejected() {
        byte[] exeBytes = {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00};
        assertThatThrownBy(() -> storageService.put(
                Bucket.WORKBOOKS,
                "u1",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream(exeBytes),
                exeBytes.length
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_08);

        long originalMaxBytes = settingsService.getLong(SettingKey.UPLOAD_MAX_BYTES);
        try {
            settingsService.updateSettings(Map.of(SettingKey.UPLOAD_MAX_BYTES.getKey(), "10"), null);
            byte[] csvBytes = "col1,col2,col3\n1,2,3\n".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> storageService.put(
                    Bucket.EXPORTS,
                    "u1",
                    "export.csv",
                    "text/csv",
                    new ByteArrayInputStream(csvBytes),
                    csvBytes.length
            ))
                    .isInstanceOf(DavsException.class)
                    .extracting(ex -> ((DavsException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ERR_PRJ_05);
        } finally {
            settingsService.updateSettings(
                    Map.of(SettingKey.UPLOAD_MAX_BYTES.getKey(), String.valueOf(originalMaxBytes)),
                    null
            );
        }
    }
}
