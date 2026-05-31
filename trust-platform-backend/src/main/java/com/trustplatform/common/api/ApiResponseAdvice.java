package com.trustplatform.common.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.trustplatform")
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip wrapping if the return type is a Resource (e.g., file downloads)
        if (org.springframework.core.io.Resource.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // Skip wrapping if it is already wrapped
        if (body instanceof ApiResponse) {
            return body;
        }

        // Skip wrapping if it is a Resource (e.g., file downloads)
        if (body instanceof org.springframework.core.io.Resource) {
            return body;
        }

        // Standardize the response structure
        ApiResponse<Object> apiResponse;
        if (body == null) {
            apiResponse = ApiResponse.success("Operation successful", null);
        } else if (body instanceof ApiErrorResponse legacyError) {
            apiResponse = ApiResponse.error(legacyError.getMessage(), legacyError.getValidationErrors(), legacyError.getStatus());
        } else if (body instanceof ApiSuccessResponse legacySuccess) {
            apiResponse = ApiResponse.success(legacySuccess.getMessage(), legacySuccess.getData(), legacySuccess.getStatus());
        } else {
            apiResponse = ApiResponse.success("Operation successful", body);
        }

        // Special handling for String return type to avoid ClassCastException in StringHttpMessageConverter
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                return "{\"success\":false,\"message\":\"Serialization error\"}";
            }
        }

        return apiResponse;
    }
}
