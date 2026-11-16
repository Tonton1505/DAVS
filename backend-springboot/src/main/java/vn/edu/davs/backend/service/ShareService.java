package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.DirectoryItemDto;
import vn.edu.davs.backend.dto.ShareRequest;
import vn.edu.davs.backend.dto.ShareResponse;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.EmailTemplate;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing UC22: Content Sharing (FR-SHR-02, BUS-SHR-05..07).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShareService {

    public static final int MAX_RECIPIENTS = 100;
    private static final String COMPANY_DOMAIN = "congty.vn";

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * Shares workbook, dashboard, or view with users, groups, or generates a link (URD-15).
     */
    @Transactional
    public ShareResponse shareContent(ShareRequest request, DavsUser actor) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        // 1. BUS-SHR-07: Maximum 100 recipients limit
        List<String> recipients = request.recipients() != null ? request.recipients() : List.of();
        if (recipients.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("Số lượng người nhận tối đa là " + MAX_RECIPIENTS);
        }

        String resType = request.resourceType() != null ? request.resourceType().trim().toUpperCase() : "WORKBOOK";
        UUID resId = request.resourceId();
        if (resId == null) {
            throw new IllegalArgumentException("resourceId must not be null");
        }

        // 2. BUS-SHR-06: Actor can only grant up to their own permission level
        String permLevel = request.permissionLevel() != null ? request.permissionLevel().trim().toUpperCase() : "VIEW";
        assertPermissionCeiling(actor, permLevel);

        String shareLink = buildShareLink(resType, resId);
        String embedCode = "<iframe src=\"" + shareLink + "?embed=true\" width=\"100%\" height=\"600\" frameborder=\"0\"></iframe>";

        List<String> validRecipients = new ArrayList<>();
        int sharedCount = 0;

        for (String rawRecipient : recipients) {
            String recipient = rawRecipient.trim();
            if (recipient.isBlank()) {
                continue;
            }

            if ("ANYONE_WITH_LINK".equalsIgnoreCase(recipient)) {
                // Public link sharing (BUS-SHR-05)
                recordShare(resType, resId, actor.getId(), "PUBLIC_LINK", null, "ANYONE_WITH_LINK",
                        permLevel, request.message(), false, shareLink);
                validRecipients.add("ANYONE_WITH_LINK");
                sharedCount++;
                continue;
            }

            // Look up User or Group
            ResolvedRecipient resolved = resolveRecipient(recipient);
            if (resolved == null) {
                // ERR-SHR-01: Recipient not found
                throw new DavsException(ErrorCode.ERR_SHR_01, recipient);
            }

            // ERR-SHR-02: Forbid sharing to external domain if enterprise policy restricts
            if (resolved.isExternal()) {
                throw new DavsException(ErrorCode.ERR_SHR_02);
            }

            // Record share
            recordShare(resType, resId, actor.getId(), resolved.type(), resolved.id(),
                    resolved.identifier(), permLevel, request.message(), request.sendEmailNotification(), shareLink);

            // Grant permission rule if user
            if ("USER".equalsIgnoreCase(resolved.type()) && resolved.id() != null) {
                grantPermissionRule(resType, resId, resolved.id(), permLevel);
            }

            // Send notification email
            if (request.sendEmailNotification() && resolved.email() != null && !resolved.email().isBlank()) {
                try {
                    String senderName = actor.getFullName() != null ? actor.getFullName() : actor.getEmail();
                    String resourceName = getResourceName(resType, resId);
                    String message = request.message() != null ? request.message() : "";
                    notificationService.sendAsync(
                            EmailTemplate.SHARE,
                            resolved.email(),
                            Map.of(
                                    "sharedBy", senderName,
                                    "resourceType", resType,
                                    "resourceName", resourceName != null ? resourceName : "Tài nguyên",
                                    "permissionLevel", permLevel,
                                    "shareLink", shareLink,
                                    "message", message
                            )
                    );
                } catch (Exception ex) {
                    log.warn("Failed to enqueue share notification to {}: {}", resolved.email(), ex.getMessage());
                }
            }

            validRecipients.add(resolved.identifier());
            sharedCount++;
        }

        // Audit Log
        auditService.log(AuditEvent.of(
                "CONTENT_SHARED",
                actor.getId(),
                actor.getEmail(),
                resType,
                resId.toString()
        ));

        return new ShareResponse(
                true,
                sharedCount,
                shareLink,
                embedCode,
                validRecipients
        );
    }

    /**
     * Gets share link and embed code for a resource (SCR-22 Liên kết & Nhúng).
     */
    public Map<String, Object> getShareLink(String resourceType, UUID resourceId, DavsUser actor) {
        String resType = resourceType != null ? resourceType.trim().toUpperCase() : "WORKBOOK";
        String link = buildShareLink(resType, resourceId);
        String embed = "<iframe src=\"" + link + "?embed=true\" width=\"100%\" height=\"600\" frameborder=\"0\"></iframe>";

        return Map.of(
                "resourceType", resType,
                "resourceId", resourceId,
                "shareLink", link,
                "embedCode", embed
        );
    }

    /**
     * Searches users and groups for directory autocomplete in the sharing modal.
     */
    public List<DirectoryItemDto> searchDirectory(String query, DavsUser actor) {
        String filter = query != null ? "%" + query.trim().toLowerCase() + "%" : "%";

        List<DirectoryItemDto> results = new ArrayList<>();

        // 1. Search Users
        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id, email, full_name
                FROM users
                WHERE deleted_at IS NULL
                  AND (lower(email) LIKE ? OR lower(full_name) LIKE ?)
                ORDER BY full_name ASC
                LIMIT 20
                """, filter, filter);

        for (Map<String, Object> u : users) {
            String email = (String) u.get("email");
            String name = (String) u.get("full_name");
            boolean external = isExternalEmail(email);
            results.add(new DirectoryItemDto(
                    (UUID) u.get("id"),
                    "USER",
                    name != null ? name : email,
                    email,
                    null,
                    external
            ));
        }

        // 2. Search Groups
        List<Map<String, Object>> groups = jdbcTemplate.queryForList("""
                SELECT id, name, description
                FROM groups
                WHERE lower(name) LIKE ?
                ORDER BY name ASC
                LIMIT 10
                """, filter);

        for (Map<String, Object> g : groups) {
            results.add(new DirectoryItemDto(
                    (UUID) g.get("id"),
                    "GROUP",
                    (String) g.get("name"),
                    null,
                    null,
                    false
            ));
        }

        return results;
    }

    private void assertPermissionCeiling(DavsUser actor, String requestedLevel) {
        if (actor.getSiteRole() == SiteRole.SITE_ADMIN) {
            return;
        }
        // If actor is VIEWER, they can only grant VIEW (BUS-SHR-06)
        if (actor.getSiteRole() == SiteRole.VIEWER) {
            if ("INTERACT".equalsIgnoreCase(requestedLevel) || "EDIT".equalsIgnoreCase(requestedLevel)) {
                throw new DavsException(ErrorCode.ERR_PRJ_03);
            }
        }
        // If actor is EXPLORER, they cannot grant EDIT (only CREATOR/ADMIN can grant EDIT)
        if (actor.getSiteRole() == SiteRole.EXPLORER && "EDIT".equalsIgnoreCase(requestedLevel)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }

    private ResolvedRecipient resolveRecipient(String identifier) {
        // Try user by email or full_name
        try {
            Map<String, Object> u = jdbcTemplate.queryForMap("""
                    SELECT id, email, full_name
                    FROM users
                    WHERE deleted_at IS NULL
                      AND (lower(email) = lower(?) OR lower(full_name) = lower(?))
                    LIMIT 1
                    """, identifier, identifier);
            if (u != null && u.get("id") != null) {
                String email = (String) u.get("email");
                return new ResolvedRecipient(
                        (UUID) u.get("id"),
                        "USER",
                        email != null ? email : identifier,
                        email,
                        isExternalEmail(email)
                );
            }
        } catch (Exception ignored) {
        }

        // Try group by name
        try {
            Map<String, Object> g = jdbcTemplate.queryForMap("""
                    SELECT id, name
                    FROM groups
                    WHERE lower(name) = lower(?)
                    LIMIT 1
                    """, identifier);
            if (g != null && g.get("id") != null) {
                return new ResolvedRecipient(
                        (UUID) g.get("id"),
                        "GROUP",
                        (String) g.get("name"),
                        null,
                        false
                );
            }
        } catch (Exception ignored) {
        }

        // If it looks like an email but not found in DB
        if (identifier.contains("@")) {
            boolean external = isExternalEmail(identifier);
            if (external) {
                // If it's external, return with isExternal=true so ERR-SHR-02 triggers properly
                return new ResolvedRecipient(null, "USER", identifier, identifier, true);
            }
        }

        return null;
    }

    private boolean isExternalEmail(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        // Disallow external consumer domains and non-internal domains
        return domain.contains("gmail.com")
                || domain.contains("yahoo.com")
                || domain.contains("hotmail.com")
                || domain.contains("external.com")
                || (!domain.endsWith(COMPANY_DOMAIN) && !domain.endsWith(".edu.vn") && !domain.equals("localhost"));
    }

    private void recordShare(
            String resourceType,
            UUID resourceId,
            UUID sharedBy,
            String recipientType,
            UUID recipientId,
            String recipientIdentifier,
            String permissionLevel,
            String message,
            boolean sendEmail,
            String shareLink
    ) {
        jdbcTemplate.update("""
                INSERT INTO content_shares (
                    id, resource_type, resource_id, shared_by, recipient_type,
                    recipient_id, recipient_identifier, permission_level, message,
                    send_email_notification, share_link, created_at
                ) VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                """,
                resourceType, resourceId, sharedBy, recipientType,
                recipientId, recipientIdentifier, permissionLevel, message,
                sendEmail, shareLink
        );
    }

    private void grantPermissionRule(String resourceType, UUID resourceId, UUID userId, String permLevel) {
        try {
            ResourceType rType = ResourceType.valueOf(resourceType);
            Capability cap = switch (permLevel.toUpperCase()) {
                case "EDIT" -> Capability.WEB_EDIT;
                case "INTERACT" -> Capability.INTERACT;
                default -> Capability.VIEW;
            };

            jdbcTemplate.update("""
                    INSERT INTO permission_rules (
                        id, resource_type, resource_id, principal_type, principal_id,
                        capability, effect, created_at
                    ) VALUES (gen_random_uuid(), ?, ?, 'USER', ?, ?, 'ALLOW', now())
                    ON CONFLICT DO NOTHING
                    """,
                    rType.name(), resourceId, userId, cap.name()
            );
        } catch (Exception ex) {
            log.debug("Grant rule ignored or unsupported resource type: {}", ex.getMessage());
        }
    }

    private String getResourceName(String resourceType, UUID resourceId) {
        try {
            if ("WORKBOOK".equalsIgnoreCase(resourceType)) {
                return jdbcTemplate.queryForObject(
                        "SELECT name FROM workbooks WHERE id = ?", String.class, resourceId);
            } else if ("DASHBOARD".equalsIgnoreCase(resourceType)) {
                return jdbcTemplate.queryForObject(
                        "SELECT title FROM dashboards WHERE id = ?", String.class, resourceId);
            }
        } catch (Exception ignored) {
        }
        return resourceType.toLowerCase() + "-" + resourceId.toString().substring(0, 8);
    }

    private String buildShareLink(String resourceType, UUID resourceId) {
        String base = "https://davs." + COMPANY_DOMAIN;
        return switch (resourceType.toUpperCase()) {
            case "DASHBOARD" -> base + "/dashboards/" + resourceId;
            case "VIEW" -> base + "/views/" + resourceId;
            default -> base + "/workbooks/" + resourceId;
        };
    }

    private record ResolvedRecipient(
            UUID id,
            String type,
            String identifier,
            String email,
            boolean isExternal
    ) {
    }
}
