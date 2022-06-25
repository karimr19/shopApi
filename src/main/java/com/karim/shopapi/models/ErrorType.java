package com.karim.shopapi.models;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * Типы ошибок, возникающих при работе программы.
 */
@Schema
public enum ErrorType {
    NOTFOUND(HttpStatus.NOT_FOUND),
    VALIDATION(HttpStatus.BAD_REQUEST);

    private final HttpStatus errorStatus;

    ErrorType(HttpStatus status) {
        this.errorStatus = status;
    }

    public HttpStatus getErrorStatus() {
        return errorStatus;
    }
}
