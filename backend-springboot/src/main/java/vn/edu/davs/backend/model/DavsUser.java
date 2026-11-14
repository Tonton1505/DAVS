package vn.edu.davs.backend.model;

import vn.edu.davs.backend.enu.SiteRole;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authenticated principal representing a DAVS user in Spring Security context.
 */
public class DavsUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final SiteRole siteRole;
    private final String passwordHash;
    private final boolean active;
    private final boolean accountNonLocked;
    private final List<GrantedAuthority> authorities;

    public DavsUser(
            UUID id,
            String email,
            String fullName,
            SiteRole siteRole,
            String passwordHash,
            boolean active,
            boolean accountNonLocked
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.fullName = fullName;
        this.siteRole = Objects.requireNonNull(siteRole, "siteRole must not be null");
        this.passwordHash = passwordHash;
        this.active = active;
        this.accountNonLocked = accountNonLocked;
        this.authorities = List.of(new SimpleGrantedAuthority(siteRole.toAuthority()));
    }

    /**
     * Convenience factory for an active, unlocked principal (e.g. reconstructed from a validated JWT).
     */
    public static DavsUser of(UUID id, String email, String fullName, SiteRole siteRole) {
        return new DavsUser(id, email, fullName, siteRole, null, true, true);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public SiteRole getSiteRole() {
        return siteRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DavsUser that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
