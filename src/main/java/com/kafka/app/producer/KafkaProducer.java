package com.kafka.app.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Service
public class KafkaProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private static final String TOPIC = "messages";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Counter messagesSentCounter;
    private final Counter messagesFailedCounter;
    
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.messagesSentCounter = Counter.builder("kafka.messages.sent")
                .description("Total number of messages sent to Kafka")
                .register(meterRegistry);
        this.messagesFailedCounter = Counter.builder("kafka.messages.failed")
                .description("Total number of failed message sends")
                .register(meterRegistry);
    }
    
    public void sendMessage(String message) {
        try {
            logger.debug("Producing message: {}", message);
            
            Message<String> kafkaMessage = MessageBuilder
                    .withPayload(message)
                    .setHeader(KafkaHeaders.TOPIC, TOPIC)
                    .build();
            
            kafkaTemplate.send(kafkaMessage);
            messagesSentCounter.increment();
            logger.info("Message sent successfully to topic: {}", TOPIC);
        } catch (Exception e) {
            messagesFailedCounter.increment();
            logger.error("Failed to send message: {}", message, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}

