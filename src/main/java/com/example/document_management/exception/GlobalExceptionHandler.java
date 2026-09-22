package com.example.document_management.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.document_management.dto.response.ApiResponse;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.<Void>error(HttpStatus.NOT_FOUND.value(), ex.getMessage(), HttpStatus.NOT_FOUND.name()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<?> handleStorageException(StorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Void>error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.name()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception ex) {
        // return
        // ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message",
        // ex.getMessage()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<Void>error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.name()));
    }
}
