package com.kafka.app.service;

import com.kafka.app.exception.InvalidMessageException;
import com.kafka.app.exception.MessageStorageException;
import com.kafka.app.model.Message;
import com.kafka.app.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for managing message storage operations.
 * Uses Spring Data JPA to persist messages to the database.
 * Provides comprehensive CRUD and query operations with proper error handling.
 */
@Service
@RequiredArgsConstructor
public class MessageStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageStorageService.class);
    
    private final MessageRepository messageRepository;

    /**
     * Add a message to the database.
     * 
     * @param message the message to add
     * @throws MessageStorageException if save operation fails
     */
    @Transactional
    public void addMessage(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        try {
            messageRepository.save(message);
            logger.debug("Message saved successfully: {}", message.getId());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid message object: {}", message.getId(), e);
            throw new InvalidMessageException("Invalid message: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error saving message: {}", message.getId(), e);
            throw new MessageStorageException("Failed to save message: " + message.getId(), e);
        }
    }

    /**
     * Retrieve all messages from the database.
     * 
     * @return list of all messages sorted by creation date
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public List<Message> getAllMessages() {
        try {
            List<Message> messages = messageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            logger.debug("Retrieved {} messages from database", messages.size());
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving all messages", e);
            throw new MessageStorageException("Failed to retrieve messages", e);
        }
    }

    /**
     * Retrieve all messages with pagination.
     * 
     * @param pageNumber the page number (0-indexed)
     * @param pageSize the page size
     * @return paginated messages
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public Page<Message> getAllMessagesPaginated(int pageNumber, int pageSize) {
        try {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messages = messageRepository.findAll(pageable);
            logger.debug("Retrieved page {} with {} messages", pageNumber, messages.getNumberOfElements());
            return messages;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid pagination parameters: page={}, size={}", pageNumber, pageSize, e);
            throw new InvalidMessageException("Invalid pagination parameters", e);
        } catch (Exception e) {
            logger.error("Error retrieving paginated messages", e);
            throw new MessageStorageException("Failed to retrieve paginated messages", e);
        }
    }

    /**
     * Get a message by ID.
     * 
     * @param id the message ID
     * @return the message or null if not found
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public Message getMessageById(String id) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        
        try {
            Optional<Message> message = messageRepository.findById(id);
            if (message.isPresent()) {
                logger.debug("Message found: {}", id);
                return message.get();
            } else {
                logger.debug("Message not found: {}", id);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving message: {}", id, e);
            throw new MessageStorageException("Failed to retrieve message: " + id, e);
        }
    }

    /**
     * Get count of all messages.
     * 
     * @return the total number of messages
     * @throws MessageStorageException if count operation fails
     */
    @Transactional(readOnly = true)
    public int getMessageCount() {
        try {
            long count = messageRepository.count();
            logger.debug("Total message count: {}", count);
            return (int) count;
        } catch (Exception e) {
            logger.error("Error counting messages", e);
            throw new MessageStorageException("Failed to count messages", e);
        }
    }

    /**
     * Get count of messages by status.
     * 
     * @param status the status to count
     * @return the number of messages with the given status
     * @throws MessageStorageException if count operation fails
     */
    @Transactional(readOnly = true)
    public long getMessageCountByStatus(String status) {
        Objects.requireNonNull(status, "Status cannot be null");
        
        try {
            long count = messageRepository.countByStatus(status);
            logger.debug("Message count for status '{}': {}", status, count);
            return count;
        } catch (Exception e) {
            logger.error("Error counting messages by status: {}", status, e);
            throw new MessageStorageException("Failed to count messages by status: " + status, e);
        }
    }

    /**
     * Clear all messages from the database.
     * 
     * @throws MessageStorageException if delete operation fails
     */
    @Transactional
    public void clearMessages() {
        try {
            long deletedCount = messageRepository.count();
            messageRepository.deleteAll();
            logger.info("All messages cleared. Total deleted: {}", deletedCount);
        } catch (Exception e) {
            logger.error("Error clearing messages", e);
            throw new MessageStorageException("Failed to clear messages", e);
        }
    }

    /**
     * Get all messages with a specific status.
     * 
     * @param status the status to filter by
     * @return list of messages with the given status
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesByStatus(String status) {
        Objects.requireNonNull(status, "Status cannot be null");
        
        try {
            List<Message> messages = messageRepository.findByStatus(status);
            logger.debug("Retrieved {} messages with status '{}'", messages.size(), status);
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving messages by status: {}", status, e);
            throw new MessageStorageException("Failed to retrieve messages by status: " + status, e);
        }
    }

    /**
     * Get messages within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of messages within the date range
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesByDateRange(Date startDate, Date endDate) {
        Objects.requireNonNull(startDate, "Start date cannot be null");
        Objects.requireNonNull(endDate, "End date cannot be null");
        
        try {
            List<Message> messages = messageRepository.findMessagesByDateRange(startDate, endDate);
            logger.debug("Retrieved {} messages between {} and {}", messages.size(), startDate, endDate);
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving messages by date range", e);
            throw new MessageStorageException("Failed to retrieve messages by date range", e);
        }
    }

    /**
     * Search messages by content.
     * 
     * @param searchTerm the search term
     * @return list of messages matching the search term
     * @throws MessageStorageException if search fails
     */
    @Transactional(readOnly = true)
    public List<Message> searchMessages(String searchTerm) {
        Objects.requireNonNull(searchTerm, "Search term cannot be null");
        
        if (searchTerm.isBlank()) {
            throw new InvalidMessageException("Search term cannot be blank");
        }
        
        try {
            List<Message> messages = messageRepository.searchByContent(searchTerm);
            logger.debug("Found {} messages matching search term: {}", messages.size(), searchTerm);
            return messages;
        } catch (Exception e) {
            logger.error("Error searching messages", e);
            throw new MessageStorageException("Failed to search messages", e);
        }
    }

    /**
     * Delete a message by ID.
     * 
     * @param id the message ID
     * @return true if message was deleted, false if not found
     * @throws MessageStorageException if delete operation fails
     */
    @Transactional
    public boolean deleteMessageById(String id) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        
        try {
            if (messageRepository.existsById(id)) {
                messageRepository.deleteById(id);
                logger.info("Message deleted: {}", id);
                return true;
            } else {
                logger.debug("Message not found for deletion: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting message: {}", id, e);
            throw new MessageStorageException("Failed to delete message: " + id, e);
        }
    }

    /**
     * Update a message.
     * 
     * @param message the message to update
     * @return the updated message
     * @throws MessageStorageException if update operation fails
     */
    @Transactional
    public Message updateMessage(Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        try {
            message.setUpdatedAt(new Date());
            Message updated = messageRepository.save(message);
            logger.info("Message updated: {}", message.getId());
            return updated;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid message object: {}", message.getId(), e);
            throw new InvalidMessageException("Invalid message: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error updating message: {}", message.getId(), e);
            throw new MessageStorageException("Failed to update message: " + message.getId(), e);
        }
    }

    /**
     * Check if a message exists by ID.
     * 
     * @param id the message ID
     * @return true if message exists, false otherwise
     * @throws MessageStorageException if check operation fails
     */
    @Transactional(readOnly = true)
    public boolean messageExists(String id) {
        Objects.requireNonNull(id, "Message ID cannot be null");
        
        try {
            return messageRepository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking message existence: {}", id, e);
            throw new MessageStorageException("Failed to check message existence: " + id, e);
        }
    }

    /**
     * Get all processed messages.
     * 
     * @return list of processed messages
     * @throws MessageStorageException if retrieval fails
     */
    @Transactional(readOnly = true)
    public List<Message> getAllProcessedMessages() {
        try {
            List<Message> messages = messageRepository.findAllProcessedMessages();
            logger.debug("Retrieved {} processed messages", messages.size());
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving processed messages", e);
            throw new MessageStorageException("Failed to retrieve processed messages", e);
        }
    }
}
