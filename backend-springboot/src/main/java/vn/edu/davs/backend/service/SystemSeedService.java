package vn.edu.davs.backend.service;

import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent bootstrap seeder creating the first {@code SITE_ADMIN} account and linking
 * default workspace entities without ever overwriting existing data (T-011).
 */
public interface SystemSeedService {


    /**
     * Seeds the initial {@code SITE_ADMIN} user if no active {@code SITE_ADMIN} exists yet.
     * Re-running this method never creates duplicates or overwrites existing records.
     *
     * @return {@code true} if a new initial admin was created, {@code false} if skipped
     */
    @Transactional
    boolean seedIfNeeded();
}
