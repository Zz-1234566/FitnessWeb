package com.zz.usercenter.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * USDA FoodData Central API 客户端
 * 用于查询食物营养数据（热量、蛋白质、碳水、脂肪、纤维）
 */
@Component
public class UsdaFoodClient {

    private static final Logger log = LoggerFactory.getLogger(UsdaFoodClient.class);

    /**
     * 营养素 ID 常量
     */
    private static final int NUTRIENT_ENERGY_ATWATER = 2048;   // Atwater 热量 (kcal)
    private static final int NUTRIENT_ENERGY_KCAL     = 1008;   // 常规热量 (kcal)
    private static final int NUTRIENT_PROTEIN         = 1003;
    private static final int NUTRIENT_CARBS            = 1005;
    private static final int NUTRIENT_FAT             = 1004;
    private static final int NUTRIENT_FIBER           = 1079;

    private static final String SOURCE = "USDA FoodData Central";

    @Value("${usda.api-key}")
    private String apiKey;

    @Value("${usda.base-url}")
    private String baseUrl;

    @Value("${usda.search-path}")
    private String searchPath;

    @Value("${usda.page-size:3}")
    private int pageSize;

    @Value("${usda.data-types:Foundation}")
    private String dataTypes;

    @Value("${usda.timeout-seconds:8}")
    private int timeoutSeconds;

    @Resource
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 搜索食物营养数据，返回与 WebSearchHelper.SearchExtractResult 兼容的 JSON 字符串
     *
     * @param foodName     原始食物名称（用于返回结果）
     * @param englishQuery 英文查询词（USDA 不支持中文）
     * @return SearchExtractResult 或 null（查不到/异常时）
     */
    public WebSearchHelper.SearchExtractResult searchFoodNutrition(String foodName, String englishQuery) {
        long start = System.currentTimeMillis();
        log.info("[USDA] 开始搜索: foodName={}, query={}", foodName, englishQuery);

        try {
            String url = String.format("%s?query=%s&pageSize=%d&api_key=%s&dataType=%s&nutrients=%d,%d,%d,%d,%d",
                    searchPath,
                    java.net.URLEncoder.encode(englishQuery, java.nio.charset.StandardCharsets.UTF_8),
                    pageSize,
                    apiKey,
                    dataTypes,
                    NUTRIENT_ENERGY_ATWATER,
                    NUTRIENT_PROTEIN,
                    NUTRIENT_CARBS,
                    NUTRIENT_FAT,
                    NUTRIENT_FIBER);

            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                    .build();

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            if (response == null) {
                log.warn("[USDA] 响应为空, 耗时={}ms", elapsed);
                return null;
            }

            Number totalHits = (Number) response.get("totalHits");
            if (totalHits == null || totalHits.intValue() == 0) {
                log.info("[USDA] 无结果: query={}, 耗时={}ms", englishQuery, elapsed);
                return null;
            }

            // 解析营养数据
            String jsonResult = parseFirstResult(foodName, response);
            if (jsonResult == null) {
                log.info("[USDA] 解析失败（无有效营养数据）: query={}", englishQuery);
                return null;
            }

            log.info("[USDA] 搜索成功: foodName={}, 耗时={}ms, 结果长度={}", foodName, elapsed, jsonResult.length());
            log.info("[USDA] 结果预览: {}", jsonResult.length() > 200 ? jsonResult.substring(0, 200) + "..." : jsonResult);
            return new WebSearchHelper.SearchExtractResult(jsonResult, start, System.currentTimeMillis(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[USDA] 搜索异常: query={}, 耗时={}ms, error={}", englishQuery, elapsed, e.getMessage());
            return null;
        }
    }

    /**
     * 从 USDA 响应中提取第一条有效结果，转换为标准 JSON 格式
     */
    @SuppressWarnings("unchecked")
    private String parseFirstResult(String foodName, Map<String, Object> response) {
        try {
            Object foodsObj = response.get("foods");
            if (!(foodsObj instanceof java.util.List)) return null;

            java.util.List<Map<String, Object>> foods = (java.util.List<Map<String, Object>>) foodsObj;

            for (Map<String, Object> food : foods) {
                Object nutrientsObj = food.get("foodNutrients");
                if (!(nutrientsObj instanceof java.util.List)) continue;

                java.util.List<Map<String, Object>> nutrients = (java.util.List<Map<String, Object>>) nutrientsObj;

                Map<String, Double> nutMap = new HashMap<>();
                for (Map<String, Object> n : nutrients) {
                    Number id = (Number) n.get("nutrientId");
                    Number val = (Number) n.get("value");
                    if (id != null && val != null) {
                        nutMap.put(id.intValue() + "", val.doubleValue());
                    }
                }

                // 优先取 Atwater 热量(2048)，备选常规热量(1008)
                double calories = nutMap.getOrDefault(NUTRIENT_ENERGY_ATWATER + "", 0.0);
                if (calories == 0) {
                    calories = nutMap.getOrDefault(NUTRIENT_ENERGY_KCAL + "", 0.0);
                }

                // 跳过没有热量数据的条目
                if (calories <= 0) continue;

                // 取碳水时处理负值（USDA 有时会返回 -0.15 这种）
                double carbs = nutMap.getOrDefault(NUTRIENT_CARBS + "", 0.0);
                if (carbs < 0) carbs = 0;

                // 构建标准 JSON
                Map<String, Object> result = new HashMap<>();
                result.put("foodName", foodName);
                result.put("suggestedAmount", 100);
                result.put("unit", "g");
                result.put("perUnitAmount", 100);

                Map<String, Object> nutrition = new HashMap<>();
                nutrition.put("calories", Math.round(calories * 100.0) / 100.0);
                nutrition.put("protein", Math.round(nutMap.getOrDefault(NUTRIENT_PROTEIN + "", 0.0) * 100.0) / 100.0);
                nutrition.put("carbs", Math.round(carbs * 100.0) / 100.0);
                nutrition.put("fat", Math.round(nutMap.getOrDefault(NUTRIENT_FAT + "", 0.0) * 100.0) / 100.0);
                nutrition.put("fiber", Math.round(nutMap.getOrDefault(NUTRIENT_FIBER + "", 0.0) * 100.0) / 100.0);
                result.put("nutritionPerUnit", nutrition);
                result.put("source", SOURCE);

                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            log.warn("[USDA] 解析异常: {}", e.getMessage());
        }
        return null;
    }
}
