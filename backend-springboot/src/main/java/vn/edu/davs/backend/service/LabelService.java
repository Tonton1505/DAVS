package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CreateLabelCategoryRequest;
import vn.edu.davs.backend.dto.CreateLabelRequest;
import vn.edu.davs.backend.dto.LabelCategoryDto;
import vn.edu.davs.backend.dto.LabelDto;
import vn.edu.davs.backend.dto.LabelTargetRequest;
import vn.edu.davs.backend.dto.UpdateLabelRequest;
import vn.edu.davs.backend.model.DavsUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core UC09 business logic for managing data label categories, labels, and target assignments.
 */
public interface LabelService {


    @Transactional(readOnly = true)
    List<LabelCategoryDto> listCategories();


    @Transactional
    LabelCategoryDto createCategory(CreateLabelCategoryRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    List<LabelDto> listLabels(UUID categoryId, String query);


    @Transactional
    LabelDto createLabel(CreateLabelRequest request, DavsUser actor);


    @Transactional
    LabelDto updateLabel(UUID id, UpdateLabelRequest request, DavsUser actor);


    @Transactional
    Map<String, Object> deleteLabel(UUID id, DavsUser actor);


    @Transactional
    LabelDto assignLabel(UUID labelId, LabelTargetRequest request, DavsUser actor);


    @Transactional
    LabelDto unassignLabel(UUID labelId, LabelTargetRequest request, DavsUser actor);


    @Transactional(readOnly = true)
    List<LabelDto> listLabelsForTarget(String targetType, UUID targetId);
}
