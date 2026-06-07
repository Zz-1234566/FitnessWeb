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
import com.zz.usercenter.model.domain.UserProfile;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.model.domain.UserWeightRecord;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.ProfileExtractionService;
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
import com.zz.usercenter.service.UserProfileService;
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
import java.time.LocalTime;
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
    private UserProfileService userProfileService;

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    @Resource
    private ProfileExtractionService profileExtractionService;

    @Resource
    private UserDietCycleService userDietCycleService;

    @Resource
    private UserDietDayTemplateService userDietDayTemplateService;

    @Resource
    private UserDietTemplateService userDietTemplateService;

    @Resource
    private com.zz.usercenter.common.WeatherHelper weatherHelper;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("M月d日");
    private static final String[] WEEKDAYS = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private static final String[] DAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};
    private static final List<String> PLAN_MEAL_ORDER = List.of("早餐", "练后餐", "午餐", "加餐", "晚餐");
    /** 中国时区，避免凌晨定时任务 UTC 错位 */
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    private String formatDateDisplay(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
        return DISPLAY_FMT.format(date) + " " + WEEKDAYS[date.getDayOfWeek().getValue()];
    }

    /**
     * 每日画像更新 — 每天凌晨1:30，读取前一天提纯文本合并到用户画像
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void extractProfiles() {
        log.info("===== 每日画像更新任务开始 =====");
        try {
            profileExtractionService.extractAllPendingProfiles();
        } catch (Exception e) {
            log.error("每日画像更新任务异常", e);
        }
        log.info("===== 每日画像更新任务结束 =====");
    }

    /**
     * 每日凌晨0:05，将前一天的体重沿用写入当天（仅对当天无体重记录的用户）
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void carryOverWeight() {
        LocalDate today = LocalDate.now(CN_ZONE);
        log.info("===== 体重沿用任务开始 date={} =====", today);
        try {
            List<User> users = userService.list();
            int carried = 0;
            for (User user : users) {
                if (user.getId() == null) continue;
                // 当天已有记录则跳过
                List<UserWeightRecord> todayRecords = userWeightRecordService.listByUserAndDateRange(user.getId(), today, today);
                if (todayRecords != null && !todayRecords.isEmpty()) continue;
                // 取前一天体重
                Double latestWeight = userWeightRecordService.getLatestWeightAtOrBefore(user.getId(), today.minusDays(1));
                if (latestWeight == null || latestWeight <= 0) continue;
                // 写入当天
                userWeightRecordService.saveOrUpdateTodayWeight(user.getId(), latestWeight);
                carried++;
            }
            log.info("===== 体重沿用任务结束: totalUsers={}, carried={} =====", users.size(), carried);
        } catch (Exception e) {
            log.error("体重沿用任务异常", e);
        }
    }

    /**
     * 每周画像全量刷新 — 每周一00:20，此时dailySummary已完成，weeklyReviews包含完整7天总结
     */
    @Scheduled(cron = "0 20 0 * * MON")
    public void weeklyProfileRefresh() {
        log.info("===== 每周画像全量刷新任务开始 =====");
        try {
            profileExtractionService.weeklyProfileRefresh();
        } catch (Exception e) {
            log.error("每周画像全量刷新任务异常", e);
        }
        log.info("===== 每周画像全量刷新任务结束 =====");
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

        String userProfile = resolveUserProfileText(user.getId());
        Long userId = user.getId();

        // 构建当天计划对比文本
        String trainingPlanText = buildTodayTrainingPlanText(userId, DAY_NAMES[targetDate.getDayOfWeek().getValue() - 1]);
        String dietPlanText = buildTodayDietPlanText(userId, targetDate);
        Double targetCalories = resolveTargetCalories(userId);
        String weightText = buildTodayWeightText(userId, targetDate);

        String prompt = buildDailySummaryPrompt(
                userProfile,
                buildDailySummaryExerciseInput(userId, targetDate),
                buildDailySummaryDietInput(userId, targetDate),
                date,
                trainingPlanText,
                dietPlanText,
                targetCalories,
                weightText
        );

        String aiResult = callAi(resolvePurificationModelName(user), prompt, 1024, 0.7);

        if (aiResult == null || aiResult.isBlank()) {
            log.warn("用户[{}]AI返回为空，使用原始记录", user.getId());
            aiResult = "总结：今天已有训练或饮食记录。\n建议：建议继续保持记录，方便生成更准确总结。\n训练："
                    + buildDailySummaryExerciseInput(userId, targetDate)
                    + "\n饮食：" + buildDailySummaryDietInput(userId, targetDate)
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

        // 无记录分支：补充饮食计划和目标热量
        String dietPlanText = buildTodayDietPlanText(user.getId(), targetDate);
        Double targetCalories = resolveTargetCalories(user.getId());

        String userProfile = resolveUserProfileText(user.getId());
        String prompt = buildPlannedSummaryPrompt(userProfile, date, daySection, dietPlanText, targetCalories);
        String aiResult = callAi(resolvePurificationModelName(user), prompt, 1024, 0.5);
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
        Long userId = user.getId();
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

        String userProfile = resolveUserProfileText(userId);
        String weeklyWeightSummary = buildWeeklyWeightSummary(userId);
        String weeklyCalorieSummary = buildWeeklyCalorieSummary(userId);

        if (allEmpty) {
            String summarySuffix = weeklyWeightSummary.contains("未记录")
                    ? ""
                    : " " + weeklyWeightSummary;
            String calorieSuffix = weeklyCalorieSummary.contains("未记录") ? "" : " " + weeklyCalorieSummary;
            String defaultWeekly = "总结：上周整体记录较少，暂时无法形成完整复盘。" + summarySuffix + calorieSuffix + "\n训练：上周缺少有效训练记录。\n饮食：上周缺少有效饮食记录。\n建议：下周尽量每天记录训练和饮食，方便生成更准确的总结。";
            userRecord.setWeeklySummary(defaultWeekly);
            userRecord.setWeeklyReviews(null);
            userRecordService.saveOrUpdateByUserId(userRecord);
            log.info("用户[{}]本周无有效记录，写入默认周总结", userId);
            return;
        }

        // ========== 第一步：后端拼装7天结构化原始数据 ==========
        LocalDate weekStart = LocalDate.now(CN_ZONE).minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        String structuredRawData = buildWeeklyStructuredRawData(userId, weekStart, weekEnd);

        // ========== 第二步：提纯模型压缩原始数据 ==========
        String purificationPrompt = "你是一个数据整理助手。将以下一周的原始健身数据整理成简洁的结构化摘要。\n" +
                "规则：\n" +
                "- 保留所有关键数字（热量、体重、训练天数等），不要丢失数据\n" +
                "- 按训练、饮食、体重三个维度分块整理\n" +
                "- 去掉冗余描述，只保留关键信息\n" +
                "- 500字以内\n" +
                "- 纯文本，不要markdown格式\n\n" +
                structuredRawData;

        String modelName = resolvePurificationModelName(user);
        String purifiedData = callAi(modelName, purificationPrompt, 800, 0.3);

        // 提纯失败时用原始数据拼接（降级）
        String dataForSummary;
        if (purifiedData == null || purifiedData.isBlank()) {
            log.warn("用户[{}]周数据提纯失败，降级使用原始文本", userId);
            dataForSummary = "体重变化：" + weeklyWeightSummary + "\n热量变化：" + weeklyCalorieSummary
                    + "\n\n每日总结：\n" + String.join("\n\n", reviews);
        } else {
            dataForSummary = purifiedData.trim();
        }

        // ========== 第三步：聪明模型生成周报 ==========
        String summaryPrompt = "你是健身助手Tatan，结合用户画像和本周整理数据生成上周复盘。\n" +
                "画像：" + userProfile + "\n\n" +
                "纯文本输出，禁止markdown格式，总字数限制在300字以内。\n" +
                "严格按以下格式输出：\n" +
                "总结：一句话概括本周整体状态。\n" +
                "训练：概括本周练得怎么样，训练频率、完成度和表现如何。\n" +
                "饮食：概括本周吃得怎么样，饮食执行和营养情况如何。\n" +
                "建议：给出1条最值得执行的下周建议。\n" +
                "数据真实，建议简洁落地。\n\n" +
                "本周数据：\n" + dataForSummary;

        String aiResult = callAi(user.getModelPreference(), summaryPrompt, 1500, 0.7);

        String weeklySummary;
        if (aiResult == null || aiResult.isBlank()) {
            weeklySummary = "总结：本周总结生成失败，请查看每日记录了解详情。\n训练：暂无可靠汇总。\n饮食：暂无可靠汇总。\n建议：下周继续保持记录，方便生成更准确的周总结。";
        } else {
            weeklySummary = aiResult.trim();
        }
        userRecord.setWeeklySummary(weeklySummary);

        // 第四步：用周报+旧画像更新用户画像（与周报解耦）
        String profilePrompt = "你是健身助手Tatan，根据旧画像和本周周报更新用户画像。\n" +
                "旧画像：" + userProfile + "\n\n" +
                "本周周报：\n" + weeklySummary + "\n\n" +
                "规则：\n" +
                "- 保留旧画像中仍然准确的内容（伤病、器械偏好、训练习惯等）\n" +
                "- 根据周报更新变化的部分\n" +
                "- 100字以内，不废话\n" +
                "- 不要加任何前缀或标题，直接输出画像内容";

        String profileResult = callAi(user.getModelPreference(), profilePrompt, 300, 0.5);
        if (profileResult != null && !profileResult.isBlank()) {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setUserProfileText(profileResult.trim());
            userProfileService.saveOrUpdate(userId, profile);
        }
        userRecord.setWeeklyReviews(null);
        userRecordService.saveOrUpdateByUserId(userRecord);

        log.info("用户[{}]每周总结+画像更新成功（结构化数据→提纯→生成）", userId);
    }

    /**
     * 拼装一周7天的结构化原始数据（运动+饮食+计划+体重+热量）
     */
    private String buildWeeklyStructuredRawData(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        StringBuilder sb = new StringBuilder();

        // 1. 体重变化
        sb.append("【体重变化】\n").append(buildWeeklyWeightSummary(userId)).append("\n\n");

        // 2. 热量汇总
        sb.append("【热量汇总】\n").append(buildWeeklyCalorieSummary(userId)).append("\n\n");

        // 3. 每日明细（7天）
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            String dayName = WEEKDAYS[date.getDayOfWeek().getValue()];
            sb.append("【").append(DISPLAY_FMT.format(date)).append(" ").append(dayName).append("】\n");

            // 训练
            String exerciseInput = buildDailySummaryExerciseInput(userId, date);
            sb.append("训练记录：").append(exerciseInput).append("\n");

            // 饮食
            String dietInput = buildDailySummaryDietInput(userId, date);
            sb.append("饮食记录：").append(dietInput).append("\n");

            // 计划对比
            String dayIndex = DAY_NAMES[date.getDayOfWeek().getValue() - 1];
            String trainingPlan = buildTodayTrainingPlanText(userId, dayIndex);
            String dietPlan = buildTodayDietPlanText(userId, date);
            if (trainingPlan != null) {
                sb.append("训练计划：").append(trainingPlan).append("\n");
            }
            if (dietPlan != null) {
                sb.append("饮食计划：").append(dietPlan).append("\n");
            }

            sb.append("\n");
        }

        // 4. 目标热量
        Double targetCal = resolveTargetCalories(userId);
        if (targetCal != null) {
            sb.append("【目标热量】每日").append(targetCal.intValue()).append("大卡\n");
        }

        return sb.toString().trim();
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
        String resolvedModel = resolveModelName(modelName);
        AiModelConfig.ModelProvider provider = aiModelConfig.getProvider(resolvedModel);
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

    private String resolveModelName(String raw) {
        if (raw == null || raw.isBlank()) return aiModelConfig.getDefaultModel();
        try {
            Map<String, String> map = new ObjectMapper().readValue(raw, new TypeReference<Map<String, String>>() {});
            String current = map.get("current");
            return (current != null && !current.isBlank()) ? current : aiModelConfig.getDefaultModel();
        } catch (Exception e) {
            return raw.isBlank() ? aiModelConfig.getDefaultModel() : raw;
        }
    }

    private String resolveUserProfileText(Long userId) {
        UserProfile p = userProfileService.getByUserId(userId);
        if (p != null && p.getUserProfileText() != null && !p.getUserProfileText().isBlank()) {
            return p.getUserProfileText();
        }
        return "暂无画像";
    }

    private String buildDailySummaryPrompt(String userProfile, String exerciseInput, String dietInput, String date,
                                            String trainingPlanText, String dietPlanText,
                                            Double targetCalories, String weightText) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是健身助手Tatan，对").append(formatDateDisplay(date)).append("的记录做总结。\n");
        sb.append("纯文本，禁止markdown，总字数限制在280字以内。严格按以下格式输出：\n");
        sb.append("总结：一句话概括今天状态。\n");
        sb.append("建议：给出1条最值得执行的建议。\n");
        sb.append("训练：列出动作、组数或时长，并一句话点评。如有训练计划，对比计划完成度。\n");
        sb.append("饮食：概括当天饮食摄入。如有饮食计划，对比计划执行度。\n");
        sb.append("匹配度：如果训练和饮食数据都有，分析训练消耗与饮食摄入的匹配情况（蛋白质是否充足、热量是否合理），表扬好的指出不足的。\n");
        sb.append("问题：如无明显问题可写\"无\"。\n");
        sb.append("数据真实，禁止虚构。\n\n");
        sb.append("用户画像：").append(userProfile).append("\n");
        if (weightText != null) {
            sb.append("体重：").append(weightText).append("\n");
        }
        if (targetCalories != null) {
            sb.append("目标热量：").append(targetCalories.intValue()).append("大卡\n");
        }
        if (trainingPlanText != null) {
            sb.append("今日训练计划：").append(trainingPlanText).append("\n");
        }
        if (dietPlanText != null) {
            sb.append("今日饮食计划：").append(dietPlanText).append("\n");
        }
        sb.append("结构化训练记录：").append(exerciseInput).append("\n");
        sb.append("结构化饮食记录：").append(dietInput);
        return sb.toString();
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
            Integer sessionCalories = parseInteger(session.get("caloriesBurned"));
            if (!sessionName.isBlank()) {
                sb.append("训练：").append(sessionName);
                if (sessionDuration != null && sessionDuration > 0) {
                    sb.append("，").append(Math.max(1, sessionDuration / 60)).append("分钟");
                }
                if (sessionCalories != null && sessionCalories > 0) {
                    sb.append("，消耗").append(sessionCalories).append("大卡");
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
        // 追加当日总消耗
        int totalBurned = exerciseRecordService.getDayTotalCaloriesBurned(userId, date);
        if (totalBurned > 0) {
            sb.append("\n当日总消耗：").append(totalBurned).append("大卡");
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
            sb.append(name);
            // 追加每餐热量
            Object cal = item.get("calories");
            if (cal != null) {
                sb.append("（").append(cal).append("kcal）");
            }
            sb.append("\n");
        }
        // 追加宏量汇总
        Map<String, Object> macro = dietRecordService.getDayMacroSummary(userId, date);
        if (Boolean.TRUE.equals(macro.get("hasRecord"))) {
            sb.append("\n当日摄入汇总：")
                    .append("热量").append(macro.get("calories")).append("kcal")
                    .append("，蛋白质").append(macro.get("protein")).append("g")
                    .append("，碳水").append(macro.get("carbs")).append("g")
                    .append("，脂肪").append(macro.get("fat")).append("g");
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

    private String buildPlannedSummaryPrompt(String userProfile, String date, String daySection,
                                              String dietPlanText, Double targetCalories) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是健身助手Tatan。用户在").append(formatDateDisplay(date))
                .append("没有主动记录运动和饮食，请根据计划生成一份提醒型昨日总结。\n");
        sb.append("纯文本，禁止markdown，总字数限制在220字以内。严格按以下格式输出：\n");
        sb.append("总结：一句话概括昨天应完成的安排。\n");
        sb.append("建议：给出1条最值得执行的建议，并在这句里自然带上\"昨日没记录哦\"。\n");
        sb.append("训练：根据计划概括昨天应完成的训练内容。\n");
        sb.append("饮食：");
        if (dietPlanText != null) {
            sb.append("根据饮食计划概括昨天应摄入的内容，但用户没有记录实际饮食。\n");
        } else {
            sb.append("暂无饮食记录。\n");
        }
        sb.append("问题：说明无法确认是否按计划完成。\n");
        sb.append("不要假装用户已经完成训练，不要编造饮食记录。\n\n");
        sb.append("用户画像：").append(userProfile).append("\n");
        if (targetCalories != null) {
            sb.append("目标热量：").append(targetCalories.intValue()).append("大卡\n");
        }
        sb.append("昨日训练计划：").append(daySection);
        if (dietPlanText != null) {
            sb.append("\n昨日饮食计划：").append(dietPlanText);
        }
        return sb.toString();
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

    // ========== 晚间主动提醒 ==========

    private static final LocalTime EVENING_REMINDER_TIME = LocalTime.of(20, 0);

    /**
     * 晚间主动提醒 — 每天19:50，检查用户今日记录vs计划，有问题则调AI生成提醒
     */
    @Scheduled(cron = "0 50 19 * * ?")
    public void eveningReminder() {
        String today = LocalDate.now(CN_ZONE).format(DATE_FMT);
        log.info("===== 晚间主动提醒任务开始，日期：{} =====", today);

        List<User> users = userService.list();
        if (users.isEmpty()) {
            log.info("无用户需要生成晚间提醒");
            return;
        }

        for (User user : users) {
            try {
                processEveningReminder(user, LocalDate.now(CN_ZONE));
            } catch (Exception e) {
                log.error("用户[{}]晚间提醒生成失败", user.getId(), e);
            }
        }
        log.info("===== 晚间主动提醒任务结束 =====");
    }

    private void processEveningReminder(User user, LocalDate today) {
        Long userId = user.getId();
        boolean hasTrainingPlan = hasActiveTrainingPlan(userId);
        boolean hasDietPlan = hasActiveDietPlan(userId);
        boolean hasExerciseRecord = exerciseRecordService.hasRecord(userId, today);
        boolean hasDietRecord = dietRecordService.hasRecord(userId, today);

        // 没有任何计划和记录，不需要提醒
        if (!hasTrainingPlan && !hasDietPlan && !hasExerciseRecord && !hasDietRecord) {
            return;
        }

        // 两种情况需要提醒：有计划但没记录，或者有记录但需要对比
        String planVsRecordDesc = buildPlanVsRecordDescription(
                userId, today, hasTrainingPlan, hasDietPlan, hasExerciseRecord, hasDietRecord);

        if (planVsRecordDesc == null || planVsRecordDesc.isBlank()) {
            return;
        }

        // 调提纯模型生成提醒内容
        String userProfile = resolveUserProfileText(userId);
        String prompt = buildEveningReminderPrompt(userProfile, planVsRecordDesc);
        String modelName = resolvePurificationModelName(user);
        String aiResult = callAi(modelName, prompt, 256, 0.7);

        if (aiResult == null || aiResult.isBlank()) {
            aiResult = buildEveningReminderFallback(planVsRecordDesc);
        }

        // 存入 UserRecord
        UserRecord userRecord = getOrCreateUserRecord(userId);
        userRecord.setEveningReminder(aiResult);
        userRecordService.saveOrUpdateByUserId(userRecord);
        log.info("用户[{}]晚间提醒已生成", userId);
    }

    private boolean hasActiveTrainingPlan(Long userId) {
        UserTrainingCycleVO cycle = userTrainingCycleService.getActiveCycle(userId);
        return cycle != null;
    }

    private boolean hasActiveDietPlan(Long userId) {
        return userDietCycleService.getActiveCycle(userId) != null;
    }

    private String buildPlanVsRecordDescription(Long userId, LocalDate today,
                                                 boolean hasTrainingPlan, boolean hasDietPlan,
                                                 boolean hasExerciseRecord, boolean hasDietRecord) {
        StringBuilder desc = new StringBuilder();
        String dayName = DAY_NAMES[today.getDayOfWeek().getValue() - 1];

        if (hasTrainingPlan) {
            String trainingPlanText = buildTodayTrainingPlanText(userId, dayName);
            if (hasExerciseRecord) {
                String exerciseRecordText = buildDailySummaryExerciseInput(userId, today);
                desc.append("今日训练计划：").append(trainingPlanText != null ? trainingPlanText : "休息日");
                desc.append("\n今日实际训练：").append(exerciseRecordText);
            } else {
                desc.append("今日训练计划：").append(trainingPlanText != null ? trainingPlanText : "休息日");
                desc.append("\n今日实际训练：无记录");
            }
        } else if (hasExerciseRecord) {
            // 没有计划但有记录，也展示
            String exerciseRecordText = buildDailySummaryExerciseInput(userId, today);
            desc.append("今日训练记录：").append(exerciseRecordText);
        }

        if (hasDietPlan) {
            String dietPlanText = buildTodayDietPlanText(userId, today);
            if (desc.length() > 0) desc.append("\n\n");
            if (hasDietRecord) {
                String dietRecordText = buildDailySummaryDietInput(userId, today);
                desc.append("今日饮食计划：").append(dietPlanText != null ? dietPlanText : "已安排");
                desc.append("\n今日实际饮食：").append(dietRecordText);
            } else {
                desc.append("今日饮食计划：").append(dietPlanText != null ? dietPlanText : "已安排");
                desc.append("\n今日实际饮食：无记录");
            }
        } else if (hasDietRecord) {
            // 没有计划但有记录，也展示
            String dietRecordText = buildDailySummaryDietInput(userId, today);
            if (desc.length() > 0) desc.append("\n\n");
            desc.append("今日饮食记录：").append(dietRecordText);
        }

        // 追加目标热量对比
        if (hasExerciseRecord || hasDietRecord) {
            desc.append("\n\n");
            int burned = exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            Map<String, Object> macro = dietRecordService.getDayMacroSummary(userId, today);
            int intake = (int) macro.getOrDefault("calories", 0);
            desc.append("训练消耗：").append(burned).append("大卡");
            desc.append("\n饮食摄入：").append(intake).append("大卡");
            Double targetCal = resolveTargetCalories(userId);
            if (targetCal != null) {
                desc.append("\n目标热量：").append(targetCal.intValue()).append("大卡");
            }
            if (burned > 0 || intake > 0) {
                desc.append("\n净热量：").append(intake - burned).append("大卡");
            }
        }

        return desc.toString().trim();
    }

    private String buildTodayTrainingPlanText(Long userId, String dayName) {
        String fullPlanText = buildTrainingPlanText(userId);
        if (fullPlanText == null || fullPlanText.isBlank()) {
            return null;
        }
        String section = extractDaySection(fullPlanText, dayName);
        return (section != null && !section.isBlank()) ? section : "休息日";
    }

    private String resolvePurificationModelName(User user) {
        try {
            Map<String, String> prefs = new ObjectMapper().readValue(
                    user.getModelPreference(), new TypeReference<Map<String, String>>() {});
            String pm = prefs.get("purificationModel");
            return (pm != null && !pm.isBlank()) ? pm.trim() : aiModelConfig.getPurificationModel();
        } catch (Exception e) {
            return aiModelConfig.getPurificationModel();
        }
    }

    private String buildEveningReminderPrompt(String userProfile, String planVsRecord) {
        return "你是健身助手Tatan，现在是晚上7:50，根据用户今日的计划和实际记录做一段简短友好的提醒。\n" +
                "纯文本，禁止markdown，字数150字以内。\n" +
                "要求：\n" +
                "1. 如果用户已完成计划，给予鼓励\n" +
                "2. 如果用户没记录或有差距，温和提醒\n" +
                "3. 结合训练消耗和饮食摄入的匹配度给出建议（如蛋白质是否充足）\n" +
                "4. 语气轻松自然，像朋友一样\n" +
                "5. 只输出提醒内容，不要输出其他分析\n\n" +
                "用户画像：" + userProfile + "\n" +
                "今日计划与记录对比：\n" + planVsRecord;
    }

    private String buildEveningReminderFallback(String planVsRecord) {
        return "晚上好！今天还没记录运动和饮食哦，记得打卡~";
    }

    // ========== 早间天气+训练计划提醒 ==========

    /**
     * 早间提醒 — 每天07:50，获取天气+今日训练计划，调AI生成提醒
     */
    @Scheduled(cron = "0 50 7 * * ?")
    public void morningReminder() {
        String today = LocalDate.now(CN_ZONE).format(DATE_FMT);
        log.info("===== 早间天气提醒任务开始，日期：{} =====", today);

        List<User> users = userService.list();
        if (users.isEmpty()) {
            log.info("无用户需要生成早间提醒");
            return;
        }

        for (User user : users) {
            try {
                processMorningReminder(user, LocalDate.now(CN_ZONE));
            } catch (Exception e) {
                log.error("用户[{}]早间提醒生成失败", user.getId(), e);
            }
        }
        log.info("===== 早间天气提醒任务结束 =====");
    }

    private void processMorningReminder(User user, LocalDate today) {
        Long userId = user.getId();
        String dayName = DAY_NAMES[today.getDayOfWeek().getValue() - 1];

        // 1. 获取天气（优先用用户城市，否则默认北京）
        String userCity = (user.getCity() != null && !user.getCity().isBlank()) ? user.getCity() : "北京";
        String userCityEn = (user.getCityEn() != null && !user.getCityEn().isBlank()) ? user.getCityEn() : "Beijing";
        String weatherContext = weatherHelper.buildWeatherContext(userCity, userCityEn);

        // 2. 获取今日训练计划
        String trainingPlanText = null;
        UserTrainingCycleVO activeCycle = userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle != null) {
            trainingPlanText = buildTodayTrainingPlanText(userId, dayName);
        }

        if (weatherContext == null && trainingPlanText == null) {
            return;
        }

        // 3. 调AI生成提醒
        String userProfile = resolveUserProfileText(userId);
        String prompt = buildMorningReminderPrompt(userProfile, weatherContext, trainingPlanText, dayName);
        String modelName = resolvePurificationModelName(user);
        String aiResult = callAi(modelName, prompt, 256, 0.7);

        if (aiResult == null || aiResult.isBlank()) {
            aiResult = buildMorningReminderFallback(weatherContext, trainingPlanText);
        }

        // 4. 存入 UserRecord
        UserRecord userRecord = getOrCreateUserRecord(userId);
        userRecord.setMorningReminder(aiResult);
        userRecordService.saveOrUpdateByUserId(userRecord);
        log.info("用户[{}]早间提醒已生成", userId);
    }

    private String buildMorningReminderPrompt(String userProfile, String weatherContext,
                                                String trainingPlanText, String dayName) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是健身助手Tatan，现在是早上7:50，给用户发一条简短的早间提醒。\n");
        sb.append("纯文本，禁止markdown，字数150字以内。\n");
        sb.append("要求：\n");
        sb.append("1. 先播报今日天气。判断冷暖体感以气温和湿度数据为准，\"天气评估\"标签是后端根据温度/湿度阈值算好的，直接使用即可。注意：阴天/多云/晴只表示云量多少，不代表体感温度，不要用它们来判断热不热\n");
        sb.append("2. 如果今天有训练计划，告诉用户今天该练什么\n");
        sb.append("3. 如果今天是休息日，提醒适当恢复\n");
        sb.append("4. 提醒用户早上记得记录体重\n");
        sb.append("5. 语气轻松自然，像朋友一样\n");
        sb.append("6. 只输出提醒内容，不要输出其他分析\n\n");
        sb.append("用户画像：").append(userProfile).append("\n");
        if (weatherContext != null) {
            sb.append("今日天气：\n").append(weatherContext).append("\n");
        }
        if (trainingPlanText != null) {
            sb.append("周").append(dayName).append("训练计划：").append(trainingPlanText).append("\n");
        } else {
            sb.append("训练计划：无激活计划或今天是休息日\n");
        }
        return sb.toString();
    }

    private String buildMorningReminderFallback(String weatherContext, String trainingPlanText) {
        StringBuilder sb = new StringBuilder();
        sb.append("早上好！新的一天开始了~");
        if (weatherContext != null) {
            sb.append("记得关注天气变化，合理安排训练。");
        }
        if (trainingPlanText != null) {
            sb.append("今天有训练计划哦，加油完成！");
        }
        sb.append("记得称一下体重记录一下哦~");
        return sb.toString();
    }

    // ========== 公共工具方法 ==========

    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) return "加餐";
        if (mealType.contains("练后")) return "练后餐";
        if (mealType.contains("早餐")) return "早餐";
        if (mealType.contains("午餐") || mealType.contains("午饭")) return "午餐";
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) return "晚餐";
        return "加餐";
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * 获取用户目标热量（优先customDailyCalories，fallback dailyCalorieBurn）
     */
    private Double resolveTargetCalories(Long userId) {
        UserProfile p = userProfileService.getByUserId(userId);
        if (p == null) return null;
        if (p.getCustomDailyCalories() != null && p.getCustomDailyCalories() > 0) {
            return p.getCustomDailyCalories();
        }
        if (p.getDailyCalorieBurn() != null && p.getDailyCalorieBurn() > 0) {
            return p.getDailyCalorieBurn();
        }
        return null;
    }

    /**
     * 构建当天饮食计划文本（具体食物+用量+热量）
     */
    private String buildTodayDietPlanText(Long userId, LocalDate date) {
        if (userId == null) return null;
        UserDietCycleVO activeCycle = userDietCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map<Long, UserDietDayTemplateVO> dayTemplateMap = userDietDayTemplateService.listDayTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserDietDayTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, UserDietTemplateVO> mealTemplateMap = userDietTemplateService.listTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        String dayName = DAY_NAMES[date.getDayOfWeek().getValue() - 1];
        // 根据日期的星期几匹配对应的dayIndex
        int targetDayIndex = date.getDayOfWeek().getValue(); // 1=周一...7=周日
        // cycle的dayIndex从1开始，dayCount决定循环
        int cycleDayIndex = ((targetDayIndex - 1) % activeCycle.getDayCount()) + 1;

        final int dayIdx = cycleDayIndex;
        UserDietCycleVO.CycleDayVO day = activeCycle.getDays().stream()
                .filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIdx)
                .findFirst()
                .orElse(null);
        if (day == null || day.getDayTemplateId() == null) {
            return null;
        }

        UserDietDayTemplateVO dayTemplate = dayTemplateMap.get(day.getDayTemplateId());
        if (dayTemplate == null || dayTemplate.getMealSlots() == null || dayTemplate.getMealSlots().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (String mealType : PLAN_MEAL_ORDER) {
            UserDietDayTemplateVO.MealSlotVO slot = dayTemplate.getMealSlots().stream()
                    .filter(meal -> mealType.equals(normalizeMealType(meal.getMealType())))
                    .findFirst()
                    .orElse(null);
            if (slot == null || slot.getTemplateId() == null) continue;
            UserDietTemplateVO mealTemplate = mealTemplateMap.get(slot.getTemplateId());
            if (mealTemplate == null || mealTemplate.getItems() == null || mealTemplate.getItems().isEmpty()) continue;
            sb.append(mealType).append("：");
            List<String> foodNames = new ArrayList<>();
            for (UserDietTemplateVO.DietTemplateItemVO item : mealTemplate.getItems()) {
                foodNames.add(safeTrim(item.getFoodName())
                        + (item.getAmount() != null ? item.getAmount().stripTrailingZeros().toPlainString() : "")
                        + safeTrim(item.getUnit()));
            }
            sb.append(String.join("、", foodNames)).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 构建当天体重文本
     */
    private String buildTodayWeightText(Long userId, LocalDate date) {
        List<UserWeightRecord> records = userWeightRecordService.listByUserAndDateRange(userId, date, date);
        if (records.isEmpty()) {
            Double latest = userWeightRecordService.getLatestWeightAtOrBefore(userId, date.minusDays(1));
            if (latest != null && latest > 0) {
                return "昨日体重" + latest + "kg，今日未记录。";
            }
            return null;
        }
        BigDecimal weight = records.get(records.size() - 1).getWeight();
        String weightText = weight.stripTrailingZeros().toPlainString() + "kg";
        // 对比目标体重
        UserProfile p = userProfileService.getByUserId(userId);
        if (p != null && p.getTargetWeight() != null && p.getTargetWeight() > 0) {
            BigDecimal diff = weight.subtract(BigDecimal.valueOf(p.getTargetWeight()));
            String diffText = diff.compareTo(BigDecimal.ZERO) > 0
                    ? "距目标+" + diff.stripTrailingZeros().toPlainString() + "kg"
                    : "距目标" + diff.stripTrailingZeros().toPlainString() + "kg";
            return "今日体重" + weightText + "（" + diffText + "）";
        }
        return "今日体重" + weightText;
    }
}
