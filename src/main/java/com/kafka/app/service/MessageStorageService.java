package com.kafka.app.service;

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
import java.util.Optional;

/**
 * Service for managing message storage operations.
 * Uses Spring Data JPA to persist messages to the database.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MessageStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageStorageService.class);
    
    private final MessageRepository messageRepository;

    /**
     * Add a message to the database.
     * 
     * @param message the message to add
     */
    public void addMessage(Message message) {
        try {
            messageRepository.save(message);
            logger.debug("Message saved successfully: {}", message.getId());
        } catch (Exception e) {
            logger.error("Error saving message: {}", message.getId(), e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    /**
     * Retrieve all messages from the database.
     * 
     * @return list of all messages
     */
    @Transactional(readOnly = true)
    public List<Message> getAllMessages() {
        try {
            List<Message> messages = messageRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            logger.debug("Retrieved {} messages from database", messages.size());
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving all messages", e);
            throw new RuntimeException("Failed to retrieve messages", e);
        }
    }

    /**
     * Retrieve all messages with pagination.
     * 
     * @param pageNumber the page number (0-indexed)
     * @param pageSize the page size
     * @return paginated messages
     */
    @Transactional(readOnly = true)
    public Page<Message> getAllMessagesPaginated(int pageNumber, int pageSize) {
        try {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Message> messages = messageRepository.findAll(pageable);
            logger.debug("Retrieved page {} with {} messages", pageNumber, messages.getNumberOfElements());
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving paginated messages", e);
            throw new RuntimeException("Failed to retrieve paginated messages", e);
        }
    }

    /**
     * Get a message by ID.
     * 
     * @param id the message ID
     * @return the message or null if not found
     */
    @Transactional(readOnly = true)
    public Message getMessageById(String id) {
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
            throw new RuntimeException("Failed to retrieve message", e);
        }
    }

    /**
     * Get count of all messages.
     * 
     * @return the total number of messages
     */
    @Transactional(readOnly = true)
    public int getMessageCount() {
        try {
            long count = messageRepository.count();
            logger.debug("Total message count: {}", count);
            return (int) count;
        } catch (Exception e) {
            logger.error("Error counting messages", e);
            throw new RuntimeException("Failed to count messages", e);
        }
    }

    /**
     * Get count of messages by status.
     * 
     * @param status the status to count
     * @return the number of messages with the given status
     */
    @Transactional(readOnly = true)
    public long getMessageCountByStatus(String status) {
        try {
            long count = messageRepository.countByStatus(status);
            logger.debug("Message count for status '{}': {}", status, count);
            return count;
        } catch (Exception e) {
            logger.error("Error counting messages by status: {}", status, e);
            throw new RuntimeException("Failed to count messages by status", e);
        }
    }

    /**
     * Clear all messages from the database.
     */
    public void clearMessages() {
        try {
            long deletedCount = messageRepository.count();
            messageRepository.deleteAll();
            logger.info("All messages cleared. Total deleted: {}", deletedCount);
        } catch (Exception e) {
            logger.error("Error clearing messages", e);
            throw new RuntimeException("Failed to clear messages", e);
        }
    }

    /**
     * Get all messages with a specific status.
     * 
     * @param status the status to filter by
     * @return list of messages with the given status
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesByStatus(String status) {
        try {
            List<Message> messages = messageRepository.findByStatus(status);
            logger.debug("Retrieved {} messages with status '{}'", messages.size(), status);
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving messages by status: {}", status, e);
            throw new RuntimeException("Failed to retrieve messages by status", e);
        }
    }

    /**
     * Get messages within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of messages within the date range
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesByDateRange(Date startDate, Date endDate) {
        try {
            List<Message> messages = messageRepository.findMessagesByDateRange(startDate, endDate);
            logger.debug("Retrieved {} messages between {} and {}", messages.size(), startDate, endDate);
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving messages by date range", e);
            throw new RuntimeException("Failed to retrieve messages by date range", e);
        }
    }

    /**
     * Search messages by content.
     * 
     * @param searchTerm the search term
     * @return list of messages matching the search term
     */
    @Transactional(readOnly = true)
    public List<Message> searchMessages(String searchTerm) {
        try {
            List<Message> messages = messageRepository.searchByContent(searchTerm);
            logger.debug("Found {} messages matching search term: {}", messages.size(), searchTerm);
            return messages;
        } catch (Exception e) {
            logger.error("Error searching messages", e);
            throw new RuntimeException("Failed to search messages", e);
        }
    }

    /**
     * Delete a message by ID.
     * 
     * @param id the message ID
     * @return true if message was deleted, false if not found
     */
    public boolean deleteMessageById(String id) {
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
            throw new RuntimeException("Failed to delete message", e);
        }
    }

    /**
     * Update a message.
     * 
     * @param message the message to update
     * @return the updated message
     */
    public Message updateMessage(Message message) {
        try {
            message.setUpdatedAt(new Date());
            Message updated = messageRepository.save(message);
            logger.info("Message updated: {}", message.getId());
            return updated;
        } catch (Exception e) {
            logger.error("Error updating message: {}", message.getId(), e);
            throw new RuntimeException("Failed to update message", e);
        }
    }

    /**
     * Check if a message exists by ID.
     * 
     * @param id the message ID
     * @return true if message exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean messageExists(String id) {
        try {
            return messageRepository.existsById(id);
        } catch (Exception e) {
            logger.error("Error checking message existence: {}", id, e);
            throw new RuntimeException("Failed to check message existence", e);
        }
    }

    /**
     * Get all processed messages.
     * 
     * @return list of processed messages
     */
    @Transactional(readOnly = true)
    public List<Message> getAllProcessedMessages() {
        try {
            List<Message> messages = messageRepository.findAllProcessedMessages();
            logger.debug("Retrieved {} processed messages", messages.size());
            return messages;
        } catch (Exception e) {
            logger.error("Error retrieving processed messages", e);
            throw new RuntimeException("Failed to retrieve processed messages", e);
        }
    }
}
