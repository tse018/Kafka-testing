package com.kafka.app.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private static final String TOPIC = "messages";
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public KafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    public void sendMessage(String message) {
        logger.info("Producing message: {}", message);
        
        Message<String> kafkaMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .build();
        
        kafkaTemplate.send(kafkaMessage);
        logger.info("Message sent successfully");
    }
}
