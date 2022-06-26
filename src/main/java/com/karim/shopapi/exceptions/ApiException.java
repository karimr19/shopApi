package com.karim.shopapi.exceptions;

import com.karim.shopapi.models.ErrorType;

/**
 * Исключение, возникающее при работе сервиса.
 */
public class ApiException extends RuntimeException {

    private ErrorType errorType;

    public ApiException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Создает исключение с кодом NotFound.
     *
     * @param id
     */
    public ApiException(String id) {
        super(String.format("Shop unit with input id doesn't exist, input id = %s", id));
        this.errorType = ErrorType.NOTFOUND;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
