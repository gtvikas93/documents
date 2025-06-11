package com.wellsfargo.chat.controller;

import com.wellsfargo.chat.model.ChatMessage;
import com.wellsfargo.chat.model.ChatSession;
import com.wellsfargo.chat.service.ChatSessionService;
import com.wellsfargo.chat.service.LLMService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Date;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {
    private final ChatSessionService chatSessionService;
    //private final LLMService llmService;

    public ChatController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
        //this.llmService = llmService;
    }

    @PostMapping("/session")
    public ResponseEntity<ChatSession> createSession() {
        ChatSession session = chatSessionService.createSession();
        return ResponseEntity.ok(session);
    }

    @PostMapping("/message")
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestHeader("X-Session-ID") String sessionId,
            @RequestBody ChatMessage message) {
        
        // Validate session
        ChatSession session = chatSessionService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        // Set message timestamp and session ID
        message.setTimestamp(new Date());
        message.setSessionId(sessionId);

        // Add user message to session
        chatSessionService.addMessage(sessionId, message);

        // Get LLM response
        String llmResponse = "Hello Boss!!"; //llmService.getLLMResponse(message.getMessage());

        // Create bot response
        ChatMessage botResponse = new ChatMessage();
        botResponse.setMessage(llmResponse);
        botResponse.setSender("bot");
        botResponse.setTimestamp(new Date());
        botResponse.setSessionId(sessionId);

        // Add bot response to session
        chatSessionService.addMessage(sessionId, botResponse);

        return ResponseEntity.ok(botResponse);
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
        return ResponseEntity.ok().build();
    }
} 