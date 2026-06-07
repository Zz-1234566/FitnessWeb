package com.zz.usercenter.common;

import com.zz.usercenter.config.AiModelConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public Map<String, Object> doPost(AiModelConfig.ModelProvider provider, String systemPrompt, String userMessage,
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

    /**
     * 流式调用文本模型，逐 token 写入 OutputStream（SSE 格式）
     * 模型 API 返回 stream: true 的 SSE 数据，每收到一个 token 立即推送到客户端
     */
    public String callTextStream(AiModelConfig.ModelProvider provider, String systemPrompt, String userMessage,
                                  int maxTokens, double temperature, OutputStream outputStream) {
        WebClient webClient = buildWebClient(provider);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("stream", true);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        StringBuilder fullContent = new StringBuilder();
        try {
            webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            line -> {
                                if (line == null || line.isBlank()) return;
                                String trimmed = line.trim();
                                if (!trimmed.startsWith("data:")) return;
                                String data = trimmed.substring(5).trim();
                                if ("[DONE]".equals(data)) return;
                                try {
                                    Map<String, Object> chunk = parseSimpleJson(data);
                                    List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                    if (choices != null && !choices.isEmpty()) {
                                        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                        if (delta != null) {
                                            Object content = delta.get("content");
                                            if (content != null) {
                                                String text = content.toString();
                                                fullContent.append(text);
                                                try {
                                                    String escaped = text.replace("\n", "\\n");
                                                    outputStream.write(("data:" + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
                                                    outputStream.flush();
                                                } catch (Exception ignored) {}
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {}
                            },
                            error -> log.error("AI流式调用失败，模型：{}", provider.getName(), error)
                    );
        } catch (Exception e) {
            log.error("AI流式调用异常，模型：{}", provider.getName(), e);
        }
        return fullContent.toString();
    }

    /** 简易 JSON 解析（不依赖 Jackson，只解析一层结构） */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return map;
        json = json.substring(1, json.length() - 1);
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inKey = true;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) { escape = false; if (inKey) key.append(c); else value.append(c); continue; }
            if (c == '\\') { escape = true; continue; }
            if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) { inString = !inString; continue; }
            if (!inString) {
                if (c == ':') { inKey = false; continue; }
                if (c == ',' || c == '{' || c == '}') {
                    if (key.length() > 0) {
                        String val = value.toString().trim();
                        Object parsed = parseValue(val);
                        map.put(key.toString().trim(), parsed);
                        key.setLength(0);
                        value.setLength(0);
                    }
                    inKey = true;
                    continue;
                }
            }
            if (inKey) key.append(c); else value.append(c);
        }
        if (key.length() > 0) map.put(key.toString().trim(), parseValue(value.toString().trim()));
        return map;
    }

    private static Object parseValue(String val) {
        if (val.startsWith("\"") && val.endsWith("\"")) return val.substring(1, val.length() - 1);
        if (val.startsWith("[")) return val; // 数组不解析，保留原始
        if ("null".equals(val)) return null;
        if ("true".equals(val)) return true;
        if ("false".equals(val)) return false;
        try { if (val.contains(".")) return Double.parseDouble(val); return Integer.parseInt(val); } catch (Exception e) { return val; }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) return null;
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        if (msg == null) return null;
        Object content = msg.get("content");
        return content instanceof String ? (String) content : content != null ? content.toString() : null;
    }

    /**
     * 多模态调用（支持图片+文本），用于视觉识别
     */
    public Map<String, Object> callVision(AiModelConfig.ModelProvider provider,
                                          List<Map<String, Object>> userContentParts,
                                          int maxTokens, double temperature) {
        WebClient webClient = buildWebClient(provider);
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userContentParts);
        messages.add(userMsg);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("messages", messages);
        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("AI视觉调用失败，模型：{}", provider.getName(), e);
            return null;
        }
    }
}
