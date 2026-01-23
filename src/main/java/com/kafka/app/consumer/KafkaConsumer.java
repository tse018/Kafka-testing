package com.kafka.app.consumer;

import com.kafka.app.model.Message;
import com.kafka.app.service.MessageStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.UUID;

@Service
public class KafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    
    private final MessageStorageService messageStorageService;
    private final Counter messagesConsumedCounter;
    
    public KafkaConsumer(MessageStorageService messageStorageService, MeterRegistry meterRegistry) {
        this.messageStorageService = messageStorageService;
        this.messagesConsumedCounter = Counter.builder("kafka.messages.consumed")
                .description("Total number of messages consumed from Kafka")
                .register(meterRegistry);
    }
    
    @KafkaListener(topics = "messages", groupId = "kafka-group")
    public void consume(String messageContent) {
        try {
            logger.debug("Consuming message: {}", messageContent);
            
            Message message = new Message(
                    UUID.randomUUID().toString(),
                    messageContent,
                    System.currentTimeMillis(),
                    "PROCESSED",
                    null,
                    null
            );
            
            messageStorageService.addMessage(message);
            messagesConsumedCounter.increment();
            
            logger.info("Message consumed and stored successfully. Total messages: {}", 
                    messageStorageService.getMessageCount());
        } catch (Exception e) {
            logger.error("Error consuming message: {}", messageContent, e);
        }
    }
}

