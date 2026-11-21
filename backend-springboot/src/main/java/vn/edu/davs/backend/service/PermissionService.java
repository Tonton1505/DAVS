package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.ResourceMetadataResolver;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.model.ResourceRef;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.enu.SiteRole;
import java.util.Set;

/**
 * Central RBAC + resource-level policy evaluation engine (T-006, context.md section 6).
 *
 * <p>Evaluation order:
 * <ol>
 *   <li>SiteRole capability ceiling (e.g. VIEWER never gets WEB_EDIT per BUS-PRJ-08)</li>
 *   <li>Parent project LOCKED constraints (DIS-PRJ-03 &amp; BUS-PRJ-09)</li>
 *   <li>Resource ownership (unless locked by parent project)</li>
 *   <li>Explicit DENY rules (user + groups) — DENY always beats ALLOW</li>
 *   <li>Explicit ALLOW rules (user + groups) or SITE_ADMIN role</li>
 *   <li>Default deny</li>
 * </ol>
 */
public interface PermissionService {


    /**
     * Allows domain modules or tests to register a {@link ResourceMetadataResolver} dynamically.
     */
    void registerResolver(ResourceMetadataResolver resolver);


    /**
     * Asserts that {@code user} holds {@code capability} on {@code resource}; otherwise throws
     * {@link DavsException} with {@link ErrorCode#ERR_PRJ_03} (HTTP 403).
     */
    void require(DavsUser user, Capability capability, ResourceRef resource);


    /**
     * Asserts that {@code user} has at least {@code minimumRole}; otherwise throws
     * {@link DavsException} with {@link ErrorCode#ERR_PRJ_03} (HTTP 403).
     */
    void requireRole(DavsUser user, SiteRole minimumRole);


    /**
     * Evaluates whether {@code user} has {@code capability} on {@code resource}.
     */
    boolean can(DavsUser user, Capability capability, ResourceRef resource);


    /**
     * Computes the full effective set of {@link Capability} values granted to {@code user} on {@code resource}.
     */
    Set<Capability> effective(DavsUser user, ResourceRef resource);
}
