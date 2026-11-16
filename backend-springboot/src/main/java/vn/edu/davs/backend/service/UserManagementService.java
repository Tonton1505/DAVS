package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcUserCreationRepository;
import vn.edu.davs.backend.dao.JdbcUserManagementRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.PageResponse;
import vn.edu.davs.backend.dto.UserManagementDto;
import vn.edu.davs.backend.enu.EmailTemplate;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC03 business logic: user search/filtering, role upgrades/downgrades,
 * activation/deactivation, invitation resend, and content ownership transfer upon deletion.
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final int INVITATION_TTL_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Logger LOG = LoggerFactory.getLogger(UserManagementService.class);

    private final JdbcUserManagementRepository repository;
    private final JdbcUserCreationRepository creationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserManagementDto> searchUsers(
            String q,
            String role,
            String group,
            String status,
            int page,
            int size
    ) {
        return repository.searchUsers(q, role, group, status, page, size);
    }

    @Transactional(readOnly = true)
    public UserManagementDto getUser(UUID userId) {
        return repository.findUserById(userId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_SHR_01, userId.toString()));
    }

    @Transactional
    public UserManagementDto changeRole(UUID userId, SiteRole newRole, DavsUser actor) {
        UserManagementDto target = getUser(userId);
        SiteRole currentRole = target.siteRole();
        if (currentRole == newRole) {
            return target;
        }

        // BUS-USR-05: Site must retain at least 1 active SITE_ADMIN
        if (currentRole == SiteRole.SITE_ADMIN
                && "ACTIVE".equalsIgnoreCase(target.status())
                && newRole != SiteRole.SITE_ADMIN
                && repository.countActiveSiteAdmins() <= 1) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        // [3b] BUS-USR-04: Cannot downgrade Creator/Admin to Explorer/Viewer if user owns active content
        if (currentRole.isAtLeast(SiteRole.CREATOR)
                && !newRole.isAtLeast(SiteRole.CREATOR)
                && target.ownedResourcesCount() > 0) {
            throw new DavsException(ErrorCode.ERR_USR_06);
        }

        // [3a] BUS-USR-01: Reserve license in new role pool if user is not DISABLED
        boolean consumesLicense = !"DISABLED".equalsIgnoreCase(target.status());
        if (consumesLicense) {
            if (!repository.reserveLicense(newRole, 1)) {
                throw new DavsException(ErrorCode.ERR_USR_04, newRole.name());
            }
            repository.releaseLicense(currentRole, 1);
        }

        repository.updateUserRole(userId, newRole);
        UserManagementDto updated = getUser(userId);

        auditService.log(AuditEvent.of("USER_ROLE_CHANGE", actor.getId(), actor.getEmail(), "USER", userId.toString())
                .withStates(
                        Map.of("siteRole", currentRole.name()),
                        Map.of("siteRole", newRole.name())
                ));
        return updated;
    }

    @Transactional
    public List<UserManagementDto> batchChangeRole(List<UUID> userIds, SiteRole newRole, DavsUser actor) {
        List<UserManagementDto> results = new ArrayList<>();
        for (UUID userId : userIds) {
            results.add(changeRole(userId, newRole, actor));
        }
        return results;
    }

    @Transactional
    public UserManagementDto deactivateUser(UUID userId, DavsUser actor) {
        UserManagementDto target = getUser(userId);
        if ("DISABLED".equalsIgnoreCase(target.status())) {
            return target;
        }

        // BUS-USR-05: Cannot deactivate the last active SITE_ADMIN
        if (target.siteRole() == SiteRole.SITE_ADMIN
                && "ACTIVE".equalsIgnoreCase(target.status())
                && repository.countActiveSiteAdmins() <= 1) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        repository.updateUserStatus(userId, "DISABLED");
        repository.revokeAllRefreshTokens(userId);
        repository.releaseLicense(target.siteRole(), 1);

        UserManagementDto updated = getUser(userId);
        auditService.log(AuditEvent.of("USER_DEACTIVATE", actor.getId(), actor.getEmail(), "USER", userId.toString())
                .withStates(
                        Map.of("status", target.status()),
                        Map.of("status", "DISABLED")
                ));
        return updated;
    }

    @Transactional
    public UserManagementDto activateUser(UUID userId, DavsUser actor) {
        UserManagementDto target = getUser(userId);
        if ("ACTIVE".equalsIgnoreCase(target.status())) {
            return target;
        }

        if ("DISABLED".equalsIgnoreCase(target.status())) {
            if (!repository.reserveLicense(target.siteRole(), 1)) {
                throw new DavsException(ErrorCode.ERR_USR_04, target.siteRole().name());
            }
        }

        repository.updateUserStatus(userId, "ACTIVE");
        UserManagementDto updated = getUser(userId);
        auditService.log(AuditEvent.of("USER_ACTIVATE", actor.getId(), actor.getEmail(), "USER", userId.toString())
                .withStates(
                        Map.of("status", target.status()),
                        Map.of("status", "ACTIVE")
                ));
        return updated;
    }

    @Transactional
    public UserManagementDto resendInvite(UUID userId, DavsUser actor) {
        UserManagementDto target = getUser(userId);
        String rawInviteToken = generateOpaqueToken();
        Instant expiresAt = Instant.now().plus(INVITATION_TTL_DAYS, ChronoUnit.DAYS);
        creationRepository.insertInvitation(userId, sha256Hex(rawInviteToken), expiresAt, actor.getId());

        String nextStatus = target.status();
        try {
            notificationService.send(
                    EmailTemplate.INVITE,
                    target.email(),
                    Map.of(
                            "email", target.email(),
                            "role", target.siteRole().name(),
                            "inviteToken", rawInviteToken,
                            "expiresDays", INVITATION_TTL_DAYS
                    )
            );
            if ("UNINVITED".equalsIgnoreCase(target.status()) || "PENDING".equalsIgnoreCase(target.status())) {
                nextStatus = "PENDING";
                repository.updateUserStatus(userId, "PENDING");
            }
        } catch (Exception ex) {
            LOG.warn("Failed to resend invite email to {}: {}", target.email(), ex.getMessage());
            if ("PENDING".equalsIgnoreCase(target.status()) || "UNINVITED".equalsIgnoreCase(target.status())) {
                nextStatus = "UNINVITED";
                repository.updateUserStatus(userId, "UNINVITED");
            }
        }

        UserManagementDto updated = getUser(userId);
        auditService.log(AuditEvent.of("USER_RESEND_INVITE", actor.getId(), actor.getEmail(), "USER", userId.toString())
                .withStates(
                        Map.of("status", target.status()),
                        Map.of("status", nextStatus)
                ));
        return updated;
    }

    @Transactional
    public void deleteUser(UUID userId, UUID newOwnerId, DavsUser actor) {
        UserManagementDto target = getUser(userId);

        // BUS-USR-05: Cannot delete the last active SITE_ADMIN
        if (target.siteRole() == SiteRole.SITE_ADMIN
                && "ACTIVE".equalsIgnoreCase(target.status())
                && repository.countActiveSiteAdmins() <= 1) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }

        int ownedCount = repository.countOwnedResources(userId);
        if (ownedCount > 0) {
            if (newOwnerId == null || newOwnerId.equals(userId)) {
                throw new DavsException(ErrorCode.ERR_USR_05);
            }
            UserManagementDto newOwner = repository.findUserById(newOwnerId)
                    .orElseThrow(() -> new DavsException(ErrorCode.ERR_USR_05));
            if ("DISABLED".equalsIgnoreCase(newOwner.status())) {
                throw new DavsException(ErrorCode.ERR_USR_05);
            }
            if (!newOwner.siteRole().isAtLeast(SiteRole.CREATOR)) {
                throw new DavsException(ErrorCode.ERR_USR_06);
            }
            repository.transferOwnership(userId, newOwnerId);
        } else if (newOwnerId != null && !newOwnerId.equals(userId)) {
            UserManagementDto newOwner = repository.findUserById(newOwnerId)
                    .orElseThrow(() -> new DavsException(ErrorCode.ERR_USR_05));
            if (!newOwner.siteRole().isAtLeast(SiteRole.CREATOR)) {
                throw new DavsException(ErrorCode.ERR_USR_06);
            }
            repository.transferOwnership(userId, newOwnerId);
        }

        if (!"DISABLED".equalsIgnoreCase(target.status())) {
            repository.releaseLicense(target.siteRole(), 1);
        }
        repository.softDeleteUser(userId);

        auditService.log(AuditEvent.of("USER_DELETE", actor.getId(), actor.getEmail(), "USER", userId.toString())
                .withStates(
                        Map.of(
                                "email", target.email(),
                                "siteRole", target.siteRole().name(),
                                "ownedResourcesCount", ownedCount
                        ),
                        newOwnerId != null ? Map.of("newOwnerId", newOwnerId.toString()) : Map.of("deleted", true)
                ));
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
