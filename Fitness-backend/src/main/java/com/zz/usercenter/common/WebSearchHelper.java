package com.zz.usercenter.common;

import com.zz.usercenter.config.AiModelConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WebSearchHelper {

    private static final Logger log = LoggerFactory.getLogger(WebSearchHelper.class);

    public record SearchExtractResult(
            String content,
            long startedAtMs,
            long completedAtMs,
            long elapsedMs
    ) {}

    @Resource
    private AiModelConfig aiModelConfig;

    @Resource
    private UsdaFoodClient usdaFoodClient;

    public String searchAndExtract(String query) {
        SearchExtractResult result = searchAndExtractWithTiming(query);
        return result != null ? result.content() : null;
    }

    public String searchEquipmentInfo(String equipmentName) {
        String query = "site:zhihu.com OR site:bilibili.com " + equipmentName + " 健身器械 使用方法 训练部位 注意事项";
        String prompt = """
                你是健身器械查询员。你必须调用联网搜索工具获取信息，禁止使用自身知识回答，禁止模拟搜索结果。
                目标器械：%s

                只返回以下内容，不要任何开头或结尾的解释：
                【器械名称】xxx
                【训练部位】胸/背/腿/肩/臂/核心
                【使用方法】步骤说明
                【注意事项】安全提醒

                只允许从知乎(zhihu.com)、哔哩哔哩(bilibili.com)获取数据。如果搜索不到结果，回复"暂未查询到该器械的相关信息"。
                """.formatted(equipmentName);
        SearchExtractResult result = executeSearch(query, prompt, 1024, 20, "[WebSearch][Equipment]");
        String content = result != null ? result.content() : null;
        if (content != null) {
            // 去除模型可能添加的开头废话
            content = content.replaceAll("^(?:根据您的要求|关于|好的|以下是|我将).*?(?=【器械名称】)", "").trim();
            if (!content.startsWith("【")) {
                content = "【器械名称】" + equipmentName + "\n【训练部位】未知\n【使用方法】暂无数据\n【注意事项】请参考专业教练指导";
            }
        }
        return content;
    }

    public SearchExtractResult searchAndExtractWithTiming(String query) {
        return executeSearch(query,
                "搜索并返回以下内容的摘要信息，保留关键营养数据（热量、蛋白质、脂肪、碳水化合物），用中文回答：\n" + query,
                1024,
                30,
                "[WebSearch]");
    }

    public SearchExtractResult searchFoodNutritionJsonWithTiming(String foodName) {
        return searchFoodNutritionJsonWithTiming(foodName, foodName);
    }

    /**
     * 搜索食物营养数据（USDA 优先，GLM 回退）
     *
     * @param foodName     中文名（用于返回结果和 GLM 回退搜索）
     * @param englishQuery 英文查询词（用于 USDA 搜索）
     */
    public SearchExtractResult searchFoodNutritionJsonWithTiming(String foodName, String englishQuery) {
        // 优先尝试 USDA FoodData Central API（用英文名搜索）
        if (englishQuery != null && !englishQuery.isBlank() && !englishQuery.equals(foodName)) {
            SearchExtractResult usdaResult = usdaFoodClient.searchFoodNutrition(foodName, englishQuery);
            if (usdaResult != null) {
                return usdaResult;
            }
        }

        // USDA 查不到，再用中文名尝试一次（处理本身就是英文的情况）
        SearchExtractResult usdaResult2 = usdaFoodClient.searchFoodNutrition(foodName, foodName);
        if (usdaResult2 != null) {
            return usdaResult2;
        }

        // USDA 全部无结果，回退到 GLM web_search（搜薄荷健康/39健康网）
        log.info("[WebSearch][FoodJson] USDA 无结果，回退 GLM web_search: foodName={}", foodName);
        String query = "site:boohee.com OR site:39.net " + foodName + " 热量 每100g 营养成分";
        String prompt = """
                你是营养数据查询员。请调用联网搜索，查询食物营养信息，并严格只返回一行 JSON。
                目标食物：%s

                规则：
                1. 只输出 JSON，不要 markdown，不要解释，不要代码块。
                2. 返回格式固定：
                {"foodName":"食物名","suggestedAmount":100,"unit":"g","perUnitAmount":100,"nutritionPerUnit":{"calories":0,"protein":0,"carbs":0,"fat":0,"fiber":0},"source":"数据来源"}
                3. calories 必须是每100g的千卡(kcal)，protein/carbs/fat/fiber 必须是克(g)。
                4. 只允许从以下权威网站获取数据：薄荷健康(boohee.com)、中国食物成分表(nhc.gov.cn)、39健康网(39.net)。
                5. 如果从这些网站搜索不到可靠数据，nutritionPerUnit 的五个字段全部填 0，source 写 not_found。
                6. 禁止编造数据，禁止从其他网站取值。找不到就返回全 0。
                7. suggestedAmount 固定 100，unit 固定 "g"，perUnitAmount 固定 100。
                """.formatted(foodName);
        return executeSearch(query, prompt, 512, 15, "[WebSearch][FoodJson]");
    }

    /**
     * 联网搜索运动动作的结构化信息
     */
    public SearchExtractResult searchExerciseJsonWithTiming(String exerciseName) {
        String query = "site:zhihu.com OR site:bilibili.com " + exerciseName + " 动作要领 训练部位 正确姿势";
        String prompt = """
                你是健身动作数据查询员。请调用联网搜索，查询运动动作的详细信息，并严格只返回一行 JSON。
                目标动作：%s

                规则：
                1. 只输出 JSON，不要 markdown，不要解释，不要代码块。
                2. 返回格式固定：
                {"name":"动作名","muscleGroup":"chest","equipment":"器械名或自重","difficulty":"beginner","steps":["步骤1","步骤2","步骤3"],"tips":["注意1","注意2"],"recommendedSets":3,"recommendedReps":"8-12","restSeconds":60,"videoUrl":null}
                3. muscleGroup 必须是以下之一：chest(胸), back(背), legs(腿), shoulders(肩), arms(手臂), core(核心)。
                4. difficulty 必须是以下之一：beginner(初级), intermediate(中级), advanced(高级)。
                5. steps 是训练步骤的 JSON 数组，至少3步，简洁明了。
                6. tips 是注意事项的 JSON 数组，至少2条。
                7. recommendedSets 是推荐组数(整数)，recommendedReps 是推荐次数(字符串，如"8-12")，restSeconds 是组间休息秒数。
                8. videoUrl 如果搜到 bilibili 视频链接则填入，否则填 null。
                9. 只允许从以下网站获取数据：知乎(zhihu.com)、哔哩哔哩(bilibili.com)、Keep(keep.com)。
                10. 禁止编造数据。搜索不到就各字段填 null 或空数组，muscleGroup 填 "core"。
                """.formatted(exerciseName);
        return executeSearch(query, prompt, 1024, 20, "[WebSearch][ExerciseJson]");
    }

    /**
     * 联网搜索运动的 MET 代谢当量
     */
    public Double searchMetValue(String exerciseName) {
        String query = exerciseName + " MET metabolic equivalent compendium";
        String prompt = """
                查询运动的MET代谢当量（Metabolic Equivalent of Task）。
                目标运动：%s

                规则：
                1. 只输出一个纯数字，不要任何解释、不要单位、不要markdown。
                2. MET值是小数，保留1位（如6.0、3.8、8.0）。
                3. 参考Compendium of Physical Activities的标准数据。
                4. 如果搜不到可靠数据，返回0。
                """.formatted(exerciseName);
        SearchExtractResult result = executeSearch(query, prompt, 128, 10, "[WebSearch][MET]");
        if (result == null || result.content() == null) return null;
        try {
            double met = Double.parseDouble(result.content().trim());
            return met > 0 ? met : null;
        } catch (NumberFormatException e) {
            // 尝试从回复中提取第一个数字
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+\\.?\\d*)").matcher(result.content());
            if (m.find()) {
                double met = Double.parseDouble(m.group(1));
                return met > 0 ? met : null;
            }
            return null;
        }
    }

    private SearchExtractResult executeSearch(String query, String userPrompt, int maxTokens, int timeoutSeconds, String logPrefix) {
        AiModelConfig.ModelProvider provider = aiModelConfig.getByModelName("glm-4-flash", false);
        if (provider == null) {
            log.warn("{} 未配置 GLM 模型", logPrefix);
            return null;
        }

        long start = System.currentTimeMillis();
        log.info("{} 开始搜索: query={}, model={}, baseUrl={}", logPrefix, query, provider.getModel(), provider.getBaseUrl());

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(provider.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> webSearchTool = new HashMap<>();
            webSearchTool.put("type", "web_search");
            webSearchTool.put("web_search", Map.of("enable", true, "search_result", true));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.0);
            requestBody.put("tools", List.of(webSearchTool));
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
            ));

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            long elapsed = System.currentTimeMillis() - start;

            if (response == null) {
                log.warn("{} 响应为空, 耗时={}ms", logPrefix, elapsed);
                return null;
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("{} choices为空, 响应keys={}, 耗时={}ms", logPrefix, response.keySet(), elapsed);
                return null;
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                log.warn("{} message为空, 耗时={}ms", logPrefix, elapsed);
                return null;
            }

            Object webSearchResult = message.get("web_search");
            if (webSearchResult != null) {
                log.info("{} 搜索结果数量: {}", logPrefix, webSearchResult);
            }

            Object content = message.get("content");
            String result = content instanceof String ? (String) content : content != null ? content.toString() : null;

            if (result != null && !result.isBlank()) {
                log.info("{} 搜索成功, 内容长度={}, 耗时={}ms", logPrefix, result.length(), elapsed);
                log.info("{} 内容预览: {}", logPrefix, result.length() > 300 ? result.substring(0, 300) + "..." : result);
                return new SearchExtractResult(result, start, System.currentTimeMillis(), elapsed);
            }

            log.warn("{} content为空, 耗时={}ms", logPrefix, elapsed);
            return null;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("{} 搜索异常: query={}, 耗时={}ms, error={}", logPrefix, query, elapsed, e.getMessage(), e);
            return null;
        }
    }
}
