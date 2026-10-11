package com.smnk107.DocMind.exceptions;


import com.smnk107.DocMind.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.error("Resource not found: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSizeExceeded(
            FileSizeExceededException ex) {

        log.error("File size exceeded: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ex.getMessage()
        );
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedFileType(
            UnsupportedFileTypeException ex) {

        log.error("Unsupported file type: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ex.getMessage()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex) {

        log.error("Duplicate resource: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileStorage(
            FileStorageException ex) {

        log.error("File storage error: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleDocumentProcessing(
            DocumentProcessingException ex) {

        log.error("Document processing failed: {}", ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex) {

        log.error("Unexpected error occurred", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            HttpStatus status,
            String message) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }
}