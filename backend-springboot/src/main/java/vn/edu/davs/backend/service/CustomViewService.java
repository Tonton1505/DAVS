package vn.edu.davs.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.dao.CustomViewDao;
import vn.edu.davs.backend.dao.DashboardDao;
import vn.edu.davs.backend.dao.WorkbookDao;
import vn.edu.davs.backend.dao.ProjectDao;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.CreateCustomViewRequest;
import vn.edu.davs.backend.dto.CustomViewDto;
import vn.edu.davs.backend.dto.SaveAsWorkbookRequest;
import vn.edu.davs.backend.dto.WorkbookDto;
import vn.edu.davs.backend.enu.Capability;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.ResourceType;
import vn.edu.davs.backend.model.DavsUser;
import vn.edu.davs.backend.model.ResourceRef;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomViewService {

    private final CustomViewDao customViewRepository;
    private final DashboardDao dashboardRepository;
    private final WorkbookDao workbookRepository;
    private final ProjectDao projectRepository;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CustomViewDto create(CreateCustomViewRequest request, DavsUser actor) {
        dashboardRepository.findById(request.dashboardId())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy dashboard"));

        String filtersJson = writeJson(request.filters());
        String paramsJson = writeJson(request.parameters());
        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        boolean isShared = Boolean.TRUE.equals(request.isShared());

        CustomViewDto created = customViewRepository.create(
                request.dashboardId(),
                request.name(),
                filtersJson,
                paramsJson,
                isDefault,
                isShared,
                actor.getId(),
                actor.getFullName()
        );

        auditService.log(AuditEvent.of("CUSTOM_VIEW_CREATE", actor.getId(), actor.getEmail(),
                "CUSTOM_VIEW", created.id().toString()));

        return created;
    }

    @Transactional(readOnly = true)
    public List<CustomViewDto> listVisible(UUID dashboardId, DavsUser actor) {
        // BUS-VIZ-18: Custom View is only visible to author unless shared
        return customViewRepository.findVisibleForUser(dashboardId, actor.getId());
    }

    @Transactional
    public void setDefault(UUID id, DavsUser actor) {
        CustomViewDto cv = customViewRepository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy view tùy chỉnh"));

        if (!cv.ownerId().equals(actor.getId())) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Chỉ tác giả mới có thể đặt làm view mặc định");
        }

        customViewRepository.setDefault(id, cv.dashboardId(), actor.getId());
    }

    @Transactional
    public void setShared(UUID id, boolean shared, DavsUser actor) {
        CustomViewDto cv = customViewRepository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy view tùy chỉnh"));

        if (!cv.ownerId().equals(actor.getId())) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Chỉ tác giả mới có thể thay đổi trạng thái chia sẻ");
        }

        customViewRepository.setShared(id, actor.getId(), shared);
    }

    @Transactional
    public void delete(UUID id, DavsUser actor) {
        CustomViewDto cv = customViewRepository.findById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy view tùy chỉnh"));

        if (!cv.ownerId().equals(actor.getId())) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Chỉ tác giả mới có quyền xóa view tùy chỉnh");
        }

        customViewRepository.delete(id, actor.getId());
    }

    @Transactional
    public WorkbookDto saveAsWorkbook(SaveAsWorkbookRequest request, DavsUser actor) {
        // UC20 [2b]: Check PUBLISH permission in target project (ERR-PRJ-03)
        ResourceRef targetProjectRef = new ResourceRef(ResourceType.PROJECT, request.targetProjectId());
        if (!permissionService.can(actor, Capability.PUBLISH, targetProjectRef)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03, "Bạn không có quyền Xuất bản (PUBLISH) tại dự án đích");
        }

        WorkbookDao.WorkbookRow source = workbookRepository.findById(request.sourceWorkbookId())
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_03, "Không tìm thấy workbook nguồn"));

        if (workbookRepository.findActiveByProjectAndName(request.targetProjectId(), request.newName()).isPresent()) {
            throw new DavsException(ErrorCode.ERR_PRJ_06, "Tên workbook đã tồn tại trong dự án đích");
        }

        UUID copyId = workbookRepository.insertWorkbook(
                request.targetProjectId(),
                request.newName(),
                request.description() != null ? request.description() : source.description(),
                actor.getId(),
                "{}",
                false
        );
        WorkbookDao.WorkbookRow savedRow = workbookRepository.findById(copyId)
                .orElseThrow(() -> new IllegalStateException("Workbook not found after copy: " + copyId));
        WorkbookDto copy = workbookRepository.toDto(savedRow);

        auditService.log(AuditEvent.of("WORKBOOK_SAVE_AS", actor.getId(), actor.getEmail(),
                "WORKBOOK", copy.id().toString()));

        return copy;
    }

    private String writeJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
