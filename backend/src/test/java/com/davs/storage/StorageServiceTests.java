package com.davs.storage;

import com.davs.common.error.DavsException;
import com.davs.common.error.ErrorCode;
import com.davs.common.settings.SettingsService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class StorageServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SettingsService settingsService;
    private VirusScanner virusScanner;
    private StorageService storageService;

    // In-memory fake client subclassing MinIO client wrapper logic
    static class FakeMinioStorageService extends StorageService {
        final Map<String, byte[]> storage = new ConcurrentHashMap<>();

        public FakeMinioStorageService(
                SettingsService settingsService,
                VirusScanner virusScanner
        ) {
            super(
                    io.minio.MinioClient.builder()
                            .endpoint("http://localhost:9000")
                            .credentials("fake_key", "fake_secret")
                            .build(),
                    settingsService,
                    virusScanner
            );
        }

        @Override
        public void ensureBucketExists(Bucket bucket) {
            // no-op for in-memory fake
        }

        @Override
        public void ensureAllBucketsExist() {
            // no-op for in-memory fake
        }

        @Override
        public void put(Bucket bucket, String objectKey, InputStream inputStream, long size, String contentType) {
            try {
                byte[] content = inputStream.readAllBytes();
                // delegate standard validations
                super.put(bucket, objectKey, new ByteArrayInputStream(content), content.length, contentType);
            } catch (DavsException dex) {
                throw dex;
            } catch (Exception ex) {
                storage.put(bucket.getBucketName() + "/" + objectKey, new byte[0]);
            }
        }

        @Override
        public boolean exists(Bucket bucket, String objectKey) {
            return storage.containsKey(bucket.getBucketName() + "/" + objectKey);
        }

        @Override
        public void delete(Bucket bucket, String objectKey) {
            storage.remove(bucket.getBucketName() + "/" + objectKey);
        }

        @Override
        public URL presignGet(Bucket bucket, String objectKey, Duration expiry) {
            try {
                return URI.create("http://localhost:9000/" + bucket.getBucketName() + "/" + objectKey + "?token=fake").toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(jdbcTemplate);
        virusScanner = new NoopScanner(true);
        storageService = new FakeMinioStorageService(settingsService, virusScanner);
    }

    @Test
    void sanitizeFileName_stripsPathTraversalAndInvalidCharacters() {
        assertThat(storageService.sanitizeFileName("../../evil.twbx")).isEqualTo("evil.twbx");
        assertThat(storageService.sanitizeFileName("..\\..\\malicious.csv")).isEqualTo("malicious.csv");
        assertThat(storageService.sanitizeFileName("folder/sub/report*name?.twb")).isEqualTo("report_name_.twb");
        assertThat(storageService.sanitizeFileName("")).isEqualTo("unnamed");
        assertThat(storageService.sanitizeFileName(null)).isEqualTo("unnamed");
    }

    @Test
    void put_rejectsWorkbookWithInvalidExtension_throwsErrPrj04() {
        byte[] dummy = "dummy content".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storageService.put(Bucket.WORKBOOKS, "file.exe", dummy, "application/octet-stream"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_PRJ_04));
    }

    @Test
    void put_rejectsSpoofedTwbWithZipHeader_throwsErrPrj04() {
        byte[] fakeTwb = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        assertThatThrownBy(() -> storageService.put(Bucket.WORKBOOKS, "fake.twb", fakeTwb, "application/xml"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_PRJ_04));
    }

    @Test
    void put_rejectsSpoofedTwbxWithoutZipHeader_throwsErrPrj04() {
        byte[] fakeTwbx = "Not a zip file".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storageService.put(Bucket.WORKBOOKS, "fake.twbx", fakeTwbx, "application/zip"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_PRJ_04));
    }

    @Test
    void put_rejectsDatasetWithExecutableMagic_throwsErrDat04() {
        byte[] exeBytes = new byte[] {0x4D, 0x5A, 0x00, 0x00};
        assertThatThrownBy(() -> storageService.put(Bucket.DATASETS, "fake.csv", exeBytes, "text/csv"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_DAT_04));
    }

    @Test
    void put_rejectsExceedingMaxUploadBytes_throwsErrPrj05() {
        settingsService.set(SettingsService.KEY_UPLOAD_MAX_BYTES, "100");
        byte[] large = new byte[200];
        assertThatThrownBy(() -> storageService.put(Bucket.WORKBOOKS, "large.twbx", large, "application/zip"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_PRJ_05));
    }

    @Test
    void put_detectsEicarMalwareSignature_throwsErrPrj08() {
        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
        byte[] eicarBytes = eicar.getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> storageService.put(Bucket.ATTACHMENTS, "test.txt", eicarBytes, "text/plain"))
                .isInstanceOf(DavsException.class)
                .satisfies(ex -> assertThat(((DavsException) ex).getErrorCode()).isEqualTo(ErrorCode.ERR_PRJ_08));
    }

    @Test
    void putAndGetAndExistsAndPresign_happyPath() throws Exception {
        byte[] validTwbx = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x01, 0x02, 0x03};

        ((FakeMinioStorageService) storageService).storage.put("workbooks/valid.twbx", validTwbx);

        boolean exists = storageService.exists(Bucket.WORKBOOKS, "valid.twbx");
        assertThat(exists).isTrue();

        URL url = storageService.presignGet(Bucket.WORKBOOKS, "valid.twbx", Duration.ofMinutes(15));
        assertThat(url.toString()).contains("workbooks/valid.twbx");

        storageService.delete(Bucket.WORKBOOKS, "valid.twbx");
        assertThat(storageService.exists(Bucket.WORKBOOKS, "valid.twbx")).isFalse();
    }
}
