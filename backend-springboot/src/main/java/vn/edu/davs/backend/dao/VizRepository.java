package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.DashboardActionDto;
import vn.edu.davs.backend.dto.DashboardDto;
import vn.edu.davs.backend.dto.ParameterDto;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import vn.edu.davs.backend.dto.SaveDashboardRequest;
import vn.edu.davs.backend.dto.SaveParameterRequest;
import vn.edu.davs.backend.dto.SaveSheetRequest;
import vn.edu.davs.backend.dto.SheetDto;
import vn.edu.davs.backend.dto.TooltipConfigDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Sheets, Calculated Fields, Parameters, and Dashboards.
 */
public interface VizRepository {

    // Sheets
    List<SheetDto> listSheets(UUID workbookId);
    Optional<SheetDto> findSheetById(UUID id);
    boolean existsSheetName(UUID workbookId, String name, UUID excludeId);
    SheetDto createSheet(UUID workbookId, SaveSheetRequest request);
    SheetDto updateSheet(UUID id, SaveSheetRequest request);
    boolean deleteSheet(UUID id);
    boolean updateSheetTooltip(UUID sheetId, TooltipConfigDto tooltipConfig);

    // Calculated Fields
    List<CalculatedFieldDto> listCalculatedFields(UUID datasourceId);
    Optional<CalculatedFieldDto> findCalculatedFieldById(UUID id);
    boolean existsCalculatedFieldName(UUID datasourceId, String name, UUID excludeId);
    CalculatedFieldDto createCalculatedField(UUID datasourceId, SaveCalculatedFieldRequest request);
    CalculatedFieldDto updateCalculatedField(UUID id, SaveCalculatedFieldRequest request);
    boolean deleteCalculatedField(UUID id);

    // Parameters
    List<ParameterDto> listParameters(UUID workbookId);
    Optional<ParameterDto> findParameterById(UUID id);
    boolean existsParameterName(UUID workbookId, String name, UUID excludeId);
    ParameterDto createParameter(UUID workbookId, SaveParameterRequest request);
    ParameterDto updateParameter(UUID id, SaveParameterRequest request);
    boolean deleteParameter(UUID id);

    // Dashboards
    List<DashboardDto> listDashboards(UUID workbookId);
    Optional<DashboardDto> findDashboardById(UUID id);
    boolean existsDashboardName(UUID workbookId, String name, UUID excludeId);
    DashboardDto createDashboard(UUID workbookId, SaveDashboardRequest request);
    DashboardDto updateDashboard(UUID id, SaveDashboardRequest request);
    boolean deleteDashboard(UUID id);
    boolean updateDashboardActions(UUID dashboardId, List<DashboardActionDto> actions);
}
