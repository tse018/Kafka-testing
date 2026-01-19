package com.kafka.app.consumer;

import com.kafka.app.model.Message;
import com.kafka.app.service.MessageStorageService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaConsumer Unit Tests")
class KafkaConsumerTest {

    @Mock
    private MessageStorageService messageStorageService;

    private KafkaConsumer kafkaConsumer;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        kafkaConsumer = new KafkaConsumer(messageStorageService, meterRegistry);
    }

    @Test
    @DisplayName("Should consume message successfully")
    void testConsumeMessageSuccess() {
        // Arrange
        String testMessage = "Test message";
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume(testMessage);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService, times(1)).addMessage(messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getContent()).isEqualTo(testMessage);
        assertThat(capturedMessage.getStatus()).isEqualTo("PROCESSED");
        assertThat(capturedMessage.getId()).isNotNull();
        assertThat(capturedMessage.getTimestamp()).isGreaterThan(0);

        // Verify counter incremented
        assertThat(meterRegistry.find("kafka.messages.consumed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should generate UUID for consumed message")
    void testConsumeMessageGeneratesUUID() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("Message");

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        Message message = messageCaptor.getValue();
        assertThat(message.getId())
                .isNotNull()
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Should set message status to PROCESSED")
    void testMessageStatusIsProcessed() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("Message");

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    @DisplayName("Should set timestamp when consuming message")
    void testMessageTimestampIsSet() {
        // Arrange
        long beforeConsume = System.currentTimeMillis();
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("Message");
        long afterConsume = System.currentTimeMillis();

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        long messageTimestamp = messageCaptor.getValue().getTimestamp();
        assertThat(messageTimestamp)
                .isGreaterThanOrEqualTo(beforeConsume)
                .isLessThanOrEqualTo(afterConsume);
    }

    @Test
    @DisplayName("Should handle empty message")
    void testConsumeEmptyMessage() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("");

        // Assert
        verify(messageStorageService, times(1)).addMessage(any(Message.class));
        assertThat(meterRegistry.find("kafka.messages.consumed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should handle long message content")
    void testConsumeLongMessage() {
        // Arrange
        String longMessage = "a".repeat(10000);
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume(longMessage);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getContent()).isEqualTo(longMessage);
    }

    @Test
    @DisplayName("Should increment consumed counter on successful consumption")
    void testCounterIncrementOnSuccess() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("Message 1");
        kafkaConsumer.consume("Message 2");
        kafkaConsumer.consume("Message 3");

        // Assert
        assertThat(meterRegistry.find("kafka.messages.consumed").counter().count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should handle exception gracefully without throwing")
    void testConsumeMessageHandlesException() {
        // Arrange
        doThrow(new RuntimeException("Storage error"))
                .when(messageStorageService).addMessage(any(Message.class));

        // Act & Assert - Should not throw, logs the error
        assertThatNoException().isThrownBy(() -> kafkaConsumer.consume("Message"));

        // Note: The exception is caught and logged, so counter might not increment
        // depending on implementation
    }

    @Test
    @DisplayName("Should call MessageStorageService addMessage")
    void testCallsMessageStorageService() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume("Test");

        // Assert
        verify(messageStorageService, times(1)).addMessage(any(Message.class));
    }

    @Test
    @DisplayName("Should consume message with special characters")
    void testConsumeMessageWithSpecialCharacters() {
        // Arrange
        String specialMessage = "Test!@#$%^&*()_+-=[]{}|;':\",./<>? 中文 العربية";
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume(specialMessage);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getContent()).isEqualTo(specialMessage);
    }

    @Test
    @DisplayName("Should handle null messageStorageService response")
    void testConsumeWithNullResponse() {
        // Arrange
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(0);

        // Act
        kafkaConsumer.consume("Message");

        // Assert
        verify(messageStorageService, times(1)).addMessage(any(Message.class));
        assertThat(meterRegistry.find("kafka.messages.consumed").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should preserve message content exactly")
    void testConsumePreservesExactContent() {
        // Arrange
        String originalMessage = "  Leading and trailing spaces  \nNewline\tTab";
        doNothing().when(messageStorageService).addMessage(any(Message.class));
        when(messageStorageService.getMessageCount()).thenReturn(1);

        // Act
        kafkaConsumer.consume(originalMessage);

        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageStorageService).addMessage(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getContent()).isEqualTo(originalMessage);
    }
}
