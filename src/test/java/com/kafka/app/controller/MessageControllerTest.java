package com.kafka.app.controller;

import com.kafka.app.dto.ApiResponse;
import com.kafka.app.dto.MessageRequest;
import com.kafka.app.model.Message;
import com.kafka.app.producer.KafkaProducer;
import com.kafka.app.service.MessageStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
@DisplayName("MessageController Unit Tests")
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KafkaProducer kafkaProducer;

    @MockBean
    private MessageStorageService messageStorageService;

    @Test
    @DisplayName("Should send message successfully via POST")
    void testSendMessageSuccess() throws Exception {
        // Arrange
        doNothing().when(kafkaProducer).sendMessage(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", "Test message")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("sent successfully")))
                .andExpect(jsonPath("$.data", is("Test message")));

        verify(kafkaProducer, times(1)).sendMessage("Test message");
    }

    @Test
    @DisplayName("Should return 500 when sending message fails")
    void testSendMessageFailure() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaProducer).sendMessage(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", "Test message")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Failed to send message")));

        verify(kafkaProducer, times(1)).sendMessage("Test message");
    }

    @Test
    @DisplayName("Should send JSON message successfully")
    void testSendJsonMessageSuccess() throws Exception {
        // Arrange
        doNothing().when(kafkaProducer).sendMessage(anyString());
        String jsonPayload = "{\"message\": \"Test JSON message\"}";

        // Act & Assert
        mockMvc.perform(post("/api/messages/send-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("sent successfully")));

        verify(kafkaProducer, times(1)).sendMessage("Test JSON message");
    }

    @Test
    @DisplayName("Should return 500 when sending JSON message fails")
    void testSendJsonMessageFailure() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaProducer).sendMessage(anyString());
        String jsonPayload = "{\"message\": \"Test message\"}";

        // Act & Assert
        mockMvc.perform(post("/api/messages/send-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));

        verify(kafkaProducer, times(1)).sendMessage(anyString());
    }

    @Test
    @DisplayName("Should retrieve all messages")
    void testGetAllMessages() throws Exception {
        // Arrange
        Message msg1 = new Message("id1", "Content 1", System.currentTimeMillis(), "PROCESSED");
        Message msg2 = new Message("id2", "Content 2", System.currentTimeMillis(), "PROCESSED");
        List<Message> messages = Arrays.asList(msg1, msg2);

        when(messageStorageService.getAllMessages()).thenReturn(messages);

        // Act & Assert
        mockMvc.perform(get("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("retrieved successfully")))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id", is("id1")))
                .andExpect(jsonPath("$.data[1].id", is("id2")));

        verify(messageStorageService, times(1)).getAllMessages();
    }

    @Test
    @DisplayName("Should return empty list when no messages stored")
    void testGetAllMessagesEmpty() throws Exception {
        // Arrange
        when(messageStorageService.getAllMessages()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(messageStorageService, times(1)).getAllMessages();
    }

    @Test
    @DisplayName("Should retrieve message by ID")
    void testGetMessageById() throws Exception {
        // Arrange
        Message message = new Message("id1", "Test content", System.currentTimeMillis(), "PROCESSED");
        when(messageStorageService.getMessageById("id1")).thenReturn(message);

        // Act & Assert
        mockMvc.perform(get("/api/messages/id1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("id1")))
                .andExpect(jsonPath("$.data.content", is("Test content")))
                .andExpect(jsonPath("$.data.status", is("PROCESSED")));

        verify(messageStorageService, times(1)).getMessageById("id1");
    }

    @Test
    @DisplayName("Should return 404 when message not found")
    void testGetMessageByIdNotFound() throws Exception {
        // Arrange
        when(messageStorageService.getMessageById("nonexistent")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/messages/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("not found")));

        verify(messageStorageService, times(1)).getMessageById("nonexistent");
    }

    @Test
    @DisplayName("Should handle exception when retrieving all messages")
    void testGetAllMessagesException() throws Exception {
        // Arrange
        when(messageStorageService.getAllMessages()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Failed to retrieve messages")));

        verify(messageStorageService, times(1)).getAllMessages();
    }

    @Test
    @DisplayName("Should handle exception when retrieving message by ID")
    void testGetMessageByIdException() throws Exception {
        // Arrange
        when(messageStorageService.getMessageById(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/messages/id1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)));

        verify(messageStorageService, times(1)).getMessageById(anyString());
    }

    @Test
    @DisplayName("Should handle empty message parameter")
    void testSendMessageWithEmptyParameter() throws Exception {
        // Arrange
        doNothing().when(kafkaProducer).sendMessage(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(kafkaProducer, times(1)).sendMessage("");
    }

    @Test
    @DisplayName("Should handle special characters in message")
    void testSendMessageWithSpecialCharacters() throws Exception {
        // Arrange
        String specialMessage = "Test!@#$%^&*()_+-=[]{}|;':\",./<>?";
        doNothing().when(kafkaProducer).sendMessage(specialMessage);

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", specialMessage)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(specialMessage)));

        verify(kafkaProducer, times(1)).sendMessage(specialMessage);
    }

    @Test
    @DisplayName("Should support CORS headers")
    void testCORSHeaders() throws Exception {
        // Arrange
        when(messageStorageService.getAllMessages()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/messages")
                        .header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should send message with long content")
    void testSendMessageWithLongContent() throws Exception {
        // Arrange
        String longMessage = "a".repeat(5000);
        doNothing().when(kafkaProducer).sendMessage(longMessage);

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", longMessage)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(kafkaProducer, times(1)).sendMessage(longMessage);
    }

    @Test
    @DisplayName("Should return API response with timestamp")
    void testApiResponseIncludesTimestamp() throws Exception {
        // Arrange
        doNothing().when(kafkaProducer).sendMessage(anyString());

        // Act & Assert
        mockMvc.perform(post("/api/messages/send")
                        .param("message", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", greaterThan(0)));
    }

    @Test
    @DisplayName("Should handle JSON request without message field")
    void testSendJsonWithoutMessageField() throws Exception {
        // Arrange
        String jsonPayload = "{}";

        // Act & Assert
        mockMvc.perform(post("/api/messages/send-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Should retrieve correct message content by ID")
    void testGetCorrectMessageContent() throws Exception {
        // Arrange
        String expectedContent = "Expected message content";
        Message message = new Message("id1", expectedContent, System.currentTimeMillis(), "PROCESSED");
        when(messageStorageService.getMessageById("id1")).thenReturn(message);

        // Act & Assert
        mockMvc.perform(get("/api/messages/id1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", is(expectedContent)));

        verify(messageStorageService, times(1)).getMessageById("id1");
    }
}
