package com.kafka.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending messages via Kafka.
 * Includes validation constraints to ensure message quality.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    
    @JsonProperty("message")
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 10000, message = "Message must be between 1 and 10000 characters")
    private String message;
}
