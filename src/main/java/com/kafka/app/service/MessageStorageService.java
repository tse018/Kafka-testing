package com.kafka.app.service;

import com.kafka.app.model.Message;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MessageStorageService {
    
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    
    public void addMessage(Message message) {
        messages.add(message);
    }
    
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages);
    }
    
    public Message getMessageById(String id) {
        return messages.stream()
                .filter(msg -> msg.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public void clearMessages() {
        messages.clear();
    }
}
