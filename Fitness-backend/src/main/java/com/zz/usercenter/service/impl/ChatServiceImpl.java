package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zz.usercenter.config.AiModelConfig;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.ChatHistoryMapper;
import com.zz.usercenter.model.domain.*;
import com.zz.usercenter.model.domain.request.*;
import com.zz.usercenter.model.domain.vo.*;
import com.zz.usercenter.service.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zz.usercenter.common.StateCode.AI_ERROR;
import static com.zz.usercenter.common.StateCode.NULL_ERROR;
import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

/**
 * AI对话 Service 实现类
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Resource
    private ChatHistoryMapper chatHistoryMapper;

    @Resource
    private UserService userService;

    @Resource
    private ExerciseService exerciseService;

    @Resource
    private UserRecordService userRecordService;

    @Resource
    private UserDailyMetricService userDailyMetricService;

    @Resource
    private DietRecordService dietRecordService;

    @Resource
    private ExerciseRecordService exerciseRecordService;

    @Resource
    private FoodItemService foodItemService;

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    @Resource
    private UserTrainingCycleService userTrainingCycleService;

    @Resource
    private UserDietTemplateService userDietTemplateService;

    @Resource
    private UserDietDayTemplateService userDietDayTemplateService;

    @Resource
    private UserDietCycleService userDietCycleService;

    @Resource
    private AiModelConfig aiModelConfig;

    @Resource
    private VectorStore vectorStore;

    /** 中国时区，统一用于所有日期计算，避免凌晨0-8点UTC错位 */
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECENT_DIALOG_LIMIT = 5;
    private static final int QUICK_STREAM_CHUNK_SIZE = 2;
    private static final long QUICK_STREAM_DELAY_MS = 42L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern DIET_SINGLE_MEAL_PATTERN = Pattern.compile("(早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜|练后餐)");
    private static final List<String> PLAN_MEAL_ORDER = List.of("早餐", "练后餐", "午餐", "加餐", "晚餐");
    private static final List<String> PLAN_MUSCLE_GROUP_ORDER = List.of("chest", "back", "shoulders", "arms", "legs", "core");
    private static final BigDecimal KJ_TO_KCAL_DIVISOR = new BigDecimal("4.184");
    private static final ThreadLocal<String> ACTIVE_CHAT_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<WebClient> CACHED_WEB_CLIENT = new ThreadLocal<>();
    private static final ThreadLocal<String> CACHED_TRAINING_PLAN_TEXT = new ThreadLocal<>();

    /**
     * 流式处理用户消息（SSE协议逐字推送到前端）
     * <p>
     * 处理流程：安全检查 → 快速匹配 → Function Calling 工具循环 → 流式回复
     */
    @Override
    public void sendMessageStream(Long userId, String message, OutputStream outputStream,
                                  StringBuilder resultHolder, java.util.concurrent.atomic.AtomicReference<String> replyTypeHolder) {
        try {
            securityCheck(message);

            String quickReply = quickMatch(message);
            if (quickReply != null) {
                writeSseDataGradually(outputStream, quickReply);
                resultHolder.append(quickReply);
                return;
            }

            User user = userService.getById(userId);
            ACTIVE_CHAT_MODEL.set(resolveUserModelName(user));

            ChatHistory history = getChatHistory(userId);
            UserRecord userRecord = userRecordService.getByUserId(userId);

            // 快速匹配：能直接路由的意图不走AI工具调用
            DirectRouteResult direct = directRoute(message, userId, user, userRecord);
            if (direct != null) {
                // 拿到了工具数据，让AI流式总结
                String summarized = streamSummarizeWithToolData(userId, message, direct.toolData(),
                        direct.systemPrompt(), outputStream, resultHolder);
                if (replyTypeHolder != null) {
                    replyTypeHolder.set(direct.replyType());
                }
                if (!summarized.isBlank()) {
                    saveOrUpdateChatHistory(userId, message, summarized);
                }
                return;
            }

            // AI 意图分类：让AI通过JSON返回意图，后端解析执行（不依赖function calling）
            AiIntentSaveResult aiClassify = classifyIntentByAi(userId, message, userRecord);
            if (aiClassify != null) {
                // AI 判定为保存意图，已执行保存，直接返回确认
                if (replyTypeHolder != null) {
                    replyTypeHolder.set(aiClassify.replyType());
                }
                writeSseDataGradually(outputStream, aiClassify.replyText());
                resultHolder.append(aiClassify.replyText());
                saveOrUpdateChatHistory(userId, message, aiClassify.replyText());
                return;
            }

            ToolCallResult result = executeToolCallingLoop(userId, message, user, history,
                    userRecord, outputStream, resultHolder);

            if (replyTypeHolder != null) {
                replyTypeHolder.set(result.replyType());
            }

            String fullResponse = resultHolder.toString();
            if (!fullResponse.isBlank()) {
                saveOrUpdateChatHistory(userId, message, fullResponse);
            }
        } catch (Exception e) {
            log.error("SSE流式对话异常", e);
            try {
                writeSseDataGradually(outputStream, "不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。");
            } catch (Exception ignored) {}
        } finally {
            ACTIVE_CHAT_MODEL.remove();
            CACHED_WEB_CLIENT.remove();
            CACHED_TRAINING_PLAN_TEXT.remove();
        }
    }

    /**
     * 向SSE流写入一条数据
     * 换行符必须转义为 \\n，因为SSE协议以 \n\n 作为消息边界，
     * 如果内容本身含 \n 会导致前端解析错位
     */
    private void writeSseData(OutputStream outputStream, String content) throws Exception {
        String escaped = content.replace("\n", "\\n");
        outputStream.write(("data:" + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * 给快速匹配类固定回复也做成轻量流式输出，避免前端一次性整段重排。
     */
    private void writeSseDataGradually(OutputStream outputStream, String content) throws Exception {
        if (content == null || content.isEmpty()) {
            return;
        }
        for (int i = 0; i < content.length(); i += QUICK_STREAM_CHUNK_SIZE) {
            int end = Math.min(content.length(), i + QUICK_STREAM_CHUNK_SIZE);
            writeSseData(outputStream, content.substring(i, end));
            if (end < content.length()) {
                try {
                    Thread.sleep(QUICK_STREAM_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 获取用户对话记录
     */
    @Override
    public ChatHistory getChatHistory(Long userId) {
        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        return chatHistoryMapper.selectOne(queryWrapper);
    }



    // ==================== 第一层：正则快速匹配 ====================

    private String quickMatch(String message) {
        String msg = message.toLowerCase();

        // 问候语（短句才匹配，避免拦截"你好帮我制定训练计划"）
        if (msg.length() <= 6 && Pattern.compile(".*(你好|hi|hello|嗨|hey|在吗).*").matcher(msg).matches()) {
            return "你好！我是你的智能健身助手 Tatan，有什么可以帮你的吗？";
        }

        // 自我介绍
        if (Pattern.compile(".*(你是谁|你叫什么|介绍.*自己|你能做什么).*").matcher(msg).matches()) {
            return "我是 Tatan 智能健身助手，可以帮你：\n1. 回答健身相关问题\n2. 制定个性化训练计划\n3. 提供饮食建议\n4. 聊聊健身心得，给你打气加油！";
        }

        // 感谢（短句才匹配，避免拦截"谢谢，再帮我制定个饮食计划"）
        if (msg.length() <= 6 && Pattern.compile(".*(谢谢|感谢|多谢|thanks).*").matcher(msg).matches()) {
            return "不客气！有任何健身问题随时问我，我会一直在这里陪你。";
        }

        return null;
    }

    // ==================== 快速意图匹配（正则直接路由） ====================

    private record DirectRouteResult(String replyType, String toolData, String systemPrompt) {}

    /**
     * 用正则直接匹配用户意图，执行对应工具拿到数据。
     * 匹配到返回 DirectRouteResult，匹配不到返回 null（交给AI function calling兜底）。
     */
    private DirectRouteResult directRoute(String message, Long userId, User user, UserRecord userRecord) {
        String msg = message.toLowerCase();

        // --- 查训练计划："练什么/该练什么/训练安排"（无"了"字，问未来安排） ---
        if (msg.contains("练什么") || msg.contains("该练") || msg.contains("训练安排") || msg.contains("健身计划")
                || (msg.contains("计划") && msg.contains("练"))) {
            if (!msg.contains("练了") && !msg.contains("做了")) {
                String targetDay = msg.contains("明天") ? "明天" : "今天";
                String data;
                if ("明天".equals(targetDay)) {
                    data = buildTrainingPlanDaySection(userId, 1);
                } else {
                    data = buildTrainingPlanDaySection(userId, 0);
                }
                String toolData = (data != null && !data.isBlank()) ? data : "暂无训练安排";
                return new DirectRouteResult("today_training_plan", toolData,
                        "你是健身助手Tatan。根据下方工具返回的训练计划数据，简洁亲切地回答用户。纯文本，不用markdown。如果数据是'暂无'，友好告知用户还没有训练计划。");
            }
        }

        // --- 查训练记录："练了什么/运动记录/训练记录"（必须明确是问"什么"，排除"我练了深蹲5组"这类保存意图） ---
        if (((msg.contains("练了什么") || msg.contains("做了什么运动") || msg.contains("运动记录") || msg.contains("训练记录"))
                || (msg.contains("练了") && msg.contains("什么")))
                && !msg.contains("吃什么") && !msg.contains("记录一下")) {
            String data = buildTodayExerciseReply(userId);
            String toolData = (data != null && !data.isBlank()) ? data : "暂无训练记录";
            return new DirectRouteResult("today_exercise_record", toolData,
                    "你是健身助手Tatan。根据下方工具返回的训练记录数据，简洁亲切地回答用户。纯文本，不用markdown。如果数据是'暂无'，告知用户今天还没有训练记录。");
        }

        // --- 查饮食计划："吃什么/饮食计划/食谱"（无"了"字） ---
        if (msg.contains("吃什么") || msg.contains("吃啥") || msg.contains("食谱") || msg.contains("饮食计划")
                || (msg.contains("计划") && msg.contains("吃"))) {
            if (!msg.contains("吃了") && !msg.contains("喝了")) {
                String mealHint = "";
                if (msg.contains("早餐")) mealHint = "早餐";
                else if (msg.contains("午餐")) mealHint = "午餐";
                else if (msg.contains("晚餐") || msg.contains("晚")) mealHint = "晚餐";
                else if (msg.contains("加餐")) mealHint = "加餐";

                String planText = buildDietPlanText(userId);
                if (planText == null || planText.isBlank()) planText = "暂无饮食计划";
                String toolData;
                if (mealHint.isEmpty()) {
                    toolData = planText;
                } else {
                    String section = extractDietMealSection(planText, mealHint);
                    toolData = (section != null && !section.isBlank()) ? section : "暂无" + mealHint + "推荐";
                }
                return new DirectRouteResult("today_diet_plan", toolData,
                        "你是健身助手Tatan。根据下方工具返回的饮食计划数据，简洁亲切地回答用户。纯文本，不用markdown。如果数据是'暂无'，告知用户还没有饮食计划。");
            }
        }

        // --- 查饮食记录："吃了什么/饮食记录"（必须明确是问"什么"，排除"我吃了鸡胸肉"这类保存意图） ---
        if ((msg.contains("吃了什么") || msg.contains("喝了什么") || msg.contains("饮食记录"))
                && !msg.contains("记录一下") && !msg.contains("帮")) {
            String data = buildTodayDietReply(userId);
            String toolData = (data != null && !data.isBlank()) ? data : "暂无饮食记录";
            return new DirectRouteResult("today_diet_record", toolData,
                    "你是健身助手Tatan。根据下方工具返回的饮食记录数据，简洁亲切地回答用户。纯文本，不用markdown。如果数据是'暂无'，告知用户今天还没有饮食记录。");
        }

        // --- 查今日综合记录 ---
        if (msg.contains("今天做了什么") || msg.contains("今天记录了什么") || msg.contains("打卡总结")) {
            String data = buildTodayRecordReply(user, userRecord);
            String toolData = (data != null && !data.isBlank()) ? data : "暂无记录";
            return new DirectRouteResult("today_exercise_record", toolData,
                    "你是健身助手Tatan。根据下方工具返回的今日记录数据，简洁亲切地回答用户。纯文本，不用markdown。");
        }

        // --- 查历史记录（必须明确是问"什么"，排除"昨天我吃了XX"这类保存意图） ---
        if ((msg.contains("昨天") || msg.contains("前天") || WEEKDAY_PATTERN.matcher(msg).find())
                && (msg.contains("什么") || msg.contains("记录") || msg.contains("总结"))
                && !msg.contains("记录一下")) {
            LocalDate target = resolveDateFromDescription(msg);
            if (target != null) {
                String data = fetchHistoryRecord(userRecord, target);
                String toolData = (data != null && !data.isBlank()) ? data : "暂无记录";
                return new DirectRouteResult("general_chat", toolData,
                        "你是健身助手Tatan。根据下方工具返回的历史记录数据，简洁亲切地回答用户。纯文本，不用markdown。");
            }
        }

        // --- 查本周记录 ---
        if ((msg.contains("这周") || msg.contains("本周") || msg.contains("上周"))
                && (msg.contains("记录") || msg.contains("总结") || msg.contains("做了"))) {
            String data = buildWeekRecordReply(user, userRecord);
            String toolData = (data != null && !data.isBlank()) ? data : "暂无记录";
            return new DirectRouteResult("general_chat", toolData,
                    "你是健身助手Tatan。根据下方工具返回的本周记录数据，简洁亲切地回答用户。纯文本，不用markdown。");
        }

        return null;
    }

    /**
     * 快速路由命中后，把工具数据喂给AI做流式总结
     */
    private String streamSummarizeWithToolData(Long userId, String userMessage, String toolData,
                                               String systemPrompt, OutputStream outputStream,
                                               StringBuilder resultHolder) throws Exception {
        AiModelConfig.ModelProvider provider = requireActiveProvider();
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", "用户问：" + userMessage + "\n\n工具返回数据：\n" + toolData));

        String summarized = callAiApiStreamWithMessages(messages, outputStream, provider, 1024);
        resultHolder.append(summarized);
        return summarized;
    }

    // ==================== 历史记录查询（策略模式） ====================

    private static final Map<String, Integer> WEEKDAY_MAP = Map.of(
            "一",1,"二",2,"三",3,"四",4,"五",5,"六",6,"日",7,"天",7);

    private record TimePattern(Pattern pattern, Function<Matcher, LocalDate> resolver) {}

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    // ==================== Function Calling 工具定义 ====================

    private static final Set<String> SIGNAL_TOOL_NAMES = Set.of(
            "generate_training_plan", "generate_diet_plan", "save_exercise_record", "save_diet_record"
    );
    private static final Set<String> SAVE_TOOL_NAMES = Set.of("save_exercise_record", "save_diet_record");
    private static final Set<String> GENERATE_TOOL_NAMES = Set.of("generate_training_plan", "generate_diet_plan");

    private record ToolCallResult(String replyType, String savePlanType) {}

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> buildChatToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. get_today_records — 综合记录
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_today_records",
                        "description", "获取用户今天的运动和饮食记录汇总。当用户问'今天做了什么''今天记录了什么''今天练了什么吃了什么'时调用。",
                        "parameters", Map.of("type", "object", "properties", Map.of(), "required", List.of())
                )
        ));

        // 2. query_training — 统一训练查询（合并记录+计划，避免AI选错）
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "query_training",
                        "description", "查询训练信息。用户问训练相关的问题时调用此工具，通过query_type区分查记录还是查计划。" +
                                "示例：'今天练了什么'→query_type='已完成的记录'，'今天该练什么'→query_type='计划安排'",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "query_type", Map.of("type", "string",
                                                "enum", List.of("已完成的记录", "计划安排"),
                                                "description", "查记录还是查计划。'练了什么/做了什么运动/运动记录'=已完成的记录；'练什么/该练什么/训练安排/健身计划'=计划安排"),
                                        "target_day", Map.of("type", "string",
                                                "enum", List.of("今天", "明天", "全部"),
                                                "description", "查看哪天。仅query_type='计划安排'时需要此参数，默认'今天'")
                                ),
                                "required", List.of("query_type")
                        )
                )
        ));

        // 3. query_diet — 统一饮食查询（合并记录+计划，避免AI选错）
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "query_diet",
                        "description", "查询饮食信息。用户问饮食相关的问题时调用此工具，通过query_type区分查记录还是查计划。" +
                                "示例：'今天吃了什么'→query_type='已完成的记录'，'晚餐吃什么'→query_type='计划安排'",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "query_type", Map.of("type", "string",
                                                "enum", List.of("已完成的记录", "计划安排"),
                                                "description", "查记录还是查计划。'吃了什么/饮食记录'=已完成的记录；'吃什么/饮食计划/推荐食谱'=计划安排"),
                                        "meal_type", Map.of("type", "string",
                                                "enum", List.of("早餐", "午餐", "晚餐", "加餐", "全部"),
                                                "description", "餐次。仅query_type='计划安排'时需要。用户问'晚餐吃什么'填'晚餐'，问'饮食计划'填'全部'，不指定时根据当前时间推断。")
                                ),
                                "required", List.of("query_type")
                        )
                )
        ));

        // 4. get_week_records
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_week_records",
                        "description", "获取用户本周的记录汇总和每日回顾。当用户问'这周做了什么''本周总结'时调用。",
                        "parameters", Map.of("type", "object", "properties", Map.of(), "required", List.of())
                )
        ));

        // 5. get_history_record
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_history_record",
                        "description", "获取用户指定日期的运动和饮食记录。支持昨天、星期几、X月X号等格式。当用户问某天的记录时调用。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "date_description", Map.of("type", "string", "description", "用户描述的日期，例如'昨天'、'上周三'、'3月15号'")
                                ),
                                "required", List.of("date_description")
                        )
                )
        ));

        // 8. search_knowledge
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "search_knowledge",
                        "description", "在健身知识库中搜索相关内容。当用户问健身专业知识（训练原理、营养知识、补剂、器械、伤病康复等）时必须调用。比如'筋膜枪有用吗''蛋白粉怎么喝''膝盖痛能不能深蹲'。日常闲聊和已有数据查询不需要调用。如果知识库没查到相关内容，后续必须明确回复固定拒答，不能自行凭常识回答。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "query", Map.of("type", "string", "description", "搜索关键词，提炼用户问题的核心主题")
                                ),
                                "required", List.of("query")
                        )
                )
        ));

        // 9. generate_training_plan (信号工具)
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "generate_training_plan",
                        "description", "用户想要生成或调整一周训练计划。当用户说'帮我制定训练计划''生成健身计划''设计训练安排'时调用。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "user_request", Map.of("type", "string", "description", "用户对训练计划的具体要求，如'想练胸和背''每周练4天'。没提特殊要求则为空字符串。"),
                                        "adjust_existing", Map.of("type", "boolean", "description", "是否在现有计划基础上调整。用户说'调整一下训练计划'则为true。")
                                ),
                                "required", List.of("user_request", "adjust_existing")
                        )
                )
        ));

        // 10. generate_diet_plan (信号工具)
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "generate_diet_plan",
                        "description", "用户想要生成饮食推荐或食谱。当用户说'帮我制定饮食计划''推荐今天的食谱''晚餐吃什么好'时调用。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "user_request", Map.of("type", "string", "description", "用户对饮食的具体要求，如'想减脂''低碳水'。没提特殊要求则为空字符串。"),
                                        "meal_scope", Map.of("type", "string", "enum", List.of("全天", "单餐"),
                                                "description", "全天食谱还是某一餐推荐。用户问'晚餐吃什么'为'单餐'，问'帮我制定饮食计划'为'全天'。")
                                ),
                                "required", List.of("user_request", "meal_scope")
                        )
                )
        ));

        // 11. save_exercise_record (信号工具)
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "save_exercise_record",
                        "description", "【必须调用】记录用户的运动/训练数据。当用户明确在描述自己已经做过的运动、或明确要求记录训练时调用。只提取真实记录内容，不要把'帮我记录一下''我练了''刚做完''麻烦记一下'这类话术写进exercise_name。用户是在提问、咨询建议、查看计划或查看记录时，绝不能调用此工具。若用户说了组数/时长，尽量结构化填completed_sets和duration_seconds；没说就留空，不要猜。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "exercise_name", Map.of("type", "string", "description", "只保留动作/运动本体及必要数量信息，例如'深蹲5组''跑步30分钟'，不要带'帮我记录''我练了'等口语前缀"),
                                        "muscle_group", Map.of("type", "string", "description", "目标肌群（chest/back/shoulders/arms/legs/core），能推断则填，不能则留空字符串"),
                                        "completed_sets", Map.of("type", "integer", "description", "完成的组数，没提则不填"),
                                        "duration_seconds", Map.of("type", "integer", "description", "持续时长（秒），用户说'30分钟'则填1800"),
                                        "note", Map.of("type", "string", "description", "备注")
                                ),
                                "required", List.of("exercise_name")
                        )
                )
        ));

        // 12. save_diet_record (信号工具)
        tools.add(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "save_diet_record",
                        "description", "【必须调用】记录用户的饮食数据。当用户明确在描述自己已经吃了/喝了什么、或明确要求记录饮食时调用。只提取真实饮食内容，不要把'帮我记录一下''我吃了''刚喝了''记一下'这类话术写进food_description。用户是在提问、咨询推荐、查看饮食计划或查看饮食记录时，绝不能调用此工具。用户没说克重/ml时，不要编造分量、热量或营养值。",
                        "parameters", Map.of("type", "object",
                                "properties", Map.of(
                                        "meal_type", Map.of("type", "string", "description", "餐次（早餐/午餐/晚餐/加餐），优先按用户原话判断；没提到可留空字符串"),
                                        "food_description", Map.of("type", "string", "description", "只保留食物内容，例如'鸡胸肉100g和米饭150g'或'鸡蛋和牛奶'，不要带'我吃了''帮我记录'等口语前缀"),
                                        "calories", Map.of("type", "integer", "description", "用户提到的总热量(kcal)，没提到则不填"),
                                        "note", Map.of("type", "string", "description", "备注")
                                ),
                                "required", List.of("food_description")
                        )
                )
        ));

        return tools;
    }

    private static final List<Map<String, Object>> CHAT_TOOLS = buildChatToolDefinitions();

    private static final String TOOL_CALLING_SYSTEM_PROMPT = """
            你是健身助手Tatan。

            【最高优先级规则】
            1. 绝对禁止编造训练动作、饮食建议、记录数据。你没有用户的计划和记录数据，必须通过工具获取。
            2. 绝对禁止跳过工具直接回复"已记录""已保存"。当用户要记录运动或饮食时，你必须调用对应的保存工具。

            工具调用规则（必须严格遵守）：
            - 用户告诉你他吃了什么 → 必须调 save_diet_record。包括："我吃了XX""晚上吃了XX""帮我记录一下我吃了XX""记一下午餐喝了XX"。提取食物描述和餐次。
            - 用户告诉你他做了什么运动 → 必须调 save_exercise_record。包括："我练了XX""做了XX运动""帮我记录训练""记一下我跑了5公里"。提取运动名称。
            - 用户问训练安排（练什么/该练什么/健身计划）→ 调 query_training(query_type="计划安排")
            - 用户问已完成的训练（练了什么/运动记录）→ 调 query_training(query_type="已完成的记录")
            - 用户问饮食安排（吃什么/食谱推荐）→ 调 query_diet(query_type="计划安排")
            - 用户问已吃过的（吃了什么/饮食记录）→ 调 query_diet(query_type="已完成的记录")
            - 用户要生成新计划 → 调 generate_training_plan 或 generate_diet_plan
            - 健身专业问题 → 必须先调 search_knowledge
            - 只有纯闲聊（你好/谢谢/你是谁）→ 不调用工具，直接回复

            区分关键：
            - "今天吃了什么" = 查记录 → query_diet(query_type="已完成的记录")
            - "我晚上吃了两个面包" = 保存记录 → save_diet_record
            - "帮我记录一下我吃了XX" = 保存记录 → save_diet_record
            - "今天练了什么" = 查记录 → query_training(query_type="已完成的记录")
            - "我练了深蹲5组" = 保存记录 → save_exercise_record
            - "帮我记录一下我练了XX" = 保存记录 → save_exercise_record
            - "筋膜枪有用吗" = 专业问题 → search_knowledge
            - "蛋白粉什么时候喝" = 专业问题 → search_knowledge
            - "膝盖痛能不能深蹲" = 专业问题 → search_knowledge

            保存工具参数约束：
            - save_exercise_record.exercise_name 只保留动作/运动内容，不要带"帮我记录""我练了""刚做完"等前缀
            - save_diet_record.food_description 只保留食物内容，不要带"我吃了""我喝了""帮我记录"等前缀
            - 用户没明确说克重/ml/热量时，不要编造数字
            - 用户在提问时不要误判为保存。例如："晚饭吃什么""今天练什么""我该怎么练"都不是保存
            - 如果一句话同时包含餐次和食物，meal_type单独填写，food_description里不要重复塞餐次前缀
            - 如果用户说了"5组""30分钟"，优先分别填completed_sets和duration_seconds
            - 对于训练、营养、补剂、器械、康复、减脂、增肌等专业问答，严禁跳过知识库直接按常识回答
            - 如果 search_knowledge 没有结果，最终只允许回复：知识库暂无该知识，我无法回答

            回答要求：简洁亲切，纯文本，不用markdown。
            """;

    // ==================== Function Calling 核心方法 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> callAiApiWithTools(List<Map<String, Object>> messages,
                                                   List<Map<String, Object>> tools,
                                                   AiModelConfig.ModelProvider provider,
                                                   int maxTokens) {
        try {
            WebClient webClient = getWebClient(provider);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.1);
            requestBody.put("tools", tools);
            requestBody.put("tool_choice", "auto");
            requestBody.put("stream", false);
            if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
                requestBody.put("enable_thinking", false);
            }
            requestBody.put("messages", messages);

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response;
        } catch (Exception e) {
            log.error("AI工具调用请求失败", e);
            return null;
        }
    }

    private String callAiApiStreamWithMessages(List<Map<String, Object>> messages,
                                               OutputStream outputStream,
                                               AiModelConfig.ModelProvider provider,
                                               int maxTokens) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", aiModelConfig.getTemperature());
        requestBody.put("stream", true);
        if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
            requestBody.put("enable_thinking", false);
        }
        requestBody.put("messages", messages);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(provider.getBaseUrl() + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + provider.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;

                Map<String, Object> response = objectMapper.readValue(data, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices == null || choices.isEmpty()) continue;
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta == null || !delta.containsKey("content")) continue;
                String content = (String) delta.get("content");
                if (content == null || content.isEmpty()) continue;

                fullText.append(content);
                writeSseData(outputStream, content);
            }
        } finally {
            conn.disconnect();
        }
        return fullText.toString();
    }

    @SuppressWarnings("unchecked")
    private String executeDataToolCall(Long userId, String toolName, Map<String, Object> arguments, User user, UserRecord userRecord) {
        try {
            return switch (toolName) {
                case "get_today_records" -> {
                    String reply = buildTodayRecordReply(user, userRecord);
                    yield reply != null ? reply : "今天暂无记录";
                }
                case "query_training" -> {
                    String queryType = toCleanString(arguments.get("query_type"));
                    if ("计划安排".equals(queryType)) {
                        String targetDay = toCleanString(arguments.getOrDefault("target_day", "今天"));
                        if ("全部".equals(targetDay)) {
                            String plan = buildTrainingPlanText(userId);
                            yield plan != null ? plan : "还没有训练计划";
                        }
                        int offset = "明天".equals(targetDay) ? 1 : 0;
                        String section = buildTrainingPlanDaySection(userId, offset);
                        yield section != null ? section : targetDay + "暂无训练安排";
                    } else {
                        String reply = buildTodayExerciseReply(userId);
                        yield !reply.isBlank() ? reply : "今天暂无训练记录";
                    }
                }
                case "query_diet" -> {
                    String queryType = toCleanString(arguments.get("query_type"));
                    if ("计划安排".equals(queryType)) {
                        String mealType = toCleanString(arguments.getOrDefault("meal_type", "全部"));
                        String planText = buildDietPlanText(userId);
                        if (planText == null) yield "还没有饮食计划";
                        if ("全部".equals(mealType)) yield planText;
                        String section = extractDietMealSection(planText, mealType);
                        yield section != null ? section : "当前饮食计划里没有" + mealType + "推荐";
                    } else {
                        String reply = buildTodayDietReply(userId);
                        yield !reply.isBlank() ? reply : "今天暂无饮食记录";
                    }
                }
                case "get_week_records" -> {
                    String reply = buildWeekRecordReply(user, userRecord);
                    yield reply != null ? reply : "本周暂无记录";
                }
                case "get_history_record" -> {
                    String dateDesc = toCleanString(arguments.get("date_description"));
                    LocalDate target = resolveDateFromDescription(dateDesc);
                    if (target == null) yield "无法识别日期，请用'昨天''星期几''几月几号'的格式";
                    String reply = fetchHistoryRecord(userRecord, target);
                    yield reply != null ? reply : dateDesc + "暂无记录";
                }
                case "search_knowledge" -> {
                    String query = toCleanString(arguments.get("query"));
                    String result = retrieveKnowledgeDirectly(query);
                    yield result != null ? result : "知识库暂无该知识，我无法回答";
                }
                default -> "未知工具：" + toolName;
            };
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return "工具执行出错：" + e.getMessage();
        }
    }

    private LocalDate resolveDateFromDescription(String dateDesc) {
        if (dateDesc == null || dateDesc.isBlank()) return null;
        // 昨天
        if (dateDesc.contains("昨天") || dateDesc.contains("昨日")) {
            return LocalDate.now(CN_ZONE).minusDays(1);
        }
        // 前天
        if (dateDesc.contains("前天")) {
            return LocalDate.now(CN_ZONE).minusDays(2);
        }
        // 星期几
        Matcher weekdayMatcher = WEEKDAY_PATTERN.matcher(dateDesc);
        if (weekdayMatcher.find()) {
            int dayNum = WEEKDAY_MAP.getOrDefault(weekdayMatcher.group(2), 0);
            DayOfWeek target = DayOfWeek.of(dayNum);
            LocalDate today = LocalDate.now(CN_ZONE);
            if (dateDesc.contains("上周") || dateDesc.contains("上星期")) {
                return today.with(TemporalAdjusters.previousOrSame(target)).minusWeeks(1);
            }
            return today.with(TemporalAdjusters.nextOrSame(target));
        }
        // X月X号
        try {
            Matcher mdMatcher = Pattern.compile("(\\d{1,2})[月/](\\d{1,2})[日号]?").matcher(dateDesc);
            if (mdMatcher.find()) {
                int month = Integer.parseInt(mdMatcher.group(1));
                int day = Integer.parseInt(mdMatcher.group(2));
                int year = LocalDate.now(CN_ZONE).getYear();
                return LocalDate.of(year, month, day);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private ToolCallResult executeToolCallingLoop(Long userId, String message, User user,
                                                   ChatHistory history, UserRecord userRecord,
                                                   OutputStream outputStream, StringBuilder resultHolder) {
        try {
            AiModelConfig.ModelProvider provider = requireActiveProvider();

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", TOOL_CALLING_SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", buildToolCallingContextMessage(message, user, history)));

            String detectedReplyType = "general_chat";
            String detectedSavePlanType = "";

            for (int round = 0; round < 5; round++) {
                Map<String, Object> apiResponse = callAiApiWithTools(messages, CHAT_TOOLS, provider, 2048);
                if (apiResponse == null) {
                    writeSseDataGradually(outputStream, "AI调用失败，请稍后再试");
                    resultHolder.append("AI调用失败");
                    return new ToolCallResult("general_chat", "");
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) apiResponse.get("choices");
                if (choices == null || choices.isEmpty()) {
                    writeSseDataGradually(outputStream, "AI返回异常，请稍后再试");
                    resultHolder.append("AI返回异常");
                    return new ToolCallResult("general_chat", "");
                }

                Map<String, Object> firstChoice = choices.get(0);
                String finishReason = (String) firstChoice.get("finish_reason");
                Map<String, Object> msgObj = getChoiceMessage(firstChoice);
                List<Map<String, Object>> toolCalls = getChoiceToolCalls(firstChoice);

                // 模型直接回复，无需工具
                if (("stop".equals(finishReason) || "end_turn".equals(finishReason))
                        && (toolCalls == null || toolCalls.isEmpty())) {
                    String content = extractMessageText(msgObj);

                    // 模型跳过了工具调用，检测是否是保存意图被遗漏
                    if (round == 0 && isLikelySaveIntent(message)) {
                        log.warn("【工具调用循环】检测到保存意图但模型未调工具，强制要求重试");
                        messages.add(Map.of("role", "assistant", "content", content != null ? content : ""));
                        messages.add(Map.of("role", "user", "content",
                                "你没有调用任何工具！你必须调用对应的工具来实际执行操作。" +
                                determineSaveToolHint(message) +
                                "请不要直接回复'已记录'，必须调用工具才能保存数据。现在请调用正确的工具。"));
                        continue;
                    }

                    if (isLikelyKnowledgeQuestion(message)) {
                        if (round == 0) {
                            messages.add(Map.of("role", "assistant", "content", content != null ? content : ""));
                            messages.add(Map.of("role", "user", "content",
                                    "这是健身专业问题，你必须先调用search_knowledge工具查询知识库，禁止直接按常识回答。"
                                            + "如果知识库没有相关内容，最终只允许回复：知识库暂无该知识，我无法回答。现在请先调用search_knowledge。"));
                            continue;
                        }
                        String fallback = "知识库暂无该知识，我无法回答";
                        writeSseDataGradually(outputStream, fallback);
                        resultHolder.append(fallback);
                        return new ToolCallResult("general_chat", detectedSavePlanType);
                    }

                    if (content != null && !content.isBlank()) {
                        writeSseDataGradually(outputStream, content);
                        resultHolder.append(content);
                    }
                    return new ToolCallResult(detectedReplyType, detectedSavePlanType);
                }

                // 工具调用
                if (toolCalls == null || toolCalls.isEmpty()) {
                    String content = extractMessageText(msgObj);
                    if (content != null && !content.isBlank()) {
                        writeSseDataGradually(outputStream, content);
                        resultHolder.append(content);
                    }
                    return new ToolCallResult(detectedReplyType, detectedSavePlanType);
                }

                // 分离信号工具和数据工具
                List<Map<String, Object>> saveCalls = new ArrayList<>();
                List<Map<String, Object>> generateCalls = new ArrayList<>();
                List<Map<String, Object>> dataCalls = new ArrayList<>();

                for (Map<String, Object> tc : toolCalls) {
                    Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                    String name = (String) fn.get("name");
                    if (SAVE_TOOL_NAMES.contains(name)) {
                        saveCalls.add(tc);
                    } else if (GENERATE_TOOL_NAMES.contains(name)) {
                        generateCalls.add(tc);
                    } else {
                        dataCalls.add(tc);
                    }
                }

                // 1. 先执行 save_* 信号工具（修改状态）
                for (Map<String, Object> saveCall : saveCalls) {
                    executeRecordSaveFromToolCall(userId, userRecord, saveCall);
                }
                if (!saveCalls.isEmpty()) {
                    detectedReplyType = detectSaveReplyType(saveCalls, detectedReplyType);
                    userRecord = userRecordService.getByUserId(userId);
                }

                // 2. 如果有 generate_* 信号工具，系统接管
                if (!generateCalls.isEmpty()) {
                    Map<String, Object> genCall = generateCalls.get(0);
                    Map<String, Object> fn = (Map<String, Object>) genCall.get("function");
                    String genName = (String) fn.get("name");
                    String genArgs = (String) fn.get("arguments");

                    String planIntent = "generate_training_plan".equals(genName) ? "training_plan" : "diet_plan";
                    String planType = "training_plan".equals(genName) ? "training" : "diet";
                    String planReply = handlePlanGenerationWithToolArgs(userId, message, user, history,
                            outputStream, resultHolder, userRecord, planIntent, genArgs);
                    if (planReply != null) {
                        resultHolder.append(planReply);
                        return new ToolCallResult(planIntent, planType);
                    }
                    // 如果生成失败，继续循环让模型兜底
                }

                // 3. 执行数据查询工具
                if (!dataCalls.isEmpty()) {
                    // 构造 assistant message（含 tool_calls）
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("tool_calls", toolCalls);
                    messages.add(assistantMsg);

                    // 执行数据工具并添加 tool results
                    for (Map<String, Object> dc : dataCalls) {
                        String tcId = (String) dc.get("id");
                        Map<String, Object> fn = (Map<String, Object>) dc.get("function");
                        String name = (String) fn.get("name");
                        String argsStr = (String) fn.get("arguments");
                        Map<String, Object> args = new HashMap<>();
                        if (argsStr != null && !argsStr.isBlank()) {
                            try { args = JSON_MAPPER.readValue(argsStr, Map.class); } catch (Exception ignored) {}
                        }

                        detectedReplyType = toolNameToReplyType(name, detectedReplyType, args);
                        String result = executeDataToolCall(userId, name, args, user, userRecord);

                        Map<String, Object> toolResultMsg = new HashMap<>();
                        toolResultMsg.put("role", "tool");
                        toolResultMsg.put("tool_call_id", tcId);
                        toolResultMsg.put("content", result);
                        messages.add(toolResultMsg);
                    }

                    // save 工具也需要 tool result
                    for (Map<String, Object> sc : saveCalls) {
                        String tcId = (String) sc.get("id");
                        Map<String, Object> toolResultMsg = new HashMap<>();
                        toolResultMsg.put("role", "tool");
                        toolResultMsg.put("tool_call_id", tcId);
                        toolResultMsg.put("content", "已保存");
                        messages.add(toolResultMsg);
                    }

                    // 继续循环，让模型基于工具结果生成回复
                    continue;
                }

                // 4. 只有 save 工具，没有数据工具也没有 generate 工具
                if (!saveCalls.isEmpty() && dataCalls.isEmpty() && generateCalls.isEmpty()) {
                    Map<String, Object> assistantMsg = new HashMap<>();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("tool_calls", toolCalls);
                    messages.add(assistantMsg);

                    for (Map<String, Object> sc : saveCalls) {
                        String tcId = (String) sc.get("id");
                        Map<String, Object> toolResultMsg = new HashMap<>();
                        toolResultMsg.put("role", "tool");
                        toolResultMsg.put("tool_call_id", tcId);
                        toolResultMsg.put("content", "已成功保存");
                        messages.add(toolResultMsg);
                    }
                    continue;
                }

                // 兜底：无任何工具类型匹配
                break;
            }

            // 超过5轮
            writeSseDataGradually(outputStream, "处理超时，请再试一次");
            resultHolder.append("处理超时");
            return new ToolCallResult("general_chat", "");
        } catch (Exception e) {
            log.error("工具调用循环异常", e);
            try {
                writeSseDataGradually(outputStream, "处理出错，请稍后再试");
            } catch (Exception ignored) {}
            resultHolder.append("处理出错");
            return new ToolCallResult("general_chat", "");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getChoiceMessage(Map<String, Object> choice) {
        if (choice == null) {
            return null;
        }
        Object message = choice.get("message");
        return message instanceof Map<?, ?> ? (Map<String, Object>) message : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getChoiceToolCalls(Map<String, Object> choice) {
        if (choice == null) {
            return Collections.emptyList();
        }
        Object directToolCalls = choice.get("tool_calls");
        if (directToolCalls instanceof List<?> directList && !directList.isEmpty()) {
            return (List<Map<String, Object>>) directList;
        }
        Map<String, Object> message = getChoiceMessage(choice);
        if (message == null) {
            return Collections.emptyList();
        }
        Object nestedToolCalls = message.get("tool_calls");
        if (nestedToolCalls instanceof List<?> nestedList && !nestedList.isEmpty()) {
            return (List<Map<String, Object>>) nestedList;
        }
        return Collections.emptyList();
    }

    private String extractMessageText(Map<String, Object> message) {
        if (message == null) {
            return null;
        }
        Object content = message.get("content");
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> map) {
                    Object text = map.get("text");
                    if (text != null) {
                        sb.append(text);
                    }
                } else if (part != null) {
                    sb.append(part);
                }
            }
            return sb.toString();
        }
        return content == null ? null : String.valueOf(content);
    }

    @SuppressWarnings("unchecked")
    private String detectSaveReplyType(List<Map<String, Object>> saveCalls, String currentReplyType) {
        for (Map<String, Object> saveCall : saveCalls) {
            Object functionObj = saveCall.get("function");
            if (!(functionObj instanceof Map<?, ?> function)) {
                continue;
            }
            String name = toCleanString(function.get("name"));
            if ("save_diet_record".equals(name)) {
                return "today_diet_record";
            }
            if ("save_exercise_record".equals(name)) {
                return "today_exercise_record";
            }
        }
        return currentReplyType;
    }

    private static final String INTENT_CLASSIFY_PROMPT = """
            你是健身助手的意图分析模块。分析用户消息，严格只返回一个JSON对象，不要返回任何其他文字。

            判断规则：
            - 用户明确在陈述"已经吃了/喝了什么"或"请帮我记录饮食" → {"intent":"save_diet","food":"仅保留食物内容，不带'我吃了/帮我记录'等前缀","meal":"餐次（早餐/午餐/晚餐/加餐，优先按原话判断）"}
            - 用户明确在陈述"已经做了什么运动"或"请帮我记录训练" → {"intent":"save_exercise","exercise":"仅保留动作/运动内容，不带'我练了/帮我记录'等前缀"}
            - 用户在询问、闲聊、问问题 → {"intent":"other"}
            - 只要是问句、咨询建议、查询计划、查询记录，一律返回 {"intent":"other"}
            - 不要把"晚饭吃什么""今天练什么""饮食计划""训练记录""我该吃什么比较好"判断成保存
            - 不要编造克重、热量、组数、时长；只抽取用户明确说过的内容

            示例：
            "我晚上吃了两个全麦面包和三个鸡蛋" → {"intent":"save_diet","food":"两个全麦面包和三个鸡蛋","meal":"晚餐"}
            "帮我记录一下我练了深蹲5组" → {"intent":"save_exercise","exercise":"深蹲5组"}
            "今天练了什么" → {"intent":"other"}
            "我晚饭吃什么比较好" → {"intent":"other"}
            "你好" → {"intent":"other"}
            """;

    private record AiIntentSaveResult(String replyType, String replyText) {}

    @SuppressWarnings("unchecked")
    private AiIntentSaveResult classifyIntentByAi(Long userId, String message, UserRecord userRecord) {
        try {
            AiModelConfig.ModelProvider provider = requireActiveProvider();
            WebClient webClient = getWebClient(provider);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", 256);
            requestBody.put("temperature", 0);
            requestBody.put("stream", false);
            if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
                requestBody.put("enable_thinking", false);
            }
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", INTENT_CLASSIFY_PROMPT),
                    Map.of("role", "user", "content", message)
            ));

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;

            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            String content = msg != null ? (String) msg.get("content") : null;
            if (content == null || content.isBlank()) return null;

            // 提取JSON
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            Map<String, Object> intentMap = JSON_MAPPER.readValue(json, Map.class);
            String intent = (String) intentMap.get("intent");

            if ("save_diet".equals(intent)) {
                String food = sanitizeDietRecordText((String) intentMap.get("food"));
                String meal = resolveMealTypeFromText((String) intentMap.get("meal"), food);
                if (food != null && !food.isBlank()) {
                    AddDietRecordRequest dietReq = new AddDietRecordRequest();
                    dietReq.setMealType(meal != null && !meal.isBlank() ? meal : getCurrentMealType());
                    dietReq.setName(food);
                    dietReq.setSource("chat");
                    appendDietRecord(userId, userRecord, dietReq);
                    return new AiIntentSaveResult(
                            "today_diet_record",
                            "好的，已记录" + (meal != null && !meal.isBlank() ? meal : getCurrentMealType()) + "：" + food
                    );
                }
            } else if ("save_exercise".equals(intent)) {
                String exercise = sanitizeExerciseRecordText((String) intentMap.get("exercise"));
                if (exercise != null && !exercise.isBlank()) {
                    AddExerciseRecordRequest exReq = new AddExerciseRecordRequest();
                    exReq.setExerciseName(exercise);
                    exReq.setSource("chat");
                    exReq.setCompletedSets(extractCompletedSets(exercise));
                    exReq.setDurationSeconds(extractDurationSeconds(exercise));
                    appendExerciseRecord(userId, userRecord, exReq);
                    return new AiIntentSaveResult("today_exercise_record", "好的，已记录运动：" + exercise);
                }
            }
            // intent=other 或解析失败，返回null走后续流程
            return null;
        } catch (Exception e) {
            log.warn("【意图分类】解析失败，走后续流程", e);
            return null;
        }
    }

    private boolean isLikelySaveIntent(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        // 用户明确要保存/记录运动或饮食
        if (m.contains("记录一下") || m.contains("帮我记录") || m.contains("记一下")) {
            return m.contains("吃") || m.contains("喝") || m.contains("练") || m.contains("运动") || m.contains("训练");
        }
        // "我吃了XX" / "我喝了XX" 但不是 "吃了什么"（后者是查询）
        if ((m.startsWith("我吃") || m.startsWith("我喝")) && !m.contains("什么")) return true;
        // "我练了XX" 但不是 "练了什么"
        if (m.startsWith("我练") && !m.contains("什么")) return true;
        // "我做了XX运动" 但不是 "做了什么"
        if (m.startsWith("我做") && !m.contains("什么") && (m.contains("运动") || m.contains("训练"))) return true;
        return false;
    }

    private boolean isLikelyKnowledgeQuestion(String message) {
        if (message == null) {
            return false;
        }
        String m = message.toLowerCase();
        if (!(m.contains("?") || m.contains("？") || m.contains("吗") || m.contains("怎么") || m.contains("为何")
                || m.contains("为什么") || m.contains("有用") || m.contains("能不能") || m.contains("可以吗")
                || m.contains("是否") || m.contains("作用") || m.contains("原理"))) {
            return false;
        }
        if (m.contains("计划") || m.contains("记录") || m.contains("练什么") || m.contains("吃什么")
                || m.contains("食谱") || m.contains("安排")) {
            return false;
        }
        if (isLikelySaveIntent(message)) {
            return false;
        }
        return m.contains("筋膜枪") || m.contains("蛋白粉") || m.contains("补剂") || m.contains("营养")
                || m.contains("热量") || m.contains("卡路里") || m.contains("增肌") || m.contains("减脂")
                || m.contains("康复") || m.contains("拉伸") || m.contains("动作") || m.contains("器械")
                || m.contains("伤") || m.contains("疼") || m.contains("酸痛") || m.contains("训练")
                || m.contains("饮食") || m.contains("深蹲") || m.contains("卧推") || m.contains("跑步");
    }

    private String determineSaveToolHint(String message) {
        String m = message.toLowerCase();
        if (m.contains("吃") || m.contains("喝")) {
            return "用户在告诉你他吃了什么，你必须调用save_diet_record工具。";
        }
        if (m.contains("练") || m.contains("运动") || m.contains("训练")) {
            return "用户在告诉你他做了什么运动，你必须调用save_exercise_record工具。";
        }
        return "";
    }

    private String toolNameToReplyType(String toolName, String currentReplyType, Map<String, Object> arguments) {
        return switch (toolName) {
            case "query_training" -> {
                String queryType = toCleanString(arguments.get("query_type"));
                yield "计划安排".equals(queryType) ? "today_training_plan" : "today_exercise_record";
            }
            case "query_diet" -> {
                String queryType = toCleanString(arguments.get("query_type"));
                yield "计划安排".equals(queryType) ? "today_diet_plan" : "today_diet_record";
            }
            case "get_today_records" -> "today_exercise_record";
            default -> currentReplyType;
        };
    }

    private static boolean needsToolCall(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        return m.contains("练") || m.contains("吃") || m.contains("记录") || m.contains("计划")
                || m.contains("食谱") || m.contains("运动") || m.contains("饮食") || m.contains("体重")
                || m.contains("热量") || m.contains("卡路里") || m.contains("打卡") || m.contains("目标")
                || m.contains("建议") || m.contains("推荐");
    }

    @SuppressWarnings("unchecked")
    private void executeRecordSaveFromToolCall(Long userId, UserRecord userRecord, Map<String, Object> toolCall) {
        try {
            Map<String, Object> fn = (Map<String, Object>) toolCall.get("function");
            String name = (String) fn.get("name");
            String argsStr = (String) fn.get("arguments");
            Map<String, Object> args = new HashMap<>();
            if (argsStr != null && !argsStr.isBlank()) {
                try { args = JSON_MAPPER.readValue(argsStr, Map.class); } catch (Exception ignored) {}
            }

            if ("save_exercise_record".equals(name)) {
                String exerciseName = sanitizeExerciseRecordText(toCleanString(args.get("exercise_name")));
                if (exerciseName.isBlank()) return;
                AddExerciseRecordRequest request = new AddExerciseRecordRequest();
                request.setExerciseName(exerciseName);
                request.setSource("chat");
                String muscleGroup = toCleanString(args.get("muscle_group"));
                request.setMuscleGroup(muscleGroup.isBlank() ? null : muscleGroup);
                Integer completedSets = parseInteger(args.get("completed_sets"));
                request.setCompletedSets(completedSets != null ? completedSets : extractCompletedSets(exerciseName));
                Integer duration = parseInteger(args.get("duration_seconds"));
                if (duration == null) {
                    duration = extractDurationSeconds(exerciseName);
                }
                request.setDurationSeconds(duration);
                String note = toCleanString(args.get("note"));
                request.setNote(note.isBlank() ? null : note);
                appendExerciseRecord(userId, userRecord, request);
            } else if ("save_diet_record".equals(name)) {
                String foodDesc = sanitizeDietRecordText(toCleanString(args.get("food_description")));
                if (foodDesc.isBlank()) return;
                AddDietRecordRequest request = new AddDietRecordRequest();
                String mealType = resolveMealTypeFromText(toCleanString(args.get("meal_type")), foodDesc);
                request.setMealType(mealType.isBlank() ? getCurrentMealType() : mealType);
                request.setName(foodDesc);
                request.setSource("chat");
                Integer calories = parseInteger(args.get("calories"));
                request.setCalories(calories);
                String note = toCleanString(args.get("note"));
                request.setNote(note.isBlank() ? null : note);
                appendDietRecord(userId, userRecord, request);
            }
        } catch (Exception e) {
            log.error("记录保存工具执行失败", e);
        }
    }

    private String handlePlanGenerationWithToolArgs(Long userId, String message, User user,
                                                     ChatHistory history, OutputStream outputStream,
                                                     StringBuilder resultHolder, UserRecord userRecord,
                                                     String planIntent, String toolArgsJson) {
        try {
            Map<String, Object> toolArgs = new HashMap<>();
            if (toolArgsJson != null && !toolArgsJson.isBlank()) {
                try { toolArgs = JSON_MAPPER.readValue(toolArgsJson, Map.class); } catch (Exception ignored) {}
            }
            String userRequest = toCleanString(toolArgs.get("user_request"));

            // 构造 MessageRoutingDecision 用于复用 handlePlanGeneration
            MessageRoutingDecision routingDecision = new MessageRoutingDecision(
                    false, false,
                    new PromptContextDecision(true, false, false, false, false, false, false, false),
                    "none", planIntent,
                    new TrainingPlanLookupDecision(false, "", false),
                    new DietPlanLookupDecision(false, "", false),
                    null, null
            );

            String extraNote = null;
            if (userRequest != null && !userRequest.isBlank()) {
                extraNote = "用户的具体要求：" + userRequest;
            }

            return handlePlanGeneration(message, user, history, outputStream, resultHolder, userRecord,
                    routingDecision, extraNote);
        } catch (Exception e) {
            log.error("计划生成信号工具执行失败", e);
            return null;
        }
    }

    private String buildToolCallingContextMessage(String currentMessage, User user, ChatHistory history) {
        StringBuilder context = new StringBuilder();
        // 只传基本身份信息，不传画像/目标/伤病（避免模型看到后跳过工具直接编造）
        if (user != null) {
            String gender = user.getGender() != null ? (user.getGender() == 0 ? "女" : "男") : "未知";
            context.append("【用户】").append(user.getUsername()).append("|").append(gender);
            if (user.getHeight() != null) context.append("|").append(user.getHeight()).append("cm");
            if (user.getWeight() != null) context.append("|").append(user.getWeight()).append("kg");
            if (user.getAge() != null) context.append("|").append(user.getAge()).append("岁");
            context.append("\n\n");
        }
        if (history != null) {
            List<String> recentMessages = parseJsonArray(history.getPendingMessages());
            if (!recentMessages.isEmpty()) {
                context.append("【历史对话（最近几轮）】\n");
                for (String item : recentMessages) {
                    context.append(item.trim()).append("\n");
                }
                context.append("\n");
            }
            if (history.getEmotionalState() != null && !history.getEmotionalState().isBlank()) {
                context.append("【用户情绪状态：").append(history.getEmotionalState()).append("】\n\n");
            }
        }
        context.append("【当前用户消息】\n").append(currentMessage);
        return context.toString();
    }

    private static final Pattern WEEKDAY_PATTERN = Pattern.compile("(这周|本周|上周|上星期|这星期|星期)([一二三四五六日天])");

    private static final List<TimePattern> TIME_PATTERNS = List.of(
            new TimePattern(Pattern.compile("昨天|昨日|前一天"), m -> LocalDate.now(CN_ZONE).minusDays(1)),
            new TimePattern(WEEKDAY_PATTERN, m -> {
                int v = WEEKDAY_MAP.get(m.group(2));
                int weekOffset = ("上周".equals(m.group(1)) || "上星期".equals(m.group(1))) ? 1 : 0;
                return LocalDate.now(CN_ZONE).minusWeeks(weekOffset).with(DayOfWeek.of(v));
            }),
            new TimePattern(Pattern.compile("(\\d{1,2})[月.](\\d{1,2})[号日]?"), m -> {
                int month = Integer.parseInt(m.group(1));
                int day = Integer.parseInt(m.group(2));
                return LocalDate.of(LocalDate.now(CN_ZONE).getYear(), month, day);
            })
    );

    private String fetchHistoryRecord(UserRecord userRecord, LocalDate target) {
        LocalDate today = LocalDate.now(CN_ZONE);
        if (target.isAfter(today)) return "该日期还没到哦~";
        if (java.time.temporal.ChronoUnit.DAYS.between(target, today) > 14)
            return "不好意思，该日期数据已清除或未记录";

        // 昨天 → 走专用字段
        if (target.equals(today.minusDays(1))) {
            String summary = userRecord.getYesterdaySummary();
            if (summary != null && !summary.isBlank()) {
                return "【昨天的总结】\n" + summary;
            }
            return "昨天暂无记录内容";
        }

        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        if (target.isBefore(currentWeekStart)) {
            String weeklySummary = userRecord.getWeeklySummary();
            if (weeklySummary != null && !weeklySummary.isBlank()) {
                return weeklySummary;
            }
            return "上周暂无总结内容";
        }

        return searchReviewByDate(userRecord, target);
    }

    private String searchReviewByDate(UserRecord userRecord, LocalDate target) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日");
        String dateKey = "【" + fmt.format(target);
        List<String> reviews = parseJsonArray(userRecord.getWeeklyReviews());

        for (String review : reviews) {
            // 去掉空白再匹配，兼容手动插入数据中日期和星期之间的换行
            if (review.replaceAll("\\s+", "").contains(dateKey)) {
                if (review.contains("暂无记录")) return fmt.format(target) + "暂无记录内容";
                return review;
            }
        }
        return fmt.format(target) + "暂无记录内容";
    }

    /**
     * 通用 JSON 数组解析（用于 weeklyReviews / pendingMessages）
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return JSON_MAPPER.readValue(fixed, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析JSON数组失败: {}", json, e);
            return extractStringItems(json);
        }
    }

    private List<String> extractStringItems(String raw) {
        List<String> items = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL).matcher(raw);
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

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord) {
        return buildContextAwareUserMessage(currentMessage, history, userRecord, null, false, false);
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord,
                                                String extraSystemNote) {
        return buildContextAwareUserMessage(currentMessage, history, userRecord, extraSystemNote, false, false);
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord,
                                                String extraSystemNote, boolean includeTrainingPlan,
                                                boolean includeDietPlan) {
        return buildContextAwareUserMessage(
                currentMessage,
                history,
                userRecord,
                null,
                extraSystemNote,
                new PromptContextDecision(false, true, true, true, true, true, includeTrainingPlan, includeDietPlan)
        );
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord,
                                                User user, String extraSystemNote,
                                                PromptContextDecision promptContext) {
        PromptContextDecision contextDecision = promptContext == null
                ? defaultPromptContextDecision()
                : promptContext;
        if (history == null && userRecord == null) {
            String userInfo = buildUserInfo(user);
            if (userInfo.isBlank() && (extraSystemNote == null || extraSystemNote.isBlank())) {
                return currentMessage;
            }
            StringBuilder minimalContext = new StringBuilder();
            if (!userInfo.isBlank()) {
                minimalContext.append(userInfo.trim()).append("\n\n");
            }
            if (extraSystemNote != null && !extraSystemNote.isBlank()) {
                minimalContext.append("【系统说明】\n").append(extraSystemNote).append("\n");
            }
            minimalContext.append("【当前用户消息】\n").append(currentMessage);
            return minimalContext.toString();
        }

        StringBuilder context = new StringBuilder();
        String userInfo = buildUserInfo(user);
        if (!userInfo.isBlank()) {
            context.append(userInfo.trim()).append("\n\n");
        }

        // 历史对话上下文
        String summary = history != null ? history.getSummary() : null;
        List<String> recentMessages = history != null ? parseJsonArray(history.getPendingMessages()) : null;
        boolean hasSummary = summary != null && !summary.isBlank();
        boolean hasRecentMessages = recentMessages != null && !recentMessages.isEmpty();

        if (contextDecision.includeRecentDialog() && (hasSummary || hasRecentMessages)) {
            context.append("【历史上下文】\n");
            if (hasSummary) {
                context.append("长期摘要：\n")
                        .append(summary.trim())
                        .append("\n\n");
            }
            if (hasRecentMessages) {
                int start = Math.max(0, recentMessages.size() - RECENT_DIALOG_LIMIT);
                context.append("最近原文对话（最多5轮）：\n");
                for (int i = start; i < recentMessages.size(); i++) {
                    String item = recentMessages.get(i);
                    if (item != null && !item.isBlank()) {
                        context.append(item.trim()).append("\n");
                    }
                }
                context.append("\n");
            }
        }

        if (contextDecision.includeTrainingPlanContext() && user != null) {
            String trainingPlanText = buildTrainingPlanText(user.getId());
            if (trainingPlanText != null && !trainingPlanText.isBlank()) {
                context.append("【当前训练计划】\n")
                        .append(trainingPlanText.trim())
                        .append("\n\n");
            }
        }

        if (contextDecision.includeDietPlanContext() && user != null) {
            String dietPlanText = buildDietPlanText(user.getId());
            if (dietPlanText != null && !dietPlanText.isBlank()) {
                context.append("【当前饮食计划】\n")
                        .append(dietPlanText.trim())
                        .append("\n\n");
            }
        }

        // 当日结构化记录
        if (contextDecision.includeTodayRecord() && user != null) {
            String exRecords = exerciseRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));
            String dietRecords = dietRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));

            if ((exRecords != null && !exRecords.isBlank()) || (dietRecords != null && !dietRecords.isBlank())) {
                context.append("【用户今日记录】\n");
                if (exRecords != null && !exRecords.isBlank()) {
                    context.append("运动记录：\n").append(exRecords.trim()).append("\n\n");
                }
                if (dietRecords != null && !dietRecords.isBlank()) {
                    context.append("饮食记录：\n").append(dietRecords.trim()).append("\n\n");
                }
            }
        }

        // 历史摘要（让AI了解用户最近的训练/饮食情况）
        if (userRecord != null) {
            String yesterdaySummary = userRecord.getYesterdaySummary();
            if (contextDecision.includeYesterdaySummary() && yesterdaySummary != null && !yesterdaySummary.isBlank()) {
                context.append("【昨日总结】\n").append(yesterdaySummary.trim()).append("\n\n");
            }
            String weeklySummary = userRecord.getWeeklySummary();
            if (contextDecision.includeWeeklySummary() && weeklySummary != null && !weeklySummary.isBlank()) {
                context.append("【上周总结】\n").append(weeklySummary.trim()).append("\n\n");
            }
        }

        // 情绪状态
        if (contextDecision.includeEmotionalState()
                && history != null && history.getEmotionalState() != null && !history.getEmotionalState().isBlank()) {
            context.append("【用户当前情绪状态：").append(history.getEmotionalState()).append("】\n");
        }

        if (extraSystemNote != null && !extraSystemNote.isBlank()) {
            context.append("【系统说明】\n").append(extraSystemNote).append("\n");
        }

        context.append("【当前用户消息】\n").append(currentMessage);
        return context.toString();
    }


    private String buildTodayRecordReply(User user, UserRecord userRecord) {
        if (user == null) return "暂无今日记录";
        boolean hasRecord = hasStructuredDietRecord(user.getId())
                || hasStructuredExerciseRecord(user.getId());
        if (hasRecord) {
            String aiSummary = summarizeDailyRecord(user.getId(), user, null);
            return aiSummary != null ? aiSummary : "今天暂无记录，运动或饮食后告诉我，我帮你记下来~";
        }
        String trainingPlanText = buildTrainingPlanText(user.getId());
        if (trainingPlanText != null && !trainingPlanText.isBlank()) {
            String daySection = buildTrainingPlanDaySection(user.getId(), 0);
            if (daySection != null) {
                String aiSummary = summarizeDailyRecord(user.getId(), user, daySection);
                return aiSummary != null ? aiSummary : "今天暂无记录，这是今天的训练安排：\n" + daySection;
            }
        }
        return "今天暂无记录，运动或饮食后告诉我，我帮你记下来~";
    }

    private String buildTodayDietReply(Long userId) {
        if (userId == null) {
            return "今天暂无饮食记录。";
        }
        List<Map<String, Object>> dietRecords = dietRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (dietRecords.isEmpty()) {
            return "今天暂无饮食记录。";
        }
        StringBuilder sb = new StringBuilder("【今日饮食记录】\n");
        for (Map<String, Object> item : dietRecords) {
            String mealType = normalizeMealType(toCleanString(item.get("mealType")));
            String name = toCleanString(item.get("name"));
            String calories = item.get("calories") == null ? "" : item.get("calories") + "kcal";
            String time = toCleanString(item.get("time"));
            sb.append("餐次：")
                    .append(mealType.isBlank() ? "未分类" : mealType)
                    .append("｜")
                    .append(calories.isBlank() ? "-" : calories);
            if (!time.isBlank()) {
                sb.append("｜").append(time);
            }
            sb.append("\n");
            sb.append("食物：").append(name.isBlank() ? "未命名食物" : name).append("\n");
        }
        return sb.toString().trim();
    }

    private String buildTodayExerciseReply(Long userId) {
        if (userId == null) {
            return "今天暂无训练记录。";
        }
        List<Map<String, Object>> sessions = exerciseRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (sessions.isEmpty()) {
            return "今天暂无训练记录。";
        }
        StringBuilder sb = new StringBuilder("【今日训练记录】\n");
        for (Map<String, Object> session : sessions) {
            String sessionName = toCleanString(session.get("name"));
            Integer sessionDuration = parseInteger(session.get("durationSeconds"));
            List<Map<String, Object>> items = castObjectList(session.get("items"));
            if (sessionName.isBlank() && items.isEmpty()) {
                continue;
            }
            sb.append("训练：")
                    .append(sessionName.isBlank() ? "未命名训练" : sessionName);
            if (sessionDuration != null && sessionDuration > 0) {
                sb.append("｜").append(Math.max(1, sessionDuration / 60)).append("分钟");
            }
            sb.append("｜").append(items.size()).append("个动作").append("\n");
            for (Map<String, Object> item : items) {
                String name = toCleanString(item.get("name"));
                String muscleGroup = toCleanString(item.get("muscleGroup"));
                Integer durationSeconds = parseInteger(item.get("durationSeconds"));
                Integer completedSets = parseInteger(item.get("completedSets"));
                if (name.isBlank()) {
                    continue;
                }
                sb.append("动作：").append(name);
                if (!muscleGroup.isBlank()) {
                    sb.append("｜").append(muscleGroup);
                }
                if (completedSets != null) {
                    sb.append("｜").append(completedSets).append("组");
                }
                if (durationSeconds != null && durationSeconds > 0) {
                    sb.append("｜").append(Math.max(1, durationSeconds / 60)).append("分钟");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildWeekRecordReply(User user, UserRecord userRecord) {
        if (user == null) return "暂无本周记录";
        if (userRecord != null && userRecord.getWeeklyReviews() != null && !userRecord.getWeeklyReviews().isBlank()) {
            List<String> reviews = parseJsonArray(userRecord.getWeeklyReviews());
            if (reviews.isEmpty()) return "本周暂无记录";
            StringBuilder sb = new StringBuilder("【本周记录】\n");
            for (String review : reviews) {
                sb.append(review).append("\n\n");
            }
            return sb.toString().trim();
        }
        return "本周暂无记录，运动或饮食后告诉我，我帮你记下来~";
    }

    private record TrainingPlanTarget(String label, DayOfWeek dayOfWeek) {}
    private record TrainingPlanLookupDecision(boolean shouldLookup, String targetDay, boolean viewAll) {}
    private record DietPlanLookupDecision(boolean shouldLookup, String mealType, boolean viewAll) {}
    private record PromptContextDecision(boolean needsRag, boolean includeRecentDialog, boolean includeTodayRecord,
                                         boolean includeYesterdaySummary, boolean includeWeeklySummary,
                                         boolean includeEmotionalState, boolean includeTrainingPlanContext,
                                         boolean includeDietPlanContext) {}
    private record DietRecordDecision(AddDietRecordRequest request, boolean continueChat,
                                      String clarificationMessage, String noticeMessage) {}
    private record ExerciseRecordDecision(AddExerciseRecordRequest request, boolean continueChat) {}
    private record StructuredDietParseResult(AddDietRecordRequest request, String clarificationMessage,
                                             String noticeMessage) {}
    private record MealNutrition(String summaryName, BigDecimal calories, BigDecimal protein,
                                 BigDecimal carbs, BigDecimal fat, BigDecimal fiber,
                                 List<Map<String, Object>> items) {}
    private record MessageRoutingDecision(boolean preferGeneralAnswer, boolean continueChatAfterRecord,
                                          PromptContextDecision promptContext,
                                          String recordLookupIntent, String planGenerationIntent,
                                          TrainingPlanLookupDecision trainingPlanLookup,
                                          DietPlanLookupDecision dietPlanLookup,
                                          ExerciseRecordDecision exerciseRecord,
                                          DietRecordDecision dietRecord) {}

    private WebClient getWebClient(AiModelConfig.ModelProvider provider) {
        WebClient cached = CACHED_WEB_CLIENT.get();
        if (cached != null) {
            return cached;
        }
        WebClient webClient = WebClient.builder()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        CACHED_WEB_CLIENT.set(webClient);
        return webClient;
    }

    private String callAiText(String systemPrompt, String userMessage, int maxTokens, double temperature) {
        try {
            AiModelConfig.ModelProvider provider = requireActiveProvider();
            WebClient webClient = getWebClient(provider);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return "";
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            Map<String, Object> msgObj = (Map<String, Object>) choices.get(0).get("message");
            String content = msgObj == null ? null : (String) msgObj.get("content");
            return content == null ? "" : content.trim();
        } catch (Exception e) {
            log.error("AI文本调用失败", e);
            return "";
        }
    }

    private String callAiSingle(String userMessage, int maxTokens, double temperature) {
        try {
            AiModelConfig.ModelProvider provider = requireActiveProvider();
            WebClient webClient = getWebClient(provider);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", userMessage)
            ));

            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> msgObj = (Map<String, Object>) choices.get(0).get("message");
            return msgObj == null ? null : (String) msgObj.get("content");
        } catch (Exception e) {
            log.error("AI单消息调用失败", e);
            return null;
        }
    }

    private static final String[] DAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};

    private String resolveMealType(String msg) {
        Matcher matcher = DIET_SINGLE_MEAL_PATTERN.matcher(msg);
        if (!matcher.find()) return null;
        String meal = matcher.group(1);
        return switch (meal) {
            case "午饭" -> "午餐";
            case "晚饭" -> "晚餐";
            case "夜宵", "宵夜" -> "加餐";
            default -> meal;
        };
    }

    private String getCurrentMealType() {
        int hour = java.time.LocalTime.now(CN_ZONE).getHour();
        if (hour < 10) return "早餐";
        if (hour < 14) return "午餐";
        if (hour < 17) return "加餐";
        if (hour < 21) return "晚餐";
        return "加餐";
    }

    private String buildTrainingPlanText(Long userId) {
        if (userId == null) {
            return null;
        }
        String cached = CACHED_TRAINING_PLAN_TEXT.get();
        if (cached != null) {
            return cached;
        }
        UserTrainingCycleVO activeCycle = userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map<Long, UserTrainingTemplateVO> templateMap = userTrainingTemplateService.listTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("训练计划\n");
        for (int i = 1; i <= activeCycle.getDayCount(); i++) {
            final int dayIndex = i;
            UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream()
                    .filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex)
                    .findFirst()
                    .orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("星期").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("：");
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
        String result = sb.toString().trim();
        CACHED_TRAINING_PLAN_TEXT.set(result);
        return result;
    }

    private String buildTrainingPlanDaySection(Long userId, int offsetDays) {
        if (userId == null) {
            return null;
        }
        UserTrainingCycleVO activeCycle = userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getTodayIndex() == null || activeCycle.getDayCount() == null
                || activeCycle.getDayCount() <= 0 || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map<Long, UserTrainingTemplateVO> templateMap = userTrainingTemplateService.listTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        int dayCount = activeCycle.getDayCount();
        int zeroBased = Math.floorMod(activeCycle.getTodayIndex() - 1 + offsetDays, dayCount);
        int targetIndex = zeroBased + 1;
        UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream()
                .filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex)
                .findFirst()
                .orElse(null);
        DayOfWeek targetDayOfWeek = LocalDate.now(CN_ZONE).plusDays(offsetDays).getDayOfWeek();
        String title = "星期" + DAY_NAMES[targetDayOfWeek.getValue() - 1];
        if (day == null || day.getTemplateId() == null) {
            return title + "（休息日）\n休息";
        }
        UserTrainingTemplateVO template = templateMap.get(day.getTemplateId());
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return title + "（休息日）\n休息";
        }
        String body = buildTrainingTemplateBody(template.getItems());
        String muscleLabel = inferTrainingDayMuscleLabel(template);
        StringBuilder sb = new StringBuilder(title)
                .append("（")
                .append(muscleLabel.isBlank() ? safeTrim(template.getName()) : muscleLabel)
                .append("）");
        if (!body.isBlank()) {
            sb.append("\n").append(body);
        }
        return sb.toString().trim();
    }

    private void appendTrainingSection(StringBuilder sb, String label, String sectionType,
                                       List<UserTrainingTemplateVO.TrainingItemVO> items) {
        List<UserTrainingTemplateVO.TrainingItemVO> sectionItems = items.stream()
                .filter(item -> sectionType.equalsIgnoreCase(safeTrim(item.getSectionType())))
                .sorted(Comparator.comparing(UserTrainingTemplateVO.TrainingItemVO::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (sectionItems.isEmpty()) {
            return;
        }
        sb.append(label).append("：");
        List<String> parts = new ArrayList<>();
        for (UserTrainingTemplateVO.TrainingItemVO item : sectionItems) {
            StringBuilder part = new StringBuilder(safeTrim(item.getExerciseName()));
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

    private String buildTrainingTemplateBody(List<UserTrainingTemplateVO.TrainingItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendTrainingSection(sb, "热身", "warmup", items);
        appendTrainingSection(sb, "正式训练", "main", items);
        appendTrainingSection(sb, "拉伸", "stretch", items);
        return sb.toString().trim();
    }

    private String inferTrainingDayMuscleLabel(UserTrainingTemplateVO template) {
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return "";
        }
        Map<String, Long> groupCount = template.getItems().stream()
                .map(UserTrainingTemplateVO.TrainingItemVO::getMuscleGroup)
                .map(this::normalizePlanMuscleGroup)
                .filter(group -> !isBlank(group))
                .collect(java.util.stream.Collectors.groupingBy(Function.identity(), LinkedHashMap::new, java.util.stream.Collectors.counting()));
        if (groupCount.isEmpty()) {
            return "";
        }
        String dominantGroup = groupCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        return switch (dominantGroup) {
            case "chest" -> "胸部";
            case "back" -> "背部";
            case "shoulders" -> "肩部";
            case "arms" -> "手臂";
            case "legs" -> "腿部";
            case "core" -> "核心";
            default -> "";
        };
    }

    private String buildDietPlanText(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDietCycleVO activeCycle = userDietCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map<Long, UserDietDayTemplateVO> dayTemplateMap = userDietDayTemplateService.listDayTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserDietDayTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, UserDietTemplateVO> mealTemplateMap = userDietTemplateService.listTemplates(userId).stream()
                .collect(java.util.stream.Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("一日食谱推荐\n");
        for (int i = 1; i <= activeCycle.getDayCount(); i++) {
            final int dayIndex = i;
            UserDietCycleVO.CycleDayVO day = activeCycle.getDays().stream()
                    .filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex)
                    .findFirst()
                    .orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("星期").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("：");
            if (day == null || day.getDayTemplateId() == null) {
                sb.append("未安排\n");
                continue;
            }
            UserDietDayTemplateVO dayTemplate = dayTemplateMap.get(day.getDayTemplateId());
            sb.append(dayTemplate == null ? "未安排" : dayTemplate.getName()).append("\n");
            if (dayTemplate == null || dayTemplate.getMealSlots() == null) {
                continue;
            }
            for (String mealType : PLAN_MEAL_ORDER) {
                UserDietDayTemplateVO.MealSlotVO slot = dayTemplate.getMealSlots().stream()
                        .filter(meal -> mealType.equals(normalizeMealType(meal.getMealType())))
                        .findFirst()
                        .orElse(null);
                if (slot == null || slot.getTemplateId() == null) {
                    continue;
                }
                UserDietTemplateVO mealTemplate = mealTemplateMap.get(slot.getTemplateId());
                if (mealTemplate == null || mealTemplate.getItems() == null || mealTemplate.getItems().isEmpty()) {
                    continue;
                }
                sb.append(mealType).append("：").append(mealTemplate.getName()).append("\n");
                for (UserDietTemplateVO.DietTemplateItemVO item : mealTemplate.getItems()) {
                    sb.append("- ")
                            .append(safeTrim(item.getFoodName()))
                            .append(" ")
                            .append(item.getAmount() == null ? "" : item.getAmount().stripTrailingZeros().toPlainString())
                            .append(safeTrim(item.getUnit()))
                            .append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String extractDietMealSection(String dietPlan, String mealType) {
        if (dietPlan == null || dietPlan.isBlank() || isBlank(mealType)) {
            return null;
        }
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> planByDay = new LinkedHashMap<>();
        String currentDay = null;
        String currentMeal = null;
        for (String rawLine : dietPlan.split("\\r?\\n")) {
            String line = safeTrim(rawLine);
            if (line.isBlank() || "一日食谱推荐".equals(line)) {
                continue;
            }
            if (line.startsWith("星期")) {
                currentDay = line;
                currentMeal = null;
                planByDay.putIfAbsent(currentDay, new LinkedHashMap<>());
                continue;
            }
            if (currentDay == null) {
                continue;
            }
            int colonIndex = line.indexOf('：');
            if (colonIndex < 0) {
                colonIndex = line.indexOf(':');
            }
            if (colonIndex > 0) {
                String normalizedMeal = normalizeMealType(line.substring(0, colonIndex));
                if (!normalizedMeal.isBlank()) {
                    currentMeal = normalizedMeal;
                    planByDay.get(currentDay).putIfAbsent(currentMeal, new ArrayList<>());
                    planByDay.get(currentDay).get(currentMeal).add(line);
                    continue;
                }
            }
            if (currentMeal != null) {
                planByDay.get(currentDay).putIfAbsent(currentMeal, new ArrayList<>());
                planByDay.get(currentDay).get(currentMeal).add(line);
            }
        }

        if (planByDay.isEmpty()) {
            return null;
        }

        String todayKey = "星期" + DAY_NAMES[LocalDate.now(CN_ZONE).getDayOfWeek().getValue() - 1];
        String todaySection = findDietMealSectionForDay(planByDay, todayKey, mealType);
        if (todaySection != null) {
            return todaySection;
        }
        for (String dayKey : planByDay.keySet()) {
            String fallback = findDietMealSectionForDay(planByDay, dayKey, mealType);
            if (fallback != null) {
                return fallback;
            }
        }
        return null;
    }

    private String findDietMealSectionForDay(LinkedHashMap<String, LinkedHashMap<String, List<String>>> planByDay,
                                             String dayKey,
                                             String mealType) {
        if (planByDay == null || isBlank(dayKey) || isBlank(mealType)) {
            return null;
        }
        LinkedHashMap<String, List<String>> meals = planByDay.get(dayKey);
        if (meals == null || meals.isEmpty()) {
            return null;
        }
        String normalizedMeal = normalizeMealType(mealType);
        List<String> lines = meals.get(normalizedMeal);
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return (dayKey + "\n" + String.join("\n", lines)).trim();
    }

    /**
     * 从训练计划文本中提取指定星期几的内容
     */
    private String extractDaySection(String trainingPlan, String dayName) {
        Pattern p = Pattern.compile(
                "(?:星期" + dayName + "|周" + dayName + ")[：:\\s]*(.*?)(?=(?:星期[一二三四五六日]|周[一二三四五六日])|$)",
                Pattern.DOTALL);
        Matcher m = p.matcher(trainingPlan);
        if (m.find()) {
            String section = m.group(1).trim();
            return section.isEmpty() ? null : section;
        }
        return null;
    }

    /**
     * 处理计划生成请求（训练计划 / 饮食谱）
     * 返回 null 表示不是计划请求，交给后续流程处理
     */
    private String handlePlanGeneration(String message, User user, ChatHistory history,
                                        OutputStream outputStream, StringBuilder resultHolder, UserRecord userRecord,
                                        MessageRoutingDecision routingDecision, String postRecordSystemNote) {
        String msg = message.toLowerCase();
        String userInfo = buildUserInfo(user);
        PromptContextDecision promptContext = routingDecision == null
                ? defaultPromptContextDecision()
                : routingDecision.promptContext();
        String contextAwareMessage = message;
        if (promptContext.includeTrainingPlanContext() && user != null) {
            String trainingPlanText = buildTrainingPlanText(user.getId());
            if (trainingPlanText != null && !trainingPlanText.isBlank()) {
                contextAwareMessage = message + "\n\n当前已有训练计划参考：\n" + trainingPlanText;
            }
        }
        String planIntent = routingDecision == null ? "none" : routingDecision.planGenerationIntent();
        if (planIntent.isBlank() || "none".equals(planIntent)) {
            return null;
        }

        // 饮食计划生成
        if ("diet_plan".equals(planIntent)) {
            String foodKnowledge = buildFoodCatalog(user != null ? user.getId() : null);
            String mealRequestInstruction = buildDietMealRequestInstruction(msg);
            String prompt = DIET_PLAN_GEN_PROMPT.replace("{userInfo}", userInfo)
                    .replace("{foodKnowledge}", foodKnowledge != null ? foodKnowledge : "无")
                    .replace("{mealRequestInstruction}", mealRequestInstruction);
            String aiReply = callAiApiStream(prompt, contextAwareMessage, outputStream, null, 900);
            return sanitizeDietPlanOutput(aiReply, resolveMealType(msg));
        }

        // 训练计划生成
        if ("training_plan".equals(planIntent)) {
            String exerciseCatalog = buildExerciseCatalog(user != null ? user.getId() : null);

            // 如果已有保存的训练计划，拼入上下文让AI可以调整而非重新生成
            String existingPlan = "";
            if (promptContext.includeTrainingPlanContext() && user != null) {
                String trainingPlanText = buildTrainingPlanText(user.getId());
                if (trainingPlanText != null && !trainingPlanText.isBlank()) {
                    existingPlan = "\n【用户当前已有的训练计划（用户反馈时在此基础上调整）】\n" + trainingPlanText + "\n";
                }
            }

            String prompt = TRAINING_PLAN_GEN_PROMPT
                    .replace("{userInfo}", userInfo)
                    .replace("{exerciseCatalog}", exerciseCatalog)
                    .replace("{existingPlan}", existingPlan);
            String aiReply = callAiApiStream(prompt, contextAwareMessage, outputStream, null, 1200);
            return sanitizeTrainingPlanOutput(aiReply, shouldForceDefaultTrainingWeek(msg));
        }

        return null;
    }

    // ==================== 第二层：安全检查 ====================

    private void securityCheck(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "消息不能为空");
        }
        if (message.length() > 2000) {
            throw new BusincessException(PARAMS_ERROR, "消息长度不能超过2000字");
        }
        // XSS 检测
        if (Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE).matcher(message).find()) {
            throw new BusincessException(PARAMS_ERROR, "输入包含非法内容");
        }
        // SQL 注入检测
        if (Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE)\\b.*\\b(FROM|INTO|TABLE|DATABASE|WHERE)\\b)",
                Pattern.CASE_INSENSITIVE).matcher(message).find()) {
            throw new BusincessException(PARAMS_ERROR, "输入包含非法内容");
        }
    }

    // ==================== RAG 检索 ====================

    /**
     * RAG 检索：返回 topK 候选文档的拼接文本，无结果返回 null
     */
    private String retrieveKnowledgeDirectly(String userMessage) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userMessage)
                            .topK(3)
                            .similarityThreshold(0.35)
                            .build()
            );
            if (results == null || results.isEmpty()) return null;

            StringBuilder sb = new StringBuilder();
            for (Document doc : results) {
                sb.append(doc.getText()).append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建用户信息上下文
     */
    private String buildUserInfo(User user) {
        if (user == null) {
            return "";
        }
        // 有画像直接用画像（已包含基本信息），没有画像就用基本信息兜底
        if (user.getUserProfile() != null && !user.getUserProfile().isBlank()) {
            return "\n【用户】" + user.getUsername() + "|画像:" + user.getUserProfile() + "\n";
        }
        String gender = user.getGender() != null ? (user.getGender() == 0 ? "女" : "男") : "未知";
        return "\n【用户】" + user.getUsername() + "|" + gender +
                "|" + (user.getHeight() != null ? user.getHeight() + "cm" : "") +
                "|" + (user.getWeight() != null ? user.getWeight() + "kg" : "") +
                "|" + (user.getAge() != null ? user.getAge() + "岁" : "") +
                "|目标:" + (user.getFitnessGoal() != null ? user.getFitnessGoal() : "") + "\n";
    }

    /**
     * 构建动作库列表（精简版：只含ID、名称、肌群，节省token）
     * 组数/次数/休息时间动作卡片上已有，不需要在提示词里重复
     */
    private String buildExerciseCatalog(Long userId) {
        List<Exercise> exercises;

        User user = userService.getById(userId);
        if (user != null && (user.getPreferredEquipment() != null || user.getExperienceLevel() != null)) {
            exercises = exerciseService.getByFilters(user.getPreferredEquipment(), user.getExperienceLevel());
        } else {
            exercises = exerciseService.getByFilters(null, null);
        }

        if (exercises == null || exercises.isEmpty()) {
            return "（动作库为空）";
        }

        // 将用户收藏的动作提前排列，让AI优先选用户喜欢的
        final Set<Long> favIds = new java.util.HashSet<>();
        if (user != null && user.getFavoritesExercises() != null && !user.getFavoritesExercises().isBlank()) {
            try {
                favIds.addAll(JSON_MAPPER.readValue(user.getFavoritesExercises(),
                        new com.fasterxml.jackson.core.type.TypeReference<Set<Long>>() {}));
            } catch (Exception ignored) {}
        }
        if (!favIds.isEmpty()) {
            exercises.sort((a, b) -> {
                boolean aFav = favIds.contains(a.getId());
                boolean bFav = favIds.contains(b.getId());
                if (aFav != bFav) return aFav ? -1 : 1;
                Integer aOrder = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
                Integer bOrder = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
                return Integer.compare(aOrder, bOrder);
            });
        }

        Map<String, String> groupMap = Map.of(
                "chest", "胸部",
                "back", "背部",
                "core", "核心",
                "arms", "手臂",
                "legs", "腿部",
                "shoulders", "肩部"
        );
        Map<String, List<String>> mainByGroup = new LinkedHashMap<>();
        Map<String, List<String>> warmupByGroup = new LinkedHashMap<>();
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            mainByGroup.put(group, new ArrayList<>());
            warmupByGroup.put(group, new ArrayList<>());
        }

        for (Exercise exercise : exercises) {
            if (exercise == null || isBlank(exercise.getName()) || isBlank(exercise.getMuscleGroup())) {
                continue;
            }
            String groupKey = normalizePlanMuscleGroup(exercise.getMuscleGroup());
            if (!mainByGroup.containsKey(groupKey)) {
                continue;
            }
            if (isWarmupExercise(exercise.getName())) {
                addDistinctLimited(warmupByGroup.get(groupKey), exercise.getName(), 4);
                continue;
            }
            if (isStretchExercise(exercise.getName())) {
                continue;
            }
            addDistinctLimited(mainByGroup.get(groupKey), exercise.getName(), 6);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【分部位训练动作库-训练动作只能从对应部位中选】\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List<String> actions = mainByGroup.get(group);
            if (actions == null || actions.isEmpty()) {
                continue;
            }
            sb.append(groupMap.getOrDefault(group, group))
                    .append("训练动作：")
                    .append(String.join("、", actions))
                    .append("\n");
        }

        sb.append("\n【分部位热身动作-热身只能从当天训练部位对应的热身动作中选】\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List<String> warmups = warmupByGroup.get(group);
            if (warmups == null || warmups.isEmpty()) {
                continue;
            }
            sb.append(groupMap.getOrDefault(group, group))
                    .append("热身动作：")
                    .append(String.join("、", warmups))
                    .append("\n");
        }

        return sb.toString();
    }

    private String normalizePlanMuscleGroup(String muscleGroup) {
        if (muscleGroup == null) {
            return "";
        }
        return switch (muscleGroup.trim().toLowerCase()) {
            case "胸部", "chest" -> "chest";
            case "背部", "back" -> "back";
            case "肩部", "shoulder", "shoulders" -> "shoulders";
            case "手臂", "arm", "arms" -> "arms";
            case "腿部", "leg", "legs" -> "legs";
            case "核心", "core" -> "core";
            default -> muscleGroup.trim().toLowerCase();
        };
    }

    private boolean isWarmupExercise(String name) {
        return !isBlank(name) && name.contains("热身");
    }

    private boolean isStretchExercise(String name) {
        return !isBlank(name) && name.contains("拉伸");
    }

    private void addDistinctLimited(List<String> target, String value, int limit) {
        if (target == null || isBlank(value) || target.size() >= limit || target.contains(value)) {
            return;
        }
        target.add(value.trim());
    }

    private String buildFoodCatalog(Long userId) {
        List<FoodItem> foods = foodItemService.searchVisibleFoods(userId, "");
        if (foods == null || foods.isEmpty()) {
            return "无";
        }
        List<FoodItem> selected = foods.stream().limit(12).toList();
        StringBuilder sb = new StringBuilder();
        sb.append("【食物参考-只优先从中选择】名称|分类|基准|热量(kJ)\n");
        for (FoodItem food : selected) {
            String baseAmount = food.getBaseAmount() == null ? "-" : food.getBaseAmount().stripTrailingZeros().toPlainString();
            sb.append(safeTrim(food.getName())).append("|")
                    .append(safeTrim(food.getCategory())).append("|")
                    .append(baseAmount).append(safeTrim(food.getUnit())).append("|")
                    .append(food.getCalories() == null ? "-" : food.getCalories().stripTrailingZeros().toPlainString())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    // ==================== 调用 AI ====================

    /**
     * 流式调用百炼API（OpenAI兼容格式），逐块将AI输出推送到前端
     * <p>
     * 使用HttpURLConnection而非WebClient，因为WebClient的bodyToFlux会缓冲整个响应，
     * 而HttpURLConnection的BufferedReader可以逐行读取，实现真正的逐字推送。
     * <p>
     * AI返回的SSE格式：每行为 data:{"choices":[{"delta":{"content":"xxx"}}]}，
     * 流结束时会收到 data:[DONE]
     *
     * @return AI完整回复文本（用于后续保存记录）
     */
    private String callAiApiStream(String systemPrompt, String userMessage, OutputStream outputStream, String modelName) {
        return callAiApiStream(systemPrompt, userMessage, outputStream, modelName, aiModelConfig.getMaxTokens());
    }

    private String callAiApiStream(String systemPrompt, String userMessage, OutputStream outputStream, String modelName, int maxTokens) {
        try {
            AiModelConfig.ModelProvider provider = isBlank(modelName) ? requireActiveProvider() : requireProvider(modelName);
            return callAiApiStreamWithProvider(systemPrompt, userMessage, outputStream, provider, maxTokens);
        } catch (BusincessException e) {
            throw e;
        } catch (Exception e) {
            log.error("流式调用AI接口失败", e);
            throw new BusincessException(AI_ERROR, "当前AI调用失败，请切换别的AI试一试");
        }
    }

    private String callAiApiStreamWithProvider(String systemPrompt, String userMessage,
                                               OutputStream outputStream,
                                               AiModelConfig.ModelProvider provider,
                                               int maxTokens) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        long startedAt = System.currentTimeMillis();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", aiModelConfig.getTemperature());
        requestBody.put("stream", true);
        if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
            requestBody.put("enable_thinking", false);
        }
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpURLConnection conn = (HttpURLConnection) new URL(provider.getBaseUrl() + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + provider.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;

                Map<String, Object> response = objectMapper.readValue(data, Map.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices == null || choices.isEmpty()) continue;
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta == null || !delta.containsKey("content")) continue;
                String content = (String) delta.get("content");
                if (content == null || content.isEmpty()) continue;

                fullText.append(content);
                writeSseData(outputStream, content);
            }
        } catch (BusincessException e) {
            throw e;
        } finally {
            conn.disconnect();
        }
        return fullText.toString();
    }

    // ==================== 保存/更新对话记录 ====================

    /**
     * 根据 ID 更新聊天记录
     * @param history
     */
    @Override
    public void updateChatHistory(ChatHistory history) {
        chatHistoryMapper.updateById(history);
    }

    /**
     * 根据 ID 创造聊天记录
     * @param history
     */
    @Override
    public void createChatHistory(ChatHistory history) {
        chatHistoryMapper.insert(history);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String saveGeneratedPlan(Long userId, String type, String content) {
        if (userId == null || content == null || content.isBlank()) {
            throw new BusincessException(PARAMS_ERROR, "没有可保存的计划内容");
        }
        if ("training".equals(type)) {
            saveGeneratedTrainingPlan(userId, content);
            return "训练计划已保存";
        }
        if ("diet".equals(type)) {
            saveGeneratedDietPlan(userId, content);
            return "饮食计划已保存";
        }
        throw new BusincessException(PARAMS_ERROR, "不支持的计划类型");
    }

    /**
     * 保存或更新对话记录
     */
    private void saveOrUpdateChatHistory(Long userId, String userMessage, String aiResponse) {
        String emotionalState = detectEmotionalState(userMessage);

        ChatHistory existing = getChatHistory(userId);

        if (existing != null) {
            existing.setUpdateTime(new java.util.Date());
            existing.setMessageCount(existing.getMessageCount() == null ? 1 : existing.getMessageCount() + 1);
            if (emotionalState != null) {
                existing.setEmotionalState(emotionalState);
            }

            // 从DB读取待总结缓冲，追加本轮消息后写回
            List<String> buffer = parseJsonArray(existing.getPendingMessages());
            buffer.add("用户：" + userMessage + "\n助手：" + aiResponse);
            if (buffer.size() > RECENT_DIALOG_LIMIT) {
                buffer = new ArrayList<>(buffer.subList(buffer.size() - RECENT_DIALOG_LIMIT, buffer.size()));
            }

            // 每5次把缓冲的消息全部总结，然后清空缓冲
            if (existing.getMessageCount() % RECENT_DIALOG_LIMIT == 0 && !buffer.isEmpty()) {
                String allMessages = String.join("\n", buffer);
                String previousSummary = existing.getSummary();
                String summary = generateSummary(previousSummary, allMessages);
                // 总结超过500字截断，防止无限膨胀
                if (summary != null && summary.length() > 500) {
                    summary = summary.substring(0, 500);
                }
                existing.setSummary(summary);
            }
            try {
                existing.setPendingMessages(JSON_MAPPER.writeValueAsString(buffer));
            } catch (Exception e) {
                log.warn("序列化pendingMessages失败", e);
            }

            chatHistoryMapper.updateById(existing);
        } else {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setUserId(userId);
            chatHistory.setMessageCount(1);
            chatHistory.setEmotionalState(emotionalState);
            try {
                chatHistory.setPendingMessages(JSON_MAPPER.writeValueAsString(List.of(
                        "用户：" + userMessage + "\n助手：" + aiResponse)));
            } catch (Exception e) {
                log.warn("序列化pendingMessages失败", e);
            }
            chatHistoryMapper.insert(chatHistory);
        }
    }

    private void saveGeneratedTrainingPlan(Long userId, String content) {
        List<ParsedTrainingDay> days = parseTrainingPlanText(content);
        if (days.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "训练计划格式无法识别");
        }
        List<String> unresolvedActions = new ArrayList<>();
        List<SaveUserTrainingCycleRequest.CycleDayDTO> cycleDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ParsedTrainingDay day = i < days.size() ? days.get(i) : new ParsedTrainingDay(i + 1, "星期" + DAY_NAMES[i], "", true, List.of(), List.of(), List.of());
            SaveUserTrainingCycleRequest.CycleDayDTO dayDTO = new SaveUserTrainingCycleRequest.CycleDayDTO();
            dayDTO.setDayIndex(i + 1);
            if (!day.rest()) {
                SaveUserTrainingTemplateRequest req = new SaveUserTrainingTemplateRequest();
                req.setName(day.title().isBlank() ? "训练日" + (i + 1) : day.title());
                List<SaveUserTrainingTemplateRequest.TrainingItemDTO> items = new ArrayList<>();
                int sort = 0;
                for (String action : day.warmups()) {
                    Long exerciseId = resolveExerciseIdForPlan(action, "warmup", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    SaveUserTrainingTemplateRequest.TrainingItemDTO dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("warmup");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                for (String action : day.trainings()) {
                    Long exerciseId = resolveExerciseIdForPlan(action, "main", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    SaveUserTrainingTemplateRequest.TrainingItemDTO dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("main");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                for (String action : day.stretches()) {
                    Long exerciseId = resolveExerciseIdForPlan(action, "stretch", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    SaveUserTrainingTemplateRequest.TrainingItemDTO dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("stretch");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                if (!items.isEmpty()) {
                    req.setItems(items);
                    Long templateId = userTrainingTemplateService.saveTemplate(userId, req);
                    dayDTO.setTemplateId(templateId);
                }
            }
            cycleDays.add(dayDTO);
        }
        if (!unresolvedActions.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR,
                    "训练计划里有动作未命中动作库，请重新生成：" + unresolvedActions.stream().distinct().limit(4).reduce((a, b) -> a + "、" + b).orElse(""));
        }
        SaveUserTrainingCycleRequest cycleRequest = new SaveUserTrainingCycleRequest();
        cycleRequest.setName("AI一周训练循环");
        cycleRequest.setDayCount(7);
        cycleRequest.setStartDate(LocalDate.now(CN_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        cycleRequest.setActivate(true);
        cycleRequest.setDays(cycleDays);
        userTrainingCycleService.saveCycle(userId, cycleRequest);
    }

    private void saveGeneratedDietPlan(Long userId, String content) {
        Map<String, String> meals = parseDietPlanText(content);
        if (meals.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "饮食计划格式无法识别");
        }
        Map<String, Long> mealConfig = new LinkedHashMap<>();
        for (String mealType : PLAN_MEAL_ORDER) {
            String foodLine = meals.get(mealType);
            if (foodLine == null || foodLine.isBlank()) {
                continue;
            }
            List<ParsedFoodAmount> parsedFoods = parseDietFoodLine(foodLine);
            if (parsedFoods.isEmpty()) {
                continue;
            }
            SaveUserDietTemplateRequest req = new SaveUserDietTemplateRequest();
            req.setName("AI" + mealType + "模板");
            req.setMealType(mealType);
            List<SaveUserDietTemplateRequest.DietTemplateItemDTO> items = new ArrayList<>();
            int sort = 0;
            for (ParsedFoodAmount parsed : parsedFoods) {
                Long foodId = resolveFoodIdForPlan(userId, parsed.name(), parsed.amount(), parsed.unit());
                SaveUserDietTemplateRequest.DietTemplateItemDTO dto = new SaveUserDietTemplateRequest.DietTemplateItemDTO();
                dto.setFoodItemId(foodId);
                dto.setAmount(parsed.amount());
                dto.setUnit(parsed.unit());
                dto.setSortOrder(sort++);
                items.add(dto);
            }
            req.setItems(items);
            Long templateId = userDietTemplateService.saveTemplate(userId, req);
            mealConfig.put(mealType, templateId);
        }
        if (mealConfig.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "饮食计划中没有可保存的餐次");
        }
        SaveUserDietDayTemplateRequest dayTemplateRequest = new SaveUserDietDayTemplateRequest();
        dayTemplateRequest.setName("AI一日饮食");
        dayTemplateRequest.setMealConfig(mealConfig);
        Long dayTemplateId = userDietDayTemplateService.saveDayTemplate(userId, dayTemplateRequest);

        SaveUserDietCycleRequest cycleRequest = new SaveUserDietCycleRequest();
        cycleRequest.setName("AI一日饮食循环");
        cycleRequest.setDayCount(1);
        cycleRequest.setStartDate(LocalDate.now(CN_ZONE));
        cycleRequest.setActivate(true);
        SaveUserDietCycleRequest.CycleDayDTO day = new SaveUserDietCycleRequest.CycleDayDTO();
        day.setDayIndex(1);
        day.setDayTemplateId(dayTemplateId);
        cycleRequest.setDays(List.of(day));
        userDietCycleService.saveCycle(userId, cycleRequest);
    }

    private List<ParsedTrainingDay> parseTrainingPlanText(String content) {
        List<ParsedTrainingDay> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("星期([一二三四五六日天])(?:（([^）]*)）)?\\s*(.*?)(?=星期[一二三四五六日天](?:（[^）]*）)?|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String dayChar = matcher.group(1);
            String muscle = safeTrim(matcher.group(2));
            String body = safeTrim(matcher.group(3));
            String title = "星期" + dayChar + (muscle.isBlank() ? "" : " · " + muscle);
            List<String> warmups = List.of();
            List<String> trainings = List.of();
            List<String> stretches = List.of();
            boolean rest = body.contains("休息");
            if (!rest) {
                warmups = parseActionLine(body, "热身");
                trainings = parseActionLine(body, "训练");
                stretches = parseActionLine(body, "拉伸");
            }
            result.add(new ParsedTrainingDay(result.size() + 1, title, muscle, rest, warmups, trainings, stretches));
        }
        return result;
    }

    private Map<String, String> parseDietPlanText(String content) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] lines = content.split("\\r?\\n");
        String currentMeal = null;
        for (String rawLine : lines) {
            String line = safeTrim(rawLine);
            if (line.isBlank()) continue;
            String normalizedMeal = normalizeMealType(line);
            if (!normalizedMeal.isBlank() && line.equals(normalizedMeal)) {
                currentMeal = normalizedMeal;
                continue;
            }
            if (currentMeal != null && line.startsWith("吃什么")) {
                int idx = line.indexOf('：');
                if (idx < 0) idx = line.indexOf(':');
                String foods = idx >= 0 ? safeTrim(line.substring(idx + 1)) : "";
                if (!foods.isBlank()) {
                    sections.put(currentMeal, foods);
                }
            }
        }
        return sections;
    }

    private List<String> parseActionLine(String body, String label) {
        Pattern pattern = Pattern.compile(label + "[：:](.*?)(?:\\n|$)");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            return List.of();
        }
        String raw = safeTrim(matcher.group(1));
        if (raw.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        String[] parts = raw.split("[、，,+＋；;]");
        for (String part : parts) {
            String action = normalizeActionName(part);
            if (!action.isBlank()) {
                result.add(action);
            }
        }
        return result;
    }

    private List<ParsedFoodAmount> parseDietFoodLine(String line) {
        List<ParsedFoodAmount> result = new ArrayList<>();
        String[] parts = line.split("[+＋]");
        for (String rawPart : parts) {
            String part = sanitizeDietRecordText(rawPart);
            if (part.isBlank()) continue;
            Matcher matcher = Pattern.compile("(.+?)(\\d+(?:\\.\\d+)?)(kg|g|ml|l|个|片|根|袋|份|只|枚)$").matcher(part.replaceAll("\\s+", ""));
            if (!matcher.find()) {
                continue;
            }
            String name = sanitizeDietRecordText(matcher.group(1));
            BigDecimal amount = new BigDecimal(matcher.group(2));
            String unit = matcher.group(3);
            if ("kg".equalsIgnoreCase(unit)) {
                amount = amount.multiply(new BigDecimal("1000"));
                unit = "g";
            } else if ("l".equalsIgnoreCase(unit)) {
                amount = amount.multiply(new BigDecimal("1000"));
                unit = "ml";
            }
            result.add(new ParsedFoodAmount(name, amount, unit));
        }
        return result;
    }

    private Long resolveExerciseIdForPlan(String rawName, String sectionType, String muscleGroup) {
        String name = normalizeActionName(rawName);
        if (name.isBlank()) {
            return null;
        }
        QueryWrapper<Exercise> exact = new QueryWrapper<>();
        exact.eq("isActive", 1).eq("name", name).last("LIMIT 1");
        Exercise matched = exerciseService.getOne(exact, false);
        if (matched == null) {
            QueryWrapper<Exercise> fuzzy = new QueryWrapper<>();
            fuzzy.eq("isActive", 1).like("name", name).last("LIMIT 5");
            List<Exercise> candidates = exerciseService.list(fuzzy);
            matched = candidates.stream()
                    .filter(item -> item.getName() != null && (item.getName().contains(name) || name.contains(item.getName())))
                    .findFirst()
                    .orElse(null);
        }
        if (matched != null) {
            return matched.getId();
        }
        return null;
    }

    private Exercise findBestExerciseForRecord(String rawName) {
        String name = normalizeActionName(rawName);
        if (name.isBlank()) {
            return null;
        }
        QueryWrapper<Exercise> exact = new QueryWrapper<>();
        exact.eq("isActive", 1).eq("name", name).last("LIMIT 1");
        Exercise matched = exerciseService.getOne(exact, false);
        if (matched != null) {
            return matched;
        }

        QueryWrapper<Exercise> fuzzy = new QueryWrapper<>();
        fuzzy.eq("isActive", 1).like("name", name).last("LIMIT 10");
        List<Exercise> candidates = exerciseService.list(fuzzy);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String normalizedKeyword = normalizeFoodKeyword(name);
        for (Exercise item : candidates) {
            String normalizedName = normalizeFoodKeyword(item.getName());
            if (normalizedName.equals(normalizedKeyword)) {
                return item;
            }
        }
        for (Exercise item : candidates) {
            String normalizedName = normalizeFoodKeyword(item.getName());
            if (normalizedName.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedName)) {
                return item;
            }
        }
        return null;
    }

    private Long resolveFoodIdForPlan(Long userId, String rawName, BigDecimal amount, String unit) {
        String name = safeTrim(rawName);
        if (name.isBlank()) {
            throw new BusincessException(PARAMS_ERROR, "饮食计划里存在空食物名");
        }
        List<FoodItem> candidates = foodItemService.searchVisibleFoods(userId, name);
        FoodItem matched = candidates.stream()
                .filter(item -> item.getName() != null && item.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(item -> item.getName() != null && (item.getName().contains(name) || name.contains(item.getName())))
                        .findFirst()
                        .orElse(null));
        if (matched != null) {
            return matched.getId();
        }
        FoodItem entity = new FoodItem();
        entity.setName(name);
        entity.setUnit(unit);
        entity.setBaseAmount(amount);
        entity.setCalories(BigDecimal.ZERO);
        entity.setProtein(BigDecimal.ZERO);
        entity.setCarbs(BigDecimal.ZERO);
        entity.setFat(BigDecimal.ZERO);
        entity.setFiber(BigDecimal.ZERO);
        entity.setCreatedBy(userId);
        entity.setIsSystem(0);
        entity.setIsDelete(0);
        foodItemService.save(entity);
        return entity.getId();
    }

    private String resolveMuscleGroupKey(String muscleGroup, String fallbackName) {
        String text = (safeTrim(muscleGroup) + " " + safeTrim(fallbackName)).toLowerCase();
        if (text.contains("胸")) return "chest";
        if (text.contains("背")) return "back";
        if (text.contains("腿")) return "legs";
        if (text.contains("肩")) return "shoulders";
        if (text.contains("臂") || text.contains("二头") || text.contains("三头")) return "arms";
        if (text.contains("腹") || text.contains("核心")) return "core";
        return "core";
    }

    private String normalizeActionName(String raw) {
        String text = sanitizeExerciseRecordText(raw);
        text = text.replaceAll("[（(].*?[）)]", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(分钟|min|秒|s|小时|h|组|次)$", "");
        text = text.replaceAll("静态拉伸$", "拉伸");
        return safeTrim(text);
    }

    private String sanitizeExerciseRecordText(String raw) {
        String text = safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经|已经在)?(?:做了|练了|跑了|走了|骑了)", "");
        text = text.replaceAll("^(?:进行|完成)(?:了)?", "");
        text = text.replaceAll("[。！!，,；;、\\s]+$", "");
        return safeTrim(text);
    }

    private String sanitizeDietRecordText(String raw) {
        String text = safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经)?(?:早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜)?(?:吃了|喝了|吃过|喝过)", "");
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经)?(?:吃了|喝了|吃过|喝过)", "");
        text = text.replaceAll("^(?:早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜)[:：]?", "");
        text = text.replaceAll("[。！!，,；;、\\s]+$", "");
        return safeTrim(text);
    }

    private String stripLeadingRecordPhrases(String raw) {
        String text = safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        String previous;
        do {
            previous = text;
            text = text.replaceAll("^(?:请|麻烦|帮忙)?(?:帮我|给我)?(?:记录一下|记录下|记录|记一下|记下|记一笔|保存一下|保存下)\\s*", "");
            text = text.replaceAll("^(?:请|麻烦|帮忙)?(?:把|将)?\\s*", "");
        } while (!previous.equals(text));
        return safeTrim(text);
    }

    private Integer extractCompletedSets(String exerciseText) {
        String text = safeTrim(exerciseText);
        if (text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*组").matcher(text);
        if (matcher.find()) {
            return parseInteger(matcher.group(1));
        }
        return null;
    }

    private Integer extractDurationSeconds(String exerciseText) {
        String text = safeTrim(exerciseText);
        if (text.isBlank()) {
            return null;
        }
        Matcher hourMinuteMatcher = Pattern.compile("(\\d{1,2})\\s*(?:小时|h|H)\\s*(\\d{1,2})?\\s*(?:分钟|min)?").matcher(text);
        if (hourMinuteMatcher.find()) {
            int hours = Integer.parseInt(hourMinuteMatcher.group(1));
            int minutes = hourMinuteMatcher.group(2) == null ? 0 : Integer.parseInt(hourMinuteMatcher.group(2));
            return hours * 3600 + minutes * 60;
        }
        Matcher minuteMatcher = Pattern.compile("(\\d{1,4})\\s*(?:分钟|min)").matcher(text);
        if (minuteMatcher.find()) {
            return Integer.parseInt(minuteMatcher.group(1)) * 60;
        }
        Matcher secondMatcher = Pattern.compile("(\\d{1,5})\\s*(?:秒钟|秒|s|S)").matcher(text);
        if (secondMatcher.find()) {
            return Integer.parseInt(secondMatcher.group(1));
        }
        return null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record ParsedTrainingDay(int dayIndex, String title, String muscleGroup, boolean rest,
                                     List<String> warmups, List<String> trainings, List<String> stretches) {}

    private record ParsedFoodAmount(String name, BigDecimal amount, String unit) {}

    /**
     * 调用AI对对话进行总结
     */
    private String generateSummary(String userMessage, String aiResponse) {
        try {
            String prompt = "总结以下对话，保留需求、建议要点、后续关注点，≤200字，去掉寒暄。\n" +
                    "用户：" + userMessage + "\n" +
                    "助手：" + aiResponse;

            String result = callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.error("生成对话总结失败", e);
        }
        // 总结失败，只保留每轮对话的前50字，避免 fallback 把全部内容拼回
        StringBuilder fallback = new StringBuilder("对话摘要(自动生成)：\n");
        String[] lines = aiResponse.split("\n");
        for (int i = 0; i < lines.length && fallback.length() < 400; i++) {
            String line = lines[i];
            if (line.startsWith("用户：") || line.startsWith("助手：")) {
                fallback.append(line, 0, Math.min(line.length(), 60)).append("...\n");
            }
        }
        return fallback.toString();
    }

    private boolean hasStructuredDietRecord(Long userId) {
        return userId != null && dietRecordService.hasRecord(userId, LocalDate.now(CN_ZONE));
    }

    private boolean hasStructuredExerciseRecord(Long userId) {
        return userId != null && exerciseRecordService.hasRecord(userId, LocalDate.now(CN_ZONE));
    }

    private String buildTodaySummaryExerciseInput(Long userId) {
        List<Map<String, Object>> sessions = userId == null
                ? Collections.emptyList()
                : exerciseRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (sessions.isEmpty()) {
            return "无训练记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> session : sessions) {
            String sessionName = toCleanString(session.get("name"));
            Integer sessionDuration = parseInteger(session.get("durationSeconds"));
            if (!sessionName.isBlank()) {
                sb.append("训练：").append(sessionName);
                if (sessionDuration != null && sessionDuration > 0) {
                    sb.append("，").append(Math.max(1, sessionDuration / 60)).append("分钟");
                }
                sb.append("\n");
            }
            for (Map<String, Object> item : castObjectList(session.get("items"))) {
                String name = toCleanString(item.get("name"));
                String muscleGroup = toCleanString(item.get("muscleGroup"));
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
        return sb.length() == 0 ? "无训练记录" : sb.toString().trim();
    }

    private String buildTodaySummaryDietInput(Long userId) {
        List<Map<String, Object>> dietRecords = userId == null
                ? Collections.emptyList()
                : dietRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (dietRecords.isEmpty()) {
            return "无饮食记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : dietRecords) {
            String mealType = normalizeMealType(toCleanString(item.get("mealType")));
            String name = toCleanString(item.get("name"));
            if (name.isBlank()) {
                continue;
            }
            sb.append("- ");
            if (!mealType.isBlank()) {
                sb.append(mealType).append("：");
            }
            sb.append(name);
            sb.append("\n");
        }
        return sb.length() == 0 ? "无饮食记录" : sb.toString().trim();
    }

    /**
     * 调用 AI 总结今日原始记录（仅展示用，不保存）
     */
    private String summarizeDailyRecord(Long userId, User user, String todayPlanSection) {
        boolean hasExerciseRecord = hasStructuredExerciseRecord(userId);
        boolean hasDietRecord = hasStructuredDietRecord(userId);
        boolean noUserRecord = !hasExerciseRecord && !hasDietRecord;
        try {
            String userInfo = buildUserInfo(user);

            String prompt;
            if (noUserRecord) {
                prompt = "你是健身助手Tatan。用户今天没有记录任何运动和饮食，但系统查到了今天的训练计划。\n\n" +
                        "请生成一份与数据库存储格式一致的提醒总结。\n" +
                        "纯文本不用markdown，总字数限制在220字以内，严格按以下格式输出：\n" +
                        "总结：一句话概括今天应完成的安排。\n" +
                        "建议：给出1条最值得执行的建议，并自然带上“今天还没记录哦”。\n" +
                        "训练：简要列出今天应完成的训练内容。\n" +
                        "饮食：写“暂无饮食记录”。\n" +
                        "问题：说明无法确认是否按计划完成。\n\n" +
                        (userInfo.isBlank() ? "" : userInfo + "\n") +
                        "【今天的训练计划】\n" + (todayPlanSection == null ? "" : todayPlanSection.trim());
            } else {
                prompt = "你是健身助手Tatan。请根据用户今天的结构化运动/饮食记录，生成一份与数据库存储格式一致的今日总结。\n\n" +
                        "纯文本不用markdown，总字数限制在220字以内，严格按以下格式输出：\n" +
                        "总结：一句话概括今天状态。\n" +
                        "建议：给出1条最值得执行的建议。\n" +
                        "训练：概括今天训练内容，没有则写“暂无训练记录”。\n" +
                        "饮食：概括今天饮食情况，没有则写“暂无饮食记录”。\n" +
                        "问题：如无明显问题可写“无”。\n\n" +
                        "重要约束：\n" +
                        "1. 只要【结构化训练记录】里有动作名，就不能写“暂无训练记录”。\n" +
                        "2. 训练记录里没有时长，不代表没有训练；只有动作名、肌群、组数也属于有效训练记录。\n" +
                        "3. 不要因为部分训练没有时长，就写“0分钟异常”“数据无效”“暂无训练记录”这类结论。\n" +
                        "4. 优先依据结构化记录总结，不要被旧的自然语言流水误导。\n" +
                        "5. 如果同类训练或同一餐明显重复出现，优先做合并概括，不要机械逐条复述。\n" +
                        "6. 如果你判断存在重复记录、疑似重复打卡、餐次重复等情况，可以在“问题：”里简短指出，但不要夸大成错误。\n" +
                        "7. “训练：”和“饮食：”要写成概括后的自然语言，不要简单抄原始列表。\n\n" +
                        (userInfo.isBlank() ? "" : userInfo + "\n") +
                        "【结构化训练记录】\n" + buildTodaySummaryExerciseInput(userId) + "\n\n" +
                        "【结构化饮食记录】\n" + buildTodaySummaryDietInput(userId);
            }

            String result = callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.error("生成今日记录总结失败", e);
        }
        if (noUserRecord) {
            return "总结：今天应按计划完成训练安排。\n建议：今天还没记录哦，练完记得及时打卡。\n训练：请参考今日训练计划。\n饮食：暂无饮食记录。\n问题：无法确认今天是否按计划完成。";
        }
        return "总结：今天已有记录，但总结生成失败。\n建议：可以稍后再查看一次今日总结。\n训练：请查看今日原始记录。\n饮食：请查看今日原始记录。\n问题：暂无可靠结构化总结。";
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public String quickSaveTodayPlan(Long userId, String type) {
        LocalDate today = LocalDate.now(CN_ZONE);
        String recordTime = LocalTime.now(CN_ZONE).format(TIME_FMT);

        if ("training".equals(type)) {
            UserTrainingCycleVO cycle = userTrainingCycleService.getActiveCycle(userId);
            if (cycle == null || cycle.getTodayIndex() == null || cycle.getDays() == null || cycle.getDays().isEmpty()) {
                throw new BusincessException(NULL_ERROR, "还没有训练计划");
            }
            Map<Long, UserTrainingTemplateVO> templateMap = userTrainingTemplateService.listTemplates(userId).stream()
                    .collect(java.util.stream.Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, java.util.LinkedHashMap::new));
            int targetIndex = cycle.getTodayIndex();
            UserTrainingCycleVO.CycleDayVO day = cycle.getDays().stream()
                    .filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex)
                    .findFirst().orElse(null);
            if (day == null || day.getTemplateId() == null) {
                throw new BusincessException(NULL_ERROR, "今天是休息日，没有训练安排");
            }
            UserTrainingTemplateVO template = templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                throw new BusincessException(NULL_ERROR, "今天的训练计划没有动作");
            }
            DayOfWeek dow = today.getDayOfWeek();
            String note = "星期" + DAY_NAMES[dow.getValue() - 1] + " · Day " + targetIndex;
            for (UserTrainingTemplateVO.TrainingItemVO item : template.getItems()) {
                AddExerciseRecordRequest req = new AddExerciseRecordRequest();
                req.setExerciseId(item.getExerciseId());
                req.setExerciseName(item.getExerciseName());
                req.setMuscleGroup(item.getMuscleGroup());
                req.setCompletedSets(item.getRecommendedSets());
                req.setDurationSeconds(null);
                req.setCaloriesBurned(null);
                req.setNote(item.getNote());
                req.setTime(recordTime);
                exerciseRecordService.saveRecord(userId, today, req, "chat");
            }
            return "已记录" + (template.getName() != null ? template.getName() : "") + "训练";
        }

        if ("diet".equals(type)) {
            UserDietCycleVO dietCycle = userDietCycleService.getActiveCycle(userId);
            if (dietCycle == null || dietCycle.getTodayIndex() == null || dietCycle.getDays() == null || dietCycle.getDays().isEmpty()) {
                throw new BusincessException(NULL_ERROR, "还没有饮食计划");
            }
            int targetIndex = dietCycle.getTodayIndex();
            UserDietCycleVO.CycleDayVO day = dietCycle.getDays().stream()
                    .filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex)
                    .findFirst().orElse(null);
            if (day == null || day.getDayTemplateId() == null) {
                throw new BusincessException(NULL_ERROR, "今天没有饮食安排");
            }
            List<UserDietDayTemplateVO> dayTemplates = userDietDayTemplateService.listDayTemplates(userId);
            UserDietDayTemplateVO dayTpl = dayTemplates.stream()
                    .filter(t -> t.getId().equals(day.getDayTemplateId())).findFirst().orElse(null);
            if (dayTpl == null || dayTpl.getMealSlots() == null || dayTpl.getMealSlots().isEmpty()) {
                throw new BusincessException(NULL_ERROR, "今天的饮食计划没有餐次");
            }
            List<UserDietTemplateVO> dietTemplates = userDietTemplateService.listTemplates(userId);
            Map<Long, UserDietTemplateVO> dtMap = dietTemplates.stream()
                    .collect(java.util.stream.Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a));
            int savedMeals = 0;
            for (UserDietDayTemplateVO.MealSlotVO slot : dayTpl.getMealSlots()) {
                if (slot.getTemplateId() == null) continue;
                UserDietTemplateVO mealTpl = dtMap.get(slot.getTemplateId());
                if (mealTpl == null || mealTpl.getItems() == null || mealTpl.getItems().isEmpty()) continue;
                AddDietRecordRequest dietReq = new AddDietRecordRequest();
                dietReq.setTime(recordTime);
                dietReq.setMealType(slot.getMealType());
                dietReq.setName(mealTpl.getName());
                dietReq.setCalories(null);
                dietReq.setNote(null);
                dietReq.setSource("chat");
                List<DietFoodItemRequest> items = new ArrayList<>();
                for (UserDietTemplateVO.DietTemplateItemVO foodItem : mealTpl.getItems()) {
                    DietFoodItemRequest ir = new DietFoodItemRequest();
                    ir.setFoodItemId(foodItem.getFoodItemId());
                    ir.setAmount(foodItem.getAmount());
                    items.add(ir);
                }
                dietReq.setItems(items);
                appendDietRecord(userId, userRecordService.getByUserId(userId), dietReq);
                savedMeals++;
            }
            return "已记录" + savedMeals + "餐饮食";
        }

        throw new BusincessException(PARAMS_ERROR, "无效的类型");
    }

    /**
     * 调用AI根据用户基本信息 + 画像表单数据生成画像总结
     */
    @Override
    public String generateUserProfile(User user, String profileFormData) {
        try {
            String gender = user.getGender() != null ? (user.getGender() == 0 ? "女" : "男") : "未知";
            String userBasic = String.format("性别:%s, 年龄:%s岁, 身高:%scm, 体重:%skg, 健身目标:%s",
                    gender,
                    user.getAge() != null ? user.getAge() : "未填",
                    user.getHeight() != null ? user.getHeight() : "未填",
                    user.getWeight() != null ? user.getWeight() : "未填",
                    user.getFitnessGoal() != null ? user.getFitnessGoal() : "未填");

            String oldProfile = user.getUserProfile() != null ? user.getUserProfile() : "暂无画像";
            String prompt = "你是一个AI健身教练的助手。请根据用户的【旧画像】、【基本信息】和【用户自填信息】，更新用户画像。\n\n" +
                    "【旧画像】" + oldProfile + "\n" +
                    "【基本信息】" + userBasic + "\n" +
                    "【用户自填】" + profileFormData + "\n\n" +
                    "要求：\n" +
                    "- 把基本信息和自填信息融合到旧画像中，不要分点罗列\n" +
                    "- 保留旧画像中仍然准确的内容\n" +
                    "- 用户自填的信息可能表述不清，帮他总结到位（比如\"办公室\"总结为\"久坐办公\"）\n" +
                    "- 伤病/饮食禁忌必须保留原意，不能遗漏\n" +
                    "- 100字以内，不废话\n" +
                    "- 不要加任何前缀，直接输出画像内容";

            String result = callAiSingle(prompt, 200, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        } catch (Exception e) {
            log.error("生成用户画像失败", e);
        }
        // AI调用失败，用前端拼接的原始数据兜底
        return profileFormData;
    }

    /**
     * 检测用户情绪状态
     */
    private String detectEmotionalState(String message) {
        String[] negative = {"累", "痛", "受伤", "坚持不", "放弃", "太难了", "焦虑", "崩溃", "绝望", "撑不住"};
        String[] positive = {"开心", "高兴", "兴奋", "期待", "进步", "成功", "做到"};

        for (String word : negative) {
            if (message.contains(word)) {
                return "低落";
            }
        }
        for (String word : positive) {
            if (message.contains(word)) {
                return "积极";
            }
        }
        return null;
    }
    // ==================== 健康记录自动识别 ====================

    private void appendDietRecord(Long userId, UserRecord cachedUserRecord, AddDietRecordRequest body) {
        try {
            String today = LocalDate.now(CN_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String recordTime = isBlank(body.getTime()) ? LocalTime.now(CN_ZONE).format(TIME_FMT) : body.getTime();
            body.setName(sanitizeDietRecordText(body.getName()));
            String resolvedMealType = resolveDietMealTypeForRecord(resolveMealTypeFromText(body.getMealType(), body.getName()), recordTime);
            if (isBlank(body.getName()) && (body.getItems() == null || body.getItems().isEmpty())) {
                return;
            }
            LocalDate recordDate = LocalDate.parse(today);
            if ((body.getItems() == null || body.getItems().isEmpty()) && !isBlank(body.getName())) {
                List<DietFoodItemRequest> matchedItems = tryMatchDietItemsForRecord(userId, body.getName());
                if (!matchedItems.isEmpty()) {
                    body.setItems(matchedItems);
                }
            }
            if (body.getItems() != null && !body.getItems().isEmpty()) {
                MealNutrition nutrition = resolveMealNutrition(body.getItems(), userId);
                dietRecordService.saveStructuredRecord(
                        userId,
                        recordDate,
                        recordTime,
                        resolvedMealType,
                        nutrition.summaryName(),
                        nutrition.calories().intValue(),
                        nutrition.protein(),
                        nutrition.carbs(),
                        nutrition.fat(),
                        nutrition.fiber(),
                        body.getNote(),
                        isBlank(body.getSource()) ? "chat" : body.getSource(),
                        nutrition.items()
                );
                body.setName(nutrition.summaryName());
                body.setCalories(nutrition.calories().intValue());
            } else {
                dietRecordService.saveSimpleRecord(
                        userId,
                        recordDate,
                        recordTime,
                        resolvedMealType,
                        body.getName().trim(),
                        body.getCalories(),
                        body.getNote(),
                        isBlank(body.getSource()) ? "chat" : body.getSource()
                );
            }
            User user = userService.getById(userId);
            userDailyMetricService.syncDailyCalories(
                    userId,
                    recordDate,
                    dietRecordService.listLegacyRecords(userId, recordDate),
                    resolveTargetCalories(user)
            );
        } catch (Exception e) {
            log.error("保存饮食记录失败", e);
        }
    }

    private MealNutrition resolveMealNutrition(List<DietFoodItemRequest> requests, Long userId) {
        List<Long> ids = requests.stream()
                .map(DietFoodItemRequest::getFoodItemId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "食物数据不能为空");
        }

        Map<Long, FoodItem> foodMap = new HashMap<>();
        for (FoodItem foodItem : foodItemService.listByIds(ids)) {
            if (foodItem == null) {
                continue;
            }
            boolean visible = Integer.valueOf(1).equals(foodItem.getIsSystem())
                    || (foodItem.getCreatedBy() != null && foodItem.getCreatedBy().equals(userId));
            if (visible) {
                foodMap.put(foodItem.getId(), foodItem);
            }
        }

        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (DietFoodItemRequest request : requests) {
            FoodItem foodItem = foodMap.get(request.getFoodItemId());
            if (foodItem == null) {
                throw new BusincessException(PARAMS_ERROR, "食物不存在或无权限使用");
            }
            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusincessException(PARAMS_ERROR, "摄入量必须大于0");
            }
            BigDecimal baseAmount = defaultIfZero(foodItem.getBaseAmount());
            BigDecimal ratio = amount.divide(baseAmount, 4, RoundingMode.HALF_UP);
            BigDecimal itemCalories = multiply(kjToKcal(foodItem.getCalories()), ratio);
            BigDecimal itemProtein = multiply(foodItem.getProtein(), ratio);
            BigDecimal itemCarbs = multiply(foodItem.getCarbs(), ratio);
            BigDecimal itemFat = multiply(foodItem.getFat(), ratio);
            BigDecimal itemFiber = multiply(foodItem.getFiber(), ratio);

            calories = calories.add(itemCalories);
            protein = protein.add(itemProtein);
            carbs = carbs.add(itemCarbs);
            fat = fat.add(itemFat);
            fiber = fiber.add(itemFiber);
            names.add(foodItem.getName());

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("foodItemId", foodItem.getId());
            item.put("name", foodItem.getName());
            item.put("imageUrl", foodItem.getImageUrl());
            item.put("amount", amount);
            item.put("unit", foodItem.getUnit());
            item.put("calories", itemCalories);
            item.put("protein", itemProtein);
            item.put("carbs", itemCarbs);
            item.put("fat", itemFat);
            item.put("fiber", itemFiber);
            items.add(item);
        }

        return new MealNutrition(
                String.join("、", names),
                calories.setScale(0, RoundingMode.HALF_UP),
                protein.setScale(1, RoundingMode.HALF_UP),
                carbs.setScale(1, RoundingMode.HALF_UP),
                fat.setScale(1, RoundingMode.HALF_UP),
                fiber.setScale(1, RoundingMode.HALF_UP),
                items
        );
    }

    private BigDecimal defaultIfZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(100) : value;
    }

    private BigDecimal multiply(BigDecimal source, BigDecimal ratio) {
        return (source == null ? BigDecimal.ZERO : source).multiply(ratio);
    }

    private BigDecimal kjToKcal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).divide(KJ_TO_KCAL_DIVISOR, 4, RoundingMode.HALF_UP);
    }

    private void appendExerciseRecord(Long userId, UserRecord cachedUserRecord, AddExerciseRecordRequest body) {
        try {
            String today = LocalDate.now(CN_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String recordTime = isBlank(body.getTime()) ? LocalTime.now(CN_ZONE).format(TIME_FMT) : body.getTime();
            body.setTime(recordTime);
            body.setExerciseName(sanitizeExerciseRecordText(body.getExerciseName()));
            if (body.getCompletedSets() == null) {
                body.setCompletedSets(extractCompletedSets(body.getExerciseName()));
            }
            if (body.getDurationSeconds() == null) {
                body.setDurationSeconds(extractDurationSeconds(body.getExerciseName()));
            }
            if (isBlank(body.getExerciseName())) {
                return;
            }
            if (body.getExerciseId() == null && !isBlank(body.getExerciseName())) {
                Exercise matched = findBestExerciseForRecord(body.getExerciseName());
                if (matched != null) {
                    body.setExerciseId(matched.getId());
                    body.setExerciseName(matched.getName());
                    if (isBlank(body.getMuscleGroup())) {
                        body.setMuscleGroup(matched.getMuscleGroup());
                    }
                }
            }
            exerciseRecordService.saveRecord(userId, LocalDate.parse(today, DateTimeFormatter.ISO_LOCAL_DATE), body, "chat");
        } catch (Exception e) {
            log.error("保存运动记录失败", e);
        }
    }

    private List<Map<String, Object>> parseObjectArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return JSON_MAPPER.readValue(fixed, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("解析对象数组失败: {}", json, e);
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castObjectList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Double resolveTargetCalories(User user) {
        if (user == null) {
            return null;
        }
        return user.getCustomDailyCalories() != null
                ? user.getCustomDailyCalories()
                : user.getDailyCalorieBurn();
    }

    private String resolveDietMealTypeForRecord(String mealType, String recordTime) {
        if (!isBlank(mealType)) {
            return normalizeMealType(mealType);
        }
        try {
            LocalTime time = LocalTime.parse(recordTime, TIME_FMT);
            int hour = time.getHour();
            if (hour < 10) return "早餐";
            if (hour < 14) return "午餐";
            if (hour < 17) return "加餐";
            if (hour < 21) return "晚餐";
            return "加餐";
        } catch (Exception ignored) {
            return getCurrentMealType();
        }
    }

    private String resolveMealTypeFromText(String preferredMealType, String rawText) {
        String normalizedPreferred = normalizeLookupMealType(preferredMealType);
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        String text = safeTrim(rawText);
        if (text.isBlank()) {
            return "";
        }
        if (text.contains("练后")) return "练后餐";
        if (text.contains("早餐")) return "早餐";
        if (text.contains("午餐") || text.contains("午饭")) return "午餐";
        if (text.contains("晚餐") || text.contains("晚饭")) return "晚餐";
        if (text.contains("加餐") || text.contains("夜宵") || text.contains("宵夜")) return "加餐";
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private TrainingPlanLookupDecision parseTrainingPlanLookupDecision(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return new TrainingPlanLookupDecision(false, "", false);
        }
        return new TrainingPlanLookupDecision(
                Boolean.TRUE.equals(map.get("shouldLookup")),
                normalizeTrainingLookupTarget(toCleanString(map.get("targetDay"))),
                Boolean.TRUE.equals(map.get("viewAll"))
        );
    }

    private PromptContextDecision parsePromptContextDecision(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return defaultPromptContextDecision();
        }
        return new PromptContextDecision(
                Boolean.TRUE.equals(map.get("needsRag")),
                Boolean.TRUE.equals(map.get("includeRecentDialog")),
                Boolean.TRUE.equals(map.get("includeTodayRecord")),
                Boolean.TRUE.equals(map.get("includeYesterdaySummary")),
                Boolean.TRUE.equals(map.get("includeWeeklySummary")),
                Boolean.TRUE.equals(map.get("includeEmotionalState")),
                Boolean.TRUE.equals(map.get("includeTrainingPlanContext")),
                Boolean.TRUE.equals(map.get("includeDietPlanContext"))
        );
    }

    private PromptContextDecision defaultPromptContextDecision() {
        return new PromptContextDecision(true, false, false, false, false, false, false, false);
    }

    @SuppressWarnings("unchecked")
    private DietPlanLookupDecision parseDietPlanLookupDecision(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return new DietPlanLookupDecision(false, "", false);
        }
        return new DietPlanLookupDecision(
                Boolean.TRUE.equals(map.get("shouldLookup")),
                normalizeLookupMealType(toCleanString(map.get("mealType"))),
                Boolean.TRUE.equals(map.get("viewAll"))
        );
    }

    @SuppressWarnings("unchecked")
    private ExerciseRecordDecision parseExerciseRecordDecision(Object raw, boolean continueChat) {
        if (!(raw instanceof Map<?, ?> map) || !Boolean.TRUE.equals(map.get("shouldRecord"))) {
            return null;
        }
        String exerciseName = toCleanString(map.get("exerciseName"));
        if (exerciseName.isBlank()) {
            return null;
        }
        AddExerciseRecordRequest request = new AddExerciseRecordRequest();
        request.setExerciseName(exerciseName);
        request.setMuscleGroup(toCleanString(map.get("muscleGroup")));
        request.setCompletedSets(parseInteger(map.get("completedSets")));
        request.setDurationSeconds(parseInteger(map.get("durationSeconds")));
        request.setNote(toCleanString(map.get("note")));
        request.setSource("chat");
        return new ExerciseRecordDecision(request, continueChat);
    }

    @SuppressWarnings("unchecked")
    private DietRecordDecision parseDietRecordDecision(Object raw, Long userId, boolean continueChat) {
        if (!(raw instanceof Map<?, ?> map) || !Boolean.TRUE.equals(map.get("shouldRecord"))) {
            return null;
        }
        String name = toCleanString(map.get("name"));
        String clarificationMessage = toCleanString(map.get("clarificationMessage"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems = map.get("items") instanceof List<?> list
                ? (List<Map<String, Object>>) (List<?>) list
                : Collections.emptyList();
        if (rawItems.isEmpty()) {
            String fallbackMessage = clarificationMessage.isBlank()
                    ? "要帮你记录饮食，请补充具体食物名和克重/份量，或者到记录饮食界面直接选择食物。"
                    : clarificationMessage;
            return new DietRecordDecision(null, continueChat, fallbackMessage, "");
        }
        StructuredDietParseResult structuredResult = tryBuildStructuredDietRequest(rawItems, userId);
        if (structuredResult.request() != null) {
            structuredResult.request().setMealType(normalizeMealType(toCleanString(map.get("mealType"))));
            structuredResult.request().setNote(toCleanString(map.get("note")));
            structuredResult.request().setSource("chat");
            return new DietRecordDecision(structuredResult.request(), continueChat, "", structuredResult.noticeMessage());
        }
        if (!structuredResult.clarificationMessage().isBlank()) {
            return new DietRecordDecision(null, continueChat, structuredResult.clarificationMessage(), "");
        }
        if (name.isBlank()) {
            return null;
        }
        AddDietRecordRequest request = new AddDietRecordRequest();
        request.setName(name);
        request.setMealType(normalizeMealType(toCleanString(map.get("mealType"))));
        request.setCalories(parseInteger(map.get("calories")));
        request.setNote(toCleanString(map.get("note")));
        request.setSource("chat");
        if (!clarificationMessage.isBlank()) {
            return new DietRecordDecision(null, continueChat, clarificationMessage, "");
        }
        return new DietRecordDecision(request, continueChat, "", "");
    }

    private StructuredDietParseResult tryBuildStructuredDietRequest(List<Map<String, Object>> rawItems, Long userId) {
        if (rawItems == null || rawItems.isEmpty()) {
            return new StructuredDietParseResult(null, "要帮你记录饮食，请补充具体食物名和克重/份量。", "");
        }
        List<DietFoodItemRequest> resolvedItems = new ArrayList<>();
        List<String> resolvedNames = new ArrayList<>();
        boolean hasIncompleteFood = false;
        for (Map<String, Object> rawItem : rawItems) {
            String rawName = toCleanString(rawItem.get("name"));
            if (rawName.isBlank()) {
                return new StructuredDietParseResult(null, "这条饮食里还缺具体食物名，补充后我再帮你记录。", "");
            }
            BigDecimal amount = parseBigDecimal(rawItem.get("amount"));
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "要帮你记录“" + rawName + "”，还需要补充克重或份量。", "");
            }
            BigDecimal fallbackCalories = parseBigDecimal(rawItem.get("calories"));
            String rawUnit = toCleanString(rawItem.get("unit"));
            String category = toCleanString(rawItem.get("category"));
            String calorieUnit = toCleanString(rawItem.get("calorieUnit"));
            String caloriesMode = toCleanString(rawItem.get("caloriesMode"));
            BigDecimal calorieBaseAmount = parseBigDecimal(rawItem.get("calorieBaseAmount"));
            String calorieBaseUnit = toCleanString(rawItem.get("calorieBaseUnit"));
            String action = normalizeDietItemAction(toCleanString(rawItem.get("action")));
            FoodItem foodItem = switch (action) {
                case "clarify" -> null;
                case "create_private_total", "create_private_per_base" -> handleAiFoodCreateAction(
                        userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit,
                        action, calorieBaseAmount, calorieBaseUnit
                );
                case "use_existing" -> handleAiFoodUseExistingAction(
                        userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit,
                        caloriesMode, calorieBaseAmount, calorieBaseUnit
                );
                default -> handleAiFoodUseExistingAction(
                        userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit,
                        caloriesMode, calorieBaseAmount, calorieBaseUnit
                );
            };
            if ("clarify".equals(action)) {
                return new StructuredDietParseResult(null,
                        "要帮你记录“" + rawName + "”，还需要补充更完整的信息。", "");
            }
            if (foodItem == null) {
                return new StructuredDietParseResult(
                        null,
                        "食物库暂无“" + rawName + "”的数据。如果想记录热量和营养物质，可以自行上传这个食物的具体数值哦。",
                        ""
                );
            }
            if (isIncompleteAiFood(foodItem)) {
                hasIncompleteFood = true;
            }
            BigDecimal normalizedAmount = normalizeDietAmount(amount, rawUnit, foodItem.getUnit());
            if (normalizedAmount == null || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "“" + rawName + "”这条还缺可换算的克重/毫升/份量，补充后我再帮你记录。", "");
            }
            DietFoodItemRequest requestItem = new DietFoodItemRequest();
            requestItem.setFoodItemId(foodItem.getId());
            requestItem.setAmount(normalizedAmount);
            resolvedItems.add(requestItem);
            resolvedNames.add(foodItem.getName());
        }
        AddDietRecordRequest request = new AddDietRecordRequest();
        request.setName(String.join("、", resolvedNames));
        request.setItems(resolvedItems);
        String noticeMessage = hasIncompleteFood
                ? "这次有食物是按你提供的热量和分量新建的简化数据，暂时不能准确判断蛋白质等营养；可以去个人中心管理自己创建的食物补全。"
                : "";
        return new StructuredDietParseResult(request, "", noticeMessage);
    }

    private FoodItem createAiFallbackFood(Long userId,
                                          String rawName,
                                          BigDecimal amount,
                                          String rawUnit,
                                          String category,
                                          BigDecimal caloriesValue,
                                          String calorieUnit,
                                          String caloriesMode,
                                          BigDecimal calorieBaseAmount,
                                          String calorieBaseUnit) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedUnit = normalizeAmountUnit(rawUnit);
        if (!"g".equals(normalizedUnit) && !"ml".equals(normalizedUnit) && !"个".equals(normalizedUnit)
                && !"片".equals(normalizedUnit) && !"根".equals(normalizedUnit) && !"袋".equals(normalizedUnit)
                && !"份".equals(normalizedUnit)) {
            return null;
        }
        String normalizedCalorieUnit = normalizeCalorieUnit(calorieUnit);
        BigDecimal storedCalories = normalizeStoredCalories(caloriesValue, normalizedCalorieUnit);
        if (storedCalories == null || storedCalories.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedMode = normalizeCaloriesMode(caloriesMode);
        BigDecimal resolvedBaseAmount = amount;
        String resolvedBaseUnit = normalizedUnit;
        if ("per_base".equals(normalizedMode)) {
            String normalizedBaseUnit = normalizeAmountUnit(calorieBaseUnit);
            if (calorieBaseAmount == null || calorieBaseAmount.compareTo(BigDecimal.ZERO) <= 0 || normalizedBaseUnit.isBlank()) {
                return null;
            }
            resolvedBaseAmount = calorieBaseAmount;
            resolvedBaseUnit = normalizedBaseUnit;
        }
        FoodItem foodItem = new FoodItem();
        foodItem.setName(rawName.trim());
        foodItem.setCategory(normalizeAiFoodCategory(category));
        foodItem.setUnit(resolvedBaseUnit);
        foodItem.setBaseAmount(resolvedBaseAmount.setScale(2, RoundingMode.HALF_UP));
        foodItem.setCalories(storedCalories.setScale(2, RoundingMode.HALF_UP));
        foodItem.setProtein(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        foodItem.setCarbs(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        foodItem.setFat(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        foodItem.setFiber(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        foodItem.setCreatedBy(userId);
        foodItem.setIsSystem(0);
        foodItem.setIsDelete(0);
        return foodItemService.save(foodItem) ? foodItem : null;
    }

    private FoodItem handleAiFoodUseExistingAction(Long userId,
                                                   String rawName,
                                                   BigDecimal amount,
                                                   String rawUnit,
                                                   String category,
                                                   BigDecimal caloriesValue,
                                                   String calorieUnit,
                                                   String caloriesMode,
                                                   BigDecimal calorieBaseAmount,
                                                   String calorieBaseUnit) {
        FoodItem foodItem = findBestVisibleFood(userId, rawName);
        if (foodItem != null && caloriesValue != null && caloriesValue.compareTo(BigDecimal.ZERO) > 0
                && shouldRefreshUserFood(foodItem, userId)) {
            FoodItem refreshedFood = refreshAiFallbackFood(
                    foodItem, rawName, amount, rawUnit, category, caloriesValue,
                    calorieUnit, caloriesMode, calorieBaseAmount, calorieBaseUnit
            );
            if (refreshedFood != null) {
                return refreshedFood;
            }
        }
        return foodItem;
    }

    private FoodItem handleAiFoodCreateAction(Long userId,
                                              String rawName,
                                              BigDecimal amount,
                                              String rawUnit,
                                              String category,
                                              BigDecimal caloriesValue,
                                              String calorieUnit,
                                              String action,
                                              BigDecimal calorieBaseAmount,
                                              String calorieBaseUnit) {
        FoodItem existing = findBestVisibleFood(userId, rawName);
        if (existing != null) {
            if (caloriesValue != null && caloriesValue.compareTo(BigDecimal.ZERO) > 0 && shouldRefreshUserFood(existing, userId)) {
                FoodItem refreshed = refreshAiFallbackFood(
                        existing, rawName, amount, rawUnit, category, caloriesValue,
                        calorieUnit,
                        "create_private_per_base".equals(action) ? "per_base" : "total",
                        calorieBaseAmount, calorieBaseUnit
                );
                if (refreshed != null) {
                    return refreshed;
                }
            }
            return existing;
        }
        if (caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return createAiFallbackFood(
                userId, rawName, amount, rawUnit, category, caloriesValue, calorieUnit,
                "create_private_per_base".equals(action) ? "per_base" : "total",
                calorieBaseAmount, calorieBaseUnit
        );
    }

    private FoodItem refreshAiFallbackFood(FoodItem foodItem,
                                           String rawName,
                                           BigDecimal amount,
                                           String rawUnit,
                                           String category,
                                           BigDecimal caloriesValue,
                                           String calorieUnit,
                                           String caloriesMode,
                                           BigDecimal calorieBaseAmount,
                                           String calorieBaseUnit) {
        if (foodItem == null || caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return foodItem;
        }
        String normalizedUnit = normalizeAmountUnit(rawUnit);
        if (normalizedUnit.isBlank()) {
            normalizedUnit = normalizeAmountUnit(foodItem.getUnit());
        }
        String normalizedCalorieUnit = normalizeCalorieUnit(calorieUnit);
        BigDecimal storedCalories = normalizeStoredCalories(caloriesValue, normalizedCalorieUnit);
        if (storedCalories == null || storedCalories.compareTo(BigDecimal.ZERO) <= 0) {
            return foodItem;
        }
        String normalizedMode = normalizeCaloriesMode(caloriesMode);
        BigDecimal resolvedBaseAmount = amount;
        String resolvedBaseUnit = normalizedUnit;
        if ("per_base".equals(normalizedMode)) {
            String normalizedBaseUnit = normalizeAmountUnit(calorieBaseUnit);
            if (calorieBaseAmount == null || calorieBaseAmount.compareTo(BigDecimal.ZERO) <= 0 || normalizedBaseUnit.isBlank()) {
                return foodItem;
            }
            resolvedBaseAmount = calorieBaseAmount;
            resolvedBaseUnit = normalizedBaseUnit;
        }
        foodItem.setName(rawName.trim());
        foodItem.setCategory(normalizeAiFoodCategory(category));
        foodItem.setUnit(resolvedBaseUnit);
        foodItem.setBaseAmount(resolvedBaseAmount.setScale(2, RoundingMode.HALF_UP));
        foodItem.setCalories(storedCalories.setScale(2, RoundingMode.HALF_UP));
        foodItemService.updateById(foodItem);
        return foodItemService.getById(foodItem.getId());
    }

    private boolean shouldRefreshUserFood(FoodItem foodItem, Long userId) {
        return foodItem != null
                && userId != null
                && !Integer.valueOf(1).equals(foodItem.getIsSystem())
                && foodItem.getCreatedBy() != null
                && foodItem.getCreatedBy().equals(userId);
    }

    private String normalizeDietItemAction(String action) {
        return switch (action) {
            case "use_existing", "create_private_total", "create_private_per_base", "clarify" -> action;
            default -> "";
        };
    }

    private boolean isIncompleteAiFood(FoodItem foodItem) {
        if (foodItem == null) {
            return false;
        }
        return isZero(foodItem.getProtein())
                && isZero(foodItem.getCarbs())
                && isZero(foodItem.getFat())
                && isZero(foodItem.getFiber());
    }

    private FoodItem findBestVisibleFood(Long userId, String rawName) {
        String keyword = rawName == null ? "" : rawName.trim();
        if (keyword.isBlank()) {
            return null;
        }
        List<FoodItem> candidates = foodItemService.searchVisibleFoods(userId, keyword);
        if (candidates == null || candidates.isEmpty()) {
            candidates = foodItemService.lambdaQuery()
                    .eq(FoodItem::getIsDelete, 0)
                    .and(q -> q.eq(FoodItem::getIsSystem, 1)
                            .or()
                            .eq(userId != null, FoodItem::getCreatedBy, userId))
                    .orderByDesc(FoodItem::getIsSystem)
                    .orderByAsc(FoodItem::getName)
                    .list();
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
        }
        String normalizedKeyword = normalizeFoodKeyword(keyword);
        for (FoodItem item : candidates) {
            if (normalizeFoodKeyword(item.getName()).equals(normalizedKeyword)) {
                return item;
            }
        }
        for (FoodItem item : candidates) {
            String normalizedName = normalizeFoodKeyword(item.getName());
            if (normalizedName.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedName)) {
                return item;
            }
        }
        for (String alias : expandFoodAliases(keyword)) {
            String normalizedAlias = normalizeFoodKeyword(alias);
            for (FoodItem item : candidates) {
                String normalizedName = normalizeFoodKeyword(item.getName());
                if (normalizedName.equals(normalizedAlias)
                        || normalizedName.contains(normalizedAlias)
                        || normalizedAlias.contains(normalizedName)) {
                    return item;
                }
            }
        }
        return candidates.get(0);
    }

    private FoodItem findStrictVisibleFood(Long userId, String rawName) {
        String keyword = rawName == null ? "" : rawName.trim();
        if (keyword.isBlank()) {
            return null;
        }
        List<FoodItem> candidates = foodItemService.searchVisibleFoods(userId, keyword);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedKeyword = normalizeFoodKeyword(keyword);
        for (FoodItem item : candidates) {
            if (normalizeFoodKeyword(item.getName()).equals(normalizedKeyword)) {
                return item;
            }
        }
        for (FoodItem item : candidates) {
            String normalizedName = normalizeFoodKeyword(item.getName());
            if (normalizedName.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedName)) {
                return item;
            }
        }
        for (String alias : expandFoodAliases(keyword)) {
            String normalizedAlias = normalizeFoodKeyword(alias);
            for (FoodItem item : candidates) {
                String normalizedName = normalizeFoodKeyword(item.getName());
                if (normalizedName.equals(normalizedAlias)
                        || normalizedName.contains(normalizedAlias)
                        || normalizedAlias.contains(normalizedName)) {
                    return item;
                }
            }
        }
        return null;
    }

    private List<DietFoodItemRequest> tryMatchDietItemsForRecord(Long userId, String rawDescription) {
        if (isBlank(rawDescription)) {
            return Collections.emptyList();
        }

        List<DietFoodItemRequest> matchedItems = new ArrayList<>();
        Set<Long> seenFoodIds = new LinkedHashSet<>();

        String normalizedLine = rawDescription.replace("以及", "+")
                .replace("还有", "+")
                .replace("并且", "+")
                .replace("然后", "+")
                .replace("再加", "+")
                .replace("搭配", "+")
                .replace("配", "+")
                .replace("和", "+")
                .replace("、", "+")
                .replace("，", "+")
                .replace(",", "+");

        for (ParsedFoodAmount parsed : parseDietFoodLine(normalizedLine)) {
            FoodItem foodItem = findStrictVisibleFood(userId, parsed.name());
            if (foodItem == null || foodItem.getId() == null || !seenFoodIds.add(foodItem.getId())) {
                continue;
            }
            DietFoodItemRequest request = new DietFoodItemRequest();
            request.setFoodItemId(foodItem.getId());
            request.setAmount(parsed.amount());
            matchedItems.add(request);
        }
        if (!matchedItems.isEmpty()) {
            return matchedItems;
        }

        String[] parts = normalizedLine.split("\\+");
        for (String rawPart : parts) {
            String keyword = normalizeFoodRecordKeyword(rawPart);
            if (keyword.isBlank()) {
                continue;
            }
            FoodItem foodItem = findStrictVisibleFood(userId, keyword);
            if (foodItem == null || foodItem.getId() == null || !seenFoodIds.add(foodItem.getId())) {
                continue;
            }
            DietFoodItemRequest request = new DietFoodItemRequest();
            request.setFoodItemId(foodItem.getId());
            request.setAmount(defaultIfZero(foodItem.getBaseAmount()));
            matchedItems.add(request);
        }
        return matchedItems;
    }

    private String normalizeFoodRecordKeyword(String rawPart) {
        String text = safeTrim(rawPart);
        if (text.isBlank()) {
            return "";
        }
        text = text.replaceAll("^(我|今天|刚刚|刚才|刚|早上|中午|晚上|夜里|夜宵|宵夜|早餐|午餐|午饭|晚餐|加餐|练后|喝了|吃了|喝|吃)+", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(kg|g|ml|l)$", "");
        text = text.replaceAll("^[零一二两三四五六七八九十百半几多少\\d]+\\s*(个|杯|碗|勺|片|块|根|袋|份|只|枚|串|盒|瓶|听|罐)", "");
        text = text.replaceAll("[零一二两三四五六七八九十百半几多少\\d]", "");
        return safeTrim(text);
    }

    private BigDecimal normalizeDietAmount(BigDecimal amount, String rawUnit, String targetUnit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedTargetUnit = normalizeAmountUnit(targetUnit);
        String normalizedRawUnit = normalizeAmountUnit(rawUnit);
        if (normalizedRawUnit.isBlank()) {
            return amount;
        }
        if (normalizedRawUnit.equals(normalizedTargetUnit)) {
            return amount;
        }
        if ("kg".equals(normalizedRawUnit) && "g".equals(normalizedTargetUnit)) {
            return amount.multiply(BigDecimal.valueOf(1000));
        }
        if ("l".equals(normalizedRawUnit) && "ml".equals(normalizedTargetUnit)) {
            return amount.multiply(BigDecimal.valueOf(1000));
        }
        return null;
    }

    private String normalizeAmountUnit(String unit) {
        String text = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        return switch (text) {
            case "g", "克", "公克", "gram", "grams" -> "g";
            case "kg", "公斤", "千克", "kilogram", "kilograms" -> "kg";
            case "ml", "毫升" -> "ml";
            case "l", "升", "liter", "liters" -> "l";
            case "个" -> "个";
            case "片" -> "片";
            case "根" -> "根";
            case "袋" -> "袋";
            case "份" -> "份";
            default -> text;
        };
    }

    private String normalizeFoodKeyword(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    private List<String> expandFoodAliases(String text) {
        String raw = text == null ? "" : text.trim();
        if (raw.isBlank()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(raw);
        aliases.add(raw.replace("某某", "").replace("这个", "").replace("这种", "").trim());
        aliases.add(raw.replace("即食", "").trim());
        aliases.add(raw.replace("去皮", "").trim());
        aliases.add(raw.replace("鸡胸肉", "鸡胸").trim());
        aliases.add(raw.replace("鸡胸", "鸡胸肉").trim());
        aliases.add(raw.replace("牛奶", "低脂牛奶").trim());
        aliases.removeIf(String::isBlank);
        return new ArrayList<>(aliases);
    }

    private String normalizeAiFoodCategory(String category) {
        String text = toCleanString(category);
        if (text.isBlank()) {
            return "未分类";
        }
        return switch (text) {
            case "碳水", "蛋白质", "脂肪", "蔬菜", "水果", "乳制品", "饮品", "零食", "补剂", "未分类" -> text;
            default -> "未分类";
        };
    }

    private String normalizeCalorieUnit(String calorieUnit) {
        String text = toCleanString(calorieUnit).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return "kcal";
        }
        return switch (text) {
            case "kj", "千焦" -> "kj";
            case "kcal", "卡", "大卡", "卡路里" -> "kcal";
            default -> "kcal";
        };
    }

    private String normalizeCaloriesMode(String caloriesMode) {
        String text = toCleanString(caloriesMode).toLowerCase(Locale.ROOT);
        return "per_base".equals(text) ? "per_base" : "total";
    }

    private BigDecimal normalizeStoredCalories(BigDecimal caloriesValue, String normalizedCalorieUnit) {
        if (caloriesValue == null) {
            return null;
        }
        if ("kj".equals(normalizedCalorieUnit)) {
            return caloriesValue;
        }
        return caloriesValue.multiply(KJ_TO_KCAL_DIVISOR);
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeTrainingLookupTarget(String targetDay) {
        if ("明天".equals(targetDay)) {
            return "明天";
        }
        return "今天";
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "加餐";
        }
        if (mealType.contains("练后")) return "练后餐";
        if (mealType.contains("早餐")) return "早餐";
        if (mealType.contains("午餐") || mealType.contains("午饭")) return "午餐";
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) return "晚餐";
        return "加餐";
    }

    private String normalizeLookupMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "";
        }
        if (mealType.contains("练后")) return "练后餐";
        if (mealType.contains("早餐")) return "早餐";
        if (mealType.contains("午餐") || mealType.contains("午饭")) return "午餐";
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) return "晚餐";
        if (mealType.contains("加餐") || mealType.contains("夜宵") || mealType.contains("宵夜")) return "加餐";
        return "";
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toCleanString(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return text;
    }

    private String resolveUserModelName(User user) {
        if (user == null || isBlank(user.getModelPreference())) {
            return aiModelConfig.getDefaultModel();
        }
        return user.getModelPreference().trim();
    }

    private String resolveActiveModelName() {
        String modelName = ACTIVE_CHAT_MODEL.get();
        return isBlank(modelName) ? aiModelConfig.getDefaultModel() : modelName.trim();
    }

    private AiModelConfig.ModelProvider requireActiveProvider() {
        return requireProvider(resolveActiveModelName());
    }

    private AiModelConfig.ModelProvider requireProvider(String modelName) {
        AiModelConfig.ModelProvider provider = aiModelConfig.getProvider(modelName);
        if (provider == null) {
            throw new BusincessException(AI_ERROR, "当前AI模型不存在，请切换别的AI试一试");
        }
        return provider;
    }

    private String normalizePlanSection(String section) {
        if (section == null) {
            return "";
        }
        return section.trim().replaceAll("\\n{3,}", "\n\n");
    }

    private String normalizeTrainingAdvice(String advice, String normalizedSection, boolean isRestDay) {
        String text = advice == null ? "" : advice.trim();
        if (text.isEmpty()) {
            return isRestDay
                    ? "建议：今天以恢复为主，做一点轻拉伸和散步就够了。"
                    : "建议：动作节奏稳一点，先把热身和拉伸做完整。";
        }
        text = text.replaceAll("^(今天|明天)[，,:：]?", "").trim();
        text = text.replaceAll("\\r", "");
        String[] suggestionParts = text.split("(?=建议[：:])");
        if (suggestionParts.length > 1) {
            String candidate = "";
            for (String part : suggestionParts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                // 优先保留更具体的建议，过滤掉“建议：练背部。”这种空泛短句
                if (trimmed.length() > candidate.length()) {
                    candidate = trimmed;
                }
            }
            if (!candidate.isEmpty()) {
                text = candidate;
            }
        }
        if (!isRestDay && (text.contains("休息日") || text.contains("恢复日"))) {
            return buildTrainingAdviceFallback(normalizedSection, false);
        }
        if (!text.startsWith("建议")) {
            text = "建议：" + text;
        }
        return text;
    }

    private String buildTrainingAdviceFallback(String normalizedSection, boolean isRestDay) {
        if (isRestDay) {
            return "建议：以恢复为主，做一点轻拉伸和散步就够了。";
        }
        if (normalizedSection.contains("胸")) {
            return "建议：练胸时重点拉伸胸大肌和肩前束，动作节奏稳一点，不要急着上重量。";
        }
        if (normalizedSection.contains("背")) {
            return "建议：练背时先把动作轨迹做稳定，练完把背阔肌和下背部拉伸到位。";
        }
        if (normalizedSection.contains("腿")) {
            return "建议：练腿时组间把呼吸节奏稳住，练完记得补水和拉伸。";
        }
        if (normalizedSection.contains("肩")) {
            return "建议：练肩时注意动作控制，避免借力过多，练完把三角肌和上背放松开。";
        }
        return "建议：动作节奏稳一点，先把热身和拉伸做完整。";
    }

    private String buildDietMealRequestInstruction(String msg) {
        String mealType = resolveMealType(msg);
        if (mealType == null) {
            return """
                    6. 如果用户没有明确指定某一餐，默认生成全天饮食（早餐、午餐、晚餐、加餐四餐）
                    7. 如果用户问法比较宽泛，如“推荐饮食”“减脂怎么吃”，也按全天饮食输出
                    8. 输出时必须覆盖早餐、午餐、晚餐、加餐四餐，不要只给其中一餐
                    """;
        }
        return """
                6. 用户这次明确只想看某一餐，请只生成【%s】这一餐，不要输出全天食谱
                7. 不要额外补早餐/午餐/晚餐/加餐其它餐次
                8. 标题和正文都围绕【%s】这一餐，不要出现“全天食谱”“一日食谱推荐”
                """.formatted(mealType, mealType);
    }

    private String sanitizeTrainingPlanOutput(String aiReply, boolean forceDefaultTrainingWeek) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        Matcher matcher = Pattern.compile("星期([一二三四五六日天])(?:（[^）]*）)?\\s*.*?(?=星期[一二三四五六日天](?:（[^）]*）)?|$)", Pattern.DOTALL)
                .matcher(aiReply);
        List<String> sections = new ArrayList<>();
        while (matcher.find()) {
            String block = safeTrim(matcher.group());
            if (block.isBlank()) {
                continue;
            }
            List<String> kept = new ArrayList<>();
            for (String rawLine : block.split("\\r?\\n")) {
                String line = safeTrim(rawLine);
                if (line.isBlank()) {
                    continue;
                }
                if (line.startsWith("星期")) {
                    kept.add(line);
                    continue;
                }
                if (line.startsWith("热身：") || line.startsWith("训练：") || line.startsWith("拉伸：")) {
                    kept.add(line);
                    continue;
                }
                if (line.startsWith("休息")) {
                    kept.add(line);
                }
            }
            if (!kept.isEmpty()) {
                sections.add(String.join("\n", kept));
            }
            if (sections.size() >= 7) {
                break;
            }
        }
        if (sections.isEmpty()) {
            return aiReply;
        }
        if (forceDefaultTrainingWeek) {
            enforceDefaultTrainingWeek(sections);
        }
        return "训练计划\n\n" + String.join("\n\n", sections);
    }

    private boolean shouldForceDefaultTrainingWeek(String message) {
        if (isBlank(message)) {
            return true;
        }
        String normalized = message.replace("每周", "");
        return !(normalized.contains("练三休一")
                || normalized.contains("练四休一")
                || normalized.contains("练一休一")
                || normalized.contains("每天都练")
                || normalized.contains("天天练")
                || normalized.contains("每周练")
                || normalized.contains("一周练")
                || normalized.contains("周末也练")
                || normalized.contains("周六练")
                || normalized.contains("周日练")
                || normalized.contains("星期六练")
                || normalized.contains("星期日练")
                || normalized.contains("周几休")
                || normalized.contains("星期几休"));
    }

    private void enforceDefaultTrainingWeek(List<String> sections) {
        Map<String, String> forcedRestMap = Map.of(
                "三", "星期三（休息日）\n休息",
                "六", "星期六（休息日）\n休息",
                "日", "星期日（休息日）\n休息",
                "天", "星期日（休息日）\n休息"
        );
        for (int i = 0; i < sections.size(); i++) {
            String section = sections.get(i);
            Matcher dayMatcher = Pattern.compile("^星期([一二三四五六日天])").matcher(section);
            if (!dayMatcher.find()) {
                continue;
            }
            String dayKey = dayMatcher.group(1);
            String forcedRest = forcedRestMap.get(dayKey);
            if (forcedRest != null) {
                sections.set(i, forcedRest);
            }
        }
    }

    private String sanitizeDietPlanOutput(String aiReply, String targetMealType) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        List<String> mealOrder = List.of("早餐", "午餐", "晚餐", "加餐", "练后餐");
        LinkedHashMap<String, String> mealMap = new LinkedHashMap<>();
        String currentMeal = null;
        for (String rawLine : aiReply.split("\\r?\\n")) {
            String line = safeTrim(rawLine);
            if (line.isBlank()) {
                continue;
            }
            String normalizedMeal = normalizeMealType(line);
            if (!normalizedMeal.isBlank() && line.equals(normalizedMeal)) {
                currentMeal = normalizedMeal;
                continue;
            }
            if (currentMeal != null && line.startsWith("吃什么：")) {
                mealMap.put(currentMeal, line);
                currentMeal = null;
            }
        }
        if (mealMap.isEmpty()) {
            return aiReply;
        }
        StringBuilder sb = new StringBuilder();
        boolean singleMeal = targetMealType != null && !targetMealType.isBlank();
        sb.append(singleMeal ? targetMealType + "推荐" : "一日食谱推荐").append("\n\n");
        if (singleMeal) {
            String foodLine = mealMap.get(targetMealType);
            if (foodLine == null) {
                return aiReply;
            }
            sb.append(targetMealType).append("\n").append(foodLine);
            return sb.toString().trim();
        }
        boolean first = true;
        for (String mealType : mealOrder) {
            String foodLine = mealMap.get(mealType);
            if (foodLine == null) {
                continue;
            }
            if (!first) {
                sb.append("\n\n");
            }
            sb.append(mealType).append("\n").append(foodLine);
            first = false;
        }
        return sb.toString().trim();
    }

    // ==================== 提示词 ====================

    /**
     * 通用对话提示词（知识库无相关知识时使用）
     * 区分专业问题和日常闲聊：专业问题拒绝回答，闲聊自由回答
     */

    private static final String TRAINING_PLAN_GEN_PROMPT = """
            你是健身助手Tatan，请根据用户信息生成或调整一周训练计划。

            规则：
            1. 输出完整7天，从星期一到星期日；休息日也写出来
            2. 默认训练节奏必须固定为：星期一训练、星期二训练、星期三休息、星期四训练、星期五训练、星期六休息、星期日休息；只有当用户明确提出别的节奏（如练三休一、每天都练、每周只练4天、周末也练）时，才允许覆盖默认节奏
            3. 先确定当天训练的主肌群，再从下方该肌群对应的“训练动作”里选训练动作；严禁跨肌群乱选
            4. 热身必须从当天训练主肌群对应的“热身动作”里选；严禁把全身热身、其他肌群热身、正式训练动作拿来充当热身
            5. 如果某天写的是“背部”，那么热身和训练动作都必须优先来自背部对应列表；胸部、腿部、肩部、手臂、核心同理
            6. 动作名必须逐字来自下方给定动作库，不能改写、不能自己发明、不能拼接库里不存在的动作
            7. 每天分成三行：热身、训练、拉伸
            8. 每天训练动作 4-5 个，不写组数次数，不写解释
            9. 同肌群尽量错开，避免连续两天重复同一主肌群
            10. 如果有【用户当前已有的训练计划】，优先按用户反馈做局部调整；没有就直接新生成
            11. 只输出计划正文，纯文本，不要 markdown，不要额外说明
            12. 严禁输出“注：”“说明：”“循环开始”“参考上周”“若需调整”“以上计划”等附加内容
            13. 每个训练日只能有4行：星期标题、热身、训练、拉伸；休息日只能有2行：星期标题、休息
            14. 星期标题只能写成“星期X（肌群）”或“星期X（休息日）”
            15. 训练行只能列动作，不要补括号解释，不要补原因，不要补恢复建议
            16. 在你正式输出前，先逐天自检：检查“标题肌群、热身动作、训练动作”三者是否属于同一肌群；如果不一致，说明结果不合格，必须重排后再输出
            17. 在你正式输出前，再自检默认模式的星期安排是否严格等于：一二训练、三休息、四五训练、六日休息；只要任何一天不符合，说明结果不合格，必须重排后再输出
            18. 默认模式下严禁输出周六训练、周日训练、连续3天训练，除非用户明确提出这类要求

            {existingPlan}

            输出格式：
            训练计划

            星期一（胸部）
            热身：开合跳 3分钟
            训练：杠铃卧推、上斜哑铃卧推、蝴蝶机夹胸、双杠臂屈伸
            拉伸：胸部静态拉伸

            星期二（背部）
            热身：高抬腿 3分钟
            训练：引体向上、杠铃划船、高位下拉、坐姿划船
            拉伸：背部静态拉伸

            星期三（休息日）
            休息，可做30分钟低强度有氧（散步/快走）

            星期四（腿部）
            ...（以此类推）

            {userInfo}
            {exerciseCatalog}
            """;

    private static final String DIET_PLAN_GEN_PROMPT = """
            你是健身助手Tatan，请根据用户信息和食物参考生成饮食推荐。

            规则：
            1. 优先从下方【食物参考】里选食物，食物尽量日常易买
            2. 先判断用户要的是全天饮食还是某一餐
            {mealRequestInstruction}
            3. 每餐只输出一行“吃什么：食物+分量”
            4. 不要输出营养分析、总计、解释、总结、注意事项
            5. 纯文本，不要 markdown
            6. 严禁输出“注：”“说明：”“建议：”“可替换”“可调整”“如果没有”之类附加文字
            7. 每个餐次只能有2行：餐次名、吃什么：...
            8. 除标题与餐次外，不要输出任何其他段落

            输出格式：
            一日食谱推荐

            早餐
            吃什么：即食鸡胸肉100g + 水煮蛋2个 + 无糖豆浆300ml

            午餐
            吃什么：去皮鸡腿150g + 糙米饭150g + 清炒西兰花200g

            晚餐
            吃什么：三文鱼120g + 红薯200g + 蒜蓉菠菜200g

            加餐
            吃什么：牛奶250ml + 香蕉1根

            【食物营养参考】
            {foodKnowledge}

            {userInfo}
            """;

    private static final String GENERAL_SYSTEM_PROMPT = """
            你是健身助手Tatan，风格简洁亲切。

            当前知识库中没有检索到与用户问题相关的知识条目。请按以下规则判断：

            1. 健身专业问题（训练、饮食、营养、减脂、增肌、伤病、康复、补剂、器械等）
               → 如果知识库没有直接相关内容，只能回复：知识库暂无该知识，我无法回答

            2. 日常闲聊、问候、情感倾诉、通用话题（天气、心情、电影、自我介绍等）
               → 像朋友一样自由回答，风格轻松，≤100字

            3. 非健身的专业请求（写代码、翻译、做题、写作等）
               → 回复：我主要擅长健身方面的问题，关于[用户提到的领域]建议咨询专业工具哦。有健身问题随时问我~

            情绪感知规则：
            - 如果上下文中用户情绪状态为"低落"，回复时主动关心和鼓励，语气温暖，适当提出放松建议
            - 如果用户情绪状态为"积极"，给予肯定和激励，语气热情
            - 如果没有情绪标注，正常回复即可

            回复格式：
            - 纯文本，不用markdown
            - 第一行用 emoji 开头
            """;

    private static final String RAG_SYSTEM_PROMPT = """
            你是健身助手Tatan。你现在处于严格 RAG 回答模式，只能依据下方【知识库检索结果】回答。

            【硬性规则】
            1. 先判断知识库条目是否能直接回答用户当前问题。
            2. “直接相关”必须满足：知识条目里的主题、动作/食物/营养/训练目标/伤病场景，与用户核心问题一致，并且能支持主要结论。
            3. 只是同属健身领域、只提到相近肌群、只出现泛泛训练原则，都不算直接相关。
            4. 如果没有直接相关知识条，或检索结果明显跑题，必须只回复：知识库暂无该知识，我无法回答
            5. 如果知识条直接相关，只能使用知识库中的内容回答；严禁补充常识、经验、推测、外部知识或自创建议。
            6. 如果知识条只覆盖了部分问题，只回答覆盖到的部分，并说明“知识库只覆盖到以上内容”。
            7. 不要因为用户画像、今日记录、历史上下文而扩展知识库没有提供的专业结论。
            8. 纯文本，不用 markdown，不输出判断过程。

            【相关性示例】
            - 用户问“胸肌训练怎么做”，知识库有“胸部训练原则” → 直接相关，可以回答。
            - 用户问“筋膜枪有用吗”，知识库只有“手臂训练原则”“胸部训练原则” → 不直接相关，只回复固定暂无内容。
            - 用户问“蛋白质怎么补”，知识库有“增肌期蛋白质摄入量” → 直接相关，可以回答。
            - 用户问“膝盖痛能不能深蹲”，知识库只有“腿部训练动作列表” → 不直接相关，只回复固定暂无内容。

            【知识相关时的回复格式】
            - 第一句直接回答结论。
            - 后面用 2-4 点说明知识库支持的依据。
            - 最后一行可以给 1 条实用建议，但必须来自知识库内容。
            - 总字数不超过 260 字。

            【知识库检索结果】
            {knowledge}
            """;
}
