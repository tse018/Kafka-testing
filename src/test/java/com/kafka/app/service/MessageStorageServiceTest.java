package com.kafka.app.service;

import com.kafka.app.model.Message;
import com.kafka.app.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MessageStorageService with actual database.
 * Uses @DataJpaTest for repository layer testing.
 */
@DataJpaTest
@DisplayName("MessageStorageService Database Integration Tests")
class MessageStorageServiceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MessageStorageService messageStorageService(MessageRepository messageRepository) {
            return new MessageStorageService(messageRepository);
        }
    }

    @Autowired
    private MessageRepository messageRepository;

    private MessageStorageService messageStorageService;

    @BeforeEach
    void setUp() {
        messageStorageService = new MessageStorageService(messageRepository);
        messageRepository.deleteAll();
    }

    @Test
    @DisplayName("Should add message to database")
    void testAddMessage() {
        // Arrange
        Message message = new Message("id1", "Test content", System.currentTimeMillis(), "PROCESSED", null, null);

        // Act
        messageStorageService.addMessage(message);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
        assertThat(messageRepository.existsById("id1")).isTrue();
    }

    @Test
    @DisplayName("Should retrieve all messages from database")
    void testGetAllMessages() {
        // Arrange
        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null);
        Message message2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED", null, null);

        messageStorageService.addMessage(message1);
        messageStorageService.addMessage(message2);

        // Act
        List<Message> messages = messageStorageService.getAllMessages();

        // Assert
        assertThat(messages).hasSize(2).extracting(Message::getId).contains("id1", "id2");
    }

    @Test
    @DisplayName("Should retrieve message by ID from database")
    void testGetMessageById() {
        // Arrange
        Message message = new Message("id123", "Test content", System.currentTimeMillis(), "PROCESSED", null, null);
        messageStorageService.addMessage(message);

        // Act
        Message retrieved = messageStorageService.getMessageById("id123");

        // Assert
        assertThat(retrieved).isNotNull().extracting(Message::getId).isEqualTo("id123");
    }

    @Test
    @DisplayName("Should return null when message not found")
    void testGetMessageByIdNotFound() {
        // Act
        Message retrieved = messageStorageService.getMessageById("nonexistent");

        // Assert
        assertThat(retrieved).isNull();
    }

    @Test
    @DisplayName("Should return correct message count")
    void testGetMessageCount() {
        // Arrange
        assertThat(messageStorageService.getMessageCount()).isEqualTo(0);

        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null);
        Message message2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED", null, null);
        Message message3 = new Message("id3", "Content 3", System.currentTimeMillis(), "PROCESSED", null, null);

        messageStorageService.addMessage(message1);
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);

        messageStorageService.addMessage(message2);
        assertThat(messageStorageService.getMessageCount()).isEqualTo(2);

        messageStorageService.addMessage(message3);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should clear all messages from database")
    void testClearMessages() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED", null, null));
        assertThat(messageStorageService.getMessageCount()).isEqualTo(2);

        // Act
        messageStorageService.clearMessages();

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(0);
        assertThat(messageStorageService.getAllMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no messages")
    void testGetAllMessagesEmpty() {
        // Act
        List<Message> messages = messageStorageService.getAllMessages();

        // Assert
        assertThat(messages).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should get messages by status")
    void testGetMessagesByStatus() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id2", "Content 2", System.currentTimeMillis(), "PENDING", null, null));
        messageStorageService.addMessage(new Message("id3", "Content 3", System.currentTimeMillis(), "PROCESSED", null, null));

        // Act
        List<Message> processed = messageStorageService.getMessagesByStatus("PROCESSED");
        List<Message> pending = messageStorageService.getMessagesByStatus("PENDING");

        // Assert
        assertThat(processed).hasSize(2);
        assertThat(pending).hasSize(1).extracting(Message::getStatus).containsOnly("PENDING");
    }

    @Test
    @DisplayName("Should count messages by status")
    void testGetMessageCountByStatus() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id3", "Content 3", System.currentTimeMillis(), "FAILED", null, null));

        // Act
        long processedCount = messageStorageService.getMessageCountByStatus("PROCESSED");
        long failedCount = messageStorageService.getMessageCountByStatus("FAILED");

        // Assert
        assertThat(processedCount).isEqualTo(2);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search messages by content")
    void testSearchMessages() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Important data", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id2", "Other data", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id3", "Important event", System.currentTimeMillis(), "PROCESSED", null, null));

        // Act
        List<Message> results = messageStorageService.searchMessages("Important");

        // Assert
        assertThat(results).hasSize(2).extracting(Message::getId).contains("id1", "id3");
    }

    @Test
    @DisplayName("Should delete message by ID")
    void testDeleteMessageById() {
        // Arrange
        Message message = new Message("id1", "Content", System.currentTimeMillis(), "PROCESSED", null, null);
        messageStorageService.addMessage(message);
        assertThat(messageStorageService.messageExists("id1")).isTrue();

        // Act
        boolean deleted = messageStorageService.deleteMessageById("id1");

        // Assert
        assertThat(deleted).isTrue();
        assertThat(messageStorageService.messageExists("id1")).isFalse();
    }

    @Test
    @DisplayName("Should return false when deleting non-existent message")
    void testDeleteMessageByIdNotFound() {
        // Act
        boolean deleted = messageStorageService.deleteMessageById("nonexistent");

        // Assert
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Should update message")
    void testUpdateMessage() {
        // Arrange
        Message message = new Message("id1", "Original content", System.currentTimeMillis(), "PROCESSED", null, null);
        messageStorageService.addMessage(message);

        // Act
        message.setContent("Updated content");
        Message updated = messageStorageService.updateMessage(message);

        // Assert
        assertThat(updated.getContent()).isEqualTo("Updated content");
        assertThat(messageStorageService.getMessageById("id1").getContent()).isEqualTo("Updated content");
    }

    @Test
    @DisplayName("Should check if message exists")
    void testMessageExists() {
        // Arrange
        Message message = new Message("id1", "Content", System.currentTimeMillis(), "PROCESSED", null, null);
        
        // Act & Assert
        assertThat(messageStorageService.messageExists("id1")).isFalse();
        messageStorageService.addMessage(message);
        assertThat(messageStorageService.messageExists("id1")).isTrue();
    }

    @Test
    @DisplayName("Should get all processed messages")
    void testGetAllProcessedMessages() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED", null, null));
        messageStorageService.addMessage(new Message("id2", "Content 2", System.currentTimeMillis(), "FAILED", null, null));
        messageStorageService.addMessage(new Message("id3", "Content 3", System.currentTimeMillis(), "PROCESSED", null, null));

        // Act
        List<Message> processed = messageStorageService.getAllProcessedMessages();

        // Assert
        assertThat(processed).hasSize(2).extracting(Message::getStatus).containsOnly("PROCESSED");
    }

    @Test
    @DisplayName("Should retrieve paginated messages")
    void testGetAllMessagesPaginated() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            messageStorageService.addMessage(
                new Message("id" + i, "Content " + i, System.currentTimeMillis(), "PROCESSED", null, null)
            );
        }

        // Act
        Page<Message> page0 = messageStorageService.getAllMessagesPaginated(0, 10);
        Page<Message> page1 = messageStorageService.getAllMessagesPaginated(1, 10);

        // Assert
        assertThat(page0.getNumberOfElements()).isEqualTo(10);
        assertThat(page0.getTotalElements()).isEqualTo(15);
        assertThat(page1.getNumberOfElements()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle large number of messages")
    void testLargeNumberOfMessages() {
        // Arrange & Act
        for (int i = 0; i < 100; i++) {
            messageStorageService.addMessage(
                new Message("id" + i, "Content " + i, System.currentTimeMillis(), "PROCESSED", null, null)
            );
        }

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(100);
        assertThat(messageStorageService.getMessageById("id50")).isNotNull();
    }

    @Test
    @DisplayName("Should preserve message content exactly")
    void testPreserveMessageContent() {
        // Arrange
        String originalContent = "  Leading and trailing spaces  \nNewline\tTab";
        Message message = new Message("id1", originalContent, System.currentTimeMillis(), "PROCESSED", null, null);

        // Act
        messageStorageService.addMessage(message);
        Message retrieved = messageStorageService.getMessageById("id1");

        // Assert
        assertThat(retrieved.getContent()).isEqualTo(originalContent);
    }
}
