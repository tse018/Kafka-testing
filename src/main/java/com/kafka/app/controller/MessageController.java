package com.kafka.app.controller;

import com.kafka.app.exception.InvalidMessageException;
import com.kafka.app.exception.KafkaProducerException;
import com.kafka.app.exception.MessageStorageException;
import com.kafka.app.producer.KafkaProducer;
import com.kafka.app.service.MessageStorageService;
import com.kafka.app.dto.ApiResponse;
import com.kafka.app.dto.MessageRequest;
import com.kafka.app.model.Message;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller for message operations.
 * Handles incoming HTTP requests for sending, retrieving, and managing messages.
 */
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MessageController {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    
    private final KafkaProducer kafkaProducer;
    private final MessageStorageService messageStorageService;
    
    public MessageController(KafkaProducer kafkaProducer, MessageStorageService messageStorageService) {
        this.kafkaProducer = kafkaProducer;
        this.messageStorageService = messageStorageService;
    }
    
    /**
     * Send a message via query parameter.
     * 
     * @param message the message to send
     * @return API response with status
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendMessage(@RequestParam String message) {
        try {
            if (message == null || message.isBlank()) {
                logger.warn("Received blank message parameter");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Message cannot be blank", null));
            }
            
            logger.info("Received request to send message");
            kafkaProducer.sendMessage(message);
            return ResponseEntity.ok(new ApiResponse<>(true, "Message sent successfully", message));
        } catch (InvalidMessageException e) {
            logger.warn("Invalid message: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (KafkaProducerException e) {
            logger.error("Kafka producer error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Kafka service unavailable", null));
        } catch (Exception e) {
            logger.error("Unexpected error sending message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to send message", null));
        }
    }
    
    /**
     * Send a message via JSON body with validation.
     * 
     * @param request the message request DTO
     * @return API response with status
     */
    @PostMapping("/send-json")
    public ResponseEntity<ApiResponse<String>> sendJsonMessage(@Valid @RequestBody MessageRequest request) {
        try {
            logger.info("Received request to send JSON message");
            kafkaProducer.sendMessage(request.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(true, "Message sent successfully", request.getMessage()));
        } catch (InvalidMessageException e) {
            logger.warn("Invalid message: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (KafkaProducerException e) {
            logger.error("Kafka producer error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Kafka service unavailable", null));
        } catch (Exception e) {
            logger.error("Unexpected error sending JSON message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to send message", null));
        }
    }
    
    /**
     * Retrieve all messages.
     * 
     * @return API response with list of messages
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Message>>> getAllMessages() {
        try {
            logger.debug("Fetching all messages");
            List<Message> messages = messageStorageService.getAllMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "Messages retrieved successfully", messages));
        } catch (MessageStorageException e) {
            logger.error("Storage error retrieving messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve messages", null));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve messages", null));
        }
    }
    
    /**
     * Retrieve a message by ID.
     * 
     * @param id the message ID
     * @return API response with message or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(@PathVariable String id) {
        try {
            if (id == null || id.isBlank()) {
                logger.warn("Received blank message ID");
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Message ID cannot be blank", null));
            }
            
            logger.debug("Fetching message by id: {}", id);
            Message message = messageStorageService.getMessageById(id);
            if (message != null) {
                return ResponseEntity.ok(new ApiResponse<>(true, "Message retrieved successfully", message));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "Message not found", null));
            }
        } catch (MessageStorageException e) {
            logger.error("Storage error retrieving message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve message", null));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving message by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve message", null));
        }
    }
    
    /**
     * Get the count of all messages.
     * 
     * @return API response with message count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getMessageCount() {
        try {
            int count = messageStorageService.getMessageCount();
            return ResponseEntity.ok(new ApiResponse<>(true, "Message count retrieved", count));
        } catch (MessageStorageException e) {
            logger.error("Storage error getting message count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to get message count", null));
        } catch (Exception e) {
            logger.error("Unexpected error getting message count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to get message count", null));
        }
    }
    
    /**
     * Clear all messages from the database.
     * 
     * @return API response with status
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> clearMessages() {
        try {
            logger.info("Clearing all messages");
            messageStorageService.clearMessages();
            return ResponseEntity.ok(new ApiResponse<>(true, "All messages cleared", null));
        } catch (MessageStorageException e) {
            logger.error("Storage error clearing messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to clear messages", null));
        } catch (Exception e) {
            logger.error("Unexpected error clearing messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to clear messages", null));
        }
    }
    
    /**
     * Health check endpoint for API availability.
     * 
     * @return API response indicating healthy status
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(new ApiResponse<>(true, "API is healthy", "Running"));
    }
}