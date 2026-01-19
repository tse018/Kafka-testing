package com.kafka.app.service;

import com.kafka.app.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MessageStorageService Unit Tests")
class MessageStorageServiceTest {

    private MessageStorageService messageStorageService;

    @BeforeEach
    void setUp() {
        messageStorageService = new MessageStorageService();
    }

    @Test
    @DisplayName("Should add message to storage")
    void testAddMessage() {
        // Arrange
        Message message = new Message("id1", "Test content", System.currentTimeMillis(), "PROCESSED");

        // Act
        messageStorageService.addMessage(message);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should retrieve all messages")
    void testGetAllMessages() {
        // Arrange
        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        Message message2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED");

        messageStorageService.addMessage(message1);
        messageStorageService.addMessage(message2);

        // Act
        List<Message> messages = messageStorageService.getAllMessages();

        // Assert
        assertThat(messages)
                .hasSize(2)
                .contains(message1, message2);
    }

    @Test
    @DisplayName("Should retrieve message by ID")
    void testGetMessageById() {
        // Arrange
        Message message = new Message("id123", "Test content", System.currentTimeMillis(), "PROCESSED");
        messageStorageService.addMessage(message);

        // Act
        Message retrieved = messageStorageService.getMessageById("id123");

        // Assert
        assertThat(retrieved)
                .isNotNull()
                .isEqualTo(message);
    }

    @Test
    @DisplayName("Should return null when message not found by ID")
    void testGetMessageByIdNotFound() {
        // Arrange
        Message message = new Message("id1", "Test", System.currentTimeMillis(), "PROCESSED");
        messageStorageService.addMessage(message);

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

        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        Message message2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED");
        Message message3 = new Message("id3", "Content 3", System.currentTimeMillis(), "PROCESSED");

        // Act
        messageStorageService.addMessage(message1);
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);

        messageStorageService.addMessage(message2);
        assertThat(messageStorageService.getMessageCount()).isEqualTo(2);

        messageStorageService.addMessage(message3);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should clear all messages")
    void testClearMessages() {
        // Arrange
        messageStorageService.addMessage(new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED"));
        messageStorageService.addMessage(new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED"));
        assertThat(messageStorageService.getMessageCount()).isEqualTo(2);

        // Act
        messageStorageService.clearMessages();

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(0);
        assertThat(messageStorageService.getAllMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no messages stored")
    void testGetAllMessagesEmpty() {
        // Act
        List<Message> messages = messageStorageService.getAllMessages();

        // Assert
        assertThat(messages)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple adds of same message object")
    void testAddDuplicateMessage() {
        // Arrange
        Message message = new Message("id1", "Content", System.currentTimeMillis(), "PROCESSED");

        // Act
        messageStorageService.addMessage(message);
        messageStorageService.addMessage(message);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should be thread-safe with CopyOnWriteArrayList")
    void testThreadSafety() throws InterruptedException {
        // Arrange
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                messageStorageService.addMessage(
                        new Message("id" + i, "Content" + i, System.currentTimeMillis(), "PROCESSED")
                );
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 100; i < 200; i++) {
                messageStorageService.addMessage(
                        new Message("id" + i, "Content" + i, System.currentTimeMillis(), "PROCESSED")
                );
            }
        });

        // Act
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should return independent copy of messages list")
    void testGetAllMessagesReturnsIndependentCopy() {
        // Arrange
        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        messageStorageService.addMessage(message1);

        // Act
        List<Message> messages = messageStorageService.getAllMessages();
        messages.clear();

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find message by exact ID match")
    void testGetMessageByIdExactMatch() {
        // Arrange
        Message msg1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        Message msg2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED");
        Message msg3 = new Message("id3", "Content 3", System.currentTimeMillis(), "PROCESSED");

        messageStorageService.addMessage(msg1);
        messageStorageService.addMessage(msg2);
        messageStorageService.addMessage(msg3);

        // Act
        Message retrieved = messageStorageService.getMessageById("id2");

        // Assert
        assertThat(retrieved).isEqualTo(msg2);
        assertThat(retrieved.getContent()).isEqualTo("Content 2");
    }

    @Test
    @DisplayName("Should store message with all properties")
    void testAddMessagePreservesAllProperties() {
        // Arrange
        String id = "test-id";
        String content = "Test content with special chars: !@#$%^&*()";
        long timestamp = System.currentTimeMillis();
        String status = "PROCESSED";
        Message message = new Message(id, content, timestamp, status);

        // Act
        messageStorageService.addMessage(message);
        Message retrieved = messageStorageService.getMessageById(id);

        // Assert
        assertThat(retrieved)
                .isNotNull()
                .extracting(Message::getId, Message::getContent, Message::getStatus)
                .containsExactly(id, content, status);
        assertThat(retrieved.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should handle large number of messages")
    void testLargeNumberOfMessages() {
        // Arrange & Act
        for (int i = 0; i < 1000; i++) {
            messageStorageService.addMessage(
                    new Message("id" + i, "Content" + i, System.currentTimeMillis(), "PROCESSED")
            );
        }

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1000);
        assertThat(messageStorageService.getMessageById("id500")).isNotNull();
    }

    @Test
    @DisplayName("Should clear and allow re-adding messages")
    void testClearAndReAdd() {
        // Arrange
        Message message1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        messageStorageService.addMessage(message1);

        // Act
        messageStorageService.clearMessages();
        Message message2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED");
        messageStorageService.addMessage(message2);

        // Assert
        assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
        assertThat(messageStorageService.getMessageById("id1")).isNull();
        assertThat(messageStorageService.getMessageById("id2")).isNotNull();
    }
}
