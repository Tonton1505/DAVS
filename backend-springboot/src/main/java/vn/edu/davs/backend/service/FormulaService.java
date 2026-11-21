package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.FormulaValidateResponse;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Formula validation, Lexer/Parser checks, and Calculated Fields (UC16).
 */
public interface FormulaService {


    FormulaValidateResponse validateFormula(UUID datasourceId, String formula, String targetFieldName);


    @Transactional(readOnly = true)
    List<CalculatedFieldDto> listCalculatedFields(UUID datasourceId);


    @Transactional(readOnly = true)
    CalculatedFieldDto getCalculatedField(UUID id);


    @Transactional
    CalculatedFieldDto createCalculatedField(UUID datasourceId, SaveCalculatedFieldRequest request);


    @Transactional
    CalculatedFieldDto updateCalculatedField(UUID id, SaveCalculatedFieldRequest request);


    @Transactional
    void deleteCalculatedField(UUID id);
}
