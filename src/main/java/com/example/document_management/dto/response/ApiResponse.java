package com.example.document_management.dto.response;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ApiResponse<T> {
    private boolean success;
    private Integer statusCode; 
    private String message;
    private T data;
    private java.util.Map<String, Object> meta;
    private String errorCode;
    private String timestamp;

    public Integer getCode() {
        return statusCode != null ? statusCode : (success ? 200 : 500);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(int statusCode, T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> success(int statusCode, T data, String message, java.util.Map<String, Object> meta) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .data(data)
                .message(message)
                .meta(meta)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(int statusCode, String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .errorCode(errorCode)
                .timestamp(Instant.now().toString())
                .build();
    }
}
