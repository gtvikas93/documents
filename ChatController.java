package com.wellsfargo.chat.controller;

import com.wellsfargo.chat.model.ChatMessage;
import com.wellsfargo.chat.model.ChatSession;
import com.wellsfargo.chat.service.ChatSessionService;
import com.wellsfargo.chat.service.LLMService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {
    private final ChatSessionService chatSessionService;
    private final LLMService llmService;
    private final ExecutorService executorService;
    private final Map<String, Map<String, String>> sessionData;

    public ChatController(ChatSessionService chatSessionService, LLMService llmService) {
        this.chatSessionService = chatSessionService;
        this.llmService = llmService;
        this.executorService = Executors.newCachedThreadPool();
        this.sessionData = new HashMap<>();
    }

    @PostMapping("/session")
    public ResponseEntity<ChatSession> createSession() {
        ChatSession session = chatSessionService.createSession();
        sessionData.put(session.getSessionId(), new HashMap<>());
        return ResponseEntity.ok(session);
    }

    @PostMapping(value = "/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestBody ChatMessage message) {
        
        // Validate session
        ChatSession session = chatSessionService.getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("Session not found");
        }

        // Set message timestamp and session ID
        message.setTimestamp(new Date());
        message.setSessionId(sessionId);

        // Add user message to session
        chatSessionService.addMessage(sessionId, message);

        // Create SSE emitter
        SseEmitter emitter = new SseEmitter();
        
        // Process message in a separate thread
        executorService.execute(() -> {
            try {
                // Get or initialize session data
                Map<String, String> data = sessionData.computeIfAbsent(sessionId, k -> new HashMap<>());
                
                // Initial response
                sendBotMessage(emitter, sessionId, "Please wait while I process your request...");
                Thread.sleep(2000);

                // Check if this is a follow-up message
                if (data.containsKey("waiting_for")) {
                    String waitingFor = data.get("waiting_for");
                    data.put(waitingFor, message.getMessage());
                    data.remove("waiting_for");

                    // Process the complete information
                    sendBotMessage(emitter, sessionId, "Thank you for providing the additional information.");
                    Thread.sleep(2000);
                    
                    // Simulate processing the complete request
                    sendBotMessage(emitter, sessionId, "Processing your complete request...");
                    Thread.sleep(2000);

                    // Send final response
                    String finalResponse = processCompleteRequest(data);
                    sendBotMessage(emitter, sessionId, finalResponse);
                    
                    // Clear the session data
                    sessionData.remove(sessionId);
                } else {
                    // Initial validation
                    sendBotMessage(emitter, sessionId, "Validating your request...");
                    Thread.sleep(2000);

                    // Simulate validation check
                    if (message.getMessage().toLowerCase().contains("transfer")) {
                        // Request more information
                        data.put("type", "transfer");
                        data.put("waiting_for", "amount");
                        sendBotMessage(emitter, sessionId, "I see you want to make a transfer. Please specify the amount you'd like to transfer.");
                    } else if (message.getMessage().toLowerCase().contains("balance")) {
                        // Process balance check
                        sendBotMessage(emitter, sessionId, "Checking your account balance...");
                        Thread.sleep(2000);
                        sendBotMessage(emitter, sessionId, "Your current balance is $12,345.67");
                    } else {
                        // Generic response
                        sendBotMessage(emitter, sessionId, "I understand you're asking about: " + message.getMessage());
                        Thread.sleep(2000);
                        sendBotMessage(emitter, sessionId, "How can I help you with this?");
                    }
                }

                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void sendBotMessage(SseEmitter emitter, String sessionId, String message) throws IOException {
        ChatMessage botResponse = new ChatMessage();
        botResponse.setMessage(message);
        botResponse.setSender("bot");
        botResponse.setTimestamp(new Date());
        botResponse.setSessionId(sessionId);

        // Add bot response to session
        chatSessionService.addMessage(sessionId, botResponse);

        // Send the message to the client
        emitter.send(SseEmitter.event()
            .name("message")
            .data(botResponse));
    }

    private String processCompleteRequest(Map<String, String> data) {
        // Simulate processing the complete request
        if (data.containsKey("type") && "transfer".equals(data.get("type"))) {
            String amount = data.get("amount");
            return String.format("I've processed your transfer request for $%s. The transfer will be completed within 1-2 business days.", amount);
        }
        return "I've processed your request. Is there anything else you need help with?";
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ChatSession> getSession(@PathVariable String sessionId) {
        ChatSession session = chatSessionService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/session/invalidate/{sessionId}")
    public ResponseEntity<Void> invalidateSession(@PathVariable String sessionId) {
        chatSessionService.invalidateSession(sessionId);
        sessionData.remove(sessionId);
        return ResponseEntity.ok().build();
    }
} 