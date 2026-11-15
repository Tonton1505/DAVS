package vn.edu.davs.backend.dto;

/**
 * Response payload returned after validating formula (UC16).
 */
public record FormulaValidateResponse(
        boolean valid,
        String returnType,
        String role,
        Integer errorPosition,
        String message
) {
}
