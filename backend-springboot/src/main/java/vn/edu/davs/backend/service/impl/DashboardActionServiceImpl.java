package vn.edu.davs.backend.service.impl;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.DashboardActionDto;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveActionsRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.common.exception.DavsException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.davs.backend.service.DashboardActionService;

@RequiredArgsConstructor
@Service
public class DashboardActionServiceImpl implements DashboardActionService {


    private static final List<String> TRUSTED_DOMAINS = List.of(
            "bao-cao.congty.vn",
            "congty.vn",
            "davs.edu.vn",
            "analytics.company.com"
    );


    private final VizRepository vizRepository;

    @Override
    public List<String> getTrustedDomains() {
        return TRUSTED_DOMAINS;
    }


    @Transactional
    @Override
    public DashboardDto saveActions(UUID workbookId, UUID dashboardId, SaveActionsRequest request) {
        DashboardDto dashboard = vizRepository.findDashboardById(dashboardId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));

        List<DashboardActionDto> actions = request.actions();

        // BUS-VIZ-13: Max 20 actions per dashboard
        if (actions.size() > MAX_ACTIONS_PER_DASHBOARD) {
            throw new DavsException(ErrorCode.ERR_PRJ_02, "Tối đa 20 hành động mỗi dashboard (BUS-VIZ-13)");
        }

        // Validate each action
        for (DashboardActionDto action : actions) {
            // [5a] ERR-VIZ-07: Sheet nguồn trùng sheet đích
            if (action.sourceSheetId() != null && action.targetSheetId() != null
                    && action.sourceSheetId().equals(action.targetSheetId())) {
                throw new DavsException(ErrorCode.ERR_VIZ_07);
            }

            // BUS-VIZ-12: URL action only https and trusted domains
            if ("URL".equalsIgnoreCase(action.type())) {
                String url = action.targetUrl();
                if (url == null || !url.toLowerCase().startsWith("https://")) {
                    throw new DavsException(ErrorCode.ERR_PRJ_08);
                }
                try {
                    // Replace potential URL parameters e.g. {Vung} to parse valid host
                    String sanitizedUrl = url.replaceAll("\\{[^}]+\\}", "placeholder");
                    URI uri = URI.create(sanitizedUrl);
                    String host = uri.getHost();
                    if (host != null) {
                        boolean isTrusted = TRUSTED_DOMAINS.stream()
                                .anyMatch(td -> host.equalsIgnoreCase(td) || host.endsWith("." + td));
                        if (!isTrusted) {
                            throw new DavsException(ErrorCode.ERR_PRJ_08);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new DavsException(ErrorCode.ERR_PRJ_08);
                }
            }
        }

        vizRepository.updateDashboardActions(dashboardId, actions);
        return vizRepository.findDashboardById(dashboardId).orElse(null);
    }
}
