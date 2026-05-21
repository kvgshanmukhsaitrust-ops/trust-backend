package com.trustplatform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    Map<String, String> errors,
    LocalDateTime timestamp,
    int status
) {
    public static <T> ApiResponse<T> success(String message, T data, int status) {
        return new ApiResponse<>(true, message, data, null, LocalDateTime.now(), status);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(message, data, 200);
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(message, null, 200);
    }

    public static <T> ApiResponse<T> error(String message, Map<String, String> errors, int status) {
        return new ApiResponse<>(false, message, null, errors, LocalDateTime.now(), status);
    }

    public static <T> ApiResponse<T> error(String message, int status) {
        return error(message, null, status);
    }
}
