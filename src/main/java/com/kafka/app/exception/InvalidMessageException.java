package com.kafka.app.exception;

/**
 * Exception thrown when a message is invalid.
 * Used for validation errors and malformed requests.
 */
public class InvalidMessageException extends RuntimeException {
    
    public InvalidMessageException(String message) {
        super(message);
    }
    
    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
