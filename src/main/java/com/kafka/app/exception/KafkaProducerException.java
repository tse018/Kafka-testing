package com.kafka.app.exception;

/**
 * Exception thrown when Kafka producer fails to send a message.
 * Used for Kafka communication errors.
 */
public class KafkaProducerException extends RuntimeException {
    
    public KafkaProducerException(String message) {
        super(message);
    }
    
    public KafkaProducerException(String message, Throwable cause) {
        super(message, cause);
    }
}
