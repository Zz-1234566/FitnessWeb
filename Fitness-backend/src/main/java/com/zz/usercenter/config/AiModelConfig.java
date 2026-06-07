package com.zz.usercenter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiModelConfig {

    private String defaultModel;

    /** 文本提纯/清洗用的模型 */
    private String purificationModel;

    /** 聪明模型（处理用户核心需求） */
    private String chatModel;

    /** 视觉理解模型 */
    private String visionModel;

    /** 图片识别最后一层是否使用深度思索模型（true=chatModel, false=purificationModel） */
    private boolean deepThinking = false;

    /** 图片识别请求超时时间（秒），前端根据此值调整等待 */
    private int recognitionTimeout = 60;

    /** 自定义模型 API Key 加密密钥 */
    private String cryptoSecret;

    private int maxTokens;

    private double temperature;

    private List<ModelProvider> providers;

    private WhisperConfig whisper;

    @Data
    public static class WhisperConfig {
        private String model;
        private String language;
    }

    @Data
    public static class ModelProvider {
        private String name;
        private String baseUrl;
        private String apiKey;
        private String model;
        private String label;
        /** text / vision / asr */
        private String type;
    }

    public ModelProvider getProvider(String name) {
        if (name == null || name.isBlank()) {
            return getByModelName(defaultModel, true);
        }
        return getByModelName(name, false);
    }

    public ModelProvider getByModelName(String name, boolean allowFallbackToFirst) {
        String normalized = normalizeProviderName(name);
        for (ModelProvider p : providers) {
            if (normalizeProviderName(p.getName()).equals(normalized)
                    || normalizeProviderName(p.getModel()).equals(normalized)
                    || normalizeProviderName(p.getLabel()).equals(normalized)) {
                return p;
            }
        }
        if (allowFallbackToFirst && providers != null && !providers.isEmpty()) {
            return providers.get(0);
        }
        return null;
    }

    private String normalizeProviderName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "zhipu", "glm", "glm4", "glm-4", "glm4flash" -> "glm-4-flash";
            case "glm-4v", "glm4v", "glm-4v-flash" -> "glm-4v-flash";
            case "qwen", "qwenplus", "qwen-plus", "qwen3.6", "qwen3.6plus" -> "qwen3.6-plus";
            case "deepseek", "deepseekv4", "deepseek-v4" -> "deepseek-v4-flash";
            default -> normalized;
        };
    }

    public List<ModelProvider> getAvailableProviders() {
        return providers;
    }

    public ModelProvider getVisionProvider() {
        String name = visionModel != null && !visionModel.isBlank() ? visionModel : "glm-4v-flash";
        AiModelConfig.ModelProvider provider = getByModelName(name, false);
        return provider != null ? provider : getByModelName("glm-4v-flash", false);
    }

    /** 图片识别最后一层用的模型：deepThinking=true → chatModel, false → purificationModel */
    public ModelProvider getRecognitionSummaryProvider() {
        String name = deepThinking ? chatModel : purificationModel;
        return getByModelName(name, true);
    }

    public ModelProvider getCustomProvider(String name, String baseUrl, String apiKey, String model) {
        ModelProvider provider = new ModelProvider();
        provider.setName(name);
        provider.setBaseUrl(baseUrl);
        provider.setApiKey(apiKey);
        provider.setModel(model);
        provider.setLabel(name);
        return provider;
    }
}
