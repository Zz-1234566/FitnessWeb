package com.zz.usercenter.common;

import com.zz.usercenter.config.AiModelConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WhisperTranscriptionHelper {

    private static final Logger log = LoggerFactory.getLogger(WhisperTranscriptionHelper.class);

    @Resource
    private AiModelConfig aiModelConfig;

    public String transcribe(byte[] audioBytes, String filename) {
        AiModelConfig.WhisperConfig config = aiModelConfig.getWhisper();
        if (config == null || config.getModel() == null) {
            log.error("Whisper 配置缺失，请检查 ai.whisper 配置");
            return null;
        }

        // 复用 qwen provider 的 base-url 和 apiKey（DashScope）
        AiModelConfig.ModelProvider qwen = aiModelConfig.getProvider("qwen3.6-plus");
        if (qwen == null) {
            log.error("未找到 qwen provider 配置，无法进行语音识别");
            return null;
        }

        String mimeType = guessMimeType(filename);
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        String dataUri = "data:" + mimeType + ";base64," + base64Audio;

        // 构造 DashScope ASR 请求（OpenAI 兼容 /chat/completions 格式）
        Map<String, Object> audioContent = new HashMap<>();
        audioContent.put("type", "input_audio");
        Map<String, String> inputData = new HashMap<>();
        inputData.put("data", dataUri);
        audioContent.put("input_audio", inputData);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", List.of(audioContent));

        Map<String, Object> asrOptions = new HashMap<>();
        asrOptions.put("language", config.getLanguage() != null ? config.getLanguage() : "zh");
        asrOptions.put("enable_itn", false);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("stream", false);
        requestBody.put("messages", List.of(userMsg));
        requestBody.put("asr_options", asrOptions);

        WebClient webClient = WebClient.builder()
                .baseUrl(qwen.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + qwen.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                log.warn("DashScope ASR 返回空响应");
                return null;
            }

            return extractContent(response);
        } catch (Exception e) {
            log.error("DashScope 语音识别失败", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return null;
        Object content = message.get("content");
        return content instanceof String s ? s.trim() : content != null ? content.toString().trim() : null;
    }

    private String guessMimeType(String filename) {
        if (filename == null) return "audio/webm";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        return "audio/webm";
    }
}
