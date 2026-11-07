package com.davs.common.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which {@link ErrorCode} should be returned as the top-level error code
 * when Jakarta Validation fails on a request DTO or controller endpoint.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidationErrorCode {

    ErrorCode value();
}
