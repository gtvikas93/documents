package com.wellsfargo.chat.model;

import lombok.Data;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSession {
    private String sessionId;
    private Date createdAt;
    private Date lastAccessedAt;
    private List<ChatMessage> messages;
    private boolean active;

    public ChatSession() {
        this.messages = new ArrayList<>();
        this.createdAt = new Date();
        this.lastAccessedAt = new Date();
        this.active = true;
    }
} 