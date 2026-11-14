package com.davs.audit;

import com.davs.common.api.PageResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuditLogPostgresIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    @Autowired
    private AuditService auditService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void T007_auditLogsRejectsUpdateAndDeleteAndParticipatesInTransaction() {
        UUID resourceId = UUID.randomUUID();
        auditService.log(AuditEvent.of("TEST_IMMUTABLE", "PROJECT", resourceId.toString())
                .withStates(null, Map.of("password", "secret123", "name", "Demo")));

        String storedPassword = jdbcTemplate.queryForObject(
                "SELECT after_state->>'password' FROM audit_logs WHERE resource_id = ?",
                String.class,
                resourceId.toString()
        );
        assertThat(storedPassword).isEqualTo(SensitiveDataMasker.REDACTED);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE audit_logs SET action = 'TAMPERED' WHERE resource_id = ?",
                resourceId.toString()
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE resource_id = ?",
                resourceId.toString()
        )).isInstanceOf(DataAccessException.class);

        // Verify transactional rollback removes uncommitted audit log
        String rolledBackId = UUID.randomUUID().toString();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> txTemplate.executeWithoutResult(status -> {
            auditService.log(AuditEvent.of("TEST_ROLLBACK", "PROJECT", rolledBackId));
            throw new IllegalStateException("Simulated business failure");
        })).isInstanceOf(IllegalStateException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE resource_id = ?",
                Integer.class,
                rolledBackId
        );
        assertThat(count).isZero();
    }

    @Test
    void T007_paginatedQueryWithOneMillionRowsCompletesWithinTwoSeconds() {
        jdbcTemplate.execute(
                "INSERT INTO audit_logs (action, actor_email, resource_type, resource_id, created_at) "
                        + "SELECT CASE WHEN g % 2 = 0 THEN 'USER_LOGIN' ELSE 'WORKBOOK_VIEW' END, "
                        + "'perf' || (g % 100) || '@davs.local', "
                        + "'WORKBOOK', "
                        + "'wb-' || g, "
                        + "now() - (g || ' seconds')::interval "
                        + "FROM generate_series(1, 1000000) AS g"
        );

        long startNanos = System.nanoTime();
        PageResponse<AuditLogResponse> page = auditService.search(
                new AuditLogFilter(null, "perf42@davs.local", "USER_LOGIN", null, null, null),
                PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        assertThat(page.content()).hasSize(25);
        assertThat(elapsedMs).isLessThanOrEqualTo(2000L);
    }
}
