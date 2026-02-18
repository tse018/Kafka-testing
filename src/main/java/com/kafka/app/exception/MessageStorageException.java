package com.kafka.app.exception;

/**
 * Exception thrown when a database operation fails.
 * Used for storage and retrieval errors.
 */
public class MessageStorageException extends RuntimeException {
    
    public MessageStorageException(String message) {
        super(message);
    }
    
    public MessageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
