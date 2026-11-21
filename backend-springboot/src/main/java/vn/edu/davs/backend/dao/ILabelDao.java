package vn.edu.davs.backend.dao;

import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC persistence for UC09 Data Labels, Categories, and Target Assignments.
 */
public interface ILabelDao {


    List<LabelCategoryDto> listCategories();


    Optional<LabelCategoryDto> findCategoryById(UUID id);


    boolean existsCategoryByName(String name);


    UUID insertCategory(String name, String description, boolean isWarning, UUID createdBy);


    List<LabelDto> listLabels(UUID categoryId, String query);


    Optional<LabelDto> findLabelById(UUID id);


    boolean existsLabelInCategory(UUID categoryId, String value, UUID excludeLabelId);


    UUID insertLabel(
            UUID categoryId,
            String value,
            String description,
            boolean isWarning,
            UUID createdBy
    );


    void updateLabel(UUID id, String value, String description, boolean isWarning);


    int countAssignments(UUID labelId);


    void deleteLabel(UUID id);


    int assignLabel(UUID labelId, String targetType, UUID targetId, UUID assignedBy);


    int unassignLabel(UUID labelId, String targetType, UUID targetId);


    List<LabelDto> listLabelsForTarget(String targetType, UUID targetId);
}
