package com.davs.seed;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "davs.seed.admin-email=RootAdmin@davs.local",
        "davs.seed.admin-initial-password=SuperSecretInitial!999"
})
class SystemSeedIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("davs")
            .withUsername("davs")
            .withPassword("davs_test_pwd");

    @Autowired
    private SystemSeedService systemSeedService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SeedProperties seedProperties;

    @Test
    void seedsSingleAdminDefaultsAreIdempotentAndPasswordNeverLogged(CapturedOutput output) {
        Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL",
                Integer.class);
        assertThat(adminCount).isEqualTo(1);

        var adminRow = jdbcTemplate.queryForMap("""
                SELECT id, email, password_hash, status, must_change_password
                FROM users
                WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL
                """);
        UUID adminId = (UUID) adminRow.get("id");
        assertThat(adminRow.get("email")).isEqualTo("rootadmin@davs.local");
        assertThat(adminRow.get("status")).isEqualTo("ACTIVE");
        assertThat(adminRow.get("must_change_password")).isEqualTo(true);
        assertThat(passwordEncoder.matches("SuperSecretInitial!999", (String) adminRow.get("password_hash"))).isTrue();

        Integer allUsersGroupCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM groups WHERE lower(name) = 'all users' AND is_system = TRUE",
                Integer.class);
        assertThat(allUsersGroupCount).isEqualTo(1);

        Integer adminMembershipCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM group_members gm
                JOIN groups g ON g.id = gm.group_id
                WHERE lower(g.name) = 'all users' AND gm.user_id = ?
                """, Integer.class, adminId);
        assertThat(adminMembershipCount).isEqualTo(1);

        UUID defaultProjectOwner = jdbcTemplate.queryForObject(
                "SELECT owner_id FROM projects WHERE lower(name) = 'default' AND parent_id IS NULL AND deleted_at IS NULL",
                UUID.class);
        assertThat(defaultProjectOwner).isEqualTo(adminId);

        Integer poolCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM license_pools", Integer.class);
        assertThat(poolCount).isEqualTo(4);
        Integer adminPoolUsed = jdbcTemplate.queryForObject(
                "SELECT used FROM license_pools WHERE role = 'SITE_ADMIN'", Integer.class);
        assertThat(adminPoolUsed).isEqualTo(1);

        // Attempt to re-run seed with changed credentials -> must NOT overwrite or duplicate.
        seedProperties.setAdminEmail("otheradmin@davs.local");
        seedProperties.setAdminInitialPassword("DifferentPassword!123");
        boolean seededAgain = systemSeedService.seedIfNeeded();
        assertThat(seededAgain).isFalse();

        Integer adminCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL",
                Integer.class);
        assertThat(adminCountAfter).isEqualTo(1);

        String emailAfter = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, adminId);
        assertThat(emailAfter).isEqualTo("rootadmin@davs.local");

        assertThat(output.getOut()).doesNotContain("SuperSecretInitial!999");
        assertThat(output.getErr()).doesNotContain("SuperSecretInitial!999");
    }
}
