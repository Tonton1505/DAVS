package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.JdbcLabelRepository;
import vn.edu.davs.backend.dto.AuditEvent;
import vn.edu.davs.backend.dto.CreateLabelCategoryRequest;
import vn.edu.davs.backend.dto.CreateLabelRequest;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.LabelTargetRequest;
import vn.edu.davs.backend.dto.UpdateLabelRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.enu.SiteRole;
import vn.edu.davs.backend.exception.DavsException;
import vn.edu.davs.backend.exception.FieldErrorItem;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC09 business logic for managing data label categories, labels, and target assignments.
 */
@Service
@RequiredArgsConstructor
public class LabelService {

    private static final int MAX_LABEL_VALUE_LENGTH = 50;
    private static final int MAX_LABEL_DESC_LENGTH = 250;
    private static final int MAX_CATEGORY_NAME_LENGTH = 100;

    private final JdbcLabelRepository labelRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<LabelCategoryDto> listCategories() {
        return labelRepository.listCategories();
    }

    @Transactional
    public LabelCategoryDto createCategory(CreateLabelCategoryRequest request, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        String name = request != null && request.name() != null ? request.name().trim() : "";
        if (name.isEmpty() || name.length() > MAX_CATEGORY_NAME_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("name", "Tên danh mục nhãn phải từ 1 đến 100 ký tự"))
            );
        }
        String description = normalizeDescription(request.description());
        if (labelRepository.existsCategoryByName(name)) {
            throw new DavsException(ErrorCode.ERR_PRJ_07);
        }
        boolean isWarning = Boolean.TRUE.equals(request.isWarning());
        UUID id = labelRepository.insertCategory(name, description, isWarning, actor.getId());
        LabelCategoryDto created = labelRepository.findCategoryById(id)
                .orElseThrow(() -> new IllegalStateException("Category not found after insert: " + id));

        auditService.log(AuditEvent.of(
                "LABEL_CATEGORY_CREATE",
                actor.getId(),
                actor.getEmail(),
                "LABEL_CATEGORY",
                id.toString()
        ).withStates(null, Map.of("name", created.name(), "isWarning", created.isWarning())));

        return created;
    }

    @Transactional(readOnly = true)
    public List<LabelDto> listLabels(UUID categoryId, String query) {
        return labelRepository.listLabels(categoryId, query);
    }

    @Transactional
    public LabelDto createLabel(CreateLabelRequest request, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        if (request == null || request.categoryId() == null) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("categoryId", "Danh mục nhãn là bắt buộc"))
            );
        }
        LabelCategoryDto category = labelRepository.findCategoryById(request.categoryId())
                .orElseThrow(() -> new DavsException(
                        ErrorCode.ERR_PRJ_02,
                        List.of(new FieldErrorItem("categoryId", "Danh mục nhãn không tồn tại"))
                ));

        String value = validateLabelValue(request.value());
        String description = normalizeDescription(request.description());

        if (labelRepository.existsLabelInCategory(category.id(), value, null)) {
            throw new DavsException(ErrorCode.ERR_PRJ_07);
        }

        boolean isWarning = request.isWarning() != null ? request.isWarning() : category.isWarning();
        UUID labelId = labelRepository.insertLabel(
                category.id(),
                value,
                description,
                isWarning,
                actor.getId()
        );
        LabelDto created = labelRepository.findLabelById(labelId)
                .orElseThrow(() -> new IllegalStateException("Label not found after insert: " + labelId));

        auditService.log(AuditEvent.of(
                "LABEL_CREATE",
                actor.getId(),
                actor.getEmail(),
                "LABEL",
                labelId.toString()
        ).withStates(null, Map.of(
                "categoryId", category.id().toString(),
                "categoryName", category.name(),
                "value", created.value(),
                "isWarning", created.isWarning()
        )));

        return created;
    }

    @Transactional
    public LabelDto updateLabel(UUID id, UpdateLabelRequest request, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        LabelDto existing = labelRepository.findLabelById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));

        String value = request != null && request.value() != null
                ? validateLabelValue(request.value())
                : existing.value();
        String description = request != null && request.description() != null
                ? normalizeDescription(request.description())
                : existing.description();
        boolean isWarning = request != null && request.isWarning() != null
                ? request.isWarning()
                : existing.isWarning();

        if (labelRepository.existsLabelInCategory(existing.categoryId(), value, id)) {
            throw new DavsException(ErrorCode.ERR_PRJ_07);
        }

        labelRepository.updateLabel(id, value, description, isWarning);
        LabelDto updated = labelRepository.findLabelById(id)
                .orElseThrow(() -> new IllegalStateException("Label not found after update: " + id));

        auditService.log(AuditEvent.of(
                "LABEL_UPDATE",
                actor.getId(),
                actor.getEmail(),
                "LABEL",
                id.toString()
        ).withStates(
                Map.of("value", existing.value(), "isWarning", existing.isWarning()),
                Map.of("value", updated.value(), "isWarning", updated.isWarning())
        ));

        return updated;
    }

    @Transactional
    public Map<String, Object> deleteLabel(UUID id, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        LabelDto existing = labelRepository.findLabelById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        int unassignedCount = labelRepository.countAssignments(id);
        labelRepository.deleteLabel(id);

        auditService.log(AuditEvent.of(
                "LABEL_DELETE",
                actor.getId(),
                actor.getEmail(),
                "LABEL",
                id.toString()
        ).withStates(
                Map.of("value", existing.value(), "unassignedCount", unassignedCount),
                null
        ));

        return Map.of(
                "deletedId", id,
                "unassignedCount", unassignedCount
        );
    }

    @Transactional
    public LabelDto assignLabel(UUID labelId, LabelTargetRequest request, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        LabelDto label = labelRepository.findLabelById(labelId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        if (request == null || request.targets() == null || request.targets().isEmpty()) {
            throw new DavsException(ErrorCode.ERR_PRJ_02);
        }

        int added = 0;
        for (LabelTargetRequest.TargetRef target : request.targets()) {
            if (target == null || target.id() == null || target.type() == null) {
                continue;
            }
            String normType = target.type().trim().toUpperCase(Locale.ROOT);
            added += labelRepository.assignLabel(labelId, normType, target.id(), actor.getId());
        }

        auditService.log(AuditEvent.of(
                "LABEL_ASSIGN",
                actor.getId(),
                actor.getEmail(),
                "LABEL",
                labelId.toString()
        ).withStates(null, Map.of("labelValue", label.value(), "assignedTargets", added)));

        return labelRepository.findLabelById(labelId).orElse(label);
    }

    @Transactional
    public LabelDto unassignLabel(UUID labelId, LabelTargetRequest request, DavsUser actor) {
        requireCreatorOrAdmin(actor);
        LabelDto label = labelRepository.findLabelById(labelId)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
        if (request == null || request.targets() == null || request.targets().isEmpty()) {
            throw new DavsException(ErrorCode.ERR_PRJ_02);
        }

        int removed = 0;
        for (LabelTargetRequest.TargetRef target : request.targets()) {
            if (target == null || target.id() == null || target.type() == null) {
                continue;
            }
            String normType = target.type().trim().toUpperCase(Locale.ROOT);
            removed += labelRepository.unassignLabel(labelId, normType, target.id());
        }

        auditService.log(AuditEvent.of(
                "LABEL_UNASSIGN",
                actor.getId(),
                actor.getEmail(),
                "LABEL",
                labelId.toString()
        ).withStates(Map.of("labelValue", label.value(), "removedTargets", removed), null));

        return labelRepository.findLabelById(labelId).orElse(label);
    }

    @Transactional(readOnly = true)
    public List<LabelDto> listLabelsForTarget(String targetType, UUID targetId) {
        if (targetType == null || targetId == null) {
            return List.of();
        }
        return labelRepository.listLabelsForTarget(targetType.trim().toUpperCase(Locale.ROOT), targetId);
    }

    private static void requireCreatorOrAdmin(DavsUser actor) {
        if (actor == null || !actor.getSiteRole().isAtLeast(SiteRole.CREATOR)) {
            throw new DavsException(ErrorCode.ERR_PRJ_03);
        }
    }

    private static String validateLabelValue(String rawValue) {
        String trimmed = rawValue != null ? rawValue.trim() : "";
        if (trimmed.isEmpty() || trimmed.length() > MAX_LABEL_VALUE_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("value", "Giá trị nhãn phải từ 1 đến 50 ký tự"))
            );
        }
        return trimmed;
    }

    private static String normalizeDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        String trimmed = rawDescription.trim();
        if (trimmed.length() > MAX_LABEL_DESC_LENGTH) {
            throw new DavsException(
                    ErrorCode.ERR_PRJ_02,
                    List.of(new FieldErrorItem("description", "Mô tả nhãn tối đa 250 ký tự"))
            );
        }
        return trimmed;
    }
}
