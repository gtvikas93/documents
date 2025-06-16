package com.wellsfargo.chat.model;

import lombok.Data;
import java.util.Date;

@Data
public class SatisfactionFeedback {
    private String customerId;
    private String ecn;
    private String xaId;
    private String originalPromptMessage;
    private String sessionId;
    private Date timestamp;
    private String satisfactoryMessage;
    private String reason;
} 