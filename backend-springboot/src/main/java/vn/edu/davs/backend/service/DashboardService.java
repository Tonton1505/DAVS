package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Dashboards layout and tiles (UC17).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    public static final int MAX_RECOMMENDED_TILES = 10;
    private final VizRepository vizRepository;

    @Transactional(readOnly = true)
    public List<DashboardDto> listDashboards(UUID workbookId) {
        return vizRepository.listDashboards(workbookId);
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard(UUID id) {
        return vizRepository.findDashboardById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional
    public DashboardDto createDashboard(UUID workbookId, SaveDashboardRequest request) {
        String name = request.name().trim();
        // Duplicate dashboard name in workbook (ERR-VIZ-05)
        if (vizRepository.existsDashboardName(workbookId, name, null)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        // BUS-VIZ-08: Warning if tiles > 10 (allowed to proceed, but logged/noted)
        return vizRepository.createDashboard(workbookId, request);
    }

    @Transactional
    public DashboardDto updateDashboard(UUID id, SaveDashboardRequest request) {
        DashboardDto existing = getDashboard(id);
        String name = request.name().trim();

        // Duplicate dashboard name in workbook (ERR-VIZ-05)
        if (vizRepository.existsDashboardName(existing.workbookId(), name, id)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        return vizRepository.updateDashboard(id, request);
    }

    @Transactional
    public void deleteDashboard(UUID id) {
        getDashboard(id);
        vizRepository.deleteDashboard(id);
    }
}
