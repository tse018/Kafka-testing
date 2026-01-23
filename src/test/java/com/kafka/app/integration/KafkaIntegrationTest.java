package com.kafka.app.integration;

import com.kafka.app.consumer.KafkaConsumer;
import com.kafka.app.model.Message;
import com.kafka.app.producer.KafkaProducer;
import com.kafka.app.service.MessageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "advertised.listeners=PLAINTEXT://localhost:9092"
        }
)
@DirtiesContext
@DisplayName("Kafka Integration Tests - Producer to Consumer Flow")
class KafkaIntegrationTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    @SuppressWarnings("unused")
    private KafkaConsumer kafkaConsumer;  // Used implicitly through message consumption flow

    @Autowired
    private MessageStorageService messageStorageService;

    @Test
    @DisplayName("Should send message through Kafka and consume it")
    void testProducerToConsumerFlow() {
        // Arrange
        String testMessage = "Integration test message";
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage(testMessage);

        // Assert - Wait for async consumption
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
                });

        Message storedMessage = messageStorageService.getAllMessages().get(0);
        assertThat(storedMessage.getContent()).isEqualTo(testMessage);
        assertThat(storedMessage.getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Should process multiple messages in sequence")
    void testMultipleMessagesFlow() {
        // Arrange
        String[] messages = {"Message 1", "Message 2", "Message 3"};
        messageStorageService.clearMessages();

        // Act
        for (String msg : messages) {
            kafkaProducer.sendMessage(msg);
        }

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(3);
                });

        assertThat(messageStorageService.getAllMessages())
                .hasSize(3)
                .extracting(Message::getContent)
                .containsExactly("Message 1", "Message 2", "Message 3");
    }

    @Test
    @DisplayName("Should preserve message content through Kafka")
    void testMessageContentPreservation() {
        // Arrange
        String complexMessage = "Test!@#$%^&*()_+-=[]{}|;':\",./<>? 中文";
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage(complexMessage);

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
                    assertThat(messageStorageService.getAllMessages().get(0).getContent())
                            .isEqualTo(complexMessage);
                });
    }

    @Test
    @DisplayName("Should set message timestamp on consumption")
    void testMessageTimestampOnConsumption() {
        // Arrange
        long beforeSend = System.currentTimeMillis();
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage("Test message");
        long afterSend = System.currentTimeMillis();

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
                    Message message = messageStorageService.getAllMessages().get(0);
                    assertThat(message.getTimestamp())
                            .isGreaterThanOrEqualTo(beforeSend)
                            .isLessThanOrEqualTo(afterSend + 1000);
                });
    }

    @Test
    @DisplayName("Should generate UUID for each consumed message")
    void testUUIDGenerationOnConsumption() {
        // Arrange
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage("Message 1");
        kafkaProducer.sendMessage("Message 2");

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(2);
                    assertThat(messageStorageService.getAllMessages())
                            .extracting(Message::getId)
                            .allMatch(id -> id != null && !id.isEmpty());
                });
    }

    @Test
    @DisplayName("Should handle empty message string")
    void testEmptyMessageFlow() {
        // Arrange
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage("");

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
                    assertThat(messageStorageService.getAllMessages().get(0).getContent()).isEmpty();
                });
    }

    @Test
    @DisplayName("Should handle large message payload")
    void testLargeMessageFlow() {
        // Arrange
        String largeMessage = "x".repeat(50000);
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage(largeMessage);

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);
                    assertThat(messageStorageService.getAllMessages().get(0).getContent())
                            .hasSize(50000);
                });
    }

    @Test
    @DisplayName("Should maintain message order within same partition")
    void testMessageOrdering() {
        // Arrange
        messageStorageService.clearMessages();
        String[] messages = new String[10];
        for (int i = 0; i < 10; i++) {
            messages[i] = "Message " + i;
        }

        // Act
        for (String msg : messages) {
            kafkaProducer.sendMessage(msg);
        }

        // Assert
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(10);
                    assertThat(messageStorageService.getAllMessages())
                            .extracting(Message::getContent)
                            .containsExactly(
                                    "Message 0", "Message 1", "Message 2", "Message 3", "Message 4",
                                    "Message 5", "Message 6", "Message 7", "Message 8", "Message 9"
                            );
                });
    }

    @Test
    @DisplayName("Should set PROCESSED status for all consumed messages")
    void testAllMessagesHaveProcessedStatus() {
        // Arrange
        messageStorageService.clearMessages();

        // Act
        kafkaProducer.sendMessage("Message 1");
        kafkaProducer.sendMessage("Message 2");
        kafkaProducer.sendMessage("Message 3");

        // Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getAllMessages())
                            .hasSize(3)
                            .extracting(Message::getStatus)
                            .containsOnly("PROCESSED");
                });
    }

    @Test
    @DisplayName("Should handle rapid successive message sends")
    void testRapidMessageSends() {
        // Arrange
        messageStorageService.clearMessages();
        int messageCount = 20;

        // Act
        for (int i = 0; i < messageCount; i++) {
            kafkaProducer.sendMessage("Rapid message " + i);
        }

        // Assert
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(messageCount);
                });
    }

    @Test
    @DisplayName("Should retrieve message by ID after Kafka processing")
    void testRetrieveMessageByIdAfterKafkaProcessing() {
        // Arrange
        messageStorageService.clearMessages();
        kafkaProducer.sendMessage("Retrievable message");

        // Act & Assert
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(messageStorageService.getMessageCount()).isEqualTo(1);

                    Message message = messageStorageService.getAllMessages().get(0);
                    String messageId = message.getId();

                    Message retrievedMessage = messageStorageService.getMessageById(messageId);
                    assertThat(retrievedMessage)
                            .isNotNull()
                            .isEqualTo(message)
                            .extracting(Message::getContent)
                            .isEqualTo("Retrievable message");
                });
    }

    @Test
    @DisplayName("Should handle clearMessages between tests")
    void testClearMessagesResets() {
        // Arrange
        kafkaProducer.sendMessage("Message 1");

        // Act & Assert - Wait for first message
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(messageStorageService.getMessageCount()).isGreaterThan(0));

        // Clear and verify reset
        messageStorageService.clearMessages();
        assertThat(messageStorageService.getMessageCount()).isEqualTo(0);

        // Send another message and verify independent count
        kafkaProducer.sendMessage("Message 2");

        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(messageStorageService.getMessageCount()).isEqualTo(1));
    }
}
