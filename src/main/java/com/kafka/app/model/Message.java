package com.kafka.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_status", columnList = "status"),
    @Index(name = "idx_message_timestamp", columnList = "timestamp")
})
public class Message {
    
    @Id
    @Column(name = "id", length = 36)
    @JsonProperty("id")
    private String id;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    @JsonProperty("content")
    private String content;
    
    @Column(name = "timestamp", nullable = false)
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private long timestamp;
    
    @Column(name = "status", length = 50, nullable = false)
    @JsonProperty("status")
    private String status;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
        updatedAt = new java.util.Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.util.Date();
    }
}
