package com.wellsfargo.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class LLMService {

    //private final RestTemplate restTemplate;
    private final String llmApiUrl;
    private final String llmApiKey;

    public LLMService(
            @Value("${chat.llm.api-url}") String llmApiUrl,
            @Value("${chat.llm.api-key}") String llmApiKey) {
       // this.restTemplate = restTemplate;
        this.llmApiUrl = llmApiUrl;
        this.llmApiKey = llmApiKey;
    }

    public String getLLMResponse(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmApiKey);

        Map<String, String> requestBody = Map.of("message", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            Map<String, String> response = restTemplate.postForObject(
                llmApiUrl,
                request,
                Map.class
            );
            return response != null ? response.get("response") : "Sorry, I couldn't process your request.";
        } catch (Exception e) {
            return "Sorry, I encountered an error while processing your request.";
        }
    }
} 