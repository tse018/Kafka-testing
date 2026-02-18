package com.kafka.app.producer;

import com.kafka.app.exception.InvalidMessageException;
import com.kafka.app.exception.KafkaProducerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import lombok.NonNull;

/**
 * Producer service for sending messages to Kafka.
 * Handles message validation, sending, and error handling with metrics.
 */
@Service
public class KafkaProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter messagesSentCounter;
    private final Counter messagesFailedCounter;
    private final String kafkaTopic;
    
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, 
                        MeterRegistry meterRegistry,
                        @Value("${kafka.topic.messages}") String kafkaTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaTopic = kafkaTopic;
        this.messagesSentCounter = Counter.builder("kafka.messages.sent")
                .description("Total number of messages sent to Kafka")
                .register(meterRegistry);
        this.messagesFailedCounter = Counter.builder("kafka.messages.failed")
                .description("Total number of failed message sends")
                .register(meterRegistry);
    }
    
    /**
     * Send a message to Kafka topic with validation.
     *
     * @param message the message content to send
     * @throws InvalidMessageException if message is null or blank
     * @throws KafkaProducerException if sending fails
     */
    public void sendMessage(@NonNull String message) {
        if (message.isBlank()) {
            logger.warn("Attempted to send blank message");
            throw new InvalidMessageException("Message cannot be blank");
        }
        
        try {
            logger.debug("Producing message: {}", message);
            
            Message<String> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, kafkaTopic)
                    .build();
            
            kafkaTemplate.send(kafkaMessage);
            messagesSentCounter.increment();
            logger.info("Message sent successfully to topic: {}", kafkaTopic);
        } catch (InvalidMessageException e) {
            messagesFailedCounter.increment();
            logger.error("Invalid message: {}", message, e);
            throw e;
        } catch (Exception e) {
            messagesFailedCounter.increment();
            logger.error("Failed to send message to Kafka: {}", message, e);
            throw new KafkaProducerException("Failed to send message to Kafka topic: " + kafkaTopic, e);
        }
    }
}

