package com.wellsfargo.chat.service;

import com.wellsfargo.chat.model.ChatMessage;
import com.wellsfargo.chat.model.ChatSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ChatSessionService {
    private final ConcurrentHashMap<String, ChatSession> sessions;
    private final int sessionTimeoutMinutes;
    private final int maxMessages;
    private final ScheduledExecutorService scheduler;

    public ChatSessionService(
            @Value("${session.timeout.minutes:30}") int sessionTimeoutMinutes,
            @Value("${session.max.messages:100}") int maxMessages) {
        this.sessions = new ConcurrentHashMap<>();
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        this.maxMessages = maxMessages;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Schedule cleanup task
        scheduler.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            1,
            1,
            TimeUnit.MINUTES
        );
    }

    public ChatSession createSession() {
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    public ChatSession getSession(String sessionId) {
        ChatSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessedAt(new Date());
        }
        return session;
    }

    public void addMessage(String sessionId, ChatMessage message) {
        ChatSession session = getSession(sessionId);
        if (session != null) {
            if (session.getMessages().size() >= maxMessages) {
                session.getMessages().remove(0);
            }
            session.getMessages().add(message);
            session.setLastAccessedAt(new Date());
        }
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void cleanupExpiredSessions() {
        Date now = new Date();
        sessions.entrySet().removeIf(entry -> {
            ChatSession session = entry.getValue();
            long lastAccessTime = session.getLastAccessedAt().getTime();
            long currentTime = now.getTime();
            return (currentTime - lastAccessTime) > (sessionTimeoutMinutes * 60 * 1000);
        });
    }

    public void shutdown() {
        scheduler.shutdown();
    }
} 