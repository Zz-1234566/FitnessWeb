package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.config.AiModelConfig;
import com.zz.usercenter.mapper.ChatHistoryMapper;
import com.zz.usercenter.model.domain.ChatHistory;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserDailyMetric;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.model.domain.UserWeightRecord;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.DietRecordService;
import com.zz.usercenter.service.ExerciseRecordService;
import com.zz.usercenter.service.UserDailyMetricService;
import com.zz.usercenter.service.UserDietCycleService;
import com.zz.usercenter.service.UserDietDayTemplateService;
import com.zz.usercenter.service.UserDietTemplateService;
import com.zz.usercenter.service.UserRecordService;
import com.zz.usercenter.service.UserService;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import com.zz.usercenter.service.UserWeightRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class HealthScheduleService {

    @Resource
    private UserService userService;

    @Resource
    private AiModelConfig aiModelConfig;

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private UserRecordService userRecordService;

    @Resource
    private UserWeightRecordService userWeightRecordService;

    @Resource
    private UserDailyMetricService userDailyMetricService;

    @Resource
    private DietRecordService dietRecordService;

    @Resource
    private ExerciseRecordService exerciseRecordService;

    @Resource
    private UserTrainingCycleService userTrainingCycleService;

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("M月d日");
    private static final String[] WEEKDAYS = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private static final String[] DAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};
    /** 中国时区，避免凌晨定时任务 UTC 错位 */
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    private String formatDateDisplay(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
        return DISPLAY_FMT.format(date) + " " + WEEKDAYS[date.getDayOfWeek().getValue()];
    }

    /**
     * 每日AI总结 — 每天00:00触发
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailySummary() {
        String yesterday = LocalDate.now(CN_ZONE).minusDays(1).format(DATE_FMT);
        log.info("===== 每日AI总结任务开始，处理日期：{} =====", yesterday);

        List<User> users = userService.list();

        if (users.isEmpty()) {
            log.info("无用户需要生成每日总结");
            return;
        }

        for (User user : users) {
            try {
                processDailySummary(user, yesterday);
            } catch (Exception e) {
                log.error("用户[{}]每日总结生成失败", user.getId(), e);
            }
        }
        log.info("===== 每日AI总结任务结束 =====");
    }

    private void processDailySummary(User user, String date) {
        UserRecord userRecord = getOrCreateUserRecord(user.getId());
        LocalDate targetDate = LocalDate.parse(date, DATE_FMT);
        boolean hasExerciseRecord = exerciseRecordService.hasRecord(user.getId(), targetDate);
        boolean hasDietRecord = dietRecordService.hasRecord(user.getId(), targetDate);
        boolean hasRecord = hasExerciseRecord || hasDietRecord;

        if (!hasRecord) {
            String plannedSummary = buildPlannedDailySummary(user, date);
            if (plannedSummary != null && !plannedSummary.isBlank()) {
                appendDailyReview(userRecord, date, plannedSummary);
                log.info("用户[{}]无记录，已按训练计划生成昨日总结", user.getId());
                return;
            }

            List<String> reviews = parseStringArray(userRecord.getWeeklyReviews());
            reviews.add("【" + formatDateDisplay(date) + "】\n总结：昨天暂无记录。\n建议：昨日没记录哦，今天记得及时打卡。\n训练：暂无记录\n饮食：暂无记录\n问题：无");

            userRecord.setYesterdaySummary("总结：昨天暂无记录。\n建议：昨日没记录哦，今天记得及时打卡。\n训练：暂无记录\n饮食：暂无记录\n问题：无");
            userRecord.setWeeklyReviews(toJson(reviews));
            userRecordService.saveOrUpdateByUserId(userRecord);
            log.info("用户[{}]无记录且无训练计划，已写入默认昨日总结", user.getId());
            return;
        }

        String userProfile = user.getUserProfile() != null ? user.getUserProfile() : "暂无画像";
        String prompt = buildDailySummaryPrompt(
                userProfile,
                buildDailySummaryExerciseInput(user.getId(), targetDate),
                buildDailySummaryDietInput(user.getId(), targetDate),
                date
        );

        String aiResult = callAi(user.getModelPreference(), prompt, 1024, 0.7);

        if (aiResult == null || aiResult.isBlank()) {
            log.warn("用户[{}]AI返回为空，使用原始记录", user.getId());
            aiResult = "总结：今天已有训练或饮食记录。\n建议：建议继续保持记录，方便生成更准确总结。\n训练："
                    + buildDailySummaryExerciseInput(user.getId(), targetDate)
                    + "\n饮食：" + buildDailySummaryDietInput(user.getId(), targetDate)
                    + "\n问题：无";
        }

        appendDailyReview(userRecord, date, aiResult);
    }

    private String buildPlannedDailySummary(User user, String date) {
        String trainingPlanText = buildTrainingPlanText(user.getId());
        if (trainingPlanText == null || trainingPlanText.isBlank()) {
            return null;
        }

        LocalDate targetDate = LocalDate.parse(date, DATE_FMT);
        String dayName = DAY_NAMES[targetDate.getDayOfWeek().getValue() - 1];
        String daySection = extractDaySection(trainingPlanText, dayName);
        if (daySection == null || daySection.isBlank()) {
            return null;
        }

        String userProfile = user.getUserProfile() != null ? user.getUserProfile() : "暂无画像";
        String prompt = buildPlannedSummaryPrompt(userProfile, date, daySection);
        String aiResult = callAi(user.getModelPreference(), prompt, 1024, 0.5);
        if (aiResult == null || aiResult.isBlank()) {
            return buildPlannedSummaryFallback(daySection);
        }
        return appendNoRecordHintToAdvice(aiResult);
    }

    private String buildTrainingPlanText(Long userId) {
        if (userId == null) {
            return null;
        }
        UserTrainingCycleVO activeCycle = userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map<Long, UserTrainingTemplateVO> templateMap = userTrainingTemplateService.listTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        StringBuilder sb = new StringBuilder("训练计划\n");
        for (int i = 1; i <= activeCycle.getDayCount(); i++) {
            final int dayIndex = i;
            UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream()
                    .filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex)
                    .findFirst()
                    .orElse(null);
            sb.append("星期").append(DAY_NAMES[(i - 1) % 7]).append("：");
            if (day == null || day.getTemplateId() == null) {
                sb.append("休息\n");
                continue;
            }
            UserTrainingTemplateVO template = templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                sb.append(day.getTemplateName() == null ? "休息" : day.getTemplateName()).append("\n");
                continue;
            }
            sb.append(template.getName()).append("\n");
            appendTrainingSection(sb, "热身", "warmup", template.getItems());
            appendTrainingSection(sb, "正式训练", "main", template.getItems());
            appendTrainingSection(sb, "拉伸", "stretch", template.getItems());
        }
        return sb.toString().trim();
    }

    private void appendTrainingSection(StringBuilder sb, String label, String sectionType,
                                       List<UserTrainingTemplateVO.TrainingItemVO> items) {
        List<UserTrainingTemplateVO.TrainingItemVO> sectionItems = items.stream()
                .filter(item -> sectionType.equalsIgnoreCase(item.getSectionType()))
                .sorted(java.util.Comparator.comparing(UserTrainingTemplateVO.TrainingItemVO::getSortOrder,
                        java.util.Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (sectionItems.isEmpty()) {
            return;
        }
        sb.append(label).append("：");
        List<String> parts = new ArrayList<>();
        for (UserTrainingTemplateVO.TrainingItemVO item : sectionItems) {
            StringBuilder part = new StringBuilder(item.getExerciseName() == null ? "" : item.getExerciseName());
            List<String> detailParts = new ArrayList<>();
            if (item.getRecommendedSets() != null) {
                detailParts.add(item.getRecommendedSets() + "组");
            }
            if (item.getRecommendedReps() != null && !item.getRecommendedReps().isBlank()) {
                detailParts.add(item.getRecommendedReps());
            }
            if (!detailParts.isEmpty()) {
                part.append("（").append(String.join("，", detailParts)).append("）");
            }
            parts.add(part.toString());
        }
        sb.append(String.join("、", parts)).append("\n");
    }

    private void appendDailyReview(UserRecord userRecord, String date, String summary) {
        List<String> reviews = parseStringArray(userRecord.getWeeklyReviews());
        String dateHeader = "【" + formatDateDisplay(date) + "】";
        reviews.removeIf(review -> review != null && review.replaceAll("\\s+", "").startsWith(dateHeader.replaceAll("\\s+", "")));
        reviews.add(dateHeader + "\n" + summary);

        userRecord.setYesterdaySummary(summary);
        userRecord.setWeeklyReviews(toJson(reviews));
        userRecordService.saveOrUpdateByUserId(userRecord);
        log.info("用户[{}]每日总结已写入", userRecord.getUserId());
    }

    /**
     * 每周AI总结 — 每周一00:30触发。
     * 周一00:00先生成星期日总结，00:30再汇总完整的周一到周日。
     */
    @Scheduled(cron = "0 30 0 * * MON")
    public void weeklySummary() {
        log.info("===== 每周AI总结任务开始 =====");

        List<User> users = userService.list();

        if (users.isEmpty()) {
            log.info("无用户需要生成每周总结");
            return;
        }

        for (User user : users) {
            try {
                processWeeklySummary(user);
            } catch (Exception e) {
                log.error("用户[{}]每周总结生成失败", user.getId(), e);
            }
        }
        log.info("===== 每周AI总结任务结束 =====");
    }

    private void processWeeklySummary(User user) {
        UserRecord userRecord = userRecordService.getByUserId(user.getId());
        if (userRecord == null) {
            return;
        }

        List<String> reviews = parseStringArray(userRecord.getWeeklyReviews());
        if (reviews.isEmpty()) {
            return;
        }

        // 判断是否全是默认空记录（没有真实运动和饮食数据）
        boolean allEmpty = reviews.stream().allMatch(r -> r.contains("暂无记录"));

        String userProfile = user.getUserProfile() != null ? user.getUserProfile() : "暂无画像";
        String weeklyWeightSummary = buildWeeklyWeightSummary(user.getId());
        String weeklyCalorieSummary = buildWeeklyCalorieSummary(user.getId());

        if (allEmpty) {
            String summarySuffix = weeklyWeightSummary.contains("未记录")
                    ? ""
                    : " " + weeklyWeightSummary;
            String calorieSuffix = weeklyCalorieSummary.contains("未记录") ? "" : " " + weeklyCalorieSummary;
            String defaultWeekly = "总结：上周整体记录较少，暂时无法形成完整复盘。" + summarySuffix + calorieSuffix + "\n训练：上周缺少有效训练记录。\n饮食：上周缺少有效饮食记录。\n建议：下周尽量每天记录训练和饮食，方便生成更准确的总结。";
            userRecord.setWeeklySummary(defaultWeekly);
            userRecord.setWeeklyReviews(null);
            userRecordService.saveOrUpdateByUserId(userRecord);
            log.info("用户[{}]本周无有效记录，写入默认周总结", user.getId());
            return;
        }

        String allReviews = String.join("\n\n", reviews);

        String prompt = "你是健身助手Tatan，结合画像和本周每日总结生成上周复盘，同时更新用户画像。\n" +
                "画像：" + userProfile + "\n" +
                "请严格按以下格式输出，两部分之间用 ===PROFILE=== 分隔，不要添加其他内容：\n\n" +
                "【第一部分：周报】纯文本输出，禁止markdown格式(**加粗**、##标题等)，总字数限制在300字以内。\n" +
                "严格按以下格式输出，不要添加别的标题：\n" +
                "总结：一句话概括本周整体状态。\n" +
                "训练：概括本周练得怎么样，训练频率、完成度和表现如何。\n" +
                "饮食：概括本周吃得怎么样，饮食执行和营养情况如何。\n" +
                "建议：给出1条最值得执行的下周建议。\n" +
                "若存在体重记录，请在总结或建议中自然体现体重变化；若无记录，不要硬写体重趋势。\n" +
                "若存在热量记录，请结合热量盈亏判断饮食执行情况；若无记录，不要脑补。\n" +
                "数据真实，建议简洁落地。\n\n" +
                "===PROFILE===\n\n" +
                "【第二部分：画像更新】根据旧画像和周报，更新用户画像。\n" +
                "- 保留旧画像中仍然准确的内容（伤病、器械偏好、训练习惯等）\n" +
                "- 根据周报更新变化的部分\n" +
                "- 100字以内，不废话\n" +
                "- 不要加任何前缀，直接输出画像内容\n\n" +
                "体重变化：" + weeklyWeightSummary + "\n\n" +
                "热量变化：" + weeklyCalorieSummary + "\n\n" +
                "每日总结：\n" + allReviews;

        String aiResult = callAi(user.getModelPreference(), prompt, 1500, 0.7);

        String weeklySummary;
        String profileResult = null;
        if (aiResult == null || aiResult.isBlank()) {
            weeklySummary = "总结：本周总结生成失败，请查看每日记录了解详情。\n训练：暂无可靠汇总。\n饮食：暂无可靠汇总。\n建议：下周继续保持记录，方便生成更准确的周总结。";
        } else if (aiResult.contains("===PROFILE===")) {
            String[] parts = aiResult.split("===PROFILE===\\s*", 2);
            weeklySummary = parts[0].trim();
            profileResult = parts.length > 1 ? parts[1].trim() : null;
        } else {
            weeklySummary = aiResult.trim();
        }

        userRecord.setWeeklySummary(weeklySummary);
        if (profileResult != null && !profileResult.isBlank()) {
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setUserProfile(profileResult);
            userService.updateById(updateUser);
        }
        userRecord.setWeeklyReviews(null);
        userRecordService.saveOrUpdateByUserId(userRecord);

        log.info("用户[{}]每周总结+画像更新成功", user.getId());
    }

    private UserRecord getOrCreateUserRecord(Long userId) {
        UserRecord userRecord = userRecordService.getByUserId(userId);
        if (userRecord != null) {
            return userRecord;
        }
        UserRecord created = new UserRecord();
        created.setUserId(userId);
        return created;
    }

    private String callAi(String modelName, String prompt, int maxTokens, double temperature) {
        AiModelConfig.ModelProvider provider = aiModelConfig.getProvider(modelName);
        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(provider.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            log.error("调用AI失败，模型：{}", provider.getName(), e);
        }
        return null;
    }

    private String buildDailySummaryPrompt(String userProfile, String exerciseInput, String dietInput, String date) {
        return "你是健身助手Tatan，对" + formatDateDisplay(date) + "的记录做总结。\n" +
                "纯文本，禁止markdown，总字数限制在220字以内。严格按以下格式输出：\n" +
                "总结：一句话概括今天状态。\n" +
                "建议：给出1条最值得执行的建议。\n" +
                "训练：列出动作、组数或时长，并一句话点评。\n" +
                "饮食：概括当天饮食摄入。\n" +
                "问题：如无明显问题可写“无”。\n" +
                "数据真实，禁止虚构。\n\n" +
                "用户画像：" + userProfile + "\n" +
                "结构化训练记录：" + exerciseInput + "\n" +
                "结构化饮食记录：" + dietInput;
    }

    private String buildDailySummaryExerciseInput(Long userId, LocalDate date) {
        List<Map<String, Object>> sessions = exerciseRecordService.listLegacyRecords(userId, date);
        if (sessions.isEmpty()) {
            return "暂无训练记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> session : sessions) {
            String sessionName = String.valueOf(session.get("name") == null ? "" : session.get("name")).trim();
            Integer sessionDuration = parseInteger(session.get("durationSeconds"));
            if (!sessionName.isBlank()) {
                sb.append("训练：").append(sessionName);
                if (sessionDuration != null && sessionDuration > 0) {
                    sb.append("，").append(Math.max(1, sessionDuration / 60)).append("分钟");
                }
                sb.append("\n");
            }
            Object rawItems = session.get("items");
            if (!(rawItems instanceof List<?> items)) {
                continue;
            }
            for (Object rawItem : items) {
                if (!(rawItem instanceof Map<?, ?> item)) {
                    continue;
                }
                String name = String.valueOf(item.get("name") == null ? "" : item.get("name")).trim();
                String muscleGroup = String.valueOf(item.get("muscleGroup") == null ? "" : item.get("muscleGroup")).trim();
                Integer completedSets = parseInteger(item.get("completedSets"));
                Integer durationSeconds = parseInteger(item.get("durationSeconds"));
                if (name.isBlank()) {
                    continue;
                }
                sb.append("- ").append(name);
                if (!muscleGroup.isBlank()) {
                    sb.append("（").append(muscleGroup).append("）");
                }
                if (completedSets != null) {
                    sb.append("，").append(completedSets).append("组");
                }
                if (durationSeconds != null && durationSeconds > 0) {
                    sb.append("，").append(Math.max(1, durationSeconds / 60)).append("分钟");
                }
                sb.append("\n");
            }
        }
        return sb.length() == 0 ? "暂无训练记录" : sb.toString().trim();
    }

    private String buildDailySummaryDietInput(Long userId, LocalDate date) {
        List<Map<String, Object>> dietRecords = dietRecordService.listLegacyRecords(userId, date);
        if (dietRecords.isEmpty()) {
            return "暂无饮食记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : dietRecords) {
            String mealType = String.valueOf(item.get("mealType") == null ? "" : item.get("mealType")).trim();
            String name = String.valueOf(item.get("name") == null ? "" : item.get("name")).trim();
            if (name.isBlank()) {
                continue;
            }
            sb.append("- ");
            if (!mealType.isBlank()) {
                sb.append(mealType).append("：");
            }
            sb.append(name).append("\n");
        }
        return sb.length() == 0 ? "暂无饮食记录" : sb.toString().trim();
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String buildPlannedSummaryPrompt(String userProfile, String date, String daySection) {
        return "你是健身助手Tatan。用户在" + formatDateDisplay(date) + "没有主动记录运动和饮食，请根据训练计划生成一份提醒型昨日总结。\n" +
                "纯文本，禁止markdown，总字数限制在220字以内。严格按以下格式输出：\n" +
                "总结：一句话概括昨天应完成的安排。\n" +
                "建议：给出1条最值得执行的建议，并在这句里自然带上“昨日没记录哦”。\n" +
                "训练：根据计划概括昨天应完成的训练内容。\n" +
                "饮食：写“暂无饮食记录”。\n" +
                "问题：说明无法确认是否按计划完成。\n" +
                "不要假装用户已经完成训练，不要编造饮食记录。\n\n" +
                "用户画像：" + userProfile + "\n" +
                "昨日训练计划：" + daySection;
    }

    private String buildPlannedSummaryFallback(String daySection) {
        return "总结：昨天应按计划完成训练安排。\n" +
                "建议：昨日没记录哦，今天记得按计划完成并及时打卡。\n" +
                "训练：" + daySection + "\n" +
                "饮食：暂无饮食记录\n" +
                "问题：无法确认昨天是否按计划完成训练。";
    }

    private String buildWeeklyWeightSummary(Long userId) {
        LocalDate weekStart = LocalDate.now(CN_ZONE).minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<UserWeightRecord> records = userWeightRecordService.listByUserAndDateRange(userId, weekStart, weekEnd);
        if (records.isEmpty()) {
            return "本周未记录体重。";
        }

        UserWeightRecord first = records.get(0);
        UserWeightRecord last = records.get(records.size() - 1);
        if (records.size() == 1) {
            return "本周仅在" + formatWeightDate(first.getRecordDate()) + "记录体重"
                    + formatWeight(first.getWeight()) + "kg，暂无法判断周内变化。";
        }

        BigDecimal diff = last.getWeight().subtract(first.getWeight());
        String diffText = diff.compareTo(BigDecimal.ZERO) > 0
                ? "+" + formatWeight(diff)
                : formatWeight(diff);

        return "本周体重首条记录为" + formatWeightDate(first.getRecordDate()) + " "
                + formatWeight(first.getWeight()) + "kg，末条记录为"
                + formatWeightDate(last.getRecordDate()) + " "
                + formatWeight(last.getWeight()) + "kg，变化"
                + diffText + "kg，共记录" + records.size() + "次。";
    }

    private String buildWeeklyCalorieSummary(Long userId) {
        LocalDate weekStart = LocalDate.now(CN_ZONE).minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<UserDailyMetric> metrics = userDailyMetricService.listByUserAndDateRange(userId, weekStart, weekEnd);
        if (metrics.isEmpty()) {
            return "本周未记录热量数据。";
        }

        int recordedDays = 0;
        int intakeTotal = 0;
        double balanceTotal = 0D;
        double targetTotal = 0D;
        int targetCount = 0;
        for (UserDailyMetric metric : metrics) {
            if (metric.getIntakeCalories() != null) {
                recordedDays++;
                intakeTotal += metric.getIntakeCalories();
            }
            if (metric.getCalorieBalance() != null) {
                balanceTotal += metric.getCalorieBalance();
            }
            if (metric.getTargetCalories() != null) {
                targetTotal += metric.getTargetCalories();
                targetCount++;
            }
        }
        if (recordedDays == 0) {
            return "本周未记录热量数据。";
        }
        String balanceText = balanceTotal > 0
                ? "+" + Math.round(balanceTotal * 100.0) / 100.0
                : String.valueOf(Math.round(balanceTotal * 100.0) / 100.0);
        String targetText = targetCount == 0
                ? "暂无目标热量"
                : "平均目标" + Math.round((targetTotal / targetCount) * 100.0) / 100.0 + "kcal";
        return "本周记录热量" + recordedDays + "天，累计摄入" + intakeTotal + "kcal，" + targetText + "，累计盈亏" + balanceText + "kcal。";
    }

    private String formatWeightDate(LocalDate date) {
        return DISPLAY_FMT.format(date);
    }

    private String formatWeight(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }

    private String appendNoRecordHintToAdvice(String summary) {
        if (summary == null || summary.isBlank()) {
            return summary;
        }
        String normalized = summary.replace("\r\n", "\n");
        String[] lines = normalized.split("\n");
        StringBuilder result = new StringBuilder();
        boolean appended = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (!appended && (trimmed.startsWith("建议：") || trimmed.startsWith("建议:"))) {
                String separator = trimmed.startsWith("建议：") ? "建议：" : "建议:";
                String content = trimmed.substring(separator.length()).trim();
                if (!content.contains("昨日没记录哦")) {
                    line = separator + (content.isEmpty() ? "昨日没记录哦，今天记得及时打卡。" : content + " 昨日没记录哦。");
                }
                appended = true;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }

        if (!appended) {
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append("建议：昨日没记录哦，今天记得及时打卡。");
        }
        return result.toString();
    }

    private ChatHistory getChatHistory(Long userId) {
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        return chatHistoryMapper.selectOne(queryWrapper);
    }

    private String extractDaySection(String trainingPlan, String dayName) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:星期" + dayName + "|周" + dayName + ")[：:\\s]*(.*?)(?=(?:星期[一二三四五六日]|周[一二三四五六日])|$)",
                java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher matcher = pattern.matcher(trainingPlan);
        if (matcher.find()) {
            String section = matcher.group(1).trim();
            return section.isEmpty() ? null : section;
        }
        return null;
    }

    private List<String> parseStringArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return objectMapper.readValue(fixed, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return extractStringItems(json);
        }
    }

    private List<String> extractStringItems(String raw) {
        List<String> items = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\"((?:\\\\.|[^\"\\\\])*)\"", java.util.regex.Pattern.DOTALL)
                .matcher(raw);
        while (matcher.find()) {
            String item = matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\r", "")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
