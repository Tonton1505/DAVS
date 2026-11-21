package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.DirectoryItemDto;
import vn.edu.davs.backend.dto.ShareRequest;
import vn.edu.davs.backend.dto.ShareResponse;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementing UC22: Content Sharing (FR-SHR-02, BUS-SHR-05..07).
 */
public interface ShareService {


    public static final int MAX_RECIPIENTS = 100;


    /**
     * Shares workbook, dashboard, or view with users, groups, or generates a link (URD-15).
     */
    @Transactional
    ShareResponse shareContent(ShareRequest request, DavsUser actor);


    /**
     * Gets share link and embed code for a resource (SCR-22 Liên kết & Nhúng).
     */
    Map<String, Object> getShareLink(String resourceType, UUID resourceId, DavsUser actor);


    /**
     * Searches users and groups for directory autocomplete in the sharing modal.
     */
    List<DirectoryItemDto> searchDirectory(String query, DavsUser actor);
}
