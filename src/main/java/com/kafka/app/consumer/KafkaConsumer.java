package com.kafka.app.consumer;

import com.kafka.app.exception.InvalidMessageException;
import com.kafka.app.exception.MessageStorageException;
import com.kafka.app.model.Message;
import com.kafka.app.service.MessageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.UUID;

/**
 * Consumer service for processing messages from Kafka.
 * Listens to Kafka topics and persists messages to the database.
 */
@Service
public class KafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private static final String MESSAGE_STATUS_PROCESSED = "PROCESSED";
    
    private final MessageStorageService messageStorageService;
    private final Counter messagesConsumedCounter;
    private final Counter messagesFailedCounter;
    
    public KafkaConsumer(MessageStorageService messageStorageService, MeterRegistry meterRegistry) {
        this.messageStorageService = messageStorageService;
        this.messagesConsumedCounter = Counter.builder("kafka.messages.consumed")
                .description("Total number of messages consumed from Kafka")
                .register(meterRegistry);
        this.messagesFailedCounter = Counter.builder("kafka.messages.consumed.failed")
                .description("Total number of failed message consumptions")
                .register(meterRegistry);
    }
    
    /**
     * Consume message from Kafka topic and store in database.
     * 
     * @param messageContent the message content from Kafka
     */
    @KafkaListener(topics = "messages", groupId = "kafka-group")
    public void consumeMessage(String messageContent) {
        try {
            if (messageContent == null || messageContent.isBlank()) {
                logger.warn("Received blank message from Kafka");
                messagesFailedCounter.increment();
                throw new InvalidMessageException("Message content cannot be blank");
            }
            
            logger.debug("Consuming message: {}", messageContent);
            
            Message message = new Message(
                    UUID.randomUUID().toString(),
                    messageContent,
                    System.currentTimeMillis(),
                    MESSAGE_STATUS_PROCESSED,
                    null,
                    null
            );
            
            messageStorageService.addMessage(message);
            messagesConsumedCounter.increment();
            
            logger.info("Message consumed and stored successfully. Total messages: {}", 
                    messageStorageService.getMessageCount());
        } catch (InvalidMessageException e) {
            messagesFailedCounter.increment();
            logger.error("Invalid message received from Kafka: {}", messageContent, e);
        } catch (MessageStorageException e) {
            messagesFailedCounter.increment();
            logger.error("Failed to store consumed message: {}", messageContent, e);
        } catch (Exception e) {
            messagesFailedCounter.increment();
            logger.error("Unexpected error consuming message: {}", messageContent, e);
        }
    }
}

