package com.zz.usercenter.common;

import com.zz.usercenter.config.AiModelConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiCallHelper {

    private static final Logger log = LoggerFactory.getLogger(AiCallHelper.class);

    @Resource
    private AiModelConfig aiModelConfig;

    public String callText(AiModelConfig.ModelProvider provider, String systemPrompt, String userMessage,
                          int maxTokens, double temperature) {
        Map<String, Object> response = doPost(provider, systemPrompt, userMessage, maxTokens, temperature);
        return extractContent(response);
    }

    public Map<String, Object> callJson(AiModelConfig.ModelProvider provider, String systemPrompt, String userMessage,
                                        int maxTokens, double temperature) {
        return doPost(provider, systemPrompt, userMessage, maxTokens, temperature);
    }

    public String callSingleMessage(AiModelConfig.ModelProvider provider, String userMessage,
                                    int maxTokens, double temperature) {
        Map<String, Object> response = doPostSingle(provider, userMessage, maxTokens, temperature);
        return extractContent(response);
    }

    private Map<String, Object> doPost(AiModelConfig.ModelProvider provider, String systemPrompt, String userMessage,
                                        int maxTokens, double temperature) {
        WebClient webClient = buildWebClient(provider);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("AI调用失败，模型：{}", provider.getName(), e);
            return null;
        }
    }

    private Map<String, Object> doPostSingle(AiModelConfig.ModelProvider provider, String userMessage,
                                              int maxTokens, double temperature) {
        WebClient webClient = buildWebClient(provider);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", userMessage)
        ));
        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("AI调用失败，模型：{}", provider.getName(), e);
            return null;
        }
    }

    private WebClient buildWebClient(AiModelConfig.ModelProvider provider) {
        return WebClient.builder()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) return null;
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return msg == null ? null : (String) msg.get("content");
    }
}
