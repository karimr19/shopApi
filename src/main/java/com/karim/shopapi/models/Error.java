package com.karim.shopapi.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * Ошибка, возвращаемая пользователю.
 */
@Data
@Schema
public class Error {
    public Error() {
    }

    public Error(ErrorType type) {
        if (type == ErrorType.NOTFOUND) {
            code = 404;
            message = "Item not found";
        } else {
            code = 400;
            message = "Validation Failed";
        }
    }

    private int code;
    @NotNull
    private String message;
}
