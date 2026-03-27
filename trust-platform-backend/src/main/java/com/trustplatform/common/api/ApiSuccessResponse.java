package com.trustplatform.common.api;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiSuccessResponse<T> {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;
}