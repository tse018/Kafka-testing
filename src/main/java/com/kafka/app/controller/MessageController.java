package com.kafka.app.controller;

import com.kafka.app.producer.KafkaProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    
    private final KafkaProducer kafkaProducer;
    
    public MessageController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }
    
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestParam String message) {
        kafkaProducer.sendMessage(message);
        return ResponseEntity.ok("Message sent to Kafka: " + message);
    }
}
