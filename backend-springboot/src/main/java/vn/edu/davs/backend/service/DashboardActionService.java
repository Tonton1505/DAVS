package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveActionsRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Dashboard Actions and Interactivity (UC18).
 */
public interface DashboardActionService {


    public static final int MAX_ACTIONS_PER_DASHBOARD = 20;


    List<String> getTrustedDomains();


    @Transactional
    DashboardDto saveActions(UUID workbookId, UUID dashboardId, SaveActionsRequest request);
}
