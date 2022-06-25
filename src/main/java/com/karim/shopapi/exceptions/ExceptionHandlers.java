package com.karim.shopapi.exceptions;

import com.karim.shopapi.models.Error;
import com.karim.shopapi.models.ErrorType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Обработчики исключений сервиса.
 */
@ControllerAdvice
class ExceptionHandlers {
    /**
     * Обрабатывет исключение ApiException и возвращает ответ с соответствующим кодом.
     * @param ex Пойманное исключение.
     * @return Ответ.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Error> handleApiException(ApiException ex) {
        ErrorType errorType = ex.getErrorType();
        Error error = new Error(errorType);
        return ResponseEntity.status(errorType.getErrorStatus()).body(error);
    }

    /**
     * Обрабатывет исключение MethodArgumentNotValidException и возвращает ответ с кодом 400.
     * @param ex Пойманное исключение.
     * @return Ответ.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ErrorType errorType = ErrorType.VALIDATION;
        Error error = new Error(errorType);
        return ResponseEntity.status(errorType.getErrorStatus()).body(error);
    }

    /**
     * Обрабатывет исключение HttpMessageNotReadableException и возвращает ответ с кодом 400.
     * @param ex Пойманное исключение.
     * @return Ответ.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ErrorType errorType = ErrorType.VALIDATION;
        Error error = new Error(errorType);
        return ResponseEntity.status(errorType.getErrorStatus()).body(error);
    }

    /**
     * Обрабатывет исключение MissingServletRequestParameterException и возвращает ответ с кодом 400.
     * @param ex Пойманное исключение.
     * @return Ответ.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Error> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ErrorType errorType = ErrorType.VALIDATION;
        Error error = new Error(errorType);
        return ResponseEntity.status(errorType.getErrorStatus()).body(error);
    }
}