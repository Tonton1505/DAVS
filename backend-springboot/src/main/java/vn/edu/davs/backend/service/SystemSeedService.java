package vn.edu.davs.backend.service;

import vn.edu.davs.backend.config.SeedProperties;

import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent bootstrap seeder creating the first {@code SITE_ADMIN} account and linking
 * default workspace entities without ever overwriting existing data (T-011).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSeedService {

    private static final String ALL_USERS_GROUP = "All Users";
    private static final String DEFAULT_PROJECT = "Default";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    /**
     * Seeds the initial {@code SITE_ADMIN} user if no active {@code SITE_ADMIN} exists yet.
     * Re-running this method never creates duplicates or overwrites existing records.
     *
     * @return {@code true} if a new initial admin was created, {@code false} if skipped
     */
    @Transactional
    public boolean seedIfNeeded() {
        if (!seedProperties.isEnabled()) {
            log.debug("System seeding is disabled via davs.seed.enabled=false");
            return false;
        }

        Integer existingAdmins = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE site_role = 'SITE_ADMIN' AND deleted_at IS NULL",
                Integer.class);
        if (existingAdmins != null && existingAdmins > 0) {
            log.debug("Initial SITE_ADMIN already exists; skipping bootstrap seed");
            return false;
        }

        String rawEmail = seedProperties.getAdminEmail();
        String rawPassword = seedProperties.getAdminInitialPassword();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new IllegalStateException("ADMIN_EMAIL / davs.seed.admin-email must not be blank");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalStateException("ADMIN_INITIAL_PASSWORD / davs.seed.admin-initial-password must not be blank");
        }

        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        String passwordHash = passwordEncoder.encode(rawPassword);

        UUID adminId = jdbcTemplate.queryForObject("""
                INSERT INTO users (
                    email, password_hash, full_name, auth_method,
                    site_role, status, must_change_password
                )
                VALUES (?, ?, 'Site Administrator', 'LOCAL', 'SITE_ADMIN', 'ACTIVE', TRUE)
                RETURNING id
                """, UUID.class, normalizedEmail, passwordHash);

        if (adminId == null) {
            throw new IllegalStateException("Failed to insert initial SITE_ADMIN user");
        }

        jdbcTemplate.update("""
                INSERT INTO group_members (group_id, user_id)
                SELECT g.id, ?
                FROM groups g
                WHERE lower(g.name) = lower(?)
                ON CONFLICT (group_id, user_id) DO NOTHING
                """, adminId, ALL_USERS_GROUP);

        jdbcTemplate.update("""
                UPDATE projects
                SET owner_id = ?, updated_at = now()
                WHERE parent_id IS NULL
                  AND deleted_at IS NULL
                  AND lower(name) = lower(?)
                  AND owner_id IS NULL
                """, adminId, DEFAULT_PROJECT);

        jdbcTemplate.update("""
                UPDATE license_pools
                SET used = LEAST(total, used + 1), updated_at = now()
                WHERE role = 'SITE_ADMIN'
                """);

        // Never log initial password or hash.
        log.info("Seeded initial SITE_ADMIN account for email={} (must_change_password=true)", normalizedEmail);
        return true;
    }
}
