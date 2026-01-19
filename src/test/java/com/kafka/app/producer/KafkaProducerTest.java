package com.kafka.app.producer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaProducer Unit Tests")
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaProducer kafkaProducer;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaProducer = new KafkaProducer(kafkaTemplate, meterRegistry);
    }

    @Test
    @DisplayName("Should send message successfully")
    void testSendMessageSuccess() {
        // Arrange
        String testMessage = "Test message";
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage(testMessage);

        // Assert
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(1)).send(messageCaptor.capture());

        Message<?> capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getPayload()).isEqualTo(testMessage);
        assertThat(capturedMessage.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo("messages");

        // Verify counter incremented
        assertThat(meterRegistry.find("kafka.messages.sent").counter()).isNotNull();
        assertThat(meterRegistry.find("kafka.messages.sent").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle empty message")
    void testSendEmptyMessage() {
        // Arrange
        String emptyMessage = "";
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage(emptyMessage);

        // Assert
        verify(kafkaTemplate, times(1)).send(any(Message.class));
        assertThat(meterRegistry.find("kafka.messages.sent").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle long message")
    void testSendLongMessage() {
        // Arrange
        String longMessage = "a".repeat(10000);
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage(longMessage);

        // Assert
        verify(kafkaTemplate, times(1)).send(any(Message.class));
        assertThat(meterRegistry.find("kafka.messages.sent").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should throw RuntimeException when send fails")
    void testSendMessageFailure() {
        // Arrange
        String testMessage = "Test message";
        doThrow(new RuntimeException("Kafka connection failed"))
                .when(kafkaTemplate).send(any(Message.class));

        // Act & Assert
        assertThatThrownBy(() -> kafkaProducer.sendMessage(testMessage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send message to Kafka");

        // Verify failure counter incremented
        assertThat(meterRegistry.find("kafka.messages.failed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment sent counter on successful send")
    void testCounterIncrementOnSuccess() {
        // Arrange
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage("Message 1");
        kafkaProducer.sendMessage("Message 2");
        kafkaProducer.sendMessage("Message 3");

        // Assert
        assertThat(meterRegistry.find("kafka.messages.sent").counter().count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should increment failed counter on each failure")
    void testCounterIncrementOnFailure() {
        // Arrange
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate).send(any(Message.class));

        // Act & Assert
        assertThatThrownBy(() -> kafkaProducer.sendMessage("Message 1"));
        assertThatThrownBy(() -> kafkaProducer.sendMessage("Message 2"));

        // Verify failure counter
        assertThat(meterRegistry.find("kafka.messages.failed").counter().count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should set correct topic header")
    void testTopicHeaderIsSet() {
        // Arrange
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage("Test");

        // Assert
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        Message<?> message = messageCaptor.getValue();
        assertThat(message.getHeaders().get(KafkaHeaders.TOPIC))
                .isEqualTo("messages");
    }

    @Test
    @DisplayName("Should send multiple messages independently")
    void testSendMultipleMessages() {
        // Arrange
        doNothing().when(kafkaTemplate).send(any(Message.class));
        String[] messages = {"Message1", "Message2", "Message3"};

        // Act
        for (String msg : messages) {
            kafkaProducer.sendMessage(msg);
        }

        // Assert
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate, times(3)).send(messageCaptor.capture());

        assertThat(messageCaptor.getAllValues())
                .hasSize(3)
                .extracting(Message::getPayload)
                .containsExactly("Message1", "Message2", "Message3");
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void testSendMessageWithSpecialCharacters() {
        // Arrange
        String specialMessage = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?";
        doNothing().when(kafkaTemplate).send(any(Message.class));

        // Act
        kafkaProducer.sendMessage(specialMessage);

        // Assert
        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getPayload()).isEqualTo(specialMessage);
    }

    @Test
    @DisplayName("Should handle null exception gracefully")
    void testSendMessageWithNullPointerException() {
        // Arrange
        doThrow(new NullPointerException("Null value"))
                .when(kafkaTemplate).send(any(Message.class));

        // Act & Assert
        assertThatThrownBy(() -> kafkaProducer.sendMessage("Message"))
                .isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.find("kafka.messages.failed").counter().count()).isEqualTo(1.0);
    }
}
