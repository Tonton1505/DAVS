package vn.edu.davs.backend.enu;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * System-wide site roles defined in DAVS with strict hierarchy and capability ceilings
 * (SRS section 2.2 / context.md section 6 / BUS-PRJ-08).
 */
public enum SiteRole {
    VIEWER(1, EnumSet.of(
            Capability.VIEW,
            Capability.INTERACT,
            Capability.FILTER,
            Capability.DOWNLOAD,
            Capability.COMMENT
    )),
    EXPLORER(2, EnumSet.of(
            Capability.VIEW,
            Capability.INTERACT,
            Capability.FILTER,
            Capability.DOWNLOAD,
            Capability.COMMENT,
            Capability.WEB_EDIT,
            Capability.SAVE,
            Capability.SHARE,
            Capability.DELETE
    )),
    CREATOR(3, EnumSet.allOf(Capability.class)),
    SITE_ADMIN(4, EnumSet.allOf(Capability.class));

    private final int level;
    private final Set<Capability> maxCapabilities;

    SiteRole(int level, EnumSet<Capability> maxCapabilities) {
        this.level = level;
        this.maxCapabilities = Collections.unmodifiableSet(maxCapabilities);
    }

    public int getLevel() {
        return level;
    }

    /**
     * Returns whether this role is at least as privileged as {@code required}.
     */
    public boolean isAtLeast(SiteRole required) {
        return required != null && this.level >= required.level;
    }

    /**
     * Returns whether this role's capability ceiling permits the given {@link Capability}.
     * For example, {@link #VIEWER} never supports {@link Capability#WEB_EDIT} (BUS-PRJ-08).
     */
    public boolean supports(Capability capability) {
        return capability != null && maxCapabilities.contains(capability);
    }

    public Set<Capability> getMaxCapabilities() {
        return maxCapabilities;
    }

    /**
     * Returns the Spring Security authority string (e.g. {@code ROLE_SITE_ADMIN}).
     *
     * @return prefixed role authority
     */
    public String toAuthority() {
        return "ROLE_" + name();
    }
}
