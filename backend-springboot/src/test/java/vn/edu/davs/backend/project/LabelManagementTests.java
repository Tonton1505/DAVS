package vn.edu.davs.backend.project;

import vn.edu.davs.backend.dao.JdbcLabelRepository;
import vn.edu.davs.backend.dto.CreateLabelRequest;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.LabelTargetRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.service.AuditService;
import vn.edu.davs.backend.service.LabelService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UC09 Data Labels Management.
 */
class LabelManagementTests {

    private JdbcLabelRepository labelRepository;
    private AuditService auditService;
    private LabelService labelService;

    private UUID categoryId;
    private UUID creatorId;
    private DavsUser creator;
    private DavsUser viewer;
    private LabelCategoryDto warningCategory;

    @BeforeEach
    void setUp() {
        labelRepository = mock(JdbcLabelRepository.class);
        auditService = mock(AuditService.class);
        labelService = new LabelService(labelRepository, auditService);

        categoryId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        creator = DavsUser.of(creatorId, "creator@davs.local", "Creator", SiteRole.CREATOR);
        viewer = DavsUser.of(UUID.randomUUID(), "viewer@davs.local", "Viewer", SiteRole.VIEWER);

        warningCategory = new LabelCategoryDto(
                categoryId,
                "Cảnh báo chất lượng",
                "Cảnh báo hiển thị nổi bật",
                true,
                creatorId,
                Instant.now()
        );
        when(labelRepository.findCategoryById(categoryId)).thenReturn(Optional.of(warningCategory));
    }

    @Test
    void uc09CreateLabelValidatesBoundariesAndDuplicateCaseInsensitive() {
        // Viewer cannot create label -> ERR-PRJ-03
        assertThatThrownBy(() -> labelService.createLabel(
                new CreateLabelRequest(categoryId, "Dữ liệu trễ", "Mô tả", true),
                viewer
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_03);

        // VAL-PRJ-19: 51 chars -> ERR-PRJ-02
        String value51 = "L".repeat(51);
        assertThatThrownBy(() -> labelService.createLabel(
                new CreateLabelRequest(categoryId, value51, "Mô tả", true),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_02);

        // [5a] BUS-PRJ-13: Duplicate (category_id, lower(value)) -> 409 ERR-PRJ-07
        when(labelRepository.existsLabelInCategory(categoryId, "Nội bộ", null)).thenReturn(true);
        assertThatThrownBy(() -> labelService.createLabel(
                new CreateLabelRequest(categoryId, "Nội bộ", "Mô tả", false),
                creator
        ))
                .isInstanceOf(DavsException.class)
                .extracting(ex -> ((DavsException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ERR_PRJ_07);

        // Valid label creation inherits warning flag from category
        UUID newLabelId = UUID.randomUUID();
        when(labelRepository.existsLabelInCategory(categoryId, "Đang bảo trì", null)).thenReturn(false);
        when(labelRepository.insertLabel(categoryId, "Đang bảo trì", "Cảnh báo ETL", true, creatorId))
                .thenReturn(newLabelId);
        LabelDto createdDto = new LabelDto(
                newLabelId,
                categoryId,
                "Cảnh báo chất lượng",
                "Đang bảo trì",
                "Cảnh báo ETL",
                true,
                0,
                creatorId,
                Instant.now(),
                Instant.now()
        );
        when(labelRepository.findLabelById(newLabelId)).thenReturn(Optional.of(createdDto));

        LabelDto result = labelService.createLabel(
                new CreateLabelRequest(categoryId, "Đang bảo trì", "Cảnh báo ETL", null),
                creator
        );
        assertThat(result.value()).isEqualTo("Đang bảo trì");
        assertThat(result.isWarning()).isTrue();
        verify(auditService).log(any());
    }

    @Test
    void uc09AssignUnassignAndCascadeDeleteLabel() {
        UUID labelId = UUID.randomUUID();
        UUID targetWorkbookId = UUID.randomUUID();
        LabelDto existing = new LabelDto(
                labelId,
                categoryId,
                "Cảnh báo chất lượng",
                "Dữ liệu thử nghiệm",
                "Chưa nghiệm thu",
                true,
                2,
                creatorId,
                Instant.now(),
                Instant.now()
        );
        when(labelRepository.findLabelById(labelId)).thenReturn(Optional.of(existing));
        when(labelRepository.assignLabel(labelId, "WORKBOOK", targetWorkbookId, creatorId)).thenReturn(1);

        // [6a] Assign label to target
        labelService.assignLabel(
                labelId,
                new LabelTargetRequest(List.of(new LabelTargetRequest.TargetRef("WORKBOOK", targetWorkbookId))),
                creator
        );
        verify(labelRepository).assignLabel(labelId, "WORKBOOK", targetWorkbookId, creatorId);

        // [6a] Unassign label from target
        labelService.unassignLabel(
                labelId,
                new LabelTargetRequest(List.of(new LabelTargetRequest.TargetRef("WORKBOOK", targetWorkbookId))),
                creator
        );
        verify(labelRepository).unassignLabel(labelId, "WORKBOOK", targetWorkbookId);

        // [6b] BUS-PRJ-15: Delete label unassigns all targets and returns unassignedCount
        when(labelRepository.countAssignments(labelId)).thenReturn(2);
        Map<String, Object> deletedRes = labelService.deleteLabel(labelId, creator);
        assertThat(deletedRes.get("unassignedCount")).isEqualTo(2);
        verify(labelRepository).deleteLabel(labelId);
    }
}
