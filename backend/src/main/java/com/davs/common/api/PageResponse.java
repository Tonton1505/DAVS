package com.davs.common.api;

import java.util.List;

/**
 * Standard paginated API response format defined in context.md section 7:
 * {@code {content, page, size, totalElements}}.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }
}
