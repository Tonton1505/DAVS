package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CreateDashboardRequest;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.DashboardViewDto;
import vn.edu.davs.backend.dto.QueryRequest;
import vn.edu.davs.backend.dto.QueryResultDto;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Dashboards layout, tiles (UC17), viewing & interactions (UC19).
 */
public interface DashboardService {


    public static final int MAX_RECOMMENDED_TILES = 10;


    // =========================================================================
    // UC17: Dashboard Authoring & Layout
    // =========================================================================

    @Transactional(readOnly = true)
    List<DashboardDto> listDashboards(UUID workbookId);


    @Transactional(readOnly = true)
    DashboardDto getDashboard(UUID id);


    @Transactional
    DashboardDto createDashboard(UUID workbookId, SaveDashboardRequest request);


    @Transactional
    DashboardDto updateDashboard(UUID id, SaveDashboardRequest request);


    @Transactional
    void deleteDashboard(UUID id);


    // =========================================================================
    // UC19: Dashboard Viewing, Effective Permissions & Interaction
    // =========================================================================

    @Transactional(readOnly = true)
    DashboardDto getById(UUID id);


    @Transactional(readOnly = true)
    List<DashboardDto> listByWorkbook(UUID workbookId, DavsUser actor);


    @Transactional
    DashboardDto create(CreateDashboardRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    DashboardViewDto getDashboardView(UUID dashboardId, DavsUser actor);


    @Transactional(readOnly = true)
    QueryResultDto executeQuery(QueryRequest request, DavsUser actor);
}
