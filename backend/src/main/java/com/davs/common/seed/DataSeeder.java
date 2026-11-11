package com.davs.common.seed;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminInitialPassword;

    public DataSeeder(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${davs.seed.admin-email:admin@davs.local}") String adminEmail,
            @Value("${davs.seed.admin-initial-password:Admin@123456!ChangeMe}") String adminInitialPassword
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail != null ? adminEmail.trim().toLowerCase() : "admin@davs.local";
        this.adminInitialPassword = adminInitialPassword != null ? adminInitialPassword : "Admin@123456!ChangeMe";
    }

    @Override
    public void run(ApplicationArguments args) {
        seedInitialAdminUser();
    }

    public boolean seedInitialAdminUser() {
        if (jdbcTemplate == null) {
            return false;
        }

        try {
            List<String> existing = jdbcTemplate.queryForList(
                    "SELECT id::text FROM users WHERE lower(email) = ? AND deleted_at IS NULL",
                    String.class,
                    adminEmail
            );

            if (!existing.isEmpty()) {
                log.info("Initial admin user already exists: {}", adminEmail);
                return false;
            }

            UUID adminId = UUID.randomUUID();
            String hashedPwd = passwordEncoder.encode(adminInitialPassword);

            jdbcTemplate.update(
                    "INSERT INTO users (id, email, password_hash, full_name, auth_method, site_role, status, "
                            + "must_change_password, created_at, updated_at) "
                            + "VALUES (?, ?, ?, 'System Administrator', 'LOCAL', 'SITE_ADMIN', 'ACTIVE', TRUE, "
                            + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    adminId, adminEmail, hashedPwd
            );

            // Add initial admin to "All Users" system group
            jdbcTemplate.update(
                    "INSERT INTO group_members (group_id, user_id, added_at) "
                            + "SELECT id, ?, CURRENT_TIMESTAMP FROM groups WHERE is_system = TRUE AND lower(name) = 'all users' "
                            + "ON CONFLICT DO NOTHING",
                    adminId
            );

            // Increment used count in license_pools for SITE_ADMIN
            jdbcTemplate.update(
                    "UPDATE license_pools SET used = used + 1, updated_at = CURRENT_TIMESTAMP WHERE role = 'SITE_ADMIN'"
            );

            log.info("Created initial admin user successfully: {}", adminEmail);
            return true;
        } catch (DataAccessException ex) {
            log.warn("Database not ready or schema not migrated yet: {}", ex.getMessage());
            return false;
        }
    }
}
