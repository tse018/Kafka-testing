package com.kafka.app.controller;

import com.kafka.app.producer.KafkaProducer;
import com.kafka.app.service.MessageStorageService;
import com.kafka.app.dto.ApiResponse;
import com.kafka.app.dto.MessageRequest;
import com.kafka.app.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    
    private final KafkaProducer kafkaProducer;
    private final MessageStorageService messageStorageService;
    
    public MessageController(KafkaProducer kafkaProducer, MessageStorageService messageStorageService) {
        this.kafkaProducer = kafkaProducer;
        this.messageStorageService = messageStorageService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendMessage(@RequestParam String message) {
        try {
            logger.info("Received request to send message: {}", message);
            kafkaProducer.sendMessage(message);
            return ResponseEntity.ok(new ApiResponse<>(true, "Message sent successfully", message));
        } catch (Exception error) {
            logger.error("Error sending message", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to send message: " + error.getMessage(), null));
        }
    }
    
    @PostMapping("/send-json")
    public ResponseEntity<ApiResponse<Message>> sendJsonMessage(@RequestBody MessageRequest request) {
        try {
            logger.info("Received request to send JSON message: {}", request.getMessage());
            kafkaProducer.sendMessage(request.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(true, "Message sent successfully", null));
        } catch (Exception error) {
            logger.error("Error sending JSON message", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to send message: " + error.getMessage(), null));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Message>>> getAllMessages() {
        try {
            logger.debug("Fetching all messages");
            List<Message> messages = messageStorageService.getAllMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "Messages retrieved successfully", messages));
        } catch (Exception error) {
            logger.error("Error retrieving messages", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve messages", null));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(@PathVariable String id) {
        try {
            logger.debug("Fetching message by id: {}", id);
            Message message = messageStorageService.getMessageById(id);
            if (message != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Message retrieved successfully", message));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Message not found", null));
            }
        } catch (Exception error) {
            logger.error("Error retrieving message by id", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve message", null));
        }
    }
    
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getMessageCount() {
        try {
            int count = messageStorageService.getMessageCount();
            return ResponseEntity.ok(new ApiResponse<>(true, "Message count retrieved", count));
        } catch (Exception error) {
            logger.error("Error getting message count", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to get message count", null));
        }
    }
    
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearMessages() {
        try {
            logger.info("Clearing all messages");
            messageStorageService.clearMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "All messages cleared", null));
        } catch (Exception error) {
            logger.error("Error clearing messages", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to clear messages", null));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(new ApiResponse<>(true, "API is healthy", "Running"));
    }
}
