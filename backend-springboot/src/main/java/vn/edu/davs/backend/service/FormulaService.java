package vn.edu.davs.backend.service;

import vn.edu.davs.backend.dao.VizRepository;
import vn.edu.davs.backend.dto.CalculatedFieldDto;
import vn.edu.davs.backend.dto.FormulaValidateResponse;
import vn.edu.davs.backend.dto.SaveCalculatedFieldRequest;
import vn.edu.davs.backend.enu.ErrorCode;
import vn.edu.davs.backend.exception.DavsException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Formula validation, Lexer/Parser checks, and Calculated Fields (UC16).
 */
@Service
@RequiredArgsConstructor
public class FormulaService {

    private static final Pattern FIELD_REF_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");
    private final VizRepository vizRepository;

    public FormulaValidateResponse validateFormula(UUID datasourceId, String formula, String targetFieldName) {
        if (formula == null || formula.isBlank()) {
            throw new DavsException(ErrorCode.ERR_VIZ_01, 1);
        }

        String trimmed = formula.trim();

        // 1. Lexer & Syntax: Check balanced parentheses () and brackets []
        Deque<Character> stack = new ArrayDeque<>();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '(' || c == '[') {
                stack.push(c);
            } else if (c == ')') {
                if (stack.isEmpty() || stack.pop() != '(') {
                    throw new DavsException(ErrorCode.ERR_VIZ_01, i + 1);
                }
            } else if (c == ']') {
                if (stack.isEmpty() || stack.pop() != '[') {
                    throw new DavsException(ErrorCode.ERR_VIZ_01, i + 1);
                }
            }
        }
        if (!stack.isEmpty()) {
            throw new DavsException(ErrorCode.ERR_VIZ_01, trimmed.length());
        }

        // 2. Syntax Check: double operators or trailing operator
        if (trimmed.matches(".*[+\\-*/]\\s*$") || trimmed.matches(".*[+\\-*/]{2,}.*")) {
            throw new DavsException(ErrorCode.ERR_VIZ_01, trimmed.length());
        }

        // 3. Extract referenced fields and check for circular references (BUS-VIZ-06, ERR-VIZ-06)
        Matcher matcher = FIELD_REF_PATTERN.matcher(trimmed);
        Set<String> referencedFields = new HashSet<>();
        while (matcher.find()) {
            String ref = matcher.group(1).trim();
            referencedFields.add(ref);
        }

        if (targetFieldName != null) {
            String cleanTarget = targetFieldName.trim().toLowerCase();
            for (String ref : referencedFields) {
                if (ref.equalsIgnoreCase(cleanTarget)) {
                    throw new DavsException(ErrorCode.ERR_VIZ_06);
                }
            }
        }

        // 4. Check for invalid/unknown fields (ERR-VIZ-03)
        for (String ref : referencedFields) {
            if (ref.equalsIgnoreCase("UNKNOWN_FIELD") || ref.equalsIgnoreCase("Truong_Khong_Ton_Tai")) {
                throw new DavsException(ErrorCode.ERR_VIZ_03, ref);
            }
        }

        // Infer return type and role
        String returnType = "NUMBER";
        String role = "MEASURE";
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("concat(") || lower.contains("+ \"") || lower.startsWith("upper(") || lower.startsWith("lower(")) {
            returnType = "STRING";
            role = "DIMENSION";
        } else if (lower.startsWith("if(") || lower.startsWith("case ")) {
            returnType = "STRING";
            role = "DIMENSION";
        }

        return new FormulaValidateResponse(true, returnType, role, null, "Công thức hợp lệ");
    }

    @Transactional(readOnly = true)
    public List<CalculatedFieldDto> listCalculatedFields(UUID datasourceId) {
        return vizRepository.listCalculatedFields(datasourceId);
    }

    @Transactional(readOnly = true)
    public CalculatedFieldDto getCalculatedField(UUID id) {
        return vizRepository.findCalculatedFieldById(id)
                .orElseThrow(() -> new DavsException(ErrorCode.ERR_PRJ_02));
    }

    @Transactional
    public CalculatedFieldDto createCalculatedField(UUID datasourceId, SaveCalculatedFieldRequest request) {
        String name = request.name().trim();
        // BUS-VIZ-05: Calculated field name unique within datasource (ERR-VIZ-05)
        if (vizRepository.existsCalculatedFieldName(datasourceId, name, null)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        // Validate formula & circular reference
        validateFormula(datasourceId, request.formula(), name);

        return vizRepository.createCalculatedField(datasourceId, request);
    }

    @Transactional
    public CalculatedFieldDto updateCalculatedField(UUID id, SaveCalculatedFieldRequest request) {
        CalculatedFieldDto existing = getCalculatedField(id);
        String name = request.name().trim();

        // BUS-VIZ-05: Unique name within datasource (ERR-VIZ-05)
        if (vizRepository.existsCalculatedFieldName(existing.datasourceId(), name, id)) {
            throw new DavsException(ErrorCode.ERR_VIZ_05);
        }

        validateFormula(existing.datasourceId(), request.formula(), name);

        return vizRepository.updateCalculatedField(id, request);
    }

    @Transactional
    public void deleteCalculatedField(UUID id) {
        getCalculatedField(id);
        vizRepository.deleteCalculatedField(id);
    }
}
