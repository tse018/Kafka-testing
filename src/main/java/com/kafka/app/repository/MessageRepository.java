package com.kafka.app.repository;

import com.kafka.app.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Message entity.
 * Provides database operations for Message CRUD and custom queries.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /**
     * Find all messages with a specific status.
     * 
     * @param status the status to filter by
     * @return list of messages with the given status
     */
    List<Message> findByStatus(String status);

    /**
     * Find messages within a date range.
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @return list of messages created within the date range
     */
    @Query("SELECT m FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<Message> findMessagesByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * Find all messages with pagination.
     * 
     * @param pageable pagination information
     * @return paginated list of messages
     */
    Page<Message> findAll(Pageable pageable);

    /**
     * Find all messages by status with pagination.
     * 
     * @param status the status to filter by
     * @param pageable pagination information
     * @return paginated list of messages with the given status
     */
    Page<Message> findByStatus(String status, Pageable pageable);

    /**
     * Find all processed messages.
     * 
     * @return list of processed messages
     */
    @Query("SELECT m FROM Message m WHERE m.status = 'PROCESSED' ORDER BY m.timestamp DESC")
    List<Message> findAllProcessedMessages();

    /**
     * Find messages by content search.
     * 
     * @param searchTerm the search term
     * @return list of messages matching the search term
     */
    @Query("SELECT m FROM Message m WHERE LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Message> searchByContent(@Param("searchTerm") String searchTerm);

    /**
     * Count messages by status.
     * 
     * @param status the status to count
     * @return count of messages with the given status
     */
    long countByStatus(String status);

    /**
     * Check if a message exists by ID.
     * 
     * @param id the message ID
     * @return true if message exists, false otherwise
     */
    boolean existsById(String id);

    /**
     * Delete all messages with a specific status.
     * 
     * @param status the status to delete
     */
    void deleteByStatus(String status);

    /**
     * Find the most recent message.
     * 
     * @return the most recent message, or empty if no messages exist
     */
    @Query(value = "SELECT m FROM Message m ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findMostRecent();
}
