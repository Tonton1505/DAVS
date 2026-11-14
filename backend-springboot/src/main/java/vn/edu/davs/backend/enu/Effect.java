package vn.edu.davs.backend.enu;

/**
 * Policy effect for a {@code permission_rules} entry. {@link #DENY} always overrides {@link #ALLOW}.
 */
public enum Effect {
    ALLOW,
    DENY
}
