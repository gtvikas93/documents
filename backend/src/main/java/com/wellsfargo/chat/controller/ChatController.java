package com.wellsfargo.chat.controller;

import com.wellsfargo.chat.model.ChatMessage;
import com.wellsfargo.chat.model.ChatSession;
import com.wellsfargo.chat.model.SatisfactionFeedback;
import com.wellsfargo.chat.service.ChatSessionService;
import com.wellsfargo.chat.service.LLMService;
import com.wellsfargo.chat.service.SatisfactionService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatSessionService chatSessionService;
    private final LLMService llmService;
    private final SatisfactionService satisfactionService;
    private final ExecutorService executorService;
    private final Map<String, Map<String, String>> sessionData;

    public ChatController(ChatSessionService chatSessionService, LLMService llmService, SatisfactionService satisfactionService) {
        this.chatSessionService = chatSessionService;
        this.llmService = llmService;
        this.satisfactionService = satisfactionService;
        this.executorService = Executors.newCachedThreadPool();
        this.sessionData = new HashMap<>();
    }

    @PostMapping("/session")
    public ResponseEntity<ChatSession> createSession(@RequestBody(required = false) Map<String, String> userInfo) {
        ChatSession session = chatSessionService.createSession();
        
        // Set user information if provided
        if (userInfo != null) {
            session.setCustomerId(userInfo.get("customerId"));
            session.setEcn(userInfo.get("ecn"));
            session.setXaId(userInfo.get("xaId"));
        }
        
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
        message.setTimestamp(new Date());
        message.setSessionId(sessionId);
        chatSessionService.addMessage(sessionId, message);

        SseEmitter emitter = new SseEmitter(300000L);
        AtomicBoolean isCompleted = new AtomicBoolean(false);
        emitter.onCompletion(() -> isCompleted.set(true));
        emitter.onTimeout(() -> isCompleted.set(true));
        emitter.onError((ex) -> isCompleted.set(true));

        executorService.execute(() -> {
            try {
                // 1. Show 'please wait' message
                if (!isCompleted.get()) {
                    sendBotMessage(emitter, sessionId, "Please wait while we process your request...");
                    Thread.sleep(2000);
                }

                // Check if session already has an LLM summary message
                LLMResult llmResult = null;
                ChatMessage lastLLMSummary = null;
                for (int i = session.getMessages().size() - 1; i >= 0; i--) {
                    ChatMessage msg = session.getMessages().get(i);
                    if ("bot".equals(msg.getSender()) && isLLMSummary(msg.getMessage())) {
                        lastLLMSummary = msg;
                        break;
                    }
                }
                if (lastLLMSummary != null) {
                    // Reconstruct LLMResult from summary
                    llmResult = reconstructLLMResultFromSummary(lastLLMSummary.getMessage());
                } else {
                    // 2. Simulate LLM call
                    llmResult = callDummyLLM(message.getMessage(), session);
                }

                if (!isCompleted.get()) {
                    if (llmResult == null || llmResult.action == null) {
                        // Send error message but do NOT save to session
                        sendBotMessage(emitter, sessionId, "Sorry, I couldn't understand your request. Could you please rephrase your query?", false);
                        emitter.complete();
                        return;
                    } else if (lastLLMSummary == null) {
                        sendBotMessage(emitter, sessionId, llmResult.summary, true);
                        Thread.sleep(2000);
                    }
                }

                // 3. Show 'validation in progress'
                if (!isCompleted.get()) {
                    sendBotMessage(emitter, sessionId, "Validation in progress...");
                    Thread.sleep(2000);
                }

                // 4. Simulate validation
                ValidationResult validationResult = callDummyValidation(llmResult, session);
                if (!isCompleted.get()) {
                    if (!validationResult.success) {
                        if (validationResult.reason.equals("not_eligible")) {
                            sendBotMessage(emitter, sessionId, "Sorry, you are not eligible for this alert.");
                            emitter.complete();
                            return;
                        } else if (validationResult.reason.equals("need_more_info")) {
                            sendBotMessage(emitter, sessionId, "To proceed, please provide the following information: " + validationResult.missingInfo);
                            emitter.complete();
                            return;
                        } else {
                            sendBotMessage(emitter, sessionId, "Validation failed. Please try again.");
                            emitter.complete();
                            return;
                        }
                    } else {
                        // Validation successful
                        sendBotMessage(emitter, sessionId, "Validation is successful.");
                        Thread.sleep(2000);
                    }
                }

                // 5. Simulate action service
                if (!isCompleted.get()) {
                    ActionResult actionResult = callDummyAction(llmResult, session);
                    sendBotMessage(emitter, sessionId, actionResult.message);
                    emitter.complete();
                }
            } catch (Exception e) {
                if (!isCompleted.get()) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("An error occurred while processing your request. Please try again."));
                        emitter.complete();
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                }
            }
        });
        return emitter;
    }

    private void sendBotMessage(SseEmitter emitter, String sessionId, String message) throws IOException {
        sendBotMessage(emitter, sessionId, message, true);
    }

    // Overloaded method to control whether to save to session
    private void sendBotMessage(SseEmitter emitter, String sessionId, String message, boolean saveToSession) throws IOException {
        try {
            ChatMessage botResponse = new ChatMessage();
            botResponse.setMessage(message);
            botResponse.setSender("bot");
            botResponse.setTimestamp(new Date());
            botResponse.setSessionId(sessionId);

            // Add bot response to session only if saveToSession is true
            if (saveToSession) {
                chatSessionService.addMessage(sessionId, botResponse);
            }

            // Send the message to the client
            emitter.send(SseEmitter.event()
                .name("message")
                .data(botResponse));
        } catch (IllegalStateException e) {
            logger.warn("Attempted to send message after emitter completion for session: {}", sessionId);
            throw e;
        } catch (IOException e) {
            logger.error("Error sending bot message for session: {} - {}", sessionId, e.getMessage());
            throw e;
        }
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

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody SatisfactionFeedback feedback) {
        try {
            // Get the session to retrieve user information
            ChatSession session = chatSessionService.getSession(feedback.getSessionId());
            if (session != null) {
                // Use session's user information if not provided in feedback
                if (feedback.getCustomerId() == null) feedback.setCustomerId(session.getCustomerId());
                if (feedback.getEcn() == null) feedback.setEcn(session.getEcn());
                if (feedback.getXaId() == null) feedback.setXaId(session.getXaId());
            }
            
            feedback.setTimestamp(new Date());
            satisfactionService.saveFeedback(feedback);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error saving feedback: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Dummy LLM result class
    private static class LLMResult {
        String messageId;
        String action; // SUBSCRIBE, UNSUBSCRIBE, DETAILS
        String summary;
        Map<String, String> params;
    }

    // Dummy validation result class
    private static class ValidationResult {
        boolean success;
        String reason; // not_eligible, need_more_info, other
        String missingInfo;
    }

    // Dummy action result class
    private static class ActionResult {
        boolean success;
        String message;
    }

    // Simulate LLM call
    private LLMResult callDummyLLM(String prompt, ChatSession session) {
        LLMResult result = new LLMResult();
        result.messageId = "msg-" + System.currentTimeMillis();
        result.params = new HashMap<>();
        String lower = prompt.toLowerCase();
        if (lower.contains("subscribe")) {
            result.action = "SUBSCRIBE";
            result.summary = "You are requesting to subscribe to an alert.";
            if (lower.contains("account")) result.params.put("accountNumber", "");
            if (lower.contains("sms")) result.params.put("channel", "SMS");
        } else if (lower.contains("unsubscribe")) {
            result.action = "UNSUBSCRIBE";
            result.summary = "You are requesting to unsubscribe from an alert.";
        } else if (lower.contains("details")) {
            result.action = "DETAILS";
            result.summary = "You are requesting details about an alert.";
        } else {
            result.action = null;
            result.summary = null;
        }
        return result;
    }

    // Simulate validation
    private ValidationResult callDummyValidation(LLMResult llmResult, ChatSession session) {
        ValidationResult result = new ValidationResult();
        // Example: if action is SUBSCRIBE, require accountNumber and channel
        if ("SUBSCRIBE".equals(llmResult.action)) {
            boolean hasAccount = false, hasChannel = false;
            // Check all previous messages for required info
            for (ChatMessage msg : session.getMessages()) {
                String text = msg.getMessage().toLowerCase();
                if (text.matches(".*\\d{6,}.*")) hasAccount = true;
                if (text.contains("sms") || text.contains("email")) hasChannel = true;
            }
            if (!hasAccount || !hasChannel) {
                result.success = false;
                result.reason = "need_more_info";
                StringBuilder missing = new StringBuilder();
                if (!hasAccount) missing.append("accountNumber ");
                if (!hasChannel) missing.append("channel (SMS or Email) ");
                result.missingInfo = missing.toString().trim();
                return result;
            }
            // Simulate eligibility check
            if (session.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("not eligible"))) {
                result.success = false;
                result.reason = "not_eligible";
                return result;
            }
            result.success = true;
            return result;
        }
        // For UNSUBSCRIBE and DETAILS, always succeed
        result.success = true;
        return result;
    }

    // Simulate action service
    private ActionResult callDummyAction(LLMResult llmResult, ChatSession session) {
        ActionResult result = new ActionResult();
        result.success = true;
        if ("SUBSCRIBE".equals(llmResult.action)) {
            result.message = "You have been successfully subscribed to the alert.";
        } else if ("UNSUBSCRIBE".equals(llmResult.action)) {
            result.message = "You have been unsubscribed from the alert.";
        } else if ("DETAILS".equals(llmResult.action)) {
            result.message = "Here are the details of your alert subscription.";
        } else {
            result.message = "Action completed.";
        }
        return result;
    }

    // Helper to check if a message is an LLM summary
    private boolean isLLMSummary(String message) {
        return message != null && (
            message.startsWith("You are requesting to subscribe to an alert.") ||
            message.startsWith("You are requesting to unsubscribe from an alert.") ||
            message.startsWith("You are requesting details about an alert.")
        );
    }

    // Helper to reconstruct LLMResult from summary
    private LLMResult reconstructLLMResultFromSummary(String summary) {
        LLMResult result = new LLMResult();
        result.messageId = "msg-" + System.currentTimeMillis();
        result.params = new HashMap<>();
        if (summary.startsWith("You are requesting to subscribe to an alert.")) {
            result.action = "SUBSCRIBE";
            result.summary = summary;
        } else if (summary.startsWith("You are requesting to unsubscribe from an alert.")) {
            result.action = "UNSUBSCRIBE";
            result.summary = summary;
        } else if (summary.startsWith("You are requesting details about an alert.")) {
            result.action = "DETAILS";
            result.summary = summary;
        } else {
            result.action = null;
            result.summary = null;
        }
        return result;
    }
} 