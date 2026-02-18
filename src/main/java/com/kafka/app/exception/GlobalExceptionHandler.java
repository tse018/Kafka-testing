package com.kafka.app.exception;

import com.kafka.app.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for the application.
 * Provides centralized error handling and consistent error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle InvalidMessageException.
     */
    @ExceptionHandler(InvalidMessageException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalidMessageException(InvalidMessageException e) {
        logger.warn("Invalid message exception: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, e.getMessage(), null));
    }
    
    /**
     * Handle MessageStorageException.
     */
    @ExceptionHandler(MessageStorageException.class)
    public ResponseEntity<ApiResponse<String>> handleMessageStorageException(MessageStorageException e) {
        logger.error("Message storage exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Database operation failed", null));
    }
    
    /**
     * Handle KafkaProducerException.
     */
    @ExceptionHandler(KafkaProducerException.class)
    public ResponseEntity<ApiResponse<String>> handleKafkaProducerException(KafkaProducerException e) {
        logger.error("Kafka producer exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, "Kafka service unavailable", null));
    }
    
    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException e) {
        StringBuilder errors = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.append(fieldName).append(": ").append(errorMessage).append("; ");
        });
        logger.warn("Validation error: {}", errors.toString());
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Validation failed: " + errors.toString(), null));
    }
    
    /**
     * Handle 404 Not Found exceptions.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(NoHandlerFoundException e) {
        logger.warn("Endpoint not found: {}", e.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Endpoint not found", null));
    }
    
    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception e) {
        logger.error("Unexpected exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "An unexpected error occurred", null));
    }
}
