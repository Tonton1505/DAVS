package com.davs.common.error;

/**
 * Represents a validation error on a specific input field.
 *
 * @param field   field name in request payload
 * @param message localized validation error message
 */
public record FieldErrorItem(String field, String message) {
}
