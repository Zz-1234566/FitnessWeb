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

    private int maxTokens;

    private double temperature;

    private List<ModelProvider> providers;

    @Data
    public static class ModelProvider {
        private String name;
        private String baseUrl;
        private String apiKey;
        private String model;
        private String label;
    }

    public ModelProvider getProvider(String name) {
        if (name == null || name.isBlank()) {
            return getByModelName(defaultModel, true);
        }
        return getByModelName(name, false);
    }

    private ModelProvider getByModelName(String name, boolean allowFallbackToFirst) {
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
            case "qwen", "qwenplus", "qwen-plus", "qwen3.6", "qwen3.6plus" -> "qwen3.6-plus";
            case "deepseek", "deepseekv4", "deepseek-v4" -> "deepseek-v4-flash";
            default -> normalized;
        };
    }

    public List<ModelProvider> getAvailableProviders() {
        return providers;
    }
}
