/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.conditions.Wrapper
 *  com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
 *  com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
 *  com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.annotation.Resource
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.ai.document.Document
 *  org.springframework.ai.vectorstore.SearchRequest
 *  org.springframework.ai.vectorstore.VectorStore
 *  org.springframework.data.redis.core.StringRedisTemplate
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.multipart.MultipartFile
 *  org.springframework.web.reactive.function.client.WebClient
 *  org.springframework.web.reactive.function.client.WebClient$RequestBodySpec
 */
package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.common.AiCallHelper;
import com.zz.usercenter.common.CryptoUtils;
import com.zz.usercenter.common.StateCode;
import com.zz.usercenter.common.WeatherHelper;
import com.zz.usercenter.common.WebSearchHelper;
import com.zz.usercenter.config.AiModelConfig;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.ChatHistoryMapper;
import com.zz.usercenter.model.domain.ChatHistory;
import com.zz.usercenter.model.domain.DietRecord;
import com.zz.usercenter.model.domain.Exercise;
import com.zz.usercenter.model.domain.ExerciseSession;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserProfile;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.model.domain.UserWeightRecord;
import com.zz.usercenter.model.domain.request.AddDietRecordRequest;
import com.zz.usercenter.model.domain.request.AddExerciseRecordRequest;
import com.zz.usercenter.model.domain.request.DietFoodItemRequest;
import com.zz.usercenter.model.domain.request.SaveRecognizedFoodRequest;
import com.zz.usercenter.model.domain.request.SaveUserDietCycleRequest;
import com.zz.usercenter.model.domain.request.SaveUserDietDayTemplateRequest;
import com.zz.usercenter.model.domain.request.SaveUserDietTemplateRequest;
import com.zz.usercenter.model.domain.request.SaveUserTrainingCycleRequest;
import com.zz.usercenter.model.domain.request.SaveUserTrainingTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.ChatService;
import com.zz.usercenter.service.DietRecordService;
import com.zz.usercenter.service.ExerciseRecordService;
import com.zz.usercenter.service.ExerciseService;
import com.zz.usercenter.service.FileService;
import com.zz.usercenter.service.FoodItemService;
import com.zz.usercenter.service.ProfileExtractionService;
import com.zz.usercenter.service.UserDailyMetricService;
import com.zz.usercenter.service.UserDietCycleService;
import com.zz.usercenter.service.UserDietDayTemplateService;
import com.zz.usercenter.service.UserDietTemplateService;
import com.zz.usercenter.service.UserProfileService;
import com.zz.usercenter.service.UserRecordService;
import com.zz.usercenter.service.UserService;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import com.zz.usercenter.service.UserWeightRecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.invoke.CallSite;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ChatServiceImpl
implements ChatService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
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
    @Resource
    private ProfileExtractionService profileExtractionService;
    @Resource
    private AiCallHelper aiCallHelper;
    @Resource
    private FileService fileService;
    @Resource
    private WebSearchHelper webSearchHelper;
    @Resource
    private WeatherHelper weatherHelper;
    @Resource
    private UserWeightRecordService userWeightRecordService;
    @Resource
    private UserProfileService userProfileService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int RECENT_DIALOG_LIMIT = 10;
    private static final int QUICK_STREAM_CHUNK_SIZE = 2;
    private static final long QUICK_STREAM_DELAY_MS = 42L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern DIET_SINGLE_MEAL_PATTERN = Pattern.compile("(\u65e9\u9910|\u5348\u9910|\u5348\u996d|\u665a\u9910|\u665a\u996d|\u52a0\u9910|\u591c\u5bb5|\u5bb5\u591c|\u7ec3\u540e\u9910)");
    private static final List<String> PLAN_MEAL_ORDER = List.of("\u65e9\u9910", "\u7ec3\u540e\u9910", "\u5348\u9910", "\u52a0\u9910", "\u665a\u9910");
    private static final List<String> PLAN_MUSCLE_GROUP_ORDER = List.of("chest", "back", "shoulders", "arms", "legs", "core");
    private static final BigDecimal KJ_TO_KCAL_DIVISOR = new BigDecimal("4.184");
    private static final ThreadLocal<String> ACTIVE_CHAT_MODEL = new ThreadLocal();
    private static final ThreadLocal<Map<String, WebClient>> CACHED_WEB_CLIENTS = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<String> CACHED_TRAINING_PLAN_TEXT = new ThreadLocal();
    private static final ThreadLocal<List<Map<String, String>>> ACTIVE_CUSTOM_MODELS = new ThreadLocal();
    private volatile boolean clientDisconnected = false;
    private static final Map<String, Integer> WEEKDAY_MAP = Map.of("\u4e00", 1, "\u4e8c", 2, "\u4e09", 3, "\u56db", 4, "\u4e94", 5, "\u516d", 6, "\u65e5", 7, "\u5929", 7);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String INTENT_CLASSIFY_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624b\u7684\u610f\u56fe\u5206\u6790\u6a21\u5757\u3002\u5206\u6790\u7528\u6237\u6d88\u606f\uff0c\u4e25\u683c\u53ea\u8fd4\u56de\u4e00\u4e2aJSON\u5bf9\u8c61\uff0c\u4e0d\u8981\u8fd4\u56de\u4efb\u4f55\u5176\u4ed6\u6587\u5b57\u3002\n\n\u53ef\u7528\u610f\u56fe\uff1a\n1. save_diet: \u7528\u6237\u5728\u9648\u8ff0\"\u5df2\u7ecf\u5403\u4e86/\u559d\u4e86\u4ec0\u4e48\"\u6216\"\u8bf7\u5e2e\u6211\u8bb0\u5f55\u996e\u98df\"\u3002\u53c2\u6570: food(\u5fc5\u586b,\u4ec5\u4fdd\u7559\u98df\u7269\u5185\u5bb9), meal(\u9910\u6b21)\n2. save_exercise: \u7528\u6237\u5728\u9648\u8ff0\"\u5df2\u7ecf\u505a\u4e86\u4ec0\u4e48\u8fd0\u52a8\"\u6216\"\u8bf7\u5e2e\u6211\u8bb0\u5f55\u8bad\u7ec3\"\u3002\u53c2\u6570: exercises(\u5fc5\u586b,\u6570\u7ec4,\u6bcf\u4e2a\u5143\u7d20\u542bname(\u52a8\u4f5c\u540d,\u5fc5\u586b),sets(\u7ec4\u6570,\u53ef\u9009),duration(\u5206\u949f\u6570,\u53ef\u9009))\n3. save_weight: \u7528\u6237\u62a5\u4f53\u91cd\u6570\u5b57\u3002\u53c2\u6570: weight_kg(\u5fc5\u586b,\u6570\u5b57,\u65a4\u9700\u6362\u7b97\u6210kg), note(\u53ef\u9009\u5907\u6ce8)\n4. query_training: \u67e5\u8bad\u7ec3\u3002\"\u7ec3\u4ec0\u4e48/\u8be5\u7ec3\u4ec0\u4e48/\u8bad\u7ec3\u5b89\u6392\"=\u8ba1\u5212\u5b89\u6392\uff0c\"\u7ec3\u4e86\u4ec0\u4e48/\u8fd0\u52a8\u8bb0\u5f55\"=\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55\u3002\u53c2\u6570: query_type(\"\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55\"/\"\u8ba1\u5212\u5b89\u6392\",\u5fc5\u586b), target_day(\"\u4eca\u5929\"/\"\u660e\u5929\"/\"\u5168\u90e8\",\u9ed8\u8ba4\u4eca\u5929)\n5. query_diet: \u67e5\u996e\u98df\u3002\"\u5403\u4e86\u4ec0\u4e48/\u996e\u98df\u8bb0\u5f55\"=\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55\uff0c\"\u5403\u4ec0\u4e48/\u98df\u8c31\"=\u8ba1\u5212\u5b89\u6392\u3002\u53c2\u6570: query_type(\u5fc5\u586b), meal_type(\u53ef\u9009)\n6. query_weight: \u67e5\u4f53\u91cd\u3002\"\u6700\u8fd1\u4f53\u91cd/\u4f53\u91cd\u8d8b\u52bf\"\u7b49\u3002\u53c2\u6570: range(\"\u4eca\u5929\"/\"\u6700\u8fd17\u5929\"/\"\u6700\u8fd130\u5929\",\u9ed8\u8ba4\u6700\u8fd17\u5929)\n7. get_today_records: \u67e5\u4eca\u5929\u7efc\u5408\u8bb0\u5f55\u3002\u65e0\u53c2\u6570\n8. get_week_records: \u67e5\u672c\u5468\u8bb0\u5f55\u3002\u65e0\u53c2\u6570\n9. get_history_record: \u67e5\u67d0\u5929\u8bb0\u5f55\u3002\u53c2\u6570: date_description(\u5fc5\u586b)\n10. search_knowledge: \u5065\u8eab\u4e13\u4e1a\u95ee\u9898(\u8bad\u7ec3\u539f\u7406/\u8425\u517b/\u8865\u5242/\u5668\u68b0/\u4f24\u75c5/\u5eb7\u590d/\u51cf\u8102/\u589e\u808c\u7b49)\u3002\u53c2\u6570: query(\u5fc5\u586b)\n11. generate_training_plan: \u751f\u6210/\u8c03\u6574\u8bad\u7ec3\u8ba1\u5212\u3002\u53c2\u6570: user_request, adjust_existing(bool)\n12. generate_diet_plan: \u751f\u6210\u996e\u98df\u63a8\u8350/\u98df\u8c31\u3002\u53c2\u6570: user_request, meal_scope(\"\u5168\u5929\"/\"\u5355\u9910\")\n13. delete_record: \u7528\u6237\u60f3\u5220\u9664/\u64a4\u56de\u5df2\u8bb0\u5f55\u7684\u5185\u5bb9\u3002\u53c2\u6570: type(\"exercise\"/\"diet\",\u5fc5\u586b), target(\u5177\u4f53\u63cf\u8ff0\u5982\"\u6df1\u8e72\"/\"\u5348\u9910\",\u53ef\u9009,\u9ed8\u8ba4\u6700\u540e\u4e00\u6761)\n14. update_weight: \u7528\u6237\u5728\u4fee\u6b63\u4e4b\u524d\u62a5\u9519\u7684\u4f53\u91cd(\u8bed\u5883\u542b\"\u4e0d\u5bf9/\u9519\u4e86/\u6539\u6210/\u5e94\u8be5\u662f/\u4fee\u6539\")\u3002\u53c2\u6570: weight_kg(\u5fc5\u586b,\u6570\u5b57)\n15. other: \u7eaf\u95f2\u804a/\u6253\u62db\u547c/\u4e0d\u5339\u914d\u4ee5\u4e0a\u4efb\u4f55\u610f\u56fe\n16. query_weather: \u7528\u6237\u8be2\u95ee\u5929\u6c14\u60c5\u51b5\u3002\u53c2\u6570: query(\u53ef\u9009,\u5982\"\u660e\u5929\u5929\u6c14\"\"\u8fd9\u5468\u5929\u6c14\")\n17. set_daily_calories: \u7528\u6237\u60f3\u8bbe\u7f6e\u6bcf\u65e5\u6444\u5165\u70ed\u91cf\u76ee\u6807\u3002\u53c2\u6570: daily_calories(\u5fc5\u586b,\u6570\u5b57,\u5355\u4f4dkcal)\n18. set_target_weight: \u7528\u6237\u60f3\u8bbe\u7f6e\u76ee\u6807\u4f53\u91cd\u3002\u53c2\u6570: target_weight(\u5fc5\u586b,\u6570\u5b57,\u5355\u4f4dkg,\u65a4\u9700\u6362\u7b97\u6210kg)\n19. report_training_issue: \u7528\u6237\u53cd\u9988\u8bad\u7ec3\u4e2d\u7684\u8eab\u4f53\u4e0d\u9002\u6216\u56f0\u96be\u3002\u53c2\u6570: issue_description(\u5fc5\u586b,\u95ee\u9898\u63cf\u8ff0), affected_exercise(\u53ef\u9009,\u51fa\u95ee\u9898\u7684\u52a8\u4f5c)\n20. report_diet_issue: \u7528\u6237\u53cd\u9988\u996e\u98df\u4e0d\u8db3\u6216\u9965\u997f\u3002\u53c2\u6570: issue_description(\u5fc5\u586b,\u95ee\u9898\u63cf\u8ff0), meal_context(\u53ef\u9009,\u54ea\u4e00\u9910/\u4ec0\u4e48\u65f6\u5019)\n21. update_location: \u7528\u6237\u544a\u77e5\u81ea\u5df1\u6240\u5728\u7684\u57ce\u5e02/\u4f4d\u7f6e\u3002\u53c2\u6570: city(\u5fc5\u586b,\u57ce\u5e02\u540d,\u5982\"\u5e7f\u5dde\"\"\u6df1\u5733\"\"\u4e0a\u6d77\")\n\n\u5224\u65ad\u89c4\u5219\uff1a\n- \"\u6211\u5403\u4e86XX\"/\"\u8bb0\u5f55\u4e00\u4e0b\u5403\u4e86XX\" \u2192 save_diet\uff08\u63d0\u53d6\u98df\u7269\uff0c\u4e0d\u8981\u5e26\"\u6211\u5403\u4e86/\u5e2e\u6211\u8bb0\u5f55\"\u524d\u7f00\uff09\n- \"\u6211\u7ec3\u4e86XX\"/\"\u8bb0\u5f55\u4e00\u4e0b\u7ec3\u4e86XX\" \u2192 save_exercise\uff08\u62c6\u5206\u4e3aexercises\u6570\u7ec4\uff0c\u6bcf\u4e2a\u52a8\u4f5c\u4e00\u4e2a\u5143\u7d20\uff0c\u63d0\u53d6\u7ec4\u6570\u548c\u65f6\u957f\uff09\n- \"\u4f53\u91cd70kg\"/\"\u79f0\u4e86\u4e00\u4e0b65.5\"/\"\u4eca\u5929\u4f53\u91cd140\u65a4\" \u2192 save_weight\uff08\u65a4\u8981\u9664\u4ee52\u8f6ckg\uff09\n- \"\u4e0d\u5bf9\uff0c\u4f53\u91cd\u662f72\"/\"\u4f53\u91cd\u6539\u621072\"/\"\u5e94\u8be5\u662f73kg\u4e0d\u662f74\" \u2192 update_weight\uff08\u4fee\u6b63\u4f53\u91cd\uff0c\u65a4\u8981\u9664\u4ee52\u8f6ckg\uff09\n- \"\u628aXX\u5220\u4e86\"/\"\u64a4\u56deXX\"/\"\u5220\u9664\u8bb0\u5f55\"/\"\u6ca1\u5403\u90a3\u4e2a\" \u2192 delete_record\n- \"\u4eca\u5929\u7ec3\u4ec0\u4e48\"/\"\u8be5\u7ec3\u4ec0\u4e48\"/\"\u8bad\u7ec3\u5b89\u6392\" \u2192 query_training(query_type=\u8ba1\u5212\u5b89\u6392)\n- \"\u4eca\u5929\u7ec3\u4e86\u4ec0\u4e48\"/\"\u8fd0\u52a8\u8bb0\u5f55\" \u2192 query_training(query_type=\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55)\n- \"\u4eca\u5929\u5403\u4ec0\u4e48\"/\"\u98df\u8c31\u63a8\u8350\" \u2192 query_diet(query_type=\u8ba1\u5212\u5b89\u6392)\n- \"\u4eca\u5929\u5403\u4e86\u4ec0\u4e48\"/\"\u996e\u98df\u8bb0\u5f55\" \u2192 query_diet(query_type=\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55)\n- \"\u6700\u8fd1\u4f53\u91cd\"/\"\u4f53\u91cd\u8d8b\u52bf\"/\"\u7626\u4e86\u591a\u5c11\" \u2192 query_weight\n- \"\u4eca\u5929\u505a\u4e86\u4ec0\u4e48\"/\"\u6253\u5361\u603b\u7ed3\" \u2192 get_today_records\n- \"\u8fd9\u5468\u505a\u4e86\u4ec0\u4e48\"/\"\u672c\u5468\u603b\u7ed3\" \u2192 get_week_records\n- \"\u6628\u5929\u7ec3\u4e86\u4ec0\u4e48\"/\"\u4e0a\u5468\u4e09\u7684\u8bb0\u5f55\" \u2192 get_history_record\n- \"\u5e2e\u6211\u5236\u5b9a\u8bad\u7ec3\u8ba1\u5212\" \u2192 generate_training_plan\n- \"\u63a8\u8350\u4eca\u5929\u7684\u98df\u8c31\" \u2192 generate_diet_plan\n- \"\u7b4b\u819c\u67aa\u6709\u7528\u5417\"/\"\u86cb\u767d\u7c89\u600e\u4e48\u559d\"/\"\u6df1\u8e72\u819d\u76d6\u75db\" \u2192 search_knowledge\n- \"\u4eca\u5929\u5929\u6c14\"/\"\u660e\u5929\u4e0b\u4e0d\u4e0b\u96e8\"/\"\u8fd9\u5468\u5929\u6c14\u600e\u4e48\u6837\"/\"\u5916\u9762\u51b7\u4e0d\u51b7\" \u2192 query_weather\n- \"\u6bcf\u5929\u54032000\u5343\u5361\"/\"\u76ee\u6807\u70ed\u91cf1800\"/\"\u6bcf\u65e5\u6444\u5165\u8bbe\u4e3a2200kcal\"/\"\u6211\u60f3\u6bcf\u5929\u54031500\u5927\u5361\" \u2192 set_daily_calories\n- \"\u76ee\u6807\u4f53\u91cd65kg\"/\"\u60f3\u51cf\u5230140\u65a4\"/\"\u6211\u8981\u7ec3\u523070\u516c\u65a4\" \u2192 set_target_weight\n- \"\u6211\u5728\u5e7f\u5dde\"/\"\u6211\u5728\u6df1\u5733\"/\"\u5750\u6807\u4e0a\u6d77\"/\"\u6211\u5728\u6b66\u6c49\" \u2192 update_location\n- \"\u4f60\u597d\"/\"\u8c22\u8c22\"/\u95f2\u804a \u2192 other\n\n\u53c2\u6570\u89c4\u5219\uff1a\n- \u4e0d\u8981\u7f16\u9020\u514b\u91cd\u3001\u70ed\u91cf\u3001\u7ec4\u6570\u3001\u65f6\u957f\uff1b\u53ea\u62bd\u53d6\u7528\u6237\u660e\u786e\u8bf4\u8fc7\u7684\u5185\u5bb9\n- weight_kg \u7edf\u4e00\u4e3akg\u5355\u4f4d\uff08\u7528\u6237\u8bf4\u65a4\u8981\u9664\u4ee52\uff09\n- exercises\u6570\u7ec4\u4e2d\u5982\u679c\u7528\u6237\u53ea\u63d0\u4e86\u4e00\u4e2a\u52a8\u4f5c\u4e5f\u8981\u7528\u6570\u7ec4\u683c\u5f0f\n- \u4e0d\u8981\u8f93\u51fa\u989d\u5916\u6587\u5b57\uff0c\u53ea\u8fd4\u56deJSON\n\n\u793a\u4f8b\uff1a\n\"\u6211\u665a\u4e0a\u5403\u4e86\u4e24\u4e2a\u5168\u9ea6\u9762\u5305\u548c\u4e09\u4e2a\u9e21\u86cb\" \u2192 {\"intent\":\"save_diet\",\"food\":\"\u4e24\u4e2a\u5168\u9ea6\u9762\u5305\u548c\u4e09\u4e2a\u9e21\u86cb\",\"meal\":\"\u665a\u9910\"}\n\"\u5e2e\u6211\u8bb0\u5f55\u4e00\u4e0b\u6211\u7ec3\u4e86\u6df1\u8e725\u7ec4\" \u2192 {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"\u6df1\u8e72\",\"sets\":5}]}\n\"\u6211\u7ec3\u4e86\u6df1\u8e724\u7ec415\u5206\u949f\u8fd8\u6709\u5367\u63a83\u7ec410\u5206\u949f\" \u2192 {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"\u6df1\u8e72\",\"sets\":4,\"duration\":15},{\"name\":\"\u5367\u63a8\",\"sets\":3,\"duration\":10}]}\n\"\u8dd1\u6b6530\u5206\u949f\" \u2192 {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"\u8dd1\u6b65\",\"duration\":30}]}\n\"\u4eca\u5929\u4f53\u91cd70.5kg\" \u2192 {\"intent\":\"save_weight\",\"weight_kg\":70.5}\n\"\u79f0\u4e86\u4e00\u4e0b140\u65a4\" \u2192 {\"intent\":\"save_weight\",\"weight_kg\":70.0}\n\"\u4e0d\u5bf9\uff0c\u6211\u4f53\u91cd\u662f72\" \u2192 {\"intent\":\"update_weight\",\"weight_kg\":72.0}\n\"\u4f53\u91cd\u6539\u621072kg\" \u2192 {\"intent\":\"update_weight\",\"weight_kg\":72.0}\n\"\u628a\u521a\u624d\u7684\u6df1\u8e72\u5220\u4e86\" \u2192 {\"intent\":\"delete_record\",\"type\":\"exercise\",\"target\":\"\u6df1\u8e72\"}\n\"\u64a4\u56de\u5348\u9910\" \u2192 {\"intent\":\"delete_record\",\"type\":\"diet\",\"target\":\"\u5348\u9910\"}\n\"\u5220\u9664\u6700\u540e\u4e00\u6761\u8bad\u7ec3\u8bb0\u5f55\" \u2192 {\"intent\":\"delete_record\",\"type\":\"exercise\"}\n\"\u4eca\u5929\u8be5\u7ec3\u4ec0\u4e48\" \u2192 {\"intent\":\"query_training\",\"query_type\":\"\u8ba1\u5212\u5b89\u6392\",\"target_day\":\"\u4eca\u5929\",\"complete\":true}\n\"\u6700\u8fd1\u4f53\u91cd\u53d8\u5316\u5927\u5417\" \u2192 {\"intent\":\"query_weight\",\"range\":\"\u6700\u8fd17\u5929\",\"complete\":true}\n\"\u6df1\u8e72\u819d\u76d6\u75db\u600e\u4e48\u529e\" \u2192 {\"intent\":\"search_knowledge\",\"query\":\"\u6df1\u8e72\u819d\u76d6\u75db\u600e\u4e48\u529e\",\"complete\":true}\n\"\u4eca\u5929\u5929\u6c14\u600e\u4e48\u6837\" \u2192 {\"intent\":\"query_weather\",\"complete\":true}\n\"\u660e\u5929\u4e0b\u96e8\u5417\" \u2192 {\"intent\":\"query_weather\",\"query\":\"\u660e\u5929\u5929\u6c14\",\"complete\":true}\n\"\u6bcf\u5929\u54032000\u5343\u5361\" \u2192 {\"intent\":\"set_daily_calories\",\"daily_calories\":2000,\"complete\":true}\n\"\u76ee\u6807\u70ed\u91cf\u8bbe\u4e3a1800\" \u2192 {\"intent\":\"set_daily_calories\",\"daily_calories\":1800,\"complete\":true}\n\"\u6bcf\u65e5\u6444\u51651500\u5927\u5361\" \u2192 {\"intent\":\"set_daily_calories\",\"daily_calories\":1500,\"complete\":true}\n\"\u76ee\u6807\u4f53\u91cd65kg\" \u2192 {\"intent\":\"set_target_weight\",\"target_weight\":65.0,\"complete\":true}\n\"\u60f3\u51cf\u5230140\u65a4\" \u2192 {\"intent\":\"set_target_weight\",\"target_weight\":70.0,\"complete\":true}\n\"\u4f60\u597d\" \u2192 {\"intent\":\"other\",\"complete\":true}\n\"\u5e2e\u6211\u5236\u5b9a\u8ba1\u5212\" \u2192 {\"intent\":\"generate_training_plan\",\"user_request\":\"\u5e2e\u6211\u5236\u5b9a\u8ba1\u5212\",\"complete\":false,\"clarify\":\"\u597d\u7684\uff0c\u5e2e\u4f60\u5236\u5b9a\u8bad\u7ec3\u8ba1\u5212\uff01\u9700\u8981\u786e\u8ba4\uff1a\u5065\u8eab\u76ee\u6807\u662f\u4ec0\u4e48\uff1f\u6bcf\u5468\u80fd\u7ec3\u51e0\u5929\uff1f\"}\n\"\u5e2e\u6211\u5236\u5b9a\u51cf\u8102\u8ba1\u5212\u6bcf\u54684\u5929\" \u2192 {\"intent\":\"generate_training_plan\",\"user_request\":\"\u5236\u5b9a\u51cf\u8102\u8ba1\u5212\u6bcf\u54684\u5929\",\"complete\":true}\n\"\u6211\u665a\u4e0a\u5403\u4e86\u4e24\u4e2a\u5168\u9ea6\u9762\u5305\u548c\u4e09\u4e2a\u9e21\u86cb\" \u2192 {\"intent\":\"save_diet\",\"food\":\"\u4e24\u4e2a\u5168\u9ea6\u9762\u5305\u548c\u4e09\u4e2a\u9e21\u86cb\",\"meal\":\"\u665a\u9910\",\"complete\":true}\n\"\u8dd1\u6b6530\u5206\u949f\" \u2192 {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"\u8dd1\u6b65\",\"duration\":30}],\"complete\":true}\n\"\u6211\u5403\u4e86\" \u2192 {\"intent\":\"save_diet\",\"complete\":false,\"clarify\":\"\u5403\u4e86\u4ec0\u4e48\u98df\u7269\u5462\uff1f\"}\n\"\u6211\u7ec3\u4e86\" \u2192 {\"intent\":\"save_exercise\",\"complete\":false,\"clarify\":\"\u505a\u4e86\u4ec0\u4e48\u8fd0\u52a8\u5462\uff1f\"}\n\"\u6df1\u8e72\u819d\u76d6\u75db\u505a\u4e0d\u4e86\" \u2192 {\"intent\":\"report_training_issue\",\"issue_description\":\"\u6df1\u8e72\u65f6\u819d\u76d6\u75db\uff0c\u65e0\u6cd5\u5b8c\u6210\",\"affected_exercise\":\"\u6df1\u8e72\",\"complete\":true}\n\"\u5367\u63a8\u592a\u91cd\u4e86\u80a9\u8180\u625b\u4e0d\u4f4f\" \u2192 {\"intent\":\"report_training_issue\",\"issue_description\":\"\u5367\u63a8\u91cd\u91cf\u592a\u5927\uff0c\u80a9\u8180\u4e0d\u8212\u670d\",\"affected_exercise\":\"\u5367\u63a8\",\"complete\":true}\n\"\u5403\u4e0d\u9971\u8001\u662f\u997f\" \u2192 {\"intent\":\"report_diet_issue\",\"issue_description\":\"\u603b\u662f\u5403\u4e0d\u9971\uff0c\u611f\u89c9\u997f\",\"complete\":true}\n\"\u70ed\u91cf\u4e0d\u591f\u5934\u6655\" \u2192 {\"intent\":\"report_diet_issue\",\"issue_description\":\"\u6444\u5165\u70ed\u91cf\u4e0d\u8db3\uff0c\u51fa\u73b0\u5934\u6655\",\"complete\":true}\n\n\u8865\u5145\u8f93\u51fa\u5b57\u6bb5\uff1a\n- complete(bool): \u5fc5\u586b\u53c2\u6570\u662f\u5426\u9f50\u5168\u3002\n  save_weight/update_weight/query_*/delete_*/get_today_records/get_week_records/get_history_record/search_knowledge/set_daily_calories/set_target_weight \u65f6\u4e00\u5f8b\u4e3atrue\u3002\n  save_diet \u6709food\u65f6\u4e3atrue\uff0cfood\u4e3a\u7a7a\u6216\u7f3a\u5931\u65f6\u4e3afalse\uff0c\u8ffd\u95ee\"\u5403\u4e86\u4ec0\u4e48\uff1f\"\u3002\n  save_exercise \u6709exercises\u65f6\u4e3atrue\uff0cexercises\u4e3a\u7a7a\u6216\u7f3a\u5931\u65f6\u4e3afalse\uff0c\u8ffd\u95ee\"\u505a\u4e86\u4ec0\u4e48\u8fd0\u52a8\uff1f\"\u3002\n  generate_* \u6709user_request\u65f6\u4e3atrue\uff0c\u7f3auser_request\u65f6\u4e3afalse\u3002\n  report_training_issue/report_diet_issue \u6709issue_description\u65f6\u4e3atrue\u3002\n  other \u65f6\u4e3atrue\u3002\n- clarify(string): \u4ec5complete=false\u65f6\u586b\u5199\uff0c\u5411\u7528\u6237\u8ffd\u95ee\u7f3a\u5931\u7684\u4fe1\u606f\uff0c\u7b80\u6d01\u4e00\u53e5\u8bdd\uff0c\u4e0d\u8d85\u8fc750\u5b57\u3002complete=true\u65f6\u4e3anull\u3002\n";
    private static final String AUDIT_CHECK_PROMPT = "\u4f60\u662f\u4fe1\u606f\u5ba1\u8ba1\u6a21\u5757\u3002\u5bf9\u6bd4\u3010\u7528\u6237\u6d88\u606f\u3011\u548c\u3010\u5df2\u63d0\u53d6\u53c2\u6570\u3011\uff0c\u68c0\u67e5\u7528\u6237\u6d88\u606f\u4e2d\u662f\u5426\u8fd8\u6709\u88ab\u9057\u6f0f\u7684\u76f8\u5173\u4fe1\u606f\u3002\n\n\u3010\u7528\u6237\u6d88\u606f\u3011\n%s\n\n\u3010\u5df2\u63d0\u53d6\u53c2\u6570\u3011\n%s\n\n\u3010\u5f53\u524d\u610f\u56fe\u3011%s\n\u53ef\u63d0\u53d6\u53c2\u6570\u5217\u8868\uff1a%s\n\n\u53ea\u5173\u6ce8\u660e\u786e\u51fa\u73b0\u5728\u7528\u6237\u6d88\u606f\u4e2d\u7684\u4fe1\u606f\uff0c\u4e0d\u8981\u63a8\u65ad\u3002\n\u5982\u679c\u6709\u9057\u6f0f\uff0c\u8fd4\u56de {\"missed\":{\"field\":\"\u503c\"}}\n\u6ca1\u6709\u9057\u6f0f\u8fd4\u56de {\"missed\":null}\n\u53ea\u8fd4\u56deJSON\uff0c\u4e0d\u8981\u5176\u4ed6\u6587\u5b57\u3002\n";
    private static final Map<String, String> CITY_EN_MAP = Map.ofEntries(Map.entry("\u5317\u4eac", "Beijing"), Map.entry("\u4e0a\u6d77", "Shanghai"), Map.entry("\u5e7f\u5dde", "Guangzhou"), Map.entry("\u6df1\u5733", "Shenzhen"), Map.entry("\u6210\u90fd", "Chengdu"), Map.entry("\u676d\u5dde", "Hangzhou"), Map.entry("\u6b66\u6c49", "Wuhan"), Map.entry("\u5357\u4eac", "Nanjing"), Map.entry("\u91cd\u5e86", "Chongqing"), Map.entry("\u5929\u6d25", "Tianjin"), Map.entry("\u897f\u5b89", "Xi'an"), Map.entry("\u82cf\u5dde", "Suzhou"), Map.entry("\u957f\u6c99", "Changsha"), Map.entry("\u90d1\u5dde", "Zhengzhou"), Map.entry("\u4e1c\u839e", "Dongguan"), Map.entry("\u9752\u5c9b", "Qingdao"), Map.entry("\u6c88\u9633", "Shenyang"), Map.entry("\u5b81\u6ce2", "Ningbo"), Map.entry("\u6606\u660e", "Kunming"), Map.entry("\u5927\u8fde", "Dalian"), Map.entry("\u53a6\u95e8", "Xiamen"), Map.entry("\u798f\u5dde", "Fuzhou"), Map.entry("\u65e0\u9521", "Wuxi"), Map.entry("\u5408\u80a5", "Hefei"), Map.entry("\u54c8\u5c14\u6ee8", "Harbin"), Map.entry("\u6d4e\u5357", "Jinan"), Map.entry("\u4f5b\u5c71", "Foshan"), Map.entry("\u957f\u6625", "Changchun"), Map.entry("\u6e29\u5dde", "Wenzhou"), Map.entry("\u77f3\u5bb6\u5e84", "Shijiazhuang"), Map.entry("\u5357\u5b81", "Nanning"), Map.entry("\u8d35\u9633", "Guiyang"), Map.entry("\u5357\u660c", "Nanchang"), Map.entry("\u6d77\u53e3", "Haikou"), Map.entry("\u5170\u5dde", "Lanzhou"), Map.entry("\u592a\u539f", "Taiyuan"), Map.entry("\u94f6\u5ddd", "Yinchuan"), Map.entry("\u897f\u5b81", "Xining"), Map.entry("\u547c\u548c\u6d69\u7279", "Hohhot"), Map.entry("\u62c9\u8428", "Lhasa"), Map.entry("\u4e4c\u9c81\u6728\u9f50", "Urumqi"), Map.entry("\u73e0\u6d77", "Zhuhai"), Map.entry("\u4e2d\u5c71", "Zhongshan"), Map.entry("\u60e0\u5dde", "Huizhou"), Map.entry("\u6c5f\u95e8", "Jiangmen"), Map.entry("\u6c55\u5934", "Shantou"), Map.entry("\u5f90\u5dde", "Xuzhou"), Map.entry("\u5e38\u5dde", "Changzhou"), Map.entry("\u70df\u53f0", "Yantai"), Map.entry("\u6f33\u5dde", "Zhangzhou"), Map.entry("\u4fdd\u5b9a", "Baoding"), Map.entry("\u90af\u90f8", "Handan"), Map.entry("\u6d1b\u9633", "Luoyang"), Map.entry("\u5609\u5174", "Jiaxing"), Map.entry("\u7ecd\u5174", "Shaoxing"), Map.entry("\u53f0\u5dde", "Taizhou"), Map.entry("\u91d1\u534e", "Jinhua"), Map.entry("\u6f4d\u574a", "Weifang"), Map.entry("\u6dc4\u535a", "Zibo"), Map.entry("\u4e34\u6c82", "Linyi"), Map.entry("\u5a01\u6d77", "Weihai"), Map.entry("\u6d4e\u5b81", "Jining"), Map.entry("\u5fb7\u5dde", "Dezhou"), Map.entry("\u8944\u9633", "Xiangyang"), Map.entry("\u5b9c\u660c", "Yichang"), Map.entry("\u5cb3\u9633", "Yueyang"), Map.entry("\u682a\u6d32", "Zhuzhou"), Map.entry("\u8861\u9633", "Hengyang"), Map.entry("\u67f3\u5dde", "Liuzhou"), Map.entry("\u6842\u6797", "Guilin"), Map.entry("\u7ef5\u9633", "Mianyang"), Map.entry("\u5b9c\u5bbe", "Yibin"), Map.entry("\u5927\u7406", "Dali"), Map.entry("\u4e3d\u6c5f", "Lijiang"), Map.entry("\u4e09\u4e9a", "Sanya"), Map.entry("\u9999\u6e2f", "Hong Kong"), Map.entry("\u6fb3\u95e8", "Macau"), Map.entry("\u53f0\u5317", "Taipei"));
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal();
    private final ThreadLocal<OutputStream> outputStreamLocal = new ThreadLocal();
    private final ThreadLocal<StringBuilder> resultHolderLocal = new ThreadLocal();
    private static final Pattern WEEKDAY_PATTERN = Pattern.compile("(\u8fd9\u5468|\u672c\u5468|\u4e0a\u5468|\u4e0a\u661f\u671f|\u8fd9\u661f\u671f|\u661f\u671f)([\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929])");
    private static final List<TimePattern> TIME_PATTERNS = List.of(new TimePattern(Pattern.compile("\u6628\u5929|\u6628\u65e5|\u524d\u4e00\u5929"), m -> LocalDate.now(CN_ZONE).minusDays(1L)), new TimePattern(WEEKDAY_PATTERN, m -> {
        int v = WEEKDAY_MAP.get(m.group(2));
        int weekOffset = "\u4e0a\u5468".equals(m.group(1)) || "\u4e0a\u661f\u671f".equals(m.group(1)) ? 1 : 0;
        return LocalDate.now(CN_ZONE).minusWeeks(weekOffset).with(DayOfWeek.of(v));
    }), new TimePattern(Pattern.compile("(\\d{1,2})[\u6708.](\\d{1,2})[\u53f7\u65e5]?"), m -> {
        int month = Integer.parseInt(m.group(1));
        int day = Integer.parseInt(m.group(2));
        return LocalDate.of(LocalDate.now(CN_ZONE).getYear(), month, day);
    }));
    private static final String[] DAY_NAMES = new String[]{"\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d", "\u65e5"};
    private static final String TRAINING_PLAN_GEN_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u8bf7\u6839\u636e\u7528\u6237\u4fe1\u606f\u751f\u6210\u6216\u8c03\u6574\u4e00\u5468\u8bad\u7ec3\u8ba1\u5212\u3002\n\n\u89c4\u5219\uff1a\n1. \u8f93\u51fa\u5b8c\u65747\u5929\uff0c\u4ece\u661f\u671f\u4e00\u5230\u661f\u671f\u65e5\uff1b\u4f11\u606f\u65e5\u4e5f\u5199\u51fa\u6765\n2. \u9ed8\u8ba4\u8bad\u7ec3\u8282\u594f\u5fc5\u987b\u56fa\u5b9a\u4e3a\uff1a\u661f\u671f\u4e00\u8bad\u7ec3\u3001\u661f\u671f\u4e8c\u8bad\u7ec3\u3001\u661f\u671f\u4e09\u4f11\u606f\u3001\u661f\u671f\u56db\u8bad\u7ec3\u3001\u661f\u671f\u4e94\u8bad\u7ec3\u3001\u661f\u671f\u516d\u4f11\u606f\u3001\u661f\u671f\u65e5\u4f11\u606f\uff1b\u53ea\u6709\u5f53\u7528\u6237\u660e\u786e\u63d0\u51fa\u522b\u7684\u8282\u594f\uff08\u5982\u7ec3\u4e09\u4f11\u4e00\u3001\u6bcf\u5929\u90fd\u7ec3\u3001\u6bcf\u5468\u53ea\u7ec34\u5929\u3001\u5468\u672b\u4e5f\u7ec3\uff09\u65f6\uff0c\u624d\u5141\u8bb8\u8986\u76d6\u9ed8\u8ba4\u8282\u594f\n3. \u5148\u786e\u5b9a\u5f53\u5929\u8bad\u7ec3\u7684\u4e3b\u808c\u7fa4\uff0c\u518d\u4ece\u4e0b\u65b9\u8be5\u808c\u7fa4\u5bf9\u5e94\u7684\u201d\u8bad\u7ec3\u52a8\u4f5c\u201d\u91cc\u9009\u8bad\u7ec3\u52a8\u4f5c\uff1b\u4e25\u7981\u8de8\u808c\u7fa4\u4e71\u9009\n3.5. \u5e26\u6709[\u6536\u85cf]\u6807\u8bb0\u7684\u52a8\u4f5c\u662f\u7528\u6237\u6536\u85cf\u7684\uff0c\u4f18\u5148\u9009\u7528\u4f46\u4e5f\u517c\u987e\u52a8\u4f5c\u591a\u6837\u6027\uff0c\u4e0d\u8981\u5168\u90e8\u53ea\u9009\u6536\u85cf\u52a8\u4f5c\n4. \u70ed\u8eab\u5fc5\u987b\u4ece\u5f53\u5929\u8bad\u7ec3\u4e3b\u808c\u7fa4\u5bf9\u5e94\u7684\u201d\u70ed\u8eab\u52a8\u4f5c\u201d\u91cc\u9009\uff1b\u4e25\u7981\u628a\u5168\u8eab\u70ed\u8eab\u3001\u5176\u4ed6\u808c\u7fa4\u70ed\u8eab\u3001\u6b63\u5f0f\u8bad\u7ec3\u52a8\u4f5c\u62ff\u6765\u5145\u5f53\u70ed\u8eab\n5. \u5982\u679c\u67d0\u5929\u5199\u7684\u662f\u201d\u80cc\u90e8\u201d\uff0c\u90a3\u4e48\u70ed\u8eab\u548c\u8bad\u7ec3\u52a8\u4f5c\u90fd\u5fc5\u987b\u4f18\u5148\u6765\u81ea\u80cc\u90e8\u5bf9\u5e94\u5217\u8868\uff1b\u80f8\u90e8\u3001\u817f\u90e8\u3001\u80a9\u90e8\u3001\u624b\u81c2\u3001\u6838\u5fc3\u540c\u7406\n6. \u52a8\u4f5c\u540d\u5fc5\u987b\u9010\u5b57\u6765\u81ea\u4e0b\u65b9\u7ed9\u5b9a\u52a8\u4f5c\u5e93\uff0c\u4e0d\u80fd\u6539\u5199\u3001\u4e0d\u80fd\u81ea\u5df1\u53d1\u660e\u3001\u4e0d\u80fd\u62fc\u63a5\u5e93\u91cc\u4e0d\u5b58\u5728\u7684\u52a8\u4f5c\n6.5. \u4e25\u683c\u9075\u5b88\u7528\u6237\u5668\u68b0\u7ea6\u675f\uff1a\u5982\u679c\u7528\u6237\u753b\u50cf/\u504f\u597d\u4e2d\u660e\u786e\u8981\u6c42\u5f92\u624b\u3001\u65e0\u5668\u68b0\u3001\u53ea\u6709\u54d1\u94c3\u7b49\uff0c\u7edd\u4e0d\u80fd\u63a8\u8350\u4e0d\u7b26\u5408\u7ea6\u675f\u7684\u52a8\u4f5c\uff1b\u52a8\u4f5c\u5e93\u4e2d\u4e0d\u7b26\u5408\u7ea6\u675f\u7684\u89c6\u4e3a\u4e0d\u53ef\u7528\n7. \u6bcf\u5929\u5206\u6210\u4e09\u4e2a\u90e8\u5206\uff1a\u70ed\u8eab\u3001\u8bad\u7ec3\u3001\u62c9\u4f38\n8. \u6bcf\u5929\u8bad\u7ec3\u52a8\u4f5c 4-5 \u4e2a\uff0c\u4e0d\u5199\u7ec4\u6570\u6b21\u6570\uff0c\u4e0d\u5199\u89e3\u91ca\n9. \u540c\u808c\u7fa4\u5c3d\u91cf\u9519\u5f00\uff0c\u907f\u514d\u8fde\u7eed\u4e24\u5929\u91cd\u590d\u540c\u4e00\u4e3b\u808c\u7fa4\n10. \u5982\u679c\u6709\u3010\u7528\u6237\u5f53\u524d\u5df2\u6709\u7684\u8bad\u7ec3\u8ba1\u5212\u3011\uff0c\u4f18\u5148\u6309\u7528\u6237\u53cd\u9988\u505a\u5c40\u90e8\u8c03\u6574\uff1b\u6ca1\u6709\u5c31\u76f4\u63a5\u65b0\u751f\u6210\n11. \u5fc5\u987b\u4f7f\u7528 Markdown \u8868\u683c\u683c\u5f0f\u8f93\u51fa\uff0c\u4e25\u683c\u6309\u4e0b\u65b9\u793a\u4f8b\u683c\u5f0f\uff0c\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u975e\u8868\u683c\u5185\u5bb9\n12. \u4e25\u7981\u8f93\u51fa\u201d\u6ce8\uff1a\u201d\u201d\u8bf4\u660e\uff1a\u201d\u201d\u5faa\u73af\u5f00\u59cb\u201d\u201d\u53c2\u8003\u4e0a\u5468\u201d\u201d\u82e5\u9700\u8c03\u6574\u201d\u201d\u4ee5\u4e0a\u8ba1\u5212\u201d\u7b49\u9644\u52a0\u5185\u5bb9\n13. \u661f\u671f\u6807\u9898\u7528 ### \u4e09\u7ea7\u6807\u9898\uff0c\u8bad\u7ec3\u65e5\u7528 Markdown \u8868\u683c\u5c55\u793a\uff0c\u4f11\u606f\u65e5\u7528 > \u5f15\u7528\u683c\u5f0f\n14. \u661f\u671f\u6807\u9898\u53ea\u80fd\u5199\u6210\u201d\u661f\u671fX \u00b7 \u808c\u7fa4\u201d\u6216\u201d\u661f\u671fX \u00b7 \u4f11\u606f\u65e5\u201d\n15. \u8bad\u7ec3\u8868\u683c\u67094\u5217\uff1a| \u9636\u6bb5 | \u5185\u5bb9 |\n16. \u8bad\u7ec3\u884c\u53ea\u80fd\u5217\u52a8\u4f5c\uff0c\u4e0d\u8981\u8865\u62ec\u53f7\u89e3\u91ca\uff0c\u4e0d\u8981\u8865\u539f\u56e0\uff0c\u4e0d\u8981\u8865\u6062\u590d\u5efa\u8bae\n17. \u5728\u4f60\u6b63\u5f0f\u8f93\u51fa\u524d\uff0c\u5148\u9010\u5929\u81ea\u68c0\uff1a\u68c0\u67e5\u201d\u6807\u9898\u808c\u7fa4\u3001\u70ed\u8eab\u52a8\u4f5c\u3001\u8bad\u7ec3\u52a8\u4f5c\u201d\u4e09\u8005\u662f\u5426\u5c5e\u4e8e\u540c\u4e00\u808c\u7fa4\uff1b\u5982\u679c\u4e0d\u4e00\u81f4\uff0c\u8bf4\u660e\u7ed3\u679c\u4e0d\u5408\u683c\uff0c\u5fc5\u987b\u91cd\u6392\u540e\u518d\u8f93\u51fa\n18. \u5728\u4f60\u6b63\u5f0f\u8f93\u51fa\u524d\uff0c\u518d\u81ea\u68c0\u9ed8\u8ba4\u6a21\u5f0f\u7684\u661f\u671f\u5b89\u6392\u662f\u5426\u4e25\u683c\u7b49\u4e8e\uff1a\u4e00\u4e8c\u8bad\u7ec3\u3001\u4e09\u4f11\u606f\u3001\u56db\u4e94\u8bad\u7ec3\u3001\u516d\u65e5\u4f11\u606f\uff1b\u53ea\u8981\u4efb\u4f55\u4e00\u5929\u4e0d\u7b26\u5408\uff0c\u8bf4\u660e\u7ed3\u679c\u4e0d\u5408\u683c\uff0c\u5fc5\u987b\u91cd\u6392\u540e\u518d\u8f93\u51fa\n19. \u9ed8\u8ba4\u6a21\u5f0f\u4e0b\u4e25\u7981\u8f93\u51fa\u5468\u516d\u8bad\u7ec3\u3001\u5468\u65e5\u8bad\u7ec3\u3001\u8fde\u7eed3\u5929\u8bad\u7ec3\uff0c\u9664\u975e\u7528\u6237\u660e\u786e\u63d0\u51fa\u8fd9\u7c7b\u8981\u6c42\n20. \u6bcf\u5929\u4e4b\u95f4\u7528\u7a7a\u884c\u5206\u9694\uff0c\u9664\u8868\u683c\u548c\u6807\u9898\u5916\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u989d\u5916\u6587\u5b57\n\n{existingPlan}\n\n\u8f93\u51fa\u683c\u5f0f\uff08\u4e25\u683c\u9075\u5faa\uff09\uff1a\n### \u661f\u671f\u4e00 \u00b7 \u80f8\u90e8\n\n| \u9636\u6bb5 | \u5185\u5bb9 |\n|------|------|\n| \ud83d\udd25 \u70ed\u8eab | \u5f00\u5408\u8df3 |\n| \ud83d\udcaa \u8bad\u7ec3 | \u6760\u94c3\u5367\u63a8\u3001\u4e0a\u659c\u54d1\u94c3\u5367\u63a8\u3001\u8774\u8776\u673a\u5939\u80f8\u3001\u53cc\u6760\u81c2\u5c48\u4f38 |\n| \ud83e\uddd8 \u62c9\u4f38 | \u624b\u81c2\u4ea4\u53c9\u4f38\u5c55 |\n\n### \u661f\u671f\u4e8c \u00b7 \u80cc\u90e8\n\n| \u9636\u6bb5 | \u5185\u5bb9 |\n|------|------|\n| \ud83d\udd25 \u70ed\u8eab | \u9ad8\u62ac\u817f |\n| \ud83d\udcaa \u8bad\u7ec3 | \u5f15\u4f53\u5411\u4e0a\u3001\u6760\u94c3\u5212\u8239\u3001\u9ad8\u4f4d\u4e0b\u62c9\u3001\u5750\u59ff\u5212\u8239 |\n| \ud83e\uddd8 \u62c9\u4f38 | \u624b\u81c2\u73af\u7ed5 |\n\n### \u661f\u671f\u4e09 \u00b7 \u4f11\u606f\u65e5\n\n> \u4f11\u606f\uff0c\u53ef\u505a30\u5206\u949f\u4f4e\u5f3a\u5ea6\u6709\u6c27\uff08\u6563\u6b65/\u5feb\u8d70\uff09\n\n### \u661f\u671f\u56db \u00b7 \u817f\u90e8\n...\uff08\u4ee5\u6b64\u7c7b\u63a8\uff0c\u6bcf\u5929\u7528\u76f8\u540c\u683c\u5f0f\uff0c\u62c9\u4f38\u52a8\u4f5c\u4e5f\u5fc5\u987b\u4ece\u52a8\u4f5c\u5e93\u9009\uff09\n\n{userInfo}\n{exerciseCatalog}\n";
    private static final String DIET_PLAN_GEN_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u8bf7\u6839\u636e\u7528\u6237\u4fe1\u606f\u548c\u98df\u7269\u8425\u517b\u53c2\u8003\u751f\u6210\u996e\u98df\u63a8\u8350\u3002\n\n\u89c4\u5219\uff1a\n1. \u4f18\u5148\u4ece\u4e0b\u65b9\u3010\u98df\u7269\u8425\u517b\u53c2\u8003\u3011\u91cc\u9009\u98df\u7269\uff1b\u5e26[\u6211\u7684\u98df\u7269]\u6807\u8bb0\u7684\u662f\u7528\u6237\u81ea\u5df1\u4e0a\u4f20\u7684\uff0c\u4f18\u5148\u8003\u8651\u9009\u7528\uff0c\u4f46\u4e5f\u517c\u987e\u591a\u6837\u6027\n1.5. \u4e25\u683c\u9075\u5b88\u7528\u6237\u996e\u98df\u7ea6\u675f\uff1a\u5982\u679c\u7528\u6237\u753b\u50cf/\u504f\u597d\u4e2d\u660e\u786e\u8bf4\u660e\u4e0d\u4f1a\u70f9\u996a\u3001\u53ea\u80fd\u5403\u5373\u98df/\u5916\u5356\u7b49\uff0c\u4e25\u7981\u63a8\u8350\u9700\u8981\u590d\u6742\u70f9\u996a\u7684\u98df\u7269\uff1b\u98df\u7269\u5e93\u4e2d\u4e0d\u7b26\u5408\u7ea6\u675f\u7684\u89c6\u4e3a\u4e0d\u53ef\u7528\n2. \u5148\u5224\u65ad\u7528\u6237\u8981\u7684\u662f\u5168\u5929\u996e\u98df\u8fd8\u662f\u67d0\u4e00\u9910\n{mealRequestInstruction}\n3. \u6bcf\u79cd\u98df\u7269\u5fc5\u987b\u6807\u6ce8\u5177\u4f53\u514b\u6570\uff08\u5982\u201c\u9e21\u80f8\u8089150g\u201d\uff09\uff0c\u514b\u6570\u6839\u636e\u7528\u6237\u76ee\u6807\u70ed\u91cf\u5408\u7406\u5206\u914d\n4. \u6839\u636e\u7528\u6237\u7684\u76ee\u6807\u70ed\u91cf\u548c\u4e09\u5927\u5b8f\u91cf\u6bd4\u4f8b\uff08\u86cb\u767d\u8d2830%\u3001\u78b3\u6c3440%\u3001\u8102\u80aa30%\uff09\u8ba1\u7b97\u6bcf\u9910\u7528\u91cf\n5. \u6bcf\u9910\u7528 Markdown \u8868\u683c\u5c55\u793a\n6. \u5fc5\u987b\u4f7f\u7528 Markdown \u8868\u683c\u683c\u5f0f\u8f93\u51fa\uff0c\u4e25\u683c\u6309\u4e0b\u65b9\u793a\u4f8b\u683c\u5f0f\n7. \u4e25\u7981\u8f93\u51fa\u201c\u6ce8\uff1a\u201d\u201c\u8bf4\u660e\uff1a\u201d\u201c\u5efa\u8bae\uff1a\u201d\u201c\u53ef\u66ff\u6362\u201d\u201c\u53ef\u8c03\u6574\u201d\u201c\u5982\u679c\u6ca1\u6709\u201d\u4e4b\u7c7b\u9644\u52a0\u6587\u5b57\n8. \u9664\u8868\u683c\u548c\u6807\u9898\u53ca\u6c47\u603b\u5916\uff0c\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u5176\u4ed6\u6bb5\u843d\n\n\u8f93\u51fa\u683c\u5f0f\uff08\u4e25\u683c\u9075\u5faa\uff09\uff1a\n\n### \u4e00\u65e5\u98df\u8c31\u63a8\u8350\n\n| \u9910\u6b21 | \u63a8\u8350\u98df\u7269 |\n|------|----------|\n| \ud83c\udf05 \u65e9\u9910 | \u5373\u98df\u9e21\u80f8\u8089100g + \u6c34\u716e\u86cb2\u4e2a(\u7ea6100g) + \u65e0\u7cd6\u8c46\u6d46300ml |\n| \ud83c\udf1e \u5348\u9910 | \u53bb\u76ae\u9e21\u817f150g + \u7cd9\u7c73\u996d150g + \u6e05\u7092\u897f\u5170\u82b1200g |\n| \ud83c\udf19 \u665a\u9910 | \u4e09\u6587\u9c7c120g + \u7ea2\u85af200g + \u849c\u84c9\u83e0\u83dc200g |\n| \ud83c\udf4c \u52a0\u9910 | \u725b\u5976250ml + \u9999\u85491\u6839(\u7ea6120g) |\n\n### \u4eca\u65e5\u8425\u517b\u4f30\u7b97\n\n| \u8425\u517b\u7d20 | \u6444\u5165\u91cf | \u76ee\u6807\u91cf | \u8bc4\u4ef7 |\n|--------|--------|--------|------|\n| \u70ed\u91cf | \u7ea61850kcal | 2000kcal | \u5408\u7406 |\n| \u86cb\u767d\u8d28 | \u7ea6110g | 150g | \u504f\u4f4e\uff0c\u5efa\u8bae\u52a0\u9910\u8865\u5145 |\n| \u78b3\u6c34\u5316\u5408\u7269 | \u7ea6220g | 200g | \u5408\u7406 |\n| \u8102\u80aa | \u7ea655g | 67g | \u5408\u7406 |\n\n\u3010\u98df\u7269\u8425\u517b\u53c2\u8003\u3011\n{foodKnowledge}\n\n{userInfo}\n";
    private static final String REPORT_TRAINING_ISSUE_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u7528\u6237\u5728\u8bad\u7ec3\u4e2d\u9047\u5230\u4e86\u8eab\u4f53\u4e0d\u9002\u6216\u56f0\u96be\uff0c\u9700\u8981\u4f60\u5206\u6790\u95ee\u9898\u5e76\u751f\u6210\u6539\u8fdb\u540e\u7684\u8bad\u7ec3\u8ba1\u5212\u3002\n\n\u5206\u6790\u8981\u6c42\uff1a\n1. \u7ed3\u5408\u7528\u6237\u4f24\u75c5\u5386\u53f2\uff0c\u5224\u65ad\u53cd\u9988\u7684\u52a8\u4f5c\u662f\u5426\u9002\u5408\u8be5\u7528\u6237\n2. \u68c0\u67e5\u5f53\u524d\u8ba1\u5212\u4e2d\u8be5\u52a8\u4f5c\u7684\u91cd\u91cf/\u7ec4\u6570\u662f\u5426\u5408\u7406\uff08\u5bf9\u6bd4\u8bad\u7ec3\u6c34\u5e73\uff09\n3. \u5982\u679c\u6709\u4f24\u75c5\uff0c\u63a8\u8350\u66ff\u4ee3\u52a8\u4f5c\uff08\u4ece\u52a8\u4f5c\u5e93\u4e2d\u9009\u62e9\uff09\n\n\u8f93\u51fa\u683c\u5f0f\uff1a\n\u7b2c\u4e00\u90e8\u5206\uff1a2-4\u53e5\u8bdd\u7684\u5206\u6790\uff0c\u8bf4\u660e\u95ee\u9898\u539f\u56e0\u548c\u6539\u8fdb\u65b9\u5411\uff08\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\uff09\n\u7b2c\u4e8c\u90e8\u5206\uff1a\u5b8c\u6574\u7684\u6539\u8fdb\u540e7\u5929\u8bad\u7ec3\u8ba1\u5212\uff08\u5fc5\u987b\u7528\u4ee5\u4e0b\u6807\u51c6markdown\u683c\u5f0f\uff09\n\n### \u661f\u671f\u4e00 \u00b7 \u808c\u7fa4\n\u70ed\u8eab\uff1a\u5f00\u5408\u8df33\u7ec415\u4e2a\n\u6b63\u5f0f\u8bad\u7ec3\uff1a\u52a8\u4f5c\u540d\uff08X\u7ec4\uff0cX\u6b21\uff09\u3001\u52a8\u4f5c\u540d\uff08X\u7ec4\uff0cX\u6b21\uff09\n\u62c9\u4f38\uff1a\u52a8\u4f5c\u540d1\u5206\u949f\n\n### \u661f\u671f\u4e8c \u00b7 \u4f11\u606f\n\n\uff08\u4ee5\u6b64\u7c7b\u63a8\u8986\u76d67\u5929\uff0c\u4f11\u606f\u65e5\u4e5f\u8981\u5199\u660e\"\u4f11\u606f\"\uff0c\u62c9\u4f38\u52a8\u4f5c\u5fc5\u987b\u4ece\u52a8\u4f5c\u5e93\u9009\uff09\n\n\u6ce8\u610f\uff1a\n- \u5fc5\u987b\u4f7f\u7528\u6807\u51c6markdown\u683c\u5f0f\uff0c### \u6807\u9898\u683c\u5f0f\u4e3a\"### \u661f\u671fX \u00b7 \u808c\u7fa4\"\n- \u52a8\u4f5c\u540d\u79f0\u5fc5\u987b\u4ece\u4e0b\u65b9\u52a8\u4f5c\u5e93\u4e2d\u9009\uff0c\u4e0d\u8981\u81ea\u5df1\u7f16\u9020\n- \u8bad\u7ec3\u8ba1\u5212\u90e8\u5206\u524d\u9762\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u989d\u5916\u8bf4\u660e\u6587\u5b57\n";
    private static final String REPORT_DIET_ISSUE_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u7528\u6237\u53cd\u9988\u996e\u98df\u4e0d\u8db3\u6216\u9965\u997f\uff0c\u9700\u8981\u4f60\u5206\u6790\u70ed\u91cf\u6444\u5165\u60c5\u51b5\u5e76\u751f\u6210\u6539\u8fdb\u540e\u7684\u996e\u98df\u8ba1\u5212\u3002\n\n\u5206\u6790\u8981\u6c42\uff1a\n1. \u8ba1\u7b97\u70ed\u91cf\u7f3a\u53e3\uff1a\u7528\u6237\u76ee\u6807\u70ed\u91cf vs \u8fd1\u671f\u5b9e\u9645\u6444\u5165\n2. \u68c0\u67e5\u5b8f\u91cf\u8425\u517b\u7d20\u662f\u5426\u5408\u7406\uff08\u86cb\u767d\u8d28\u662f\u5426\u5145\u8db3\u3001\u78b3\u6c34\u662f\u5426\u592a\u4f4e\uff09\n3. \u7ed3\u5408\u8bad\u7ec3\u6d88\u8017\u5224\u65ad\u662f\u5426\u9700\u8981\u989d\u5916\u8865\u5145\n\n\u8f93\u51fa\u683c\u5f0f\uff1a\n\u7b2c\u4e00\u90e8\u5206\uff1a2-4\u53e5\u8bdd\u7684\u5206\u6790\uff0c\u8bf4\u660e\u70ed\u91cf\u7f3a\u53e3\u548c\u8425\u517b\u7d20\u95ee\u9898\uff08\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\uff09\n\u7b2c\u4e8c\u90e8\u5206\uff1a\u6539\u8fdb\u540e\u7684\u5168\u5929\u996e\u98df\u8ba1\u5212\uff08\u5fc5\u987b\u7528\u4ee5\u4e0b\u6807\u51c6markdown\u8868\u683c\u683c\u5f0f\uff09\n\n### \u6539\u8fdb\u98df\u8c31\u63a8\u8350\n\n| \u9910\u6b21 | \u63a8\u8350\u98df\u7269 |\n|------|----------|\n| \u65e9\u9910 | \u98df\u7269A 150g + \u98df\u7269B 200g |\n| \u5348\u9910 | \u98df\u7269C 150g + \u98df\u7269D 100g |\n| \u665a\u9910 | \u98df\u7269E 120g + \u98df\u7269F 200g |\n| \u52a0\u9910 | \u98df\u7269G 250ml |\n\n\u6ce8\u610f\uff1a\n- \u6bcf\u79cd\u98df\u7269\u5fc5\u987b\u6807\u6ce8\u5177\u4f53\u514b\u6570\n- \u98df\u7269\u540d\u79f0\u5fc5\u987b\u4ece\u4e0b\u65b9\u98df\u7269\u8425\u517b\u53c2\u8003\u4e2d\u9009\n- \u996e\u98df\u8ba1\u5212\u8868\u683c\u524d\u9762\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u989d\u5916\u8bf4\u660e\u6587\u5b57\n";
    private static final String GENERAL_SYSTEM_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u98ce\u683c\u7b80\u6d01\u4eb2\u5207\u3002\n\n\u56de\u590d\u89c4\u5219\uff1a\n1. \u5065\u8eab\u4e13\u4e1a\u95ee\u9898\uff08\u8bad\u7ec3\u3001\u996e\u98df\u3001\u8425\u517b\u3001\u51cf\u8102\u3001\u589e\u808c\u3001\u4f24\u75c5\u3001\u5eb7\u590d\u3001\u8865\u5242\u3001\u5668\u68b0\u7b49\uff09\n   \u2192 \u4f9d\u636e\u4e0a\u4e0b\u6587\u4e2d\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\u56de\u7b54\uff1b\u5982\u679c\u6ca1\u6709\u77e5\u8bc6\u5e93\u7ed3\u679c\uff0c\u53ea\u80fd\u56de\u590d\uff1a\u77e5\u8bc6\u5e93\u6682\u65e0\u8be5\u77e5\u8bc6\uff0c\u6211\u65e0\u6cd5\u56de\u7b54\n\n2. \u65e5\u5e38\u95f2\u804a\u3001\u95ee\u5019\u3001\u60c5\u611f\u503e\u8bc9\u3001\u901a\u7528\u8bdd\u9898\uff08\u5929\u6c14\u3001\u5fc3\u60c5\u3001\u7535\u5f71\u3001\u81ea\u6211\u4ecb\u7ecd\u7b49\uff09\n   \u2192 \u50cf\u670b\u53cb\u4e00\u6837\u81ea\u7531\u56de\u7b54\uff0c\u98ce\u683c\u8f7b\u677e\uff0c\u2264100\u5b57\n\n3. \u975e\u5065\u8eab\u7684\u4e13\u4e1a\u8bf7\u6c42\uff08\u5199\u4ee3\u7801\u3001\u7ffb\u8bd1\u3001\u505a\u9898\u3001\u5199\u4f5c\u7b49\uff09\n   \u2192 \u56de\u590d\uff1a\u6211\u4e3b\u8981\u64c5\u957f\u5065\u8eab\u65b9\u9762\u7684\u95ee\u9898\uff0c\u5173\u4e8e[\u7528\u6237\u63d0\u5230\u7684\u9886\u57df]\u5efa\u8bae\u54a8\u8be2\u4e13\u4e1a\u5de5\u5177\u54e6\u3002\u6709\u5065\u8eab\u95ee\u9898\u968f\u65f6\u95ee\u6211~\n\n\u60c5\u7eea\u611f\u77e5\u89c4\u5219\uff1a\n- \u5982\u679c\u4e0a\u4e0b\u6587\u4e2d\u7528\u6237\u60c5\u7eea\u72b6\u6001\u4e3a\"\u4f4e\u843d\"\uff0c\u56de\u590d\u65f6\u4e3b\u52a8\u5173\u5fc3\u548c\u9f13\u52b1\uff0c\u8bed\u6c14\u6e29\u6696\uff0c\u9002\u5f53\u63d0\u51fa\u653e\u677e\u5efa\u8bae\n- \u5982\u679c\u7528\u6237\u60c5\u7eea\u72b6\u6001\u4e3a\"\u79ef\u6781\"\uff0c\u7ed9\u4e88\u80af\u5b9a\u548c\u6fc0\u52b1\uff0c\u8bed\u6c14\u70ed\u60c5\n- \u5982\u679c\u6ca1\u6709\u60c5\u7eea\u6807\u6ce8\uff0c\u6b63\u5e38\u56de\u590d\u5373\u53ef\n\n\u56de\u590d\u683c\u5f0f\uff1a\n- \u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\n- \u7b2c\u4e00\u884c\u7528 emoji \u5f00\u5934\n";
    private static final String GENERAL_SYSTEM_PROMPT_WITH_KNOWLEDGE = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u98ce\u683c\u7b80\u6d01\u4eb2\u5207\u3002\u4e0a\u4e0b\u6587\u4e2d\u5df2\u63d0\u4f9b\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\uff0c\u8bf7\u4f9d\u636e\u5b83\u56de\u7b54\u5065\u8eab\u4e13\u4e1a\u95ee\u9898\u3002\n\n\u56de\u590d\u89c4\u5219\uff1a\n1. \u5065\u8eab\u4e13\u4e1a\u95ee\u9898 \u2192 \u4f9d\u636e\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\u56de\u7b54\uff0c\u4f18\u5148\u4f7f\u7528\u77e5\u8bc6\u5e93\u5185\u5bb9\n2. \u65e5\u5e38\u95f2\u804a \u2192 \u50cf\u670b\u53cb\u4e00\u6837\u81ea\u7531\u56de\u7b54\uff0c\u98ce\u683c\u8f7b\u677e\uff0c\u2264100\u5b57\n3. \u975e\u5065\u8eab\u4e13\u4e1a\u8bf7\u6c42 \u2192 \u56de\u590d\uff1a\u6211\u4e3b\u8981\u64c5\u957f\u5065\u8eab\u65b9\u9762\u7684\u95ee\u9898\uff0c\u5efa\u8bae\u54a8\u8be2\u4e13\u4e1a\u5de5\u5177\u54e6\n\n\u60c5\u7eea\u611f\u77e5\u89c4\u5219\uff1a\n- \u7528\u6237\u60c5\u7eea\"\u4f4e\u843d\"\u2192\u4e3b\u52a8\u5173\u5fc3\u9f13\u52b1\uff0c\u8bed\u6c14\u6e29\u6696\n- \u7528\u6237\u60c5\u7eea\"\u79ef\u6781\"\u2192\u7ed9\u4e88\u80af\u5b9a\u6fc0\u52b1\uff0c\u8bed\u6c14\u70ed\u60c5\n- \u6ca1\u6709\u60c5\u7eea\u6807\u6ce8 \u2192 \u6b63\u5e38\u56de\u590d\n\n\u56de\u590d\u683c\u5f0f\uff1a\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\uff0c\u7b2c\u4e00\u884c\u7528emoji\u5f00\u5934\n";
    private static final String RAG_SYSTEM_PROMPT = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u4f60\u73b0\u5728\u5904\u4e8e\u4e25\u683c RAG \u56de\u7b54\u6a21\u5f0f\uff0c\u53ea\u80fd\u4f9d\u636e\u4e0b\u65b9\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\u56de\u7b54\u3002\n\n\u3010\u786c\u6027\u89c4\u5219\u3011\n1. \u5148\u5224\u65ad\u77e5\u8bc6\u5e93\u6761\u76ee\u662f\u5426\u80fd\u76f4\u63a5\u56de\u7b54\u7528\u6237\u5f53\u524d\u95ee\u9898\u3002\n2. \u201c\u76f4\u63a5\u76f8\u5173\u201d\u5fc5\u987b\u6ee1\u8db3\uff1a\u77e5\u8bc6\u6761\u76ee\u91cc\u7684\u4e3b\u9898\u3001\u52a8\u4f5c/\u98df\u7269/\u8425\u517b/\u8bad\u7ec3\u76ee\u6807/\u4f24\u75c5\u573a\u666f\uff0c\u4e0e\u7528\u6237\u6838\u5fc3\u95ee\u9898\u4e00\u81f4\uff0c\u5e76\u4e14\u80fd\u652f\u6301\u4e3b\u8981\u7ed3\u8bba\u3002\n3. \u53ea\u662f\u540c\u5c5e\u5065\u8eab\u9886\u57df\u3001\u53ea\u63d0\u5230\u76f8\u8fd1\u808c\u7fa4\u3001\u53ea\u51fa\u73b0\u6cdb\u6cdb\u8bad\u7ec3\u539f\u5219\uff0c\u90fd\u4e0d\u7b97\u76f4\u63a5\u76f8\u5173\u3002\n4. \u5982\u679c\u6ca1\u6709\u76f4\u63a5\u76f8\u5173\u77e5\u8bc6\u6761\uff0c\u6216\u68c0\u7d22\u7ed3\u679c\u660e\u663e\u8dd1\u9898\uff0c\u5fc5\u987b\u53ea\u56de\u590d\uff1a\u77e5\u8bc6\u5e93\u6682\u65e0\u8be5\u77e5\u8bc6\uff0c\u6211\u65e0\u6cd5\u56de\u7b54\n5. \u5982\u679c\u77e5\u8bc6\u6761\u76f4\u63a5\u76f8\u5173\uff0c\u53ea\u80fd\u4f7f\u7528\u77e5\u8bc6\u5e93\u4e2d\u7684\u5185\u5bb9\u56de\u7b54\uff1b\u4e25\u7981\u8865\u5145\u5e38\u8bc6\u3001\u7ecf\u9a8c\u3001\u63a8\u6d4b\u3001\u5916\u90e8\u77e5\u8bc6\u6216\u81ea\u521b\u5efa\u8bae\u3002\n6. \u5982\u679c\u77e5\u8bc6\u6761\u53ea\u8986\u76d6\u4e86\u90e8\u5206\u95ee\u9898\uff0c\u53ea\u56de\u7b54\u8986\u76d6\u5230\u7684\u90e8\u5206\uff0c\u5e76\u8bf4\u660e\u201c\u77e5\u8bc6\u5e93\u53ea\u8986\u76d6\u5230\u4ee5\u4e0a\u5185\u5bb9\u201d\u3002\n7. \u4e0d\u8981\u56e0\u4e3a\u7528\u6237\u753b\u50cf\u3001\u4eca\u65e5\u8bb0\u5f55\u3001\u5386\u53f2\u4e0a\u4e0b\u6587\u800c\u6269\u5c55\u77e5\u8bc6\u5e93\u6ca1\u6709\u63d0\u4f9b\u7684\u4e13\u4e1a\u7ed3\u8bba\u3002\n8. \u7eaf\u6587\u672c\uff0c\u4e0d\u7528 markdown\uff0c\u4e0d\u8f93\u51fa\u5224\u65ad\u8fc7\u7a0b\u3002\n\n\u3010\u76f8\u5173\u6027\u793a\u4f8b\u3011\n- \u7528\u6237\u95ee\u201c\u80f8\u808c\u8bad\u7ec3\u600e\u4e48\u505a\u201d\uff0c\u77e5\u8bc6\u5e93\u6709\u201c\u80f8\u90e8\u8bad\u7ec3\u539f\u5219\u201d \u2192 \u76f4\u63a5\u76f8\u5173\uff0c\u53ef\u4ee5\u56de\u7b54\u3002\n- \u7528\u6237\u95ee\u201c\u7b4b\u819c\u67aa\u6709\u7528\u5417\u201d\uff0c\u77e5\u8bc6\u5e93\u53ea\u6709\u201c\u624b\u81c2\u8bad\u7ec3\u539f\u5219\u201d\u201c\u80f8\u90e8\u8bad\u7ec3\u539f\u5219\u201d \u2192 \u4e0d\u76f4\u63a5\u76f8\u5173\uff0c\u53ea\u56de\u590d\u56fa\u5b9a\u6682\u65e0\u5185\u5bb9\u3002\n- \u7528\u6237\u95ee\u201c\u86cb\u767d\u8d28\u600e\u4e48\u8865\u201d\uff0c\u77e5\u8bc6\u5e93\u6709\u201c\u589e\u808c\u671f\u86cb\u767d\u8d28\u6444\u5165\u91cf\u201d \u2192 \u76f4\u63a5\u76f8\u5173\uff0c\u53ef\u4ee5\u56de\u7b54\u3002\n- \u7528\u6237\u95ee\u201c\u819d\u76d6\u75db\u80fd\u4e0d\u80fd\u6df1\u8e72\u201d\uff0c\u77e5\u8bc6\u5e93\u53ea\u6709\u201c\u817f\u90e8\u8bad\u7ec3\u52a8\u4f5c\u5217\u8868\u201d \u2192 \u4e0d\u76f4\u63a5\u76f8\u5173\uff0c\u53ea\u56de\u590d\u56fa\u5b9a\u6682\u65e0\u5185\u5bb9\u3002\n\n\u3010\u77e5\u8bc6\u76f8\u5173\u65f6\u7684\u56de\u590d\u683c\u5f0f\u3011\n- \u7b2c\u4e00\u53e5\u76f4\u63a5\u56de\u7b54\u7ed3\u8bba\u3002\n- \u540e\u9762\u7528 2-4 \u70b9\u8bf4\u660e\u77e5\u8bc6\u5e93\u652f\u6301\u7684\u4f9d\u636e\u3002\n- \u6700\u540e\u4e00\u884c\u53ef\u4ee5\u7ed9 1 \u6761\u5b9e\u7528\u5efa\u8bae\uff0c\u4f46\u5fc5\u987b\u6765\u81ea\u77e5\u8bc6\u5e93\u5185\u5bb9\u3002\n- \u603b\u5b57\u6570\u4e0d\u8d85\u8fc7 260 \u5b57\u3002\n\n\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\n{knowledge}\n";
    private static final String FOOD_IDENTIFY_PROMPT = "\u4f60\u662f\u98df\u7269\u8bc6\u522b\u52a9\u624b\u3002\u8bc6\u522b\u56fe\u7247\u4e2d\u7684\u98df\u7269\u5e76\u9884\u4f30\u5206\u91cf\u3002\n\u4e25\u683c\u53ea\u8fd4\u56de\u4e00\u884c JSON\uff0c\u4e0d\u8981 markdown\u3001\u4e0d\u8981\u89e3\u91ca\u3001\u4e0d\u8981\u4ee3\u7801\u5757\u3002\n\u683c\u5f0f\uff1a{\"name\":\"\u98df\u7269\u540d\",\"nameEn\":\"English name\",\"grams\":\u9884\u4f30\u514b\u6570}\n\u89c4\u5219\uff1a\n1. name\uff1a\u53ea\u5199\u4e2d\u6587\u98df\u7269\u540d\u79f0\uff0c\u591a\u79cd\u98df\u7269\u7528\u987f\u53f7\u5206\u9694\uff0c\u5982\"\u9e21\u80f8\u8089\u3001\u897f\u5170\u82b1\u3001\u7c73\u996d\"\n2. nameEn\uff1a\u5bf9\u5e94\u82f1\u6587\u540d\u79f0\uff0c\u591a\u79cd\u98df\u7269\u7528\u9017\u53f7\u5206\u9694\uff0c\u5982\"chicken breast, broccoli, cooked rice\"\u3002\u5982\u679c\u662f\u8425\u517b\u6807\u7b7e\u56fe\u7247\u65e0\u6cd5\u5224\u65ad\u82f1\u6587\uff0c\u586b null\n3. grams \u53ea\u586b\u4e00\u4e2a\u7eaf\u6570\u5b57\uff08\u6574\u6570\uff09\uff0c\u4ee3\u8868\u56fe\u7247\u4e2d\u98df\u7269\u7684\u603b\u514b\u6570\uff0c\u7981\u6b62\u586b\u5199\u4efb\u4f55\u6587\u5b57\u8bf4\u660e\u3002\n   \u4e25\u683c\u6839\u636e\u56fe\u7247\u4e2d\u98df\u7269\u7684\u89c6\u89c9\u5927\u5c0f\u5224\u65ad\uff0c\u7981\u6b62\u968f\u610f\u7f16\u9020\u6570\u503c\u3002\n   \u8bf7\u5bf9\u7167\u5e38\u89c1\u5b9e\u7269\u53c2\u8003\uff1a\n   - \u4e00\u4e2a\u666e\u901a\u6c49\u5821\u7ea6200g\n   - \u4e00\u7897\u7c73\u996d\u7ea6150g\u3001\u4e00\u76d8\u7c73\u996d\u7ea6250g\n   - \u4e00\u4e2a\u9e21\u86cb\u7ea650g\n   - \u4e00\u5757\u9e21\u80f8\u8089\u7ea6150g\n   - \u4e00\u5757\u725b\u6392\u7ea6200g\n   - \u4e00\u4efd\u6c99\u62c9\u7ea6150g\n   - \u4e00\u4e2a\u82f9\u679c\u7ea6200g\u3001\u4e00\u6839\u9999\u8549\u7ea6120g\n   - \u4e00\u676f\u725b\u5976\u7ea6250ml\n   - \u4e00\u4e2a\u9992\u5934\u7ea6100g\u3001\u4e00\u7897\u9762\u6761\u7ea6250g\n   - \u4e00\u5757\u62ab\u8428(\u6807\u51c6\u5207\u7247)\u7ea6100g\n   - \u4ec5\u4f9b\u53c2\u8003\uff0c\u5fc5\u987b\u6839\u636e\u56fe\u7247\u4e2d\u98df\u7269\u7684\u5b9e\u9645\u5927\u5c0f\u6bd4\u4f8b\u8fdb\u884c\u8c03\u6574\n4. \u5982\u679c\u56fe\u7247\u4e2d\u65e0\u6cd5\u5224\u65ad\u5206\u91cf\u5927\u5c0f\uff0cgrams \u586b null\n5. \u5982\u679c\u56fe\u7247\u662f\u8425\u517b\u6210\u5206\u8868\u6807\u7b7e\uff0c\u4ece\u6807\u7b7e\u4e0a\u8bfb\u53d6\u98df\u7269\u540d\u79f0\uff0cgrams \u586b null\n";

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void sendMessageStream(Long userId, String message, OutputStream outputStream, StringBuilder resultHolder, AtomicReference<String> replyTypeHolder) {
        long t0 = System.currentTimeMillis();
        this.clientDisconnected = false;
        this.trySaveUserCity(userId);
        try {
            String fullResponse;
            this.securityCheck(message);
            String quickReply = this.quickMatch(message);
            if (quickReply != null) {
                log.info("\u3010\u6d41\u7a0b\u3011quickMatch\u547d\u4e2d, \u8017\u65f6{}ms", (Object)(System.currentTimeMillis() - t0));
                this.writeSseDataGradually(outputStream, quickReply);
                resultHolder.append(quickReply);
                return;
            }
            User user = (User)this.userService.getById(userId);
            UserProfile userProfile = this.userProfileService.getByUserId(userId);
            ACTIVE_CHAT_MODEL.set(this.resolveUserModelName(user));
            ACTIVE_CUSTOM_MODELS.set(this.loadCustomModelsFromRedis(userId));
            ChatHistory history = this.getChatHistory(userId);
            UserRecord userRecord = this.userRecordService.getByUserId(userId);
            long t1 = System.currentTimeMillis();
            log.info("\u3010\u6d41\u7a0b\u3011\u521d\u59cb\u5316\u5b8c\u6210(DB+Redis), \u8017\u65f6{}ms", (Object)(t1 - t0));
            CompletableFuture<String> purificationFuture = this.profileExtractionService.purifyDirtyText(userId);
            DirectRouteResult direct = this.directRoute(message, userId, user, userRecord);
            if (direct != null) {
                log.info("\u3010\u6d41\u7a0b\u3011directRoute\u547d\u4e2d, \u8017\u65f6{}ms", (Object)(System.currentTimeMillis() - t0));
                String summarized = this.streamSummarizeWithToolData(userId, user, message, direct.toolData(), direct.systemPrompt(), outputStream, resultHolder);
                if (replyTypeHolder != null) {
                    replyTypeHolder.set(direct.replyType());
                }
                if (!summarized.isBlank()) {
                    this.saveOrUpdateChatHistory(userId, message, summarized);
                }
                return;
            }
            String pendingContext = this.getPendingIntent(userId);
            try {
                purificationFuture.get(500L, TimeUnit.MILLISECONDS);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                this.writeSseStatus(outputStream, "\u6b63\u5728\u5206\u6790\u610f\u56fe...");
            }
            catch (Exception exception) {
                // empty catch block
            }
            long t2 = System.currentTimeMillis();
            ClassifyResult classifyResult = this.classifyAndAudit(userId, message, user, history, pendingContext, userProfile);
            long t3 = System.currentTimeMillis();
            log.info("\u3010\u6d41\u7a0b\u3011classifyAndAudit\u8017\u65f6{}ms, \u7ed3\u679c={}", (Object)(t3 - t2), (Object)classifyResult);
            if (classifyResult == null) {
                String fullResponse2;
                log.warn("\u3010\u6d41\u7a0b\u3011classifyAndAudit\u8fd4\u56denull, \u8d70\u95f2\u804a\u515c\u5e95");
                try {
                    this.writeSseStatus(outputStream, "\u6b63\u5728\u56de\u590d...");
                }
                catch (Exception exception) {
                    // empty catch block
                }
                String purifiedText = this.profileExtractionService.getPurifiedText(userId);
                ToolCallResult fallbackResult = this.handleGeneralChatFallback(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder);
                if (replyTypeHolder != null) {
                    replyTypeHolder.set(fallbackResult.replyType());
                }
                if (!(fullResponse2 = resultHolder.toString()).isBlank()) {
                    this.saveOrUpdateChatHistory(userId, message, fullResponse2);
                }
                return;
            }
            String intent = classifyResult.intent();
            Map<String, Object> params = classifyResult.params();
            boolean complete = classifyResult.complete();
            String clarify = classifyResult.clarify();
            if (!complete && clarify != null && !clarify.isBlank()) {
                log.info("\u3010\u6d41\u7a0b\u3011\u8ffd\u95ee\u5206\u652f: intent={}, clarify={}", (Object)intent, (Object)clarify);
                this.writeSseDataGradually(outputStream, clarify);
                resultHolder.append(clarify);
                this.savePendingIntent(userId, intent, params, clarify);
                if (replyTypeHolder != null) {
                    replyTypeHolder.set("general_chat");
                }
                return;
            }
            this.clearPendingIntent(userId);
            if ("other".equals(intent)) {
                String fullResponse3;
                log.info("\u3010\u6d41\u7a0b\u3011\u95f2\u804a\u5206\u652f(other), \u8017\u65f6{}ms", (Object)(System.currentTimeMillis() - t0));
                try {
                    this.writeSseStatus(outputStream, "\u6b63\u5728\u56de\u590d...");
                }
                catch (Exception exception) {
                    // empty catch block
                }
                String purifiedText = this.profileExtractionService.getPurifiedText(userId);
                ToolCallResult fallbackResult = this.handleGeneralChatFallback(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder);
                if (replyTypeHolder != null) {
                    replyTypeHolder.set(fallbackResult.replyType());
                }
                if (!(fullResponse3 = resultHolder.toString()).isBlank()) {
                    this.saveOrUpdateChatHistory(userId, message, fullResponse3);
                }
                return;
            }
            long t4 = System.currentTimeMillis();
            params = this.auditCheck(intent, params, message, user);
            long t5 = System.currentTimeMillis();
            log.info("\u3010\u6d41\u7a0b\u3011auditCheck\u8017\u65f6{}ms", (Object)(t5 - t4));
            params = this.autoFillFromDB(intent, params, userId);
            String purifiedText = this.profileExtractionService.getPurifiedText(userId);
            this.outputStreamLocal.set(outputStream);
            this.resultHolderLocal.set(resultHolder);
            try {
                String statusHint = intent.startsWith("generate_") ? "\u6b63\u5728\u751f\u6210\u8ba1\u5212..." : (intent.startsWith("query_") || intent.startsWith("get_") || intent.equals("search_knowledge") ? "\u6b63\u5728\u67e5\u8be2\u6570\u636e..." : "\u6b63\u5728\u5904\u7406...");
                try {
                    this.writeSseStatus(outputStream, statusHint);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                long t6 = System.currentTimeMillis();
                ToolCallResult result = this.dispatchIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, intent, params);
                long t7 = System.currentTimeMillis();
                log.info("\u3010\u6d41\u7a0b\u3011dispatchIntent({})\u8017\u65f6{}ms, result={}", new Object[]{intent, t7 - t6, result});
                if (result != null) {
                    String fullResponse4;
                    if (replyTypeHolder != null) {
                        replyTypeHolder.set(result.replyType());
                    }
                    if (!(fullResponse4 = resultHolder.toString()).isBlank()) {
                        this.saveOrUpdateChatHistory(userId, message, fullResponse4);
                    }
                    log.info("\u3010\u6d41\u7a0b\u3011\u603b\u8017\u65f6{}ms, replyType={}", (Object)(System.currentTimeMillis() - t0), (Object)result.replyType());
                    return;
                }
            }
            finally {
                this.outputStreamLocal.remove();
                this.resultHolderLocal.remove();
            }
            log.warn("\u3010\u6d41\u7a0b\u3011dispatchIntent\u8fd4\u56denull, \u8d70\u95f2\u804a\u515c\u5e95, intent={}", (Object)intent);
            try {
                this.writeSseStatus(outputStream, "\u6b63\u5728\u56de\u590d...");
            }
            catch (Exception statusHint) {
                // empty catch block
            }
            ToolCallResult fallbackResult = this.handleGeneralChatFallback(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder);
            if (replyTypeHolder != null) {
                replyTypeHolder.set(fallbackResult.replyType());
            }
            if (!(fullResponse = resultHolder.toString()).isBlank()) {
                this.saveOrUpdateChatHistory(userId, message, fullResponse);
            }
            log.info("\u3010\u6d41\u7a0b\u3011\u603b\u8017\u65f6{}ms, replyType=general_chat(fallback)", (Object)(System.currentTimeMillis() - t0));
        }
        catch (Exception e) {
            log.error("SSE\u6d41\u5f0f\u5bf9\u8bdd\u5f02\u5e38", (Throwable)e);
            try {
                this.writeSseDataGradually(outputStream, "\u4e0d\u597d\u610f\u601d\uff0cAI \u8c03\u7528\u5931\u8d25\uff0c\u8bf7\u8054\u7cfb\u5de5\u4f5c\u4eba\u5458\u6216\u7a0d\u7b49\u7247\u523b\u518d\u8bd5\u3002");
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        finally {
            ACTIVE_CHAT_MODEL.remove();
            CACHED_WEB_CLIENTS.remove();
            CACHED_TRAINING_PLAN_TEXT.remove();
            ACTIVE_CUSTOM_MODELS.remove();
        }
    }

    private ToolCallResult dispatchIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, String intent, Map<String, Object> params) {
        log.info("\u3010\u610f\u56fe\u5206\u53d1\u3011userId={}, intent={}", (Object)userId, (Object)intent);
        return switch (intent) {
            case "save_diet" -> this.handleSaveDietIntent(userId, userRecord, params);
            case "save_exercise" -> this.handleSaveExerciseIntent(userId, userRecord, params);
            case "save_weight" -> this.handleSaveWeightIntent(userId, params, false);
            case "query_training" -> this.handleQueryTrainingIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, params);
            case "query_diet" -> this.handleQueryDietIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, params);
            case "query_weight" -> this.handleQueryWeightIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, params);
            case "get_today_records" -> this.handleQueryDataIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, "get_today_records", params, "general_chat", "");
            case "get_week_records" -> this.handleQueryDataIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, "get_week_records", params, "general_chat", "");
            case "get_history_record" -> this.handleQueryDataIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, "get_history_record", params, "general_chat", "");
            case "search_knowledge" -> this.handleSearchKnowledgeIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, params);
            case "generate_training_plan" -> this.handleGenerateIntent(userId, message, user, history, userRecord, outputStream, resultHolder, "training_plan", params);
            case "generate_diet_plan" -> this.handleGenerateIntent(userId, message, user, history, userRecord, outputStream, resultHolder, "diet_plan", params);
            case "delete_record" -> this.handleDeleteRecordIntent(userId, userRecord, params);
            case "update_weight" -> this.handleUpdateWeightIntent(userId, params);
            case "set_daily_calories" -> this.handleSetDailyCaloriesIntent(userId, params);
            case "set_target_weight" -> this.handleSetTargetWeightIntent(userId, params);
            case "query_weather" -> this.handleQueryWeatherIntent(user, message, outputStream);
            case "report_training_issue" -> this.handleReportTrainingIssueIntent(userId, message, user, history, userRecord, outputStream, resultHolder, params);
            case "report_diet_issue" -> this.handleReportDietIssueIntent(userId, message, user, history, userRecord, outputStream, resultHolder, params);
            case "update_location" -> this.handleUpdateLocationIntent(userId, params);
            default -> {
                log.warn("\u3010\u610f\u56fe\u5206\u53d1\u3011\u672a\u77e5intent: {}", (Object)intent);
                yield null;
            }
        };
    }

    private void writeSseData(OutputStream outputStream, String content) {
        try {
            String escaped = content.replace("\n", "\\n");
            outputStream.write(("data:" + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
        catch (Exception e) {
            this.clientDisconnected = true;
        }
    }

    private void writeSseStatus(OutputStream outputStream, String status) {
        try {
            String payload = String.format("{\"status\":\"%s\"}", status);
            outputStream.write(("event:status\ndata:" + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
        catch (Exception e) {
            this.clientDisconnected = true;
        }
    }

    private void writeSseDataGradually(OutputStream outputStream, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        for (int i = 0; i < content.length() && !this.clientDisconnected; i += 2) {
            int end = Math.min(content.length(), i + 2);
            this.writeSseData(outputStream, content.substring(i, end));
            if (end >= content.length()) continue;
            try {
                Thread.sleep(42L);
                continue;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public ChatHistory getChatHistory(Long userId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq((Object)"userId", (Object)userId);
        return (ChatHistory)this.chatHistoryMapper.selectOne((Wrapper)queryWrapper);
    }

    private String quickMatch(String message) {
        String msg = message.toLowerCase();
        if (msg.length() <= 6 && Pattern.compile(".*(\u4f60\u597d|hi|hello|\u55e8|hey|\u5728\u5417).*").matcher(msg).matches()) {
            return "\u4f60\u597d\uff01\u6211\u662f\u4f60\u7684\u667a\u80fd\u5065\u8eab\u52a9\u624b Tatan\uff0c\u6709\u4ec0\u4e48\u53ef\u4ee5\u5e2e\u4f60\u7684\u5417\uff1f";
        }
        if (Pattern.compile(".*(\u4f60\u662f\u8c01|\u4f60\u53eb\u4ec0\u4e48|\u4ecb\u7ecd.*\u81ea\u5df1|\u4f60\u80fd\u505a\u4ec0\u4e48).*").matcher(msg).matches()) {
            return "\u6211\u662f Tatan \u667a\u80fd\u5065\u8eab\u52a9\u624b\uff0c\u53ef\u4ee5\u5e2e\u4f60\uff1a\n1. \u56de\u7b54\u5065\u8eab\u76f8\u5173\u95ee\u9898\n2. \u5236\u5b9a\u4e2a\u6027\u5316\u8bad\u7ec3\u8ba1\u5212\n3. \u63d0\u4f9b\u996e\u98df\u5efa\u8bae\n4. \u804a\u804a\u5065\u8eab\u5fc3\u5f97\uff0c\u7ed9\u4f60\u6253\u6c14\u52a0\u6cb9\uff01";
        }
        if (msg.length() <= 6 && Pattern.compile(".*(\u8c22\u8c22|\u611f\u8c22|\u591a\u8c22|thanks).*").matcher(msg).matches()) {
            return "\u4e0d\u5ba2\u6c14\uff01\u6709\u4efb\u4f55\u5065\u8eab\u95ee\u9898\u968f\u65f6\u95ee\u6211\uff0c\u6211\u4f1a\u4e00\u76f4\u5728\u8fd9\u91cc\u966a\u4f60\u3002";
        }
        return null;
    }

    private DirectRouteResult directRoute(String message, Long userId, User user, UserRecord userRecord) {
        String msg = message.toLowerCase();
        if (msg.equals("\u4eca\u5929\u505a\u4e86\u4ec0\u4e48") || msg.equals("\u4eca\u5929\u8bb0\u5f55\u4e86\u4ec0\u4e48") || msg.equals("\u6253\u5361\u603b\u7ed3")) {
            String data = this.buildTodayRecordReply(user, userRecord);
            String toolData = data != null && !data.isBlank() ? data : "\u6682\u65e0\u8bb0\u5f55";
            return new DirectRouteResult("today_exercise_record", toolData, "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u5de5\u5177\u8fd4\u56de\u7684\u4eca\u65e5\u8bb0\u5f55\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5448\u73b0\u6240\u6709\u8bad\u7ec3\u548c\u996e\u98df\u6570\u636e\uff0c\u4e0d\u80fd\u7701\u7565\u4efb\u4f55\u8bb0\u5f55\u3002");
        }
        if (msg.equals("\u8fd9\u5468\u505a\u4e86\u4ec0\u4e48") || msg.equals("\u672c\u5468\u603b\u7ed3") || msg.equals("\u8fd9\u5468\u603b\u7ed3")) {
            String data = this.buildWeekRecordReply(user, userRecord);
            String toolData = data != null && !data.isBlank() ? data : "\u6682\u65e0\u8bb0\u5f55";
            return new DirectRouteResult("general_chat", toolData, "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u5de5\u5177\u8fd4\u56de\u7684\u672c\u5468\u8bb0\u5f55\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5448\u73b0\u6240\u6709\u6570\u636e\uff0c\u4e0d\u80fd\u7701\u7565\u4efb\u4f55\u4e00\u5929\u7684\u8bb0\u5f55\u3002");
        }
        return null;
    }

    private String streamSummarizeWithToolData(Long userId, User user, String userMessage, String toolData, String systemPrompt, OutputStream outputStream, StringBuilder resultHolder) {
        try {
            AiModelConfig.ModelProvider provider = this.requireProvider(this.resolvePurificationModel(user));
            String userContent = "\u7528\u6237\u95ee\uff1a" + userMessage + "\n\n\u5de5\u5177\u8fd4\u56de\u6570\u636e\uff1a\n" + toolData;
            String summarized = this.callAiApiStreamWithProvider(systemPrompt, userContent, outputStream, provider, 1024);
            resultHolder.append(summarized);
            return summarized;
        }
        catch (Exception e) {
            log.error("streamSummarizeWithToolData\u5f02\u5e38", (Throwable)e);
            String fallback = "\u6570\u636e\u83b7\u53d6\u6210\u529f\uff0c\u4f46AI\u603b\u7ed3\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002";
            try {
                this.writeSseDataGradually(outputStream, fallback);
            }
            catch (Exception exception) {
                // empty catch block
            }
            resultHolder.append(fallback);
            return fallback;
        }
    }

    private ClassifyResult classifyAndAudit(Long userId, String message, User user, ChatHistory history, String pendingContext, UserProfile userProfile) {
        try {
            Map intentMap;
            String content;
            boolean hasRecentMessages;
            AiModelConfig.ModelProvider provider = this.requireProvider(this.resolveChatModel(user));
            WebClient webClient = this.getWebClient(provider);
            StringBuilder userMsg = new StringBuilder();
            String summary = history != null ? history.getSummary() : null;
            List<String> recentMessages = history != null ? this.parseJsonArray(history.getPendingMessages()) : null;
            boolean hasSummary = summary != null && !summary.isBlank();
            boolean bl = hasRecentMessages = recentMessages != null && !recentMessages.isEmpty();
            if (hasSummary || hasRecentMessages) {
                userMsg.append("\u3010\u5bf9\u8bdd\u5386\u53f2\u3011\n");
                if (hasSummary) {
                    userMsg.append("\u957f\u671f\u6458\u8981\uff1a\n").append(summary.trim()).append("\n\n");
                }
                if (hasRecentMessages) {
                    int start = Math.max(0, recentMessages.size() - 10);
                    userMsg.append("\u6700\u8fd1\u5bf9\u8bdd\uff08\u6700\u591a5\u8f6e\uff09\uff1a\n");
                    for (int i = start; i < recentMessages.size(); ++i) {
                        String item = recentMessages.get(i);
                        if (item == null || item.isBlank()) continue;
                        userMsg.append(item.trim()).append("\n");
                    }
                    userMsg.append("\n");
                }
            }
            if (pendingContext != null && !pendingContext.isBlank()) {
                userMsg.append("\u3010\u5f85\u8865\u5168\u4fe1\u606f\u3011\u4e0a\u6b21\u8ffd\u95ee\u4e86\uff1a\n").append(pendingContext).append("\n");
            }
            if (userProfile != null && userProfile.getUserProfileText() != null && !userProfile.getUserProfileText().isBlank()) {
                userMsg.append("\u3010\u7528\u6237\u753b\u50cf\u6458\u8981\u3011\n").append(userProfile.getUserProfileText()).append("\n");
            }
            userMsg.append("\u3010\u7528\u6237\u6700\u65b0\u6d88\u606f\u3011\n").append(message);
            HashMap<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0);
            requestBody.put("stream", false);
            if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen")) {
                requestBody.put("enable_thinking", false);
            }
            requestBody.put("messages", List.of(Map.of("role", "system", "content", INTENT_CLASSIFY_PROMPT), Map.of("role", "user", "content", userMsg.toString())));
            Map response = (Map)((WebClient.RequestBodySpec)webClient.post().uri("/chat/completions", new Object[0])).bodyValue(requestBody).retrieve().bodyToMono(Map.class).block();
            if (response == null) {
                log.warn("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011API\u8fd4\u56denull");
                return null;
            }
            List choices = (List)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011choices\u4e3a\u7a7a, response={}", (Object)response);
                return null;
            }
            Map msg = (Map)((Map)choices.get(0)).get("message");
            String string = content = msg != null ? (String)msg.get("content") : null;
            if (content == null || content.isBlank()) {
                log.warn("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011content\u4e3a\u7a7a, msg={}", (Object)msg);
                return null;
            }
            String json = content.trim();
            log.info("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011AI\u539f\u59cb\u8fd4\u56de: {}", (Object)json);
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            try {
                intentMap = (Map)JSON_MAPPER.readValue(json, Map.class);
            }
            catch (Exception e) {
                log.warn("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011JSON\u89e3\u6790\u5931\u8d25: {}", (Object)json);
                return null;
            }
            String intent = this.toCleanString(intentMap.get("intent"));
            if (intent == null || intent.isBlank()) {
                return null;
            }
            boolean complete = "other".equals(intent) || Boolean.TRUE.equals(intentMap.get("complete"));
            String clarify = this.toCleanString(intentMap.get("clarify"));
            HashMap<String, Object> params = new HashMap<String, Object>(intentMap);
            params.remove("intent");
            params.remove("complete");
            params.remove("clarify");
            log.info("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011userId={}, intent={}, complete={}, params={}", new Object[]{userId, intent, complete, params.keySet()});
            return new ClassifyResult(intent, params, complete, clarify);
        }
        catch (Exception e) {
            log.warn("\u3010\u5206\u7c7b+\u5ba1\u8ba1\u3011\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private Map<String, Object> auditCheck(String intent, Map<String, Object> params, String message, User user) {
        if ("other".equals(intent)) {
            return params;
        }
        String extractableParams = switch (intent) {
            case "save_diet" -> "food(\u5fc5\u586b), meal(\u53ef\u9009)";
            case "save_exercise" -> "exercises(\u5fc5\u586b\u6570\u7ec4, \u6bcf\u9879\u542bname/sets/duration)";
            case "save_weight" -> "weight_kg(\u5fc5\u586b), note(\u53ef\u9009)";
            case "query_training" -> "query_type(\u5fc5\u586b), target_day(\u53ef\u9009)";
            case "query_diet" -> "query_type(\u5fc5\u586b), meal_type(\u53ef\u9009)";
            case "query_weight" -> "range(\u53ef\u9009)";
            case "get_today_records", "get_week_records" -> "\u65e0\u53c2\u6570";
            case "get_history_record" -> "date_description(\u5fc5\u586b)";
            case "search_knowledge" -> "query(\u5fc5\u586b)";
            case "generate_training_plan" -> "user_request(\u5fc5\u586b), fitness_goal(\u53ef\u9009), days_per_week(\u53ef\u9009), equipment(\u53ef\u9009), medical_history(\u53ef\u9009)";
            case "generate_diet_plan" -> "user_request(\u5fc5\u586b), meal_scope(\u53ef\u9009)";
            case "report_training_issue" -> "issue_description(\u5fc5\u586b), affected_exercise(\u53ef\u9009)";
            case "report_diet_issue" -> "issue_description(\u5fc5\u586b), meal_context(\u53ef\u9009)";
            case "update_location" -> "city(\u5fc5\u586b)";
            case "delete_record" -> "type(\u5fc5\u586b), target(\u53ef\u9009)";
            case "update_weight" -> "weight_kg(\u5fc5\u586b)";
            default -> "\u65e0\u53c2\u6570";
        };
        try {
            Map auditResult;
            String content;
            AiModelConfig.ModelProvider provider = this.requireProvider(this.resolvePurificationModel(user));
            WebClient webClient = this.getWebClient(provider);
            String paramsJson = JSON_MAPPER.writeValueAsString(params);
            String prompt = String.format(AUDIT_CHECK_PROMPT, message, paramsJson, intent, extractableParams);
            HashMap<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", 256);
            requestBody.put("temperature", 0);
            requestBody.put("stream", false);
            if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
                requestBody.put("enable_thinking", false);
            }
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            Map response = (Map)((WebClient.RequestBodySpec)webClient.post().uri("/chat/completions", new Object[0])).bodyValue(requestBody).retrieve().bodyToMono(Map.class).block();
            if (response == null) {
                return params;
            }
            List choices = (List)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return params;
            }
            Map respMsg = (Map)((Map)choices.get(0)).get("message");
            String string = content = respMsg != null ? (String)respMsg.get("content") : null;
            if (content == null || content.isBlank()) {
                return params;
            }
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            try {
                auditResult = (Map)JSON_MAPPER.readValue(json, Map.class);
            }
            catch (Exception e) {
                return params;
            }
            Object missedObj = auditResult.get("missed");
            if (missedObj == null || "null".equals(String.valueOf(missedObj))) {
                return params;
            }
            if (missedObj instanceof Map) {
                Map missed = (Map)missedObj;
                for (Map.Entry entry : missed.entrySet()) {
                    String key = (String)entry.getKey();
                    if (params.containsKey(key)) continue;
                    params.put(key, entry.getValue());
                    log.info("\u3010\u5ba1\u8ba1\u8865\u6f0f\u3011\u8865\u5165: {}={}", (Object)key, entry.getValue());
                }
            }
            return params;
        }
        catch (Exception e) {
            log.warn("\u3010\u5ba1\u8ba1\u68c0\u67e5\u3011\u5f02\u5e38\uff0c\u8fd4\u56de\u539fparams", (Throwable)e);
            return params;
        }
    }

    private Map<String, Object> autoFillFromDB(String intent, Map<String, Object> params, Long userId) {
        if (!"generate_training_plan".equals(intent) && !"generate_diet_plan".equals(intent)) {
            return params;
        }
        UserProfile profile = this.userProfileService.getByUserId(userId);
        if (profile == null) {
            return params;
        }
        if ("generate_training_plan".equals(intent)) {
            if (!params.containsKey("fitness_goal") && profile.getFitnessGoal() != null) {
                params.put("fitness_goal", profile.getFitnessGoal());
            }
            if (!params.containsKey("experience_level") && profile.getExperienceLevel() != null) {
                params.put("experience_level", profile.getExperienceLevel());
            }
            if (!params.containsKey("equipment") && profile.getPreferredEquipment() != null) {
                params.put("equipment", profile.getPreferredEquipment());
            }
            if (!params.containsKey("days_per_week") && profile.getWeeklyTrainingDays() != null) {
                params.put("days_per_week", profile.getWeeklyTrainingDays());
            }
            if (!params.containsKey("training_duration") && profile.getTrainingDuration() != null) {
                params.put("training_duration", profile.getTrainingDuration());
            }
            if (!params.containsKey("medical_history") && profile.getMedicalHistory() != null) {
                params.put("medical_history", profile.getMedicalHistory());
            }
        } else if ("generate_diet_plan".equals(intent)) {
            if (!params.containsKey("fitness_goal") && profile.getFitnessGoal() != null) {
                params.put("fitness_goal", profile.getFitnessGoal());
            }
            if (!params.containsKey("diet_preference") && profile.getDietPreference() != null) {
                params.put("diet_preference", profile.getDietPreference());
            }
            if (!params.containsKey("medical_history") && profile.getMedicalHistory() != null) {
                params.put("medical_history", profile.getMedicalHistory());
            }
        }
        return params;
    }

    private String getPendingIntent(Long userId) {
        try {
            String key = "chat:pending_intent:" + userId;
            String val = (String)this.stringRedisTemplate.opsForValue().get((Object)key);
            if (val == null || val.isBlank()) {
                return null;
            }
            Map pending = (Map)JSON_MAPPER.readValue(val, Map.class);
            return this.toCleanString(pending.get("clarify"));
        }
        catch (Exception e) {
            return null;
        }
    }

    private void savePendingIntent(Long userId, String intent, Map<String, Object> params, String clarify) {
        try {
            HashMap<String, Object> pending = new HashMap<String, Object>();
            pending.put("intent", intent);
            pending.put("params", params);
            pending.put("clarify", clarify);
            String key = "chat:pending_intent:" + userId;
            this.stringRedisTemplate.opsForValue().set((Object)key, (Object)JSON_MAPPER.writeValueAsString(pending), 5L, TimeUnit.MINUTES);
            log.info("\u3010\u8ffd\u95ee\u4fdd\u5b58\u3011userId={}, intent={}", (Object)userId, (Object)intent);
        }
        catch (Exception e) {
            log.warn("\u3010\u8ffd\u95ee\u4fdd\u5b58\u3011Redis\u5199\u5165\u5931\u8d25", (Throwable)e);
        }
    }

    private void clearPendingIntent(Long userId) {
        try {
            this.stringRedisTemplate.delete((Object)("chat:pending_intent:" + userId));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private ToolCallResult handleSaveDietIntent(Long userId, UserRecord userRecord, Map<String, Object> intentMap) {
        try {
            String food = this.sanitizeDietRecordText(this.toCleanString(intentMap.get("food")));
            if (food == null || food.isBlank()) {
                return null;
            }
            String meal = this.resolveMealTypeFromText(this.toCleanString(intentMap.get("meal")), food);
            AddDietRecordRequest dietReq = new AddDietRecordRequest();
            dietReq.setMealType(meal != null && !meal.isBlank() ? meal : this.getCurrentMealType());
            dietReq.setName(food);
            dietReq.setSource("chat");
            this.appendDietRecord(userId, userRecord, dietReq);
            String mealLabel = meal != null && !meal.isBlank() ? meal : this.getCurrentMealType();
            String reply = "\u597d\u7684\uff0c\u5df2\u8bb0\u5f55" + mealLabel + "\uff1a" + food;
            String macroSummary = this.buildMacroSummaryText(userId, LocalDate.now(CN_ZONE));
            if (!macroSummary.isEmpty()) {
                reply = reply + "\n" + macroSummary;
            }
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("today_diet_record", "");
        }
        catch (Exception e) {
            log.error("handleSaveDietIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleSaveExerciseIntent(Long userId, UserRecord userRecord, Map<String, Object> intentMap) {
        try {
            List<Map<String, Object>> exercisesList = this.castObjectList(intentMap.get("exercises"));
            if (exercisesList.isEmpty()) {
                String exercise = this.sanitizeExerciseRecordText(this.toCleanString(intentMap.get("exercise")));
                if (exercise == null || exercise.isBlank()) {
                    return null;
                }
                exercisesList = List.of(Map.of("name", exercise));
            }
            LocalDate today = LocalDate.now(CN_ZONE);
            String recordTime = LocalTime.now(CN_ZONE).format(TIME_FMT);
            ArrayList<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            ArrayList<String> names = new ArrayList<String>();
            int totalDuration = 0;
            for (Map<String, Object> ex : exercisesList) {
                Integer n;
                String name = this.sanitizeExerciseRecordText(this.toCleanString(ex.get("name")));
                if (name == null || name.isBlank()) continue;
                Object object = ex.get("sets");
                if (object instanceof Number) {
                    Number n2 = (Number)object;
                    n = n2.intValue();
                } else {
                    n = this.extractCompletedSets(name);
                }
                Integer sets = n;
                Object durationRaw = ex.get("duration");
                int durationSec = 0;
                if (durationRaw instanceof Number) {
                    Number dn = (Number)durationRaw;
                    durationSec = dn.intValue() * 60;
                } else {
                    Integer extracted = this.extractDurationSeconds(name);
                    if (extracted != null) {
                        durationSec = extracted;
                    }
                }
                totalDuration += durationSec;
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("name", name);
                item.put("completedSets", sets);
                item.put("durationSeconds", durationSec > 0 ? Integer.valueOf(durationSec) : null);
                Exercise matched = this.findBestExerciseForRecord(name);
                if (matched != null) {
                    item.put("exerciseId", matched.getId());
                    item.put("name", matched.getName());
                    item.put("muscleGroup", matched.getMuscleGroup());
                    names.add(matched.getName());
                } else {
                    Exercise searched = this.searchAndSaveExercise(name);
                    if (searched != null) {
                        item.put("exerciseId", searched.getId());
                        item.put("name", searched.getName());
                        item.put("muscleGroup", searched.getMuscleGroup());
                        names.add(searched.getName());
                    } else {
                        names.add(name);
                    }
                }
                items.add(item);
            }
            if (items.isEmpty()) {
                return null;
            }
            String sessionName = String.join((CharSequence)"\u3001", names);
            this.exerciseRecordService.saveStructuredRecord(userId, today, recordTime, sessionName, totalDuration > 0 ? Integer.valueOf(totalDuration) : null, null, sessionName, "chat", items);
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            String reply = "\u597d\u7684\uff0c\u5df2\u8bb0\u5f55\u8fd0\u52a8\uff1a" + sessionName;
            if (todayBurned > 0) {
                reply = reply + String.format("\uff08\u4eca\u65e5\u7d2f\u8ba1\u6d88\u8017\u7ea6%d\u5927\u5361\uff09", todayBurned);
            }
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("today_exercise_record", "");
        }
        catch (Exception e) {
            log.error("handleSaveExerciseIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleSaveWeightIntent(Long userId, Map<String, Object> intentMap, boolean isUpdate) {
        try {
            double weightKg;
            Object weightObj = intentMap.get("weight_kg");
            if (weightObj == null) {
                return null;
            }
            if (weightObj instanceof Number) {
                Number num = (Number)weightObj;
                weightKg = num.doubleValue();
            } else {
                try {
                    weightKg = Double.parseDouble(String.valueOf(weightObj));
                }
                catch (Exception e) {
                    log.warn("{}: \u65e0\u6cd5\u89e3\u6790weight_kg={}", (Object)(isUpdate ? "update_weight" : "save_weight"), weightObj);
                    return null;
                }
            }
            if (weightKg <= 0.0 || weightKg > 500.0) {
                return null;
            }
            this.userWeightRecordService.saveOrUpdateTodayWeight(userId, weightKg);
            User dbUser = (User)this.userService.getById(userId);
            if (dbUser != null) {
                dbUser.setWeight(weightKg);
                this.userService.updateById(dbUser);
            }
            log.info("{}: userId={}, weight={}kg", new Object[]{isUpdate ? "update_weight" : "save_weight", userId, weightKg});
            String weightStr = weightKg % 1.0 == 0.0 ? String.valueOf((int)weightKg) : String.valueOf(weightKg);
            String reply = isUpdate ? "\u597d\u7684\uff0c\u5df2\u4fee\u6b63\u4eca\u65e5\u4f53\u91cd\u4e3a\uff1a" + weightStr + "kg" : "\u597d\u7684\uff0c\u5df2\u8bb0\u5f55\u4eca\u65e5\u4f53\u91cd\uff1a" + weightStr + "kg";
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("{}\u5f02\u5e38", (Object)(isUpdate ? "handleUpdateWeightIntent" : "handleSaveWeightIntent"), (Object)e);
            return null;
        }
    }

    private ToolCallResult handleUpdateWeightIntent(Long userId, Map<String, Object> intentMap) {
        return this.handleSaveWeightIntent(userId, intentMap, true);
    }

    private ToolCallResult handleSetDailyCaloriesIntent(Long userId, Map<String, Object> intentMap) {
        try {
            double calories;
            Object calObj = intentMap.get("daily_calories");
            if (calObj == null) {
                return null;
            }
            if (calObj instanceof Number) {
                Number num = (Number)calObj;
                calories = num.doubleValue();
            } else {
                calories = Double.parseDouble(String.valueOf(calObj).trim());
            }
            if (calories < 500.0 || calories > 10000.0) {
                String reply = "\u6bcf\u65e5\u70ed\u91cf\u76ee\u6807\u5e94\u5728 500~10000 kcal \u4e4b\u95f4\uff0c\u8bf7\u786e\u8ba4\u540e\u91cd\u65b0\u8bbe\u7f6e";
                this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
                this.resultHolderLocal.get().append(reply);
                return new ToolCallResult("general_chat", "");
            }
            UserProfile profile = this.userProfileService.getByUserId(userId);
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
            }
            profile.setCustomDailyCalories(calories);
            this.userProfileService.saveOrUpdate(userId, profile);
            log.info("set_daily_calories: userId={}, calories={}", (Object)userId, (Object)calories);
            Object reply = String.format("\u5df2\u5c06\u6bcf\u65e5\u6444\u5165\u70ed\u91cf\u76ee\u6807\u8bbe\u4e3a %.0fkcal", calories);
            try {
                User user = (User)this.userService.getById(userId);
                if (user != null && user.getWeight() != null) {
                    reply = (String)reply + String.format("\uff08\u5f53\u524d\u4f53\u91cd%.1fkg", user.getWeight());
                    if (profile.getTargetWeight() != null && profile.getTargetWeight() > 0.0) {
                        double diff = user.getWeight() - profile.getTargetWeight();
                        reply = (String)reply + String.format("\uff0c\u8ddd\u76ee\u6807%.1fkg\u8fd8\u5dee%.1fkg", profile.getTargetWeight(), Math.abs(diff));
                    }
                    reply = (String)reply + "\uff09";
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            this.writeSseDataGradually(this.outputStreamLocal.get(), (String)reply);
            this.resultHolderLocal.get().append((String)reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleSetDailyCaloriesIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleSetTargetWeightIntent(Long userId, Map<String, Object> intentMap) {
        try {
            double targetWeight;
            Object wObj = intentMap.get("target_weight");
            if (wObj == null) {
                return null;
            }
            if (wObj instanceof Number) {
                Number num = (Number)wObj;
                targetWeight = num.doubleValue();
            } else {
                targetWeight = Double.parseDouble(String.valueOf(wObj).trim());
            }
            if (targetWeight < 30.0 || targetWeight > 300.0) {
                String reply = "\u76ee\u6807\u4f53\u91cd\u5e94\u5728 30~300 kg \u4e4b\u95f4\uff0c\u8bf7\u786e\u8ba4\u540e\u91cd\u65b0\u8bbe\u7f6e";
                this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
                this.resultHolderLocal.get().append(reply);
                return new ToolCallResult("general_chat", "");
            }
            UserProfile profile = this.userProfileService.getByUserId(userId);
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
            }
            profile.setTargetWeight(targetWeight);
            this.userProfileService.saveOrUpdate(userId, profile);
            log.info("set_target_weight: userId={}, targetWeight={}", (Object)userId, (Object)targetWeight);
            Object reply = String.format("\u5df2\u5c06\u76ee\u6807\u4f53\u91cd\u8bbe\u4e3a %.1fkg", targetWeight);
            try {
                User user = (User)this.userService.getById(userId);
                if (user != null && user.getWeight() != null) {
                    double diff = user.getWeight() - targetWeight;
                    reply = Math.abs(diff) > 0.1 ? (String)reply + String.format("\uff0c\u5f53\u524d%.1fkg\uff0c%s%.1fkg", user.getWeight(), diff > 0.0 ? "\u8fd8\u9700\u51cf" : "\u8fd8\u9700\u589e", Math.abs(diff)) : (String)reply + "\uff0c\u5f53\u524d\u4f53\u91cd\u5df2\u63a5\u8fd1\u76ee\u6807\uff0c\u7ee7\u7eed\u4fdd\u6301\uff01";
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            reply = (String)reply + "\uff0c\u52a0\u6cb9\uff01";
            this.writeSseDataGradually(this.outputStreamLocal.get(), (String)reply);
            this.resultHolderLocal.get().append((String)reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleSetTargetWeightIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleUpdateLocationIntent(Long userId, Map<String, Object> intentMap) {
        try {
            User user;
            Object cityObj = intentMap.get("city");
            if (cityObj == null) {
                return null;
            }
            String city = String.valueOf(cityObj).trim();
            if (city.isBlank()) {
                return null;
            }
            if (city.endsWith("\u5e02")) {
                city = city.substring(0, city.length() - 1);
            }
            if ((user = (User)this.userService.getById(userId)) == null) {
                return null;
            }
            user.setCity(city);
            user.setCityEn(CITY_EN_MAP.getOrDefault(city, ""));
            this.userService.updateById(user);
            log.info("update_location: userId={}, city={}, cityEn={}", new Object[]{userId, city, user.getCityEn()});
            String reply = "\u597d\u7684\uff0c\u5df2\u66f4\u65b0\u4f60\u7684\u4f4d\u7f6e\u4e3a" + city + "\uff0c\u4ee5\u540e\u5929\u6c14\u63d0\u9192\u4f1a\u6309" + city + "\u7684\u5929\u6c14\u6765\u63a8\u9001\u3002";
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleUpdateLocationIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleReportTrainingIssueIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> params) {
        try {
            String dietPlan;
            LocalDate today = LocalDate.now(CN_ZONE);
            LocalDate yesterday = today.minusDays(1L);
            StringBuilder context = new StringBuilder();
            context.append("\u7528\u6237\u95ee\u9898\uff1a").append(this.toCleanString(params.get("issue_description")));
            String affectedExercise = this.toCleanString(params.get("affected_exercise"));
            if (!affectedExercise.isBlank()) {
                context.append("\uff0c\u6d89\u53ca\u52a8\u4f5c\uff1a").append(affectedExercise);
            }
            context.append("\n\n");
            context.append(this.buildUserInfo(user)).append("\n");
            String trainingPlan = this.buildTrainingPlanText(userId);
            if (trainingPlan != null && !trainingPlan.isBlank()) {
                context.append("\u3010\u5f53\u524d\u8bad\u7ec3\u8ba1\u5212\u3011\n").append(trainingPlan).append("\n\n");
            }
            if ((dietPlan = this.buildDietPlanText(userId)) != null && !dietPlan.isBlank()) {
                context.append("\u3010\u5f53\u524d\u996e\u98df\u8ba1\u5212\u3011\n").append(dietPlan).append("\n\n");
            }
            Map<String, Object> todayMacro = this.dietRecordService.getDayMacroSummary(userId, today);
            Map<String, Object> yesterdayMacro = this.dietRecordService.getDayMacroSummary(userId, yesterday);
            context.append("\u3010\u8fd1\u671f\u996e\u98df\u6570\u636e\u3011\n");
            context.append("\u4eca\u65e5\u6444\u5165\uff1a\u70ed\u91cf").append(todayMacro.get("calories")).append("kcal\uff0c\u86cb\u767d\u8d28").append(todayMacro.get("protein")).append("g\uff0c\u78b3\u6c34").append(todayMacro.get("carbs")).append("g\uff0c\u8102\u80aa").append(todayMacro.get("fat")).append("g\n");
            context.append("\u6628\u65e5\u6444\u5165\uff1a\u70ed\u91cf").append(yesterdayMacro.get("calories")).append("kcal\uff0c\u86cb\u767d\u8d28").append(yesterdayMacro.get("protein")).append("g\uff0c\u78b3\u6c34").append(yesterdayMacro.get("carbs")).append("g\uff0c\u8102\u80aa").append(yesterdayMacro.get("fat")).append("g\n\n");
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            int yesterdayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, yesterday);
            context.append("\u3010\u8fd1\u671f\u8bad\u7ec3\u6d88\u8017\u3011\n\u4eca\u65e5\uff1a").append(todayBurned).append("\u5927\u5361\uff0c\u6628\u65e5\uff1a").append(yesterdayBurned).append("\u5927\u5361\n\n");
            String exerciseCatalog = this.buildExerciseCatalog(userId, null);
            context.append("\u3010\u52a8\u4f5c\u5e93\u3011\n").append(exerciseCatalog).append("\n\n");
            context.append("\u7528\u6237\u7684\u5177\u4f53\u8981\u6c42\uff1a").append(message);
            String aiReply = this.callAiApiStream(REPORT_TRAINING_ISSUE_PROMPT, context.toString(), outputStream, null, 1500);
            if (!this.isBlank(aiReply)) {
                resultHolder.append(aiReply);
            }
            return new ToolCallResult("training_plan", "training");
        }
        catch (Exception e) {
            log.error("handleReportTrainingIssueIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleReportDietIssueIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> params) {
        try {
            String dietPlan;
            LocalDate today = LocalDate.now(CN_ZONE);
            LocalDate yesterday = today.minusDays(1L);
            StringBuilder context = new StringBuilder();
            context.append("\u7528\u6237\u95ee\u9898\uff1a").append(this.toCleanString(params.get("issue_description")));
            String mealContext = this.toCleanString(params.get("meal_context"));
            if (!mealContext.isBlank()) {
                context.append("\uff0c\u76f8\u5173\u9910\u6b21\uff1a").append(mealContext);
            }
            context.append("\n\n");
            context.append(this.buildUserInfo(user)).append("\n");
            String trainingPlan = this.buildTrainingPlanText(userId);
            if (trainingPlan != null && !trainingPlan.isBlank()) {
                context.append("\u3010\u5f53\u524d\u8bad\u7ec3\u8ba1\u5212\u3011\n").append(trainingPlan).append("\n\n");
            }
            if ((dietPlan = this.buildDietPlanText(userId)) != null && !dietPlan.isBlank()) {
                context.append("\u3010\u5f53\u524d\u996e\u98df\u8ba1\u5212\u3011\n").append(dietPlan).append("\n\n");
            }
            Map<String, Object> todayMacro = this.dietRecordService.getDayMacroSummary(userId, today);
            Map<String, Object> yesterdayMacro = this.dietRecordService.getDayMacroSummary(userId, yesterday);
            context.append("\u3010\u8fd1\u671f\u996e\u98df\u6570\u636e\u3011\n");
            context.append("\u4eca\u65e5\u6444\u5165\uff1a\u70ed\u91cf").append(todayMacro.get("calories")).append("kcal\uff0c\u86cb\u767d\u8d28").append(todayMacro.get("protein")).append("g\uff0c\u78b3\u6c34").append(todayMacro.get("carbs")).append("g\uff0c\u8102\u80aa").append(todayMacro.get("fat")).append("g\n");
            context.append("\u6628\u65e5\u6444\u5165\uff1a\u70ed\u91cf").append(yesterdayMacro.get("calories")).append("kcal\uff0c\u86cb\u767d\u8d28").append(yesterdayMacro.get("protein")).append("g\uff0c\u78b3\u6c34").append(yesterdayMacro.get("carbs")).append("g\uff0c\u8102\u80aa").append(yesterdayMacro.get("fat")).append("g\n\n");
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            int yesterdayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, yesterday);
            context.append("\u3010\u8fd1\u671f\u8bad\u7ec3\u6d88\u8017\u3011\n\u4eca\u65e5\uff1a").append(todayBurned).append("\u5927\u5361\uff0c\u6628\u65e5\uff1a").append(yesterdayBurned).append("\u5927\u5361\n\n");
            String foodCatalog = this.buildFoodCatalog(userId, null);
            context.append("\u3010\u98df\u7269\u8425\u517b\u53c2\u8003\u3011\n").append(foodCatalog).append("\n\n");
            context.append("\u7528\u6237\u7684\u5177\u4f53\u8981\u6c42\uff1a").append(message);
            String aiReply = this.callAiApiStream(REPORT_DIET_ISSUE_PROMPT, context.toString(), outputStream, null, 1500);
            if (!this.isBlank(aiReply)) {
                resultHolder.append(aiReply);
            }
            return new ToolCallResult("diet_plan", "diet");
        }
        catch (Exception e) {
            log.error("handleReportDietIssueIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleQueryWeatherIntent(User user, String message, OutputStream outputStream) {
        try {
            String weatherContext = this.getWeatherContextForUser(user);
            String systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u7528\u6237\u5728\u8be2\u95ee\u5929\u6c14\u60c5\u51b5\uff0c\u8bf7\u7ed3\u5408\u5929\u6c14\u7ed9\u51fa\u8fd0\u52a8\u5efa\u8bae\u3002\u5982\u679c\u5929\u6c14\u6076\u52a3\u5efa\u8bae\u5ba4\u5185\u8bad\u7ec3\uff0c\u5929\u6c14\u597d\u53ef\u4ee5\u9f13\u52b1\u6237\u5916\u8fd0\u52a8\u3002\u56de\u590d\u7b80\u6d01\u53cb\u597d\uff0c100\u5b57\u4ee5\u5185\u3002";
            Object userMsg = message;
            userMsg = weatherContext != null ? (String)userMsg + "\n\n" + weatherContext : (String)userMsg + "\n\n\uff08\u5929\u6c14\u6570\u636e\u83b7\u53d6\u5931\u8d25\uff0c\u8bf7\u6839\u636e\u5e38\u8bc6\u56de\u7b54\uff09";
            String reply = this.callAiApiStream(systemPrompt, (String)userMsg, outputStream, null, 256);
            return new ToolCallResult("query_weather", "");
        }
        catch (Exception e) {
            log.error("\u67e5\u8be2\u5929\u6c14\u5931\u8d25", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleDeleteRecordIntent(Long userId, UserRecord userRecord, Map<String, Object> intentMap) {
        try {
            boolean deleted;
            String type = this.toCleanString(intentMap.get("type"));
            if (!"exercise".equals(type) && !"diet".equals(type)) {
                return null;
            }
            String target = this.toCleanString(intentMap.get("target"));
            LocalDate today = LocalDate.now(CN_ZONE);
            if ("exercise".equals(type)) {
                List<ExerciseSession> sessions = this.exerciseRecordService.listByUserAndDate(userId, today);
                index = this.resolveDeleteIndex(sessions, target);
                if (index < 0 || index >= sessions.size()) {
                    String reply = "\u6ca1\u6709\u627e\u5230" + (String)(target != null ? "\u5305\u542b\"" + target + "\"\u7684" : "") + "\u8bad\u7ec3\u8bb0\u5f55";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
                    this.resultHolderLocal.get().append(reply);
                    return new ToolCallResult("general_chat", "");
                }
                deleted = this.exerciseRecordService.deleteTodayRecord(userId, today, index);
            } else {
                List<DietRecord> records = this.dietRecordService.listByUserAndDate(userId, today);
                index = this.resolveDeleteIndex(records, target);
                if (index < 0 || index >= records.size()) {
                    String reply = "\u6ca1\u6709\u627e\u5230" + (String)(target != null ? "\u5305\u542b\"" + target + "\"\u7684" : "") + "\u996e\u98df\u8bb0\u5f55";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
                    this.resultHolderLocal.get().append(reply);
                    return new ToolCallResult("general_chat", "");
                }
                deleted = this.dietRecordService.deleteTodayRecord(userId, today, index);
                if (deleted) {
                    User user = (User)this.userService.getById(userId);
                    this.userDailyMetricService.syncDailyCalories(userId, today, this.dietRecordService.listLegacyRecords(userId, today), this.resolveTargetCalories(user));
                }
            }
            String typeName = "exercise".equals(type) ? "\u8bad\u7ec3" : "\u996e\u98df";
            Object reply = deleted ? "\u597d\u7684\uff0c\u5df2\u5220\u9664" + typeName + "\u8bb0\u5f55" : "\u5220\u9664\u5931\u8d25\uff0c\u53ef\u80fd\u8bb0\u5f55\u5df2\u4e0d\u5b58\u5728";
            this.writeSseDataGradually(this.outputStreamLocal.get(), (String)reply);
            this.resultHolderLocal.get().append((String)reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleDeleteRecordIntent\u5f02\u5e38", (Throwable)e);
            return null;
        }
    }

    private int resolveDeleteIndex(List<?> records, String target) {
        if (records.isEmpty()) {
            return -1;
        }
        if (target != null && !target.isBlank()) {
            for (int i = 0; i < records.size(); ++i) {
                Object r = records.get(i);
                String searchableName = this.buildSearchableName(r);
                if (searchableName == null || !searchableName.contains(target)) continue;
                return i;
            }
            return -1;
        }
        return records.size() - 1;
    }

    private String buildSearchableName(Object r) {
        if (r instanceof ExerciseSession) {
            ExerciseSession s = (ExerciseSession)r;
            if (s.getNote() != null && !s.getNote().isBlank()) {
                return s.getNote();
            }
            return null;
        }
        if (r instanceof DietRecord) {
            DietRecord d = (DietRecord)r;
            return d.getName();
        }
        return null;
    }

    private ToolCallResult handleQueryTrainingIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        String queryType = this.toCleanString(intentMap.getOrDefault("query_type", "\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55"));
        String targetDay = this.toCleanString(intentMap.getOrDefault("target_day", "\u4eca\u5929"));
        if ("\u8ba1\u5212\u5b89\u6392".equals(queryType)) {
            boolean isRestDay;
            String data;
            String replyType = "today_training_plan";
            if ("\u5168\u90e8".equals(targetDay)) {
                data = this.buildTrainingPlanText(userId);
            } else {
                int offset = "\u660e\u5929".equals(targetDay) ? 1 : 0;
                data = this.buildTrainingPlanDaySection(userId, offset);
            }
            if (data == null || data.isBlank()) {
                data = "\u6682\u65e0\u8bad\u7ec3\u5b89\u6392";
            }
            boolean bl = isRestDay = data.contains("\u4f11\u606f\u65e5") || data.contains("\u4f11\u606f");
            if (isRestDay) {
                replyType = "today_rest_day";
            }
            String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5217\u51fa\u6240\u6709\u5185\u5bb9\u3002";
            String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, "");
        }
        String replyType = "today_exercise_record";
        String data = this.buildTodayExerciseReply(userId);
        if (data == null || data.isBlank()) {
            data = "\u4eca\u5929\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55";
        }
        String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5217\u51fa\u6bcf\u4e2a\u52a8\u4f5c\u3002";
        String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
        return new ToolCallResult(replyType, "");
    }

    private ToolCallResult handleQueryDietIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        String queryType = this.toCleanString(intentMap.getOrDefault("query_type", "\u5df2\u5b8c\u6210\u7684\u8bb0\u5f55"));
        String mealType = this.toCleanString(intentMap.getOrDefault("meal_type", "\u5168\u90e8"));
        if ("\u8ba1\u5212\u5b89\u6392".equals(queryType)) {
            String section;
            String replyType = "today_diet_plan";
            String planText = this.buildDietPlanText(userId);
            Object data = planText == null || planText.isBlank() ? "\u8fd8\u6ca1\u6709\u996e\u98df\u8ba1\u5212" : ("\u5168\u90e8".equals(mealType) ? planText : ((section = this.extractDietMealSection(planText, mealType)) != null && !section.isBlank() ? section : "\u5f53\u524d\u996e\u98df\u8ba1\u5212\u91cc\u6ca1\u6709" + mealType + "\u63a8\u8350"));
            String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5217\u51fa\u6bcf\u4e2a\u9910\u6b21\u3002";
            String summarized = this.streamSummarizeWithToolData(userId, user, message, (String)data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, "");
        }
        String replyType = "today_diet_record";
        String data = this.buildTodayDietReply(userId);
        if (data == null || data.isBlank()) {
            data = "\u4eca\u5929\u6682\u65e0\u996e\u98df\u8bb0\u5f55";
        }
        String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5217\u51fa\u6bcf\u4e2a\u9910\u6b21\u548c\u98df\u7269\u3002";
        String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
        return new ToolCallResult(replyType, "");
    }

    private ToolCallResult handleQueryWeightIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        String data;
        String range = this.toCleanString(intentMap.getOrDefault("range", "\u6700\u8fd17\u5929"));
        LocalDate endDate = LocalDate.now(CN_ZONE);
        int days = switch (range) {
            case "\u4eca\u5929" -> 0;
            case "\u6700\u8fd130\u5929" -> 29;
            default -> 6;
        };
        LocalDate startDate = endDate.minusDays(days);
        List<UserWeightRecord> records = this.userWeightRecordService.listByUserAndDateRange(userId, startDate, endDate);
        if (records == null || records.isEmpty()) {
            data = "\u6682\u65e0\u4f53\u91cd\u8bb0\u5f55";
        } else {
            StringBuilder ws = new StringBuilder("\u4f53\u91cd\u8bb0\u5f55\uff08").append(range).append("\uff09\uff1a\n");
            for (UserWeightRecord r : records) {
                ws.append(r.getRecordDate()).append("\uff1a").append(r.getWeight()).append("kg\n");
            }
            data = ws.toString().trim();
        }
        String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u4f53\u91cd\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5982\u679c\u6709\u53d8\u5316\u8d8b\u52bf\u53ef\u4ee5\u9002\u5f53\u603b\u7ed3\u3002";
        String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
        return new ToolCallResult("general_chat", "");
    }

    private ToolCallResult handleQueryDataIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, String toolName, Map<String, Object> intentMap, String replyType, String savePlanType) {
        try {
            String data;
            HashMap<String, Object> args = new HashMap<String, Object>();
            if ("get_history_record".equals(toolName)) {
                args.put("date_description", this.toCleanString(intentMap.get("date_description")));
            }
            if ((data = this.executeDataToolCall(userId, toolName, args, user, userRecord)) == null || data.isBlank()) {
                data = "\u6682\u65e0\u8bb0\u5f55";
            }
            String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u6570\u636e\uff0c\u7b80\u6d01\u4eb2\u5207\u5730\u56de\u7b54\u7528\u6237\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5fc5\u987b\u5b8c\u6574\u5217\u51fa\u6240\u6709\u8bb0\u5f55\u3002";
            this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, savePlanType);
        }
        catch (Exception e) {
            log.error("handleQueryDataIntent\u5f02\u5e38", (Throwable)e);
            return new ToolCallResult("general_chat", "");
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private ToolCallResult handleSearchKnowledgeIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        try {
            String data;
            String query = this.toCleanString(intentMap.get("query"));
            String knowledgeResult = this.retrieveKnowledgeDirectly(query);
            if (knowledgeResult != null && !knowledgeResult.isBlank()) {
                data = knowledgeResult;
            } else {
                String exerciseName = this.extractExerciseNameFromQuery(query);
                if (exerciseName == null) {
                    String data2 = "\u77e5\u8bc6\u5e93\u6682\u65e0\u8be5\u77e5\u8bc6\uff0c\u6211\u65e0\u6cd5\u56de\u7b54";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), data2);
                    this.resultHolderLocal.get().append(data2);
                    return new ToolCallResult("general_chat", "");
                }
                String exerciseInfo = this.searchExerciseInfo(exerciseName);
                if (exerciseInfo != null && !exerciseInfo.isBlank()) {
                    data = exerciseInfo;
                } else {
                    String data3 = "\u77e5\u8bc6\u5e93\u6682\u65e0\u8be5\u77e5\u8bc6\uff0c\u6211\u65e0\u6cd5\u56de\u7b54";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), data3);
                    this.resultHolderLocal.get().append(data3);
                    return new ToolCallResult("general_chat", "");
                }
            }
            String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u6839\u636e\u4e0b\u65b9\u77e5\u8bc6\u5e93\u5185\u5bb9\uff0c\u7b80\u6d01\u4e13\u4e1a\u5730\u56de\u7b54\u7528\u6237\u7684\u5065\u8eab\u95ee\u9898\u3002\u7eaf\u6587\u672c\uff0c\u4e0d\u7528markdown\u3002\u5982\u679c\u77e5\u8bc6\u5e93\u5185\u5bb9\u4e0d\u8db3\u4ee5\u56de\u7b54\uff0c\u56de\u590d\uff1a\u77e5\u8bc6\u5e93\u6682\u65e0\u8be5\u77e5\u8bc6\uff0c\u6211\u65e0\u6cd5\u56de\u7b54\u3002";
            this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleSearchKnowledgeIntent\u5f02\u5e38", (Throwable)e);
            return new ToolCallResult("general_chat", "");
        }
    }

    public static void setClientIp(String ip) {
        CLIENT_IP.set(ip);
    }

    public static void clearClientIp() {
        CLIENT_IP.remove();
    }

    private String getWeatherContextForUser(User user) {
        try {
            String userCityEn;
            String userCity = user.getCity() != null && !user.getCity().isBlank() ? user.getCity() : null;
            String string = userCityEn = user.getCityEn() != null && !user.getCityEn().isBlank() ? user.getCityEn() : null;
            if (userCity != null) {
                return this.weatherHelper.buildWeatherContext(userCity, userCityEn != null ? userCityEn : userCity);
            }
            return this.getWeatherContextAsync();
        }
        catch (Exception e) {
            log.warn("[Weather] \u83b7\u53d6\u5929\u6c14\u4e0a\u4e0b\u6587\u5931\u8d25: {}", (Object)e.getMessage());
            return null;
        }
    }

    private String getWeatherContextAsync() {
        try {
            String ip = CLIENT_IP.get();
            return this.weatherHelper.buildWeatherContextByIp(ip);
        }
        catch (Exception e) {
            log.warn("[Weather] \u83b7\u53d6\u5929\u6c14\u4e0a\u4e0b\u6587\u5931\u8d25: {}", (Object)e.getMessage());
            return null;
        }
    }

    private void trySaveUserCity(Long userId) {
        try {
            User dbUser = (User)this.userService.getById(userId);
            if (dbUser == null || dbUser.getCity() != null && !dbUser.getCity().isBlank()) {
                return;
            }
            String ip = CLIENT_IP.get();
            if (ip == null || ip.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                return;
            }
            String city = this.weatherHelper.getCityByIp(ip);
            String cityEn = this.weatherHelper.getCityEnByIp(ip);
            if (city != null && !city.isBlank()) {
                User update = new User();
                update.setId(userId);
                update.setCity(city);
                update.setCityEn(cityEn);
                this.userService.updateById(update);
                log.info("[Weather] \u5df2\u4fdd\u5b58\u7528\u6237[{}]\u57ce\u5e02: {} / {}", new Object[]{userId, city, cityEn});
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private ToolCallResult handleGenerateIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, OutputStream outputStream, StringBuilder resultHolder, String planIntent, Map<String, Object> intentMap) {
        String argsJson = null;
        try {
            HashMap<String, Object> toolArgs = new HashMap<String, Object>(intentMap);
            toolArgs.remove("intent");
            argsJson = JSON_MAPPER.writeValueAsString(toolArgs);
        }
        catch (Exception toolArgs) {
            // empty catch block
        }
        String planReply = this.handlePlanGenerationWithToolArgs(userId, message, user, history, outputStream, resultHolder, userRecord, planIntent, argsJson);
        String savePlanType = "training_plan".equals(planIntent) ? "training" : "diet";
        return new ToolCallResult(planIntent, savePlanType);
    }

    private OutputStream getOutputStream(OutputStream fallback) {
        OutputStream os = this.outputStreamLocal.get();
        return os != null ? os : fallback;
    }

    private ToolCallResult handleGeneralChatFallback(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder) {
        try {
            String streamed;
            String weatherContext;
            String msgLower = message.toLowerCase();
            boolean hasFitnessKeyword = msgLower.contains("\u7ec3") || msgLower.contains("\u5403") || msgLower.contains("\u8bb0\u5f55") || msgLower.contains("\u8ba1\u5212") || msgLower.contains("\u98df\u8c31") || msgLower.contains("\u8fd0\u52a8") || msgLower.contains("\u996e\u98df") || msgLower.contains("\u4f53\u91cd") || msgLower.contains("\u70ed\u91cf") || msgLower.contains("\u5361\u8def\u91cc") || msgLower.contains("\u6253\u5361") || msgLower.contains("\u8bad\u7ec3") || msgLower.contains("\u51cf\u8102") || msgLower.contains("\u589e\u808c") || msgLower.contains("\u86cb\u767d") || msgLower.contains("\u78b3\u6c34") || msgLower.contains("\u8102\u80aa");
            String knowledgeResult = hasFitnessKeyword ? this.retrieveKnowledgeDirectly(message) : null;
            AiModelConfig.ModelProvider provider = this.requireActiveProvider();
            PromptContextDecision ctx = new PromptContextDecision(false, true, false, false, false, true, false, false);
            Object contextMsg = this.buildContextAwareUserMessage(message, history, userRecord, user, null, ctx);
            if (purifiedText != null && !purifiedText.isBlank()) {
                contextMsg = (String)contextMsg + "\n\u3010\u7528\u6237\u8fd1\u671f\u753b\u50cf\u4fe1\u606f\u3011\n" + purifiedText + "\n";
            }
            String systemPrompt = GENERAL_SYSTEM_PROMPT;
            if (knowledgeResult != null && !knowledgeResult.isBlank()) {
                contextMsg = (String)contextMsg + "\n\u3010\u77e5\u8bc6\u5e93\u68c0\u7d22\u7ed3\u679c\u3011\n" + knowledgeResult + "\n";
                systemPrompt = GENERAL_SYSTEM_PROMPT_WITH_KNOWLEDGE;
            }
            if (hasFitnessKeyword && (weatherContext = this.getWeatherContextAsync()) != null) {
                contextMsg = (String)contextMsg + "\n" + weatherContext + "\n";
            }
            if (!this.isBlank(streamed = this.callAiApiStreamWithProvider(systemPrompt, (String)contextMsg, outputStream, provider, 2048))) {
                resultHolder.append(streamed);
            }
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("\u901a\u7528\u5bf9\u8bdd\u5f02\u5e38", (Throwable)e);
            return new ToolCallResult("general_chat", "");
        }
    }

    private String handlePlanGenerationWithToolArgs(Long userId, String message, User user, ChatHistory history, OutputStream outputStream, StringBuilder resultHolder, UserRecord userRecord, String planIntent, String toolArgsJson) {
        try {
            Map toolArgs = new HashMap();
            if (toolArgsJson != null && !toolArgsJson.isBlank()) {
                try {
                    toolArgs = (Map)JSON_MAPPER.readValue(toolArgsJson, Map.class);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            String userRequest = this.toCleanString(toolArgs.get("user_request"));
            MessageRoutingDecision routingDecision = new MessageRoutingDecision(false, false, new PromptContextDecision(true, false, false, false, false, false, false, false), "none", planIntent, new TrainingPlanLookupDecision(false, "", false), new DietPlanLookupDecision(false, "", false), null, null);
            String extraNote = null;
            if (userRequest != null && !userRequest.isBlank()) {
                extraNote = "\u7528\u6237\u7684\u5177\u4f53\u8981\u6c42\uff1a" + userRequest;
            }
            return this.handlePlanGeneration(message, user, history, outputStream, resultHolder, userRecord, routingDecision, extraNote);
        }
        catch (Exception e) {
            log.error("\u8ba1\u5212\u751f\u6210\u4fe1\u53f7\u5de5\u5177\u6267\u884c\u5931\u8d25", (Throwable)e);
            return null;
        }
    }

    private LocalDate resolveDateFromDescription(String text) {
        for (TimePattern tp : TIME_PATTERNS) {
            Matcher m = tp.pattern().matcher(text);
            if (!m.find()) continue;
            return tp.resolver().apply(m);
        }
        return null;
    }

    private String executeDataToolCall(Long userId, String toolName, Map<String, Object> args, User user, UserRecord userRecord) {
        if ("get_history_record".equals(toolName)) {
            String dateDesc = this.toCleanString(args.get("date_description"));
            LocalDate target = this.resolveDateFromDescription(dateDesc != null ? dateDesc : "");
            if (target == null) {
                target = LocalDate.now(CN_ZONE).minusDays(1L);
            }
            return this.fetchHistoryRecord(userRecord, target);
        }
        if ("get_today_records".equals(toolName)) {
            String data = this.buildTodayRecordReply(user, userRecord);
            return data != null && !data.isBlank() ? data : "\u6682\u65e0\u8bb0\u5f55";
        }
        if ("get_week_records".equals(toolName)) {
            String data = this.buildWeekRecordReply(user, userRecord);
            return data != null && !data.isBlank() ? data : "\u6682\u65e0\u8bb0\u5f55";
        }
        return null;
    }

    private String fetchHistoryRecord(UserRecord userRecord, LocalDate target) {
        LocalDate today = LocalDate.now(CN_ZONE);
        if (target.isAfter(today)) {
            return "\u8be5\u65e5\u671f\u8fd8\u6ca1\u5230\u54e6~";
        }
        if (ChronoUnit.DAYS.between(target, today) > 14L) {
            return "\u4e0d\u597d\u610f\u601d\uff0c\u8be5\u65e5\u671f\u6570\u636e\u5df2\u6e05\u9664\u6216\u672a\u8bb0\u5f55";
        }
        if (target.equals(today.minusDays(1L))) {
            String summary = userRecord.getYesterdaySummary();
            if (summary != null && !summary.isBlank()) {
                return "\u3010\u6628\u5929\u7684\u603b\u7ed3\u3011\n" + summary;
            }
            return "\u6628\u5929\u6682\u65e0\u8bb0\u5f55\u5185\u5bb9";
        }
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        if (target.isBefore(currentWeekStart)) {
            String weeklySummary = userRecord.getWeeklySummary();
            if (weeklySummary != null && !weeklySummary.isBlank()) {
                return weeklySummary;
            }
            return "\u4e0a\u5468\u6682\u65e0\u603b\u7ed3\u5185\u5bb9";
        }
        return this.searchReviewByDate(userRecord, target);
    }

    private String searchReviewByDate(UserRecord userRecord, LocalDate target) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M\u6708d\u65e5");
        String dateKey = "\u3010" + fmt.format(target);
        List<String> reviews = this.parseJsonArray(userRecord.getWeeklyReviews());
        for (String review : reviews) {
            if (!review.replaceAll("\\s+", "").contains(dateKey)) continue;
            if (review.contains("\u6682\u65e0\u8bb0\u5f55")) {
                return fmt.format(target) + "\u6682\u65e0\u8bb0\u5f55\u5185\u5bb9";
            }
            return review;
        }
        return fmt.format(target) + "\u6682\u65e0\u8bb0\u5f55\u5185\u5bb9";
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<String>();
        }
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return (List)JSON_MAPPER.readValue(fixed, (TypeReference)new TypeReference<List<String>>(){});
        }
        catch (Exception e) {
            log.warn("\u89e3\u6790JSON\u6570\u7ec4\u5931\u8d25: {}", (Object)json, (Object)e);
            return this.extractStringItems(json);
        }
    }

    private List<String> extractStringItems(String raw) {
        ArrayList<String> items = new ArrayList<String>();
        Matcher matcher = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"", 32).matcher(raw);
        while (matcher.find()) {
            String item = matcher.group(1).replace("\\n", "\n").replace("\\r", "").replace("\\\"", "\"").replace("\\\\", "\\").trim();
            if (item.isEmpty()) continue;
            items.add(item);
        }
        return items;
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord) {
        return this.buildContextAwareUserMessage(currentMessage, history, userRecord, null, false, false);
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord, String extraSystemNote) {
        return this.buildContextAwareUserMessage(currentMessage, history, userRecord, extraSystemNote, false, false);
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord, String extraSystemNote, boolean includeTrainingPlan, boolean includeDietPlan) {
        return this.buildContextAwareUserMessage(currentMessage, history, userRecord, null, extraSystemNote, new PromptContextDecision(false, true, true, true, true, true, includeTrainingPlan, includeDietPlan));
    }

    private String buildContextAwareUserMessage(String currentMessage, ChatHistory history, UserRecord userRecord, User user, String extraSystemNote, PromptContextDecision promptContext) {
        String dietPlanText;
        String trainingPlanText;
        boolean hasRecentMessages;
        PromptContextDecision contextDecision;
        PromptContextDecision promptContextDecision = contextDecision = promptContext == null ? this.defaultPromptContextDecision() : promptContext;
        if (history == null && userRecord == null) {
            String userInfo = this.buildUserInfo(user);
            if (userInfo.isBlank() && (extraSystemNote == null || extraSystemNote.isBlank())) {
                return currentMessage;
            }
            StringBuilder minimalContext = new StringBuilder();
            if (!userInfo.isBlank()) {
                minimalContext.append(userInfo.trim()).append("\n\n");
            }
            if (extraSystemNote != null && !extraSystemNote.isBlank()) {
                minimalContext.append("\u3010\u7cfb\u7edf\u8bf4\u660e\u3011\n").append(extraSystemNote).append("\n");
            }
            minimalContext.append("\u3010\u5f53\u524d\u7528\u6237\u6d88\u606f\u3011\n").append(currentMessage);
            return minimalContext.toString();
        }
        StringBuilder context = new StringBuilder();
        String userInfo = this.buildUserInfo(user);
        if (!userInfo.isBlank()) {
            context.append(userInfo.trim()).append("\n\n");
        }
        String summary = history != null ? history.getSummary() : null;
        List<String> recentMessages = history != null ? this.parseJsonArray(history.getPendingMessages()) : null;
        boolean hasSummary = summary != null && !summary.isBlank();
        boolean bl = hasRecentMessages = recentMessages != null && !recentMessages.isEmpty();
        if (contextDecision.includeRecentDialog() && (hasSummary || hasRecentMessages)) {
            context.append("\u3010\u5386\u53f2\u4e0a\u4e0b\u6587\u3011\n");
            if (hasSummary) {
                context.append("\u957f\u671f\u6458\u8981\uff1a\n").append(summary.trim()).append("\n\n");
            }
            if (hasRecentMessages) {
                int start = Math.max(0, recentMessages.size() - 10);
                context.append("\u6700\u8fd1\u539f\u6587\u5bf9\u8bdd\uff08\u6700\u591a5\u8f6e\uff09\uff1a\n");
                for (int i = start; i < recentMessages.size(); ++i) {
                    String item = recentMessages.get(i);
                    if (item == null || item.isBlank()) continue;
                    context.append(item.trim()).append("\n");
                }
                context.append("\n");
            }
        }
        if (contextDecision.includeTrainingPlanContext() && user != null && (trainingPlanText = this.buildTrainingPlanText(user.getId())) != null && !trainingPlanText.isBlank()) {
            context.append("\u3010\u5f53\u524d\u8bad\u7ec3\u8ba1\u5212\u3011\n").append(trainingPlanText.trim()).append("\n\n");
        }
        if (contextDecision.includeDietPlanContext() && user != null && (dietPlanText = this.buildDietPlanText(user.getId())) != null && !dietPlanText.isBlank()) {
            context.append("\u3010\u5f53\u524d\u996e\u98df\u8ba1\u5212\u3011\n").append(dietPlanText.trim()).append("\n\n");
        }
        if (contextDecision.includeTodayRecord() && user != null) {
            String exRecords = this.exerciseRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));
            String dietRecords = this.dietRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));
            if (exRecords != null && !exRecords.isBlank() || dietRecords != null && !dietRecords.isBlank()) {
                context.append("\u3010\u7528\u6237\u4eca\u65e5\u8bb0\u5f55\u3011\n");
                if (exRecords != null && !exRecords.isBlank()) {
                    context.append("\u8fd0\u52a8\u8bb0\u5f55\uff1a\n").append(exRecords.trim()).append("\n\n");
                }
                if (dietRecords != null && !dietRecords.isBlank()) {
                    context.append("\u996e\u98df\u8bb0\u5f55\uff1a\n").append(dietRecords.trim()).append("\n\n");
                }
            }
        }
        if (userRecord != null) {
            String yesterdaySummary = userRecord.getYesterdaySummary();
            if (contextDecision.includeYesterdaySummary() && yesterdaySummary != null && !yesterdaySummary.isBlank()) {
                context.append("\u3010\u6628\u65e5\u603b\u7ed3\u3011\n").append(yesterdaySummary.trim()).append("\n\n");
            }
            String weeklySummary = userRecord.getWeeklySummary();
            if (contextDecision.includeWeeklySummary() && weeklySummary != null && !weeklySummary.isBlank()) {
                context.append("\u3010\u4e0a\u5468\u603b\u7ed3\u3011\n").append(weeklySummary.trim()).append("\n\n");
            }
        }
        if (contextDecision.includeEmotionalState() && history != null && history.getEmotionalState() != null && !history.getEmotionalState().isBlank()) {
            context.append("\u3010\u7528\u6237\u5f53\u524d\u60c5\u7eea\u72b6\u6001\uff1a").append(history.getEmotionalState()).append("\u3011\n");
        }
        if (extraSystemNote != null && !extraSystemNote.isBlank()) {
            context.append("\u3010\u7cfb\u7edf\u8bf4\u660e\u3011\n").append(extraSystemNote).append("\n");
        }
        context.append("\u3010\u5f53\u524d\u7528\u6237\u6d88\u606f\u3011\n").append(currentMessage);
        return context.toString();
    }

    private String buildTodayRecordReply(User user, UserRecord userRecord) {
        String daySection;
        boolean hasRecord;
        if (user == null) {
            return "\u6682\u65e0\u4eca\u65e5\u8bb0\u5f55";
        }
        boolean bl = hasRecord = this.hasStructuredDietRecord(user.getId()) || this.hasStructuredExerciseRecord(user.getId());
        if (hasRecord) {
            String aiSummary = this.summarizeDailyRecord(user.getId(), user, null);
            return aiSummary != null ? aiSummary : "\u4eca\u5929\u6682\u65e0\u8bb0\u5f55\uff0c\u8fd0\u52a8\u6216\u996e\u98df\u540e\u544a\u8bc9\u6211\uff0c\u6211\u5e2e\u4f60\u8bb0\u4e0b\u6765~";
        }
        String trainingPlanText = this.buildTrainingPlanText(user.getId());
        if (trainingPlanText != null && !trainingPlanText.isBlank() && (daySection = this.buildTrainingPlanDaySection(user.getId(), 0)) != null) {
            String aiSummary = this.summarizeDailyRecord(user.getId(), user, daySection);
            return aiSummary != null ? aiSummary : "\u4eca\u5929\u6682\u65e0\u8bb0\u5f55\uff0c\u8fd9\u662f\u4eca\u5929\u7684\u8bad\u7ec3\u5b89\u6392\uff1a\n" + daySection;
        }
        return "\u4eca\u5929\u6682\u65e0\u8bb0\u5f55\uff0c\u8fd0\u52a8\u6216\u996e\u98df\u540e\u544a\u8bc9\u6211\uff0c\u6211\u5e2e\u4f60\u8bb0\u4e0b\u6765~";
    }

    private String buildTodayDietReply(Long userId) {
        if (userId == null) {
            return "\u4eca\u5929\u6682\u65e0\u996e\u98df\u8bb0\u5f55\u3002";
        }
        LocalDate today = LocalDate.now(CN_ZONE);
        List<Map<String, Object>> dietRecords = this.dietRecordService.listLegacyRecords(userId, today);
        if (dietRecords.isEmpty()) {
            return "\u4eca\u5929\u6682\u65e0\u996e\u98df\u8bb0\u5f55\u3002";
        }
        StringBuilder sb = new StringBuilder("\u3010\u4eca\u65e5\u996e\u98df\u8bb0\u5f55\u3011\n");
        for (Map<String, Object> item : dietRecords) {
            String mealType = this.normalizeMealType(this.toCleanString(item.get("mealType")));
            String name = this.toCleanString(item.get("name"));
            String calories = item.get("calories") == null ? "" : String.valueOf(item.get("calories")) + "kcal";
            String protein = item.get("protein") != null ? String.format("%.0fg", ((Number)item.get("protein")).doubleValue()) : "";
            String time = this.toCleanString(item.get("time"));
            sb.append("\u9910\u6b21\uff1a").append(mealType.isBlank() ? "\u672a\u5206\u7c7b" : mealType).append("\uff5c").append(calories.isBlank() ? "-" : calories);
            if (!protein.isEmpty()) {
                sb.append("\uff5c\u86cb\u767d\u8d28").append(protein);
            }
            if (!time.isBlank()) {
                sb.append("\uff5c").append(time);
            }
            sb.append("\n");
            sb.append("\u98df\u7269\uff1a").append(name.isBlank() ? "\u672a\u547d\u540d\u98df\u7269" : name).append("\n");
        }
        String macroSummary = this.buildMacroSummaryText(userId, today);
        if (!macroSummary.isEmpty()) {
            sb.append("\n").append(macroSummary);
        }
        return sb.toString().trim();
    }

    private String buildTodayExerciseReply(Long userId) {
        if (userId == null) {
            return "\u4eca\u5929\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55\u3002";
        }
        LocalDate today = LocalDate.now(CN_ZONE);
        List<Map<String, Object>> sessions = this.exerciseRecordService.listLegacyRecords(userId, today);
        if (sessions.isEmpty()) {
            return "\u4eca\u5929\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55\u3002";
        }
        StringBuilder sb = new StringBuilder("\u3010\u4eca\u65e5\u8bad\u7ec3\u8bb0\u5f55\u3011\n");
        for (Map<String, Object> session : sessions) {
            String sessionName = this.toCleanString(session.get("name"));
            Integer sessionDuration = this.parseInteger(session.get("durationSeconds"));
            Integer sessionCalories = this.parseInteger(session.get("caloriesBurned"));
            List<Map<String, Object>> items = this.castObjectList(session.get("items"));
            if (sessionName.isBlank() && items.isEmpty()) continue;
            sb.append("\u8bad\u7ec3\uff1a").append(sessionName.isBlank() ? "\u672a\u547d\u540d\u8bad\u7ec3" : sessionName);
            if (sessionDuration != null && sessionDuration > 0) {
                sb.append("\uff5c").append(Math.max(1, sessionDuration / 60)).append("\u5206\u949f");
            }
            if (sessionCalories != null && sessionCalories > 0) {
                sb.append("\uff5c\u6d88\u8017").append(sessionCalories).append("\u5927\u5361");
            }
            sb.append("\uff5c").append(items.size()).append("\u4e2a\u52a8\u4f5c").append("\n");
            for (Map<String, Object> item : items) {
                String name = this.toCleanString(item.get("name"));
                String muscleGroup = this.toCleanString(item.get("muscleGroup"));
                Integer durationSeconds = this.parseInteger(item.get("durationSeconds"));
                Integer completedSets = this.parseInteger(item.get("completedSets"));
                if (name.isBlank()) continue;
                sb.append("\u52a8\u4f5c\uff1a").append(name);
                if (!muscleGroup.isBlank()) {
                    sb.append("\uff5c").append(muscleGroup);
                }
                if (completedSets != null) {
                    sb.append("\uff5c").append(completedSets).append("\u7ec4");
                }
                if (durationSeconds != null && durationSeconds > 0) {
                    sb.append("\uff5c").append(Math.max(1, durationSeconds / 60)).append("\u5206\u949f");
                }
                sb.append("\n");
            }
        }
        int totalBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
        if (totalBurned > 0) {
            sb.append("\n\u4eca\u65e5\u7d2f\u8ba1\u6d88\u8017\uff1a").append(totalBurned).append("\u5927\u5361");
        }
        return sb.toString().trim();
    }

    private String buildMacroSummaryText(Long userId, LocalDate date) {
        Map<String, Object> macro = this.dietRecordService.getDayMacroSummary(userId, date);
        boolean hasRecord = Boolean.TRUE.equals(macro.get("hasRecord"));
        if (!hasRecord) {
            return "";
        }
        int calories = (Integer)macro.get("calories");
        double protein = (Double)macro.get("protein");
        double carbs = (Double)macro.get("carbs");
        double fat = (Double)macro.get("fat");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\u4eca\u65e5\u5df2\u6444\u5165\uff1a\u70ed\u91cf%dkcal\uff0c\u86cb\u767d\u8d28%.0fg\uff0c\u78b3\u6c34%.0fg\uff0c\u8102\u80aa%.0fg", calories, protein, carbs, fat));
        try {
            UserProfile profile = this.userProfileService.getByUserId(userId);
            if (profile != null) {
                double target;
                double d = profile.getCustomDailyCalories() != null && profile.getCustomDailyCalories() > 0.0 ? profile.getCustomDailyCalories() : (target = profile.getDailyCalorieBurn() != null ? profile.getDailyCalorieBurn() : 0.0);
                if (target > 0.0) {
                    int remaining = (int)Math.round(target - (double)calories);
                    sb.append(String.format("\uff08\u76ee\u6807%.0fkcal\uff0c%s%.0fkcal\uff09", target, remaining >= 0 ? "\u8fd8\u5dee" : "\u8d85\u51fa", Math.abs(remaining)));
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return sb.toString();
    }

    private String buildWeekRecordReply(User user, UserRecord userRecord) {
        if (user == null) {
            return "\u6682\u65e0\u672c\u5468\u8bb0\u5f55";
        }
        if (userRecord != null && userRecord.getWeeklyReviews() != null && !userRecord.getWeeklyReviews().isBlank()) {
            List<String> reviews = this.parseJsonArray(userRecord.getWeeklyReviews());
            if (reviews.isEmpty()) {
                return "\u672c\u5468\u6682\u65e0\u8bb0\u5f55";
            }
            StringBuilder sb = new StringBuilder("\u3010\u672c\u5468\u8bb0\u5f55\u3011\n");
            for (String review : reviews) {
                sb.append(review).append("\n\n");
            }
            return sb.toString().trim();
        }
        return "\u672c\u5468\u6682\u65e0\u8bb0\u5f55\uff0c\u8fd0\u52a8\u6216\u996e\u98df\u540e\u544a\u8bc9\u6211\uff0c\u6211\u5e2e\u4f60\u8bb0\u4e0b\u6765~";
    }

    private WebClient getWebClient(AiModelConfig.ModelProvider provider) {
        Map<String, WebClient> cache = CACHED_WEB_CLIENTS.get();
        String key = provider.getBaseUrl() + "|" + provider.getName();
        return cache.computeIfAbsent(key, k -> WebClient.builder().baseUrl(provider.getBaseUrl()).defaultHeader("Authorization", new String[]{"Bearer " + provider.getApiKey()}).defaultHeader("Content-Type", new String[]{"application/json"}).build());
    }

    private String callAiText(String systemPrompt, String userMessage, int maxTokens, double temperature) {
        try {
            AiModelConfig.ModelProvider provider = this.requireActiveProvider();
            WebClient webClient = this.getWebClient(provider);
            HashMap<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", List.of(Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", userMessage)));
            Map response = (Map)((WebClient.RequestBodySpec)webClient.post().uri("/chat/completions", new Object[0])).bodyValue(requestBody).retrieve().bodyToMono(Map.class).block();
            if (response == null) {
                return "";
            }
            List choices = (List)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "";
            }
            Map msgObj = (Map)((Map)choices.get(0)).get("message");
            String content = msgObj == null ? null : (String)msgObj.get("content");
            return content == null ? "" : content.trim();
        }
        catch (Exception e) {
            log.error("AI\u6587\u672c\u8c03\u7528\u5931\u8d25", (Throwable)e);
            return "";
        }
    }

    private String callAiSingle(String userMessage, int maxTokens, double temperature) {
        try {
            AiModelConfig.ModelProvider provider = this.requireActiveProvider();
            WebClient webClient = this.getWebClient(provider);
            HashMap<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", userMessage)));
            Map response = (Map)((WebClient.RequestBodySpec)webClient.post().uri("/chat/completions", new Object[0])).bodyValue(requestBody).retrieve().bodyToMono(Map.class).block();
            if (response == null) {
                return null;
            }
            List choices = (List)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map msgObj = (Map)((Map)choices.get(0)).get("message");
            return msgObj == null ? null : (String)msgObj.get("content");
        }
        catch (Exception e) {
            log.error("AI\u5355\u6d88\u606f\u8c03\u7528\u5931\u8d25", (Throwable)e);
            return null;
        }
    }

    private String resolveMealType(String msg) {
        String meal;
        Matcher matcher = DIET_SINGLE_MEAL_PATTERN.matcher(msg);
        if (!matcher.find()) {
            return null;
        }
        return switch (meal = matcher.group(1)) {
            case "\u5348\u996d" -> "\u5348\u9910";
            case "\u665a\u996d" -> "\u665a\u9910";
            case "\u591c\u5bb5", "\u5bb5\u591c" -> "\u52a0\u9910";
            default -> meal;
        };
    }

    private String getCurrentMealType() {
        int hour = LocalTime.now(CN_ZONE).getHour();
        if (hour < 10) {
            return "\u65e9\u9910";
        }
        if (hour < 14) {
            return "\u5348\u9910";
        }
        if (hour < 17) {
            return "\u52a0\u9910";
        }
        if (hour < 21) {
            return "\u665a\u9910";
        }
        return "\u52a0\u9910";
    }

    private String buildTrainingPlanText(Long userId) {
        if (userId == null) {
            return null;
        }
        String cached = CACHED_TRAINING_PLAN_TEXT.get();
        if (cached != null) {
            return cached;
        }
        UserTrainingCycleVO activeCycle = this.userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map templateMap = this.userTrainingTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("\u8bad\u7ec3\u8ba1\u5212\n");
        for (int i = 1; i <= activeCycle.getDayCount(); ++i) {
            int dayIndex = i;
            UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex).findFirst().orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("\u661f\u671f").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("\uff1a");
            if (day == null || day.getTemplateId() == null) {
                sb.append("\u4f11\u606f\n");
                continue;
            }
            UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                sb.append(day.getTemplateName() == null ? "\u4f11\u606f" : day.getTemplateName()).append("\n");
                continue;
            }
            sb.append(template.getName()).append("\n");
            this.appendTrainingSection(sb, "\u70ed\u8eab", "warmup", template.getItems());
            this.appendTrainingSection(sb, "\u6b63\u5f0f\u8bad\u7ec3", "main", template.getItems());
            this.appendTrainingSection(sb, "\u62c9\u4f38", "stretch", template.getItems());
        }
        String result = sb.toString().trim();
        CACHED_TRAINING_PLAN_TEXT.set(result);
        return result;
    }

    private String buildTrainingPlanDaySection(Long userId, int offsetDays) {
        if (userId == null) {
            return null;
        }
        UserTrainingCycleVO activeCycle = this.userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getTodayIndex() == null || activeCycle.getDayCount() == null || activeCycle.getDayCount() <= 0 || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map templateMap = this.userTrainingTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int dayCount = activeCycle.getDayCount();
        int zeroBased = Math.floorMod(activeCycle.getTodayIndex() - 1 + offsetDays, dayCount);
        int targetIndex = zeroBased + 1;
        UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex).findFirst().orElse(null);
        DayOfWeek targetDayOfWeek = LocalDate.now(CN_ZONE).plusDays(offsetDays).getDayOfWeek();
        String title = "\u661f\u671f" + DAY_NAMES[targetDayOfWeek.getValue() - 1];
        if (day == null || day.getTemplateId() == null) {
            return title + "\uff08\u4f11\u606f\u65e5\uff09\n\u4f11\u606f";
        }
        UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return title + "\uff08\u4f11\u606f\u65e5\uff09\n\u4f11\u606f";
        }
        String body = this.buildTrainingTemplateBody(template.getItems());
        String muscleLabel = this.inferTrainingDayMuscleLabel(template);
        StringBuilder sb = new StringBuilder(title).append("\uff08").append(muscleLabel.isBlank() ? this.safeTrim(template.getName()) : muscleLabel).append("\uff09");
        if (!body.isBlank()) {
            sb.append("\n").append(body);
        }
        return sb.toString().trim();
    }

    private void appendTrainingSection(StringBuilder sb, String label, String sectionType, List<UserTrainingTemplateVO.TrainingItemVO> items) {
        List<UserTrainingTemplateVO.TrainingItemVO> sectionItems = items.stream().filter(item -> sectionType.equalsIgnoreCase(this.safeTrim(item.getSectionType()))).sorted(Comparator.comparing(UserTrainingTemplateVO.TrainingItemVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))).toList();
        if (sectionItems.isEmpty()) {
            return;
        }
        sb.append(label).append("\uff1a");
        ArrayList<String> parts = new ArrayList<String>();
        for (UserTrainingTemplateVO.TrainingItemVO item2 : sectionItems) {
            StringBuilder part = new StringBuilder(this.safeTrim(item2.getExerciseName()));
            ArrayList<Object> detailParts = new ArrayList<Object>();
            if (item2.getRecommendedSets() != null) {
                detailParts.add(item2.getRecommendedSets() + "\u7ec4");
            }
            if (item2.getRecommendedReps() != null && !item2.getRecommendedReps().isBlank()) {
                detailParts.add(item2.getRecommendedReps());
            }
            if (!detailParts.isEmpty()) {
                part.append("\uff08").append(String.join((CharSequence)"\uff0c", detailParts)).append("\uff09");
            }
            parts.add(part.toString());
        }
        sb.append(String.join((CharSequence)"\u3001", parts)).append("\n");
    }

    private String buildTrainingTemplateBody(List<UserTrainingTemplateVO.TrainingItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        this.appendTrainingSection(sb, "\u70ed\u8eab", "warmup", items);
        this.appendTrainingSection(sb, "\u6b63\u5f0f\u8bad\u7ec3", "main", items);
        this.appendTrainingSection(sb, "\u62c9\u4f38", "stretch", items);
        return sb.toString().trim();
    }

    private String inferTrainingDayMuscleLabel(UserTrainingTemplateVO template) {
        String dominantGroup;
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return "";
        }
        Map groupCount = template.getItems().stream().map(UserTrainingTemplateVO.TrainingItemVO::getMuscleGroup).map(this::normalizePlanMuscleGroup).filter(group -> !this.isBlank((String)group)).collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        if (groupCount.isEmpty()) {
            return "";
        }
        return switch (dominantGroup = groupCount.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("")) {
            case "chest" -> "\u80f8\u90e8";
            case "back" -> "\u80cc\u90e8";
            case "shoulders" -> "\u80a9\u90e8";
            case "arms" -> "\u624b\u81c2";
            case "legs" -> "\u817f\u90e8";
            case "core" -> "\u6838\u5fc3";
            default -> "";
        };
    }

    private String buildDietPlanText(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDietCycleVO activeCycle = this.userDietCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map dayTemplateMap = this.userDietDayTemplateService.listDayTemplates(userId).stream().collect(Collectors.toMap(UserDietDayTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map mealTemplateMap = this.userDietTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("\u4e00\u65e5\u98df\u8c31\u63a8\u8350\n");
        for (int i = 1; i <= activeCycle.getDayCount(); ++i) {
            int dayIndex = i;
            UserDietCycleVO.CycleDayVO day = activeCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex).findFirst().orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("\u661f\u671f").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("\uff1a");
            if (day == null || day.getDayTemplateId() == null) {
                sb.append("\u672a\u5b89\u6392\n");
                continue;
            }
            UserDietDayTemplateVO dayTemplate = (UserDietDayTemplateVO)dayTemplateMap.get(day.getDayTemplateId());
            sb.append(dayTemplate == null ? "\u672a\u5b89\u6392" : dayTemplate.getName()).append("\n");
            if (dayTemplate == null || dayTemplate.getMealSlots() == null) continue;
            for (String mealType : PLAN_MEAL_ORDER) {
                UserDietTemplateVO mealTemplate;
                UserDietDayTemplateVO.MealSlotVO slot = dayTemplate.getMealSlots().stream().filter(meal -> mealType.equals(this.normalizeMealType(meal.getMealType()))).findFirst().orElse(null);
                if (slot == null || slot.getTemplateId() == null || (mealTemplate = (UserDietTemplateVO)mealTemplateMap.get(slot.getTemplateId())) == null || mealTemplate.getItems() == null || mealTemplate.getItems().isEmpty()) continue;
                sb.append(mealType).append("\uff1a").append(mealTemplate.getName()).append("\n");
                for (UserDietTemplateVO.DietTemplateItemVO item : mealTemplate.getItems()) {
                    sb.append("- ").append(this.safeTrim(item.getFoodName())).append(" ").append(item.getAmount() == null ? "" : item.getAmount().stripTrailingZeros().toPlainString()).append(this.safeTrim(item.getUnit())).append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String extractDietMealSection(String dietPlan, String mealType) {
        if (dietPlan == null || dietPlan.isBlank() || this.isBlank(mealType)) {
            return null;
        }
        LinkedHashMap<String, LinkedHashMap<String, List<String>>> planByDay = new LinkedHashMap<String, LinkedHashMap<String, List<String>>>();
        String currentDay = null;
        String currentMeal = null;
        for (String rawLine : dietPlan.split("\\r?\\n")) {
            String normalizedMeal;
            String line = this.safeTrim(rawLine);
            if (line.isBlank() || "\u4e00\u65e5\u98df\u8c31\u63a8\u8350".equals(line)) continue;
            if (line.startsWith("\u661f\u671f")) {
                currentDay = line;
                currentMeal = null;
                planByDay.putIfAbsent(currentDay, new LinkedHashMap());
                continue;
            }
            if (currentDay == null) continue;
            int colonIndex = line.indexOf(65306);
            if (colonIndex < 0) {
                colonIndex = line.indexOf(58);
            }
            if (colonIndex > 0 && !(normalizedMeal = this.normalizeMealType(line.substring(0, colonIndex))).isBlank()) {
                currentMeal = normalizedMeal;
                planByDay.get(currentDay).putIfAbsent(currentMeal, new ArrayList());
                planByDay.get(currentDay).get(currentMeal).add(line);
                continue;
            }
            if (currentMeal == null) continue;
            planByDay.get(currentDay).putIfAbsent(currentMeal, new ArrayList());
            planByDay.get(currentDay).get(currentMeal).add(line);
        }
        if (planByDay.isEmpty()) {
            return null;
        }
        String todayKey = "\u661f\u671f" + DAY_NAMES[LocalDate.now(CN_ZONE).getDayOfWeek().getValue() - 1];
        String todaySection = this.findDietMealSectionForDay(planByDay, todayKey, mealType);
        if (todaySection != null) {
            return todaySection;
        }
        for (String dayKey : planByDay.keySet()) {
            String fallback = this.findDietMealSectionForDay(planByDay, dayKey, mealType);
            if (fallback == null) continue;
            return fallback;
        }
        return null;
    }

    private String findDietMealSectionForDay(LinkedHashMap<String, LinkedHashMap<String, List<String>>> planByDay, String dayKey, String mealType) {
        if (planByDay == null || this.isBlank(dayKey) || this.isBlank(mealType)) {
            return null;
        }
        LinkedHashMap<String, List<String>> meals = planByDay.get(dayKey);
        if (meals == null || meals.isEmpty()) {
            return null;
        }
        String normalizedMeal = this.normalizeMealType(mealType);
        List<String> lines = meals.get(normalizedMeal);
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        return (dayKey + "\n" + String.join((CharSequence)"\n", lines)).trim();
    }

    private String extractDaySection(String trainingPlan, String dayName) {
        Pattern p = Pattern.compile("(?:\u661f\u671f" + dayName + "|\u5468" + dayName + ")[\uff1a:\\s]*(.*?)(?=(?:\u661f\u671f[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5]|\u5468[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5])|$)", 32);
        Matcher m = p.matcher(trainingPlan);
        if (m.find()) {
            String section = m.group(1).trim();
            return section.isEmpty() ? null : section;
        }
        return null;
    }

    private PlanFilterConditions extractPlanFilterConditions(String planIntent, String userMessage, User user, ChatHistory history) {
        try {
            String content;
            Map msg;
            List choices;
            String userInfo = this.buildUserInfo(user);
            StringBuilder recentUserMsgs = new StringBuilder();
            if (history != null && history.getPendingMessages() != null && !history.getPendingMessages().isBlank()) {
                int startIdx;
                List<String> pendingItems = this.parseJsonArray(history.getPendingMessages());
                for (int i = startIdx = Math.max(0, pendingItems.size() - 5); i < pendingItems.size(); ++i) {
                    String[] lines;
                    String item = pendingItems.get(i);
                    for (String line : lines = item.split("\n")) {
                        if (!line.startsWith("\u7528\u6237\uff1a")) continue;
                        recentUserMsgs.append(line.substring(3)).append("\n");
                    }
                }
            }
            String historyContext = recentUserMsgs.length() > 0 ? "\u3010\u8fd1\u671f\u5bf9\u8bdd\u8bb0\u5f55\uff08\u7528\u6237\u6d88\u606f\uff09\u3011\n" + String.valueOf(recentUserMsgs) + "\n" : "";
            String prompt = "training_plan".equals(planIntent) ? "\u6839\u636e\u7528\u6237\u6d88\u606f\u3001\u8fd1\u671f\u5bf9\u8bdd\u548c\u7528\u6237\u4fe1\u606f\uff0c\u63d0\u53d6\u8bad\u7ec3\u8ba1\u5212\u7684\u5668\u68b0\u7b5b\u9009\u6761\u4ef6\u3002\n\n" + historyContext + "\u7528\u6237\u4fe1\u606f\uff1a" + userInfo + "\n\n\u7528\u6237\u6d88\u606f\uff1a" + userMessage + "\n\n\u8bf7\u4e25\u683c\u6309\u4ee5\u4e0bJSON\u683c\u5f0f\u8fd4\u56de\uff0c\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u5176\u4ed6\u5185\u5bb9\uff1a\n{\"equipment\": \"\u5668\u68b0\u6761\u4ef6\"}\n\nequipment\u586b\u5199\u89c4\u5219\uff1a\n1. \u5982\u679c\u7528\u6237\u660e\u786e\u8bf4\u5f92\u624b/\u65e0\u5668\u68b0/\u81ea\u91cd\u8bad\u7ec3\uff0c\u586b \"\u5f92\u624b\"\n2. \u5982\u679c\u7528\u6237\u8bf4\u53ea\u6709\u67d0\u79cd\u5668\u68b0\uff08\u5982\u53ea\u6709\u54d1\u94c3\u3001\u53ea\u6709\u5f39\u529b\u5e26\uff09\uff0c\u586b\u8be5\u5668\u68b0\u540d\u79f0\uff08\u9017\u53f7\u5206\u9694\uff09\n3. \u5982\u679c\u7528\u6237\u8bf4\u4e0d\u80fd\u4f7f\u7528\u67d0\u79cd\u5668\u68b0\uff08\u5982\u6ca1\u6709\u6760\u94c3\u3001\u53bb\u4e0d\u4e86\u5065\u8eab\u623f\uff09\uff0c\u4ece\u7528\u6237\u753b\u50cf\u5668\u68b0\u4e2d\u6392\u9664\u4e0d\u53ef\u7528\u7684\uff0c\u586b\u5269\u4f59\u53ef\u7528\u5668\u68b0\n4. \u5982\u679c\u7528\u6237\u6ca1\u63d0\u5230\u5668\u68b0\u9650\u5236\uff0c\u586b \"null\"\uff08\u76f4\u63a5\u586bnull\u4e09\u4e2a\u5b57\u6bcd\uff09\uff0c\u5c06\u4f7f\u7528\u7528\u6237\u753b\u50cf\u4e2d\u7684\u5668\u68b0\u8bbe\u7f6e\n5. \u6ce8\u610f\u7528\u6237\u753b\u50cf\u4e2d\u7684\u8bad\u7ec3\u6c34\u5e73\uff08\u65b0\u624b/\u4e2d\u7ea7/\u9ad8\u7ea7\uff09\uff0c\u7b5b\u9009\u65f6\u8981\u914d\u5408\u5176\u6c34\u5e73\n6. \u4e0d\u8981\u81ea\u5df1\u7f16\u9020\u5668\u68b0\uff0c\u53ea\u6839\u636e\u7528\u6237\u6d88\u606f\u548c\u7528\u6237\u4fe1\u606f\u63a8\u65ad" : "\u6839\u636e\u7528\u6237\u6d88\u606f\u3001\u8fd1\u671f\u5bf9\u8bdd\u548c\u7528\u6237\u4fe1\u606f\uff0c\u63d0\u53d6\u996e\u98df\u8ba1\u5212\u7684\u98df\u7269\u7b5b\u9009\u6761\u4ef6\u3002\n\n" + historyContext + "\u7528\u6237\u4fe1\u606f\uff1a" + userInfo + "\n\n\u7528\u6237\u6d88\u606f\uff1a" + userMessage + "\n\n\u8bf7\u4e25\u683c\u6309\u4ee5\u4e0bJSON\u683c\u5f0f\u8fd4\u56de\uff0c\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u5176\u4ed6\u5185\u5bb9\uff1a\n{\"foodCategory\": \"\u5206\u7c7b\u6761\u4ef6\"}\n\nfoodCategory\u586b\u5199\u89c4\u5219\uff1a\n1. \u5982\u679c\u7528\u6237\u8bf4\u4e0d\u4f1a\u505a\u996d/\u4e0d\u60f3\u505a\u996d/\u53ea\u80fd\u5403\u5373\u98df\uff0c\u586b \"\u5373\u98df\"\n2. \u5982\u679c\u7528\u6237\u63d0\u5230\u996e\u98df\u504f\u597d\uff08\u5982\u51cf\u8102\u3001\u4f4e\u78b3\u6c34\u3001\u9ad8\u86cb\u767d\uff09\uff0c\u586b\u5bf9\u5e94\u7684\u98df\u7269\u5206\u7c7b\u504f\u597d\n3. \u5982\u679c\u7528\u6237\u6ca1\u63d0\u5230\u996e\u98df\u9650\u5236\uff0c\u586b \"null\"\uff08\u76f4\u63a5\u586bnull\u4e09\u4e2a\u5b57\u6bcd\uff09\n4. \u53ea\u6839\u636e\u7528\u6237\u6d88\u606f\u63a8\u65ad\uff0c\u4e0d\u8981\u81ea\u5df1\u7f16\u9020";
            AiModelConfig.ModelProvider provider = this.requireProvider(this.resolvePurificationModel(user));
            HashMap<String, Object> requestBody = new HashMap<String, Object>();
            requestBody.put("model", provider.getModel());
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            WebClient webClient = WebClient.builder().baseUrl(provider.getBaseUrl()).defaultHeader("Authorization", new String[]{"Bearer " + provider.getApiKey()}).defaultHeader("Content-Type", new String[]{"application/json"}).build();
            Map response = (Map)((WebClient.RequestBodySpec)webClient.post().uri("/chat/completions", new Object[0])).bodyValue(requestBody).retrieve().bodyToMono(Map.class).block();
            if (response != null && (choices = (List)response.get("choices")) != null && !choices.isEmpty() && (msg = (Map)((Map)choices.get(0)).get("message")) != null && (content = (String)msg.get("content")) != null && !content.isBlank()) {
                String foodCategory;
                content = content.trim().replaceAll("^[\\s\\S]*?\\{", "{").replaceAll("\\}[\\s\\S]*$", "}");
                Map parsed = (Map)JSON_MAPPER.readValue(content, (TypeReference)new TypeReference<Map<String, Object>>(){});
                String equipment = "training_plan".equals(planIntent) ? this.getAsString(parsed.get("equipment")) : null;
                String string = foodCategory = "diet_plan".equals(planIntent) ? this.getAsString(parsed.get("foodCategory")) : null;
                if ("null".equals(equipment)) {
                    equipment = null;
                }
                if ("null".equals(foodCategory)) {
                    foodCategory = null;
                }
                log.info("AI\u63d0\u53d6\u7b5b\u9009\u6761\u4ef6: planIntent={}, equipment={}, foodCategory={}", new Object[]{planIntent, equipment, foodCategory});
                return new PlanFilterConditions(equipment, foodCategory);
            }
        }
        catch (Exception e) {
            log.warn("\u63d0\u53d6\u8ba1\u5212\u7b5b\u9009\u6761\u4ef6\u5931\u8d25\uff0c\u4f7f\u7528\u9ed8\u8ba4\u503c: {}", (Object)e.getMessage());
        }
        return new PlanFilterConditions(null, null);
    }

    private String getAsString(Object obj) {
        if (obj == null) {
            return null;
        }
        String s = String.valueOf(obj).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private String handlePlanGeneration(String message, User user, ChatHistory history, OutputStream outputStream, StringBuilder resultHolder, UserRecord userRecord, MessageRoutingDecision routingDecision, String postRecordSystemNote) {
        String planIntent;
        String trainingPlanText;
        String msg = message.toLowerCase();
        String userInfo = this.buildUserInfo(user);
        PromptContextDecision promptContext = routingDecision == null ? this.defaultPromptContextDecision() : routingDecision.promptContext();
        Object contextAwareMessage = message;
        if (promptContext.includeTrainingPlanContext() && user != null && (trainingPlanText = this.buildTrainingPlanText(user.getId())) != null && !trainingPlanText.isBlank()) {
            contextAwareMessage = message + "\n\n\u5f53\u524d\u5df2\u6709\u8bad\u7ec3\u8ba1\u5212\u53c2\u8003\uff1a\n" + trainingPlanText;
        }
        String string = planIntent = routingDecision == null ? "none" : routingDecision.planGenerationIntent();
        if (planIntent.isBlank() || "none".equals(planIntent)) {
            return null;
        }
        PlanFilterConditions filterConditions = this.extractPlanFilterConditions(planIntent, message, user, history);
        if ("diet_plan".equals(planIntent)) {
            String foodKnowledge = this.buildFoodCatalog(user != null ? user.getId() : null, filterConditions.foodCategory());
            String mealRequestInstruction = this.buildDietMealRequestInstruction(msg);
            String prompt = DIET_PLAN_GEN_PROMPT.replace("{userInfo}", userInfo).replace("{foodKnowledge}", foodKnowledge != null ? foodKnowledge : "\u65e0").replace("{mealRequestInstruction}", mealRequestInstruction);
            String aiReply = this.callAiApiStream(prompt, (String)contextAwareMessage, outputStream, null, 900);
            return this.sanitizeDietPlanOutput(aiReply, this.resolveMealType(msg));
        }
        if ("training_plan".equals(planIntent)) {
            String trainingPlanText2;
            String exerciseCatalog = this.buildExerciseCatalog(user != null ? user.getId() : null, filterConditions.equipment());
            String weatherContext = this.getWeatherContextAsync();
            if (weatherContext != null) {
                contextAwareMessage = (String)contextAwareMessage + "\n\n" + weatherContext + "\n\u8bf7\u53c2\u8003\u5929\u6c14\u60c5\u51b5\u5408\u7406\u5b89\u6392\u5ba4\u5185/\u5ba4\u5916\u8bad\u7ec3\u3002";
            }
            Object existingPlan = "";
            if (promptContext.includeTrainingPlanContext() && user != null && (trainingPlanText2 = this.buildTrainingPlanText(user.getId())) != null && !trainingPlanText2.isBlank()) {
                existingPlan = "\n\u3010\u7528\u6237\u5f53\u524d\u5df2\u6709\u7684\u8bad\u7ec3\u8ba1\u5212\uff08\u7528\u6237\u53cd\u9988\u65f6\u5728\u6b64\u57fa\u7840\u4e0a\u8c03\u6574\uff09\u3011\n" + trainingPlanText2 + "\n";
            }
            String prompt = TRAINING_PLAN_GEN_PROMPT.replace("{userInfo}", userInfo).replace("{exerciseCatalog}", exerciseCatalog).replace("{existingPlan}", (CharSequence)existingPlan);
            String aiReply = this.callAiApiStream(prompt, (String)contextAwareMessage, outputStream, null, 1200);
            return this.sanitizeTrainingPlanOutput(aiReply, this.shouldForceDefaultTrainingWeek(msg));
        }
        return null;
    }

    private void securityCheck(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u6d88\u606f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (message.length() > 2000) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u6d88\u606f\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc72000\u5b57");
        }
        if (Pattern.compile("<script.*?>.*?</script>", 2).matcher(message).find()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u8f93\u5165\u5305\u542b\u975e\u6cd5\u5185\u5bb9");
        }
        if (Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE)\\b.*\\b(FROM|INTO|TABLE|DATABASE|WHERE)\\b)", 2).matcher(message).find()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u8f93\u5165\u5305\u542b\u975e\u6cd5\u5185\u5bb9");
        }
    }

    private String retrieveKnowledgeDirectly(String userMessage) {
        try {
            List results = this.vectorStore.similaritySearch(SearchRequest.builder().query(userMessage).topK(3).similarityThreshold(0.35).build());
            if (results == null || results.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Document doc : results) {
                sb.append(doc.getText()).append("\n");
            }
            return sb.toString().trim();
        }
        catch (Exception e) {
            log.warn("RAG \u68c0\u7d22\u5931\u8d25: {}", (Object)e.getMessage());
            return null;
        }
    }

    private String buildUserInfo(User user) {
        UserProfile p;
        if (user == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\u3010\u7528\u6237\u3011" + user.getUsername() + "|");
        String gender = user.getGender() != null ? (user.getGender() == 0 ? "\u5973" : "\u7537") : "\u672a\u77e5";
        sb.append(gender);
        if (user.getAge() != null) {
            sb.append("|").append(user.getAge()).append("\u5c81");
        }
        if (user.getHeight() != null) {
            sb.append("|").append(user.getHeight()).append("cm");
        }
        if (user.getWeight() != null) {
            sb.append("|").append(user.getWeight()).append("kg");
        }
        if ((p = this.userProfileService.getByUserId(user.getId())) != null) {
            if (p.getFitnessGoal() != null) {
                sb.append("|\u76ee\u6807:").append(p.getFitnessGoal());
            }
            if (p.getTargetWeight() != null) {
                sb.append("|\u76ee\u6807\u4f53\u91cd:").append(p.getTargetWeight()).append("kg");
            }
            if (p.getActivityLevel() != null) {
                sb.append("|\u6d3b\u52a8\u6c34\u5e73:").append(p.getActivityLevel());
            }
            if (p.getCustomDailyCalories() != null) {
                sb.append("|\u76ee\u6807\u70ed\u91cf:").append(p.getCustomDailyCalories().intValue()).append("kcal");
            }
            if (p.getExperienceLevel() != null) {
                sb.append("|\u8bad\u7ec3\u6c34\u5e73:").append(p.getExperienceLevel());
            }
            if (p.getPreferredEquipment() != null) {
                sb.append("|\u5668\u68b0:").append(p.getPreferredEquipment());
            }
            if (p.getWeeklyTrainingDays() != null) {
                sb.append("|\u6bcf\u5468\u7ec3").append(p.getWeeklyTrainingDays()).append("\u5929");
            }
            if (p.getTrainingDuration() != null) {
                sb.append("|\u6bcf\u6b21").append(p.getTrainingDuration()).append("\u5206\u949f");
            }
            if (p.getOccupation() != null) {
                sb.append("|\u804c\u4e1a:").append(p.getOccupation());
            }
            if (p.getPersonality() != null) {
                sb.append("|\u6027\u683c:").append(p.getPersonality());
            }
            if (p.getMedicalHistory() != null) {
                sb.append("|\u4f24\u75c5:").append(p.getMedicalHistory());
            }
            if (p.getDietPreference() != null) {
                sb.append("|\u996e\u98df:").append(p.getDietPreference());
            }
            if (p.getTrainingPreference() != null) {
                sb.append("|\u8bad\u7ec3\u504f\u597d:").append(p.getTrainingPreference());
            }
            if (p.getUserProfileText() != null && !p.getUserProfileText().isBlank()) {
                sb.append("|\u753b\u50cf:").append(p.getUserProfileText());
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private String buildExerciseCatalog(Long userId, String overrideEquipment) {
        UserProfile profile = this.userProfileService.getByUserId(userId);
        String effectiveEquipment = overrideEquipment;
        if (effectiveEquipment == null && profile != null) {
            effectiveEquipment = profile.getPreferredEquipment();
        }
        String effectiveLevel = profile != null ? profile.getExperienceLevel() : null;
        List<Exercise> exercises = this.exerciseService.getByFilters(effectiveEquipment, effectiveLevel);
        User user = (User)this.userService.getById(userId);
        if (exercises == null || exercises.isEmpty()) {
            return "\uff08\u52a8\u4f5c\u5e93\u4e3a\u7a7a\uff09";
        }
        HashSet favIds = new HashSet();
        if (user != null && user.getFavoritesExercises() != null && !user.getFavoritesExercises().isBlank()) {
            try {
                favIds.addAll((Collection)JSON_MAPPER.readValue(user.getFavoritesExercises(), (TypeReference)new TypeReference<Set<Long>>(){}));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (!favIds.isEmpty()) {
            exercises.sort((a, b) -> {
                boolean bFav;
                boolean aFav = favIds.contains(a.getId());
                if (aFav != (bFav = favIds.contains(b.getId()))) {
                    return aFav ? -1 : 1;
                }
                Integer aOrder = a.getSortOrder() == null ? Integer.MAX_VALUE : a.getSortOrder();
                Integer bOrder = b.getSortOrder() == null ? Integer.MAX_VALUE : b.getSortOrder();
                return Integer.compare(aOrder, bOrder);
            });
        }
        Map<String, String> groupMap = Map.of("chest", "\u80f8\u90e8", "back", "\u80cc\u90e8", "core", "\u6838\u5fc3", "arms", "\u624b\u81c2", "legs", "\u817f\u90e8", "shoulders", "\u80a9\u90e8");
        LinkedHashMap mainByGroup = new LinkedHashMap();
        LinkedHashMap warmupByGroup = new LinkedHashMap();
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            mainByGroup.put(group, new ArrayList());
            warmupByGroup.put(group, new ArrayList());
        }
        for (Exercise exercise : exercises) {
            String groupKey;
            if (exercise == null || this.isBlank(exercise.getName()) || this.isBlank(exercise.getMuscleGroup()) || !mainByGroup.containsKey(groupKey = this.normalizePlanMuscleGroup(exercise.getMuscleGroup()))) continue;
            if (this.isWarmupExercise(exercise.getName())) {
                Object wName = favIds.contains(exercise.getId()) ? exercise.getName() + "[\u6536\u85cf]" : exercise.getName();
                this.addDistinctLimited((List)warmupByGroup.get(groupKey), (String)wName, 4);
                continue;
            }
            if (this.isStretchExercise(exercise.getName())) continue;
            Object mName = favIds.contains(exercise.getId()) ? exercise.getName() + "[\u6536\u85cf]" : exercise.getName();
            this.addDistinctLimited((List)mainByGroup.get(groupKey), (String)mName, 6);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\u3010\u5206\u90e8\u4f4d\u8bad\u7ec3\u52a8\u4f5c\u5e93-\u8bad\u7ec3\u52a8\u4f5c\u53ea\u80fd\u4ece\u5bf9\u5e94\u90e8\u4f4d\u4e2d\u9009\u3011\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List actions = (List)mainByGroup.get(group);
            if (actions == null || actions.isEmpty()) continue;
            sb.append(groupMap.getOrDefault(group, group)).append("\u8bad\u7ec3\u52a8\u4f5c\uff1a").append(String.join((CharSequence)"\u3001", actions)).append("\n");
        }
        sb.append("\n\u3010\u5206\u90e8\u4f4d\u70ed\u8eab\u52a8\u4f5c-\u70ed\u8eab\u53ea\u80fd\u4ece\u5f53\u5929\u8bad\u7ec3\u90e8\u4f4d\u5bf9\u5e94\u7684\u70ed\u8eab\u52a8\u4f5c\u4e2d\u9009\u3011\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List warmups = (List)warmupByGroup.get(group);
            if (warmups == null || warmups.isEmpty()) continue;
            sb.append(groupMap.getOrDefault(group, group)).append("\u70ed\u8eab\u52a8\u4f5c\uff1a").append(String.join((CharSequence)"\u3001", warmups)).append("\n");
        }
        return sb.toString();
    }

    private String normalizePlanMuscleGroup(String muscleGroup) {
        if (muscleGroup == null) {
            return "";
        }
        return switch (muscleGroup.trim().toLowerCase()) {
            case "\u80f8\u90e8", "chest" -> "chest";
            case "\u80cc\u90e8", "back" -> "back";
            case "\u80a9\u90e8", "shoulder", "shoulders" -> "shoulders";
            case "\u624b\u81c2", "arm", "arms" -> "arms";
            case "\u817f\u90e8", "leg", "legs" -> "legs";
            case "\u6838\u5fc3", "core" -> "core";
            default -> muscleGroup.trim().toLowerCase();
        };
    }

    private boolean isWarmupExercise(String name) {
        return !this.isBlank(name) && name.contains("\u70ed\u8eab");
    }

    private boolean isStretchExercise(String name) {
        return !this.isBlank(name) && name.contains("\u62c9\u4f38");
    }

    private void addDistinctLimited(List<String> target, String value, int limit) {
        if (target == null || this.isBlank(value) || target.size() >= limit || target.contains(value)) {
            return;
        }
        target.add(value.trim());
    }

    private String buildFoodCatalog(Long userId, String overrideCategory) {
        List<FoodItem> foods = this.foodItemService.searchVisibleFoods(userId, "");
        if (foods == null || foods.isEmpty()) {
            return "\u65e0";
        }
        UserProfile profile = this.userProfileService.getByUserId(userId);
        ArrayList<FoodItem> myFoods = new ArrayList<FoodItem>();
        ArrayList<FoodItem> systemFoods = new ArrayList<FoodItem>();
        for (FoodItem food : foods) {
            if (food.getCreatedBy() != null && food.getCreatedBy().equals(userId) && !Integer.valueOf(1).equals(food.getIsSystem())) {
                myFoods.add(food);
                continue;
            }
            systemFoods.add(food);
        }
        String effectiveCategory = overrideCategory;
        if ((effectiveCategory == null || effectiveCategory.isBlank() || "null".equals(effectiveCategory)) && profile != null) {
            effectiveCategory = profile.getDietPreference();
        }
        List<FoodItem> filteredSystem = systemFoods;
        if (effectiveCategory != null && !effectiveCategory.isBlank() && !"null".equals(effectiveCategory)) {
            HashSet<String> allowedCategories = new HashSet<String>();
            for (String cat : effectiveCategory.split("[,\uff0c]")) {
                String c = cat.trim();
                if (c.isEmpty()) continue;
                allowedCategories.add(c);
            }
            if (!allowedCategories.isEmpty()) {
                filteredSystem = systemFoods.stream().filter(f -> {
                    String cat = f.getCategory() == null ? "" : f.getCategory();
                    String name = f.getName() == null ? "" : f.getName();
                    return allowedCategories.stream().anyMatch(allowed -> cat.contains((CharSequence)allowed) || name.contains((CharSequence)allowed));
                }).toList();
            }
        }
        ArrayList<FoodItem> selected = new ArrayList<FoodItem>();
        selected.addAll(myFoods);
        selected.addAll(filteredSystem.stream().limit(Math.max(1, 12 - myFoods.size())).toList());
        StringBuilder sb = new StringBuilder();
        sb.append("\u3010\u98df\u7269\u8425\u517b\u53c2\u8003\u3011\u540d\u79f0|\u5206\u7c7b|\u57fa\u51c6|\u70ed\u91cf(kcal)|\u86cb\u767d\u8d28(g)|\u78b3\u6c34(g)|\u8102\u80aa(g)\n");
        for (FoodItem food : selected) {
            boolean isMine = food.getCreatedBy() != null && food.getCreatedBy().equals(userId) && !Integer.valueOf(1).equals(food.getIsSystem());
            Object name = isMine ? this.safeTrim(food.getName()) + "[\u6211\u7684\u98df\u7269]" : this.safeTrim(food.getName());
            String baseAmount = food.getBaseAmount() == null ? "-" : food.getBaseAmount().stripTrailingZeros().toPlainString();
            double calKcal = food.getCalories() != null ? food.getCalories().doubleValue() / 4.184 : 0.0;
            double protein = food.getProtein() != null ? food.getProtein().doubleValue() : 0.0;
            double carbs = food.getCarbs() != null ? food.getCarbs().doubleValue() : 0.0;
            double fat = food.getFat() != null ? food.getFat().doubleValue() : 0.0;
            sb.append((String)name).append("|").append(this.safeTrim(food.getCategory())).append("|").append(baseAmount).append(this.safeTrim(food.getUnit())).append("|").append(Math.round(calKcal)).append("|").append((double)Math.round(protein * 10.0) / 10.0).append("|").append((double)Math.round(carbs * 10.0) / 10.0).append("|").append((double)Math.round(fat * 10.0) / 10.0).append("\n");
        }
        return sb.toString().trim();
    }

    private String callAiApiStream(String systemPrompt, String userMessage, OutputStream outputStream, String modelName) {
        return this.callAiApiStream(systemPrompt, userMessage, outputStream, modelName, this.aiModelConfig.getMaxTokens());
    }

    private String callAiApiStream(String systemPrompt, String userMessage, OutputStream outputStream, String modelName, int maxTokens) {
        try {
            AiModelConfig.ModelProvider provider = this.isBlank(modelName) ? this.requireActiveProvider() : this.requireProvider(modelName);
            return this.callAiApiStreamWithProvider(systemPrompt, userMessage, outputStream, provider, maxTokens);
        }
        catch (BusincessException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("\u6d41\u5f0f\u8c03\u7528AI\u63a5\u53e3\u5931\u8d25", (Throwable)e);
            throw new BusincessException(StateCode.AI_ERROR, "\u5f53\u524dAI\u8c03\u7528\u5931\u8d25\uff0c\u8bf7\u5207\u6362\u522b\u7684AI\u8bd5\u4e00\u8bd5");
        }
    }

    private String callAiApiStreamWithProvider(String systemPrompt, String userMessage, OutputStream outputStream, AiModelConfig.ModelProvider provider, int maxTokens) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        long startedAt = System.currentTimeMillis();
        HashMap<String, Object> requestBody = new HashMap<String, Object>();
        requestBody.put("model", provider.getModel());
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", this.aiModelConfig.getTemperature());
        requestBody.put("stream", true);
        if (provider.getModel() != null && provider.getModel().toLowerCase().contains("qwen3")) {
            requestBody.put("enable_thinking", false);
        }
        requestBody.put("messages", List.of(Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", userMessage)));
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpURLConnection conn = (HttpURLConnection)new URL(provider.getBaseUrl() + "/chat/completions").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + provider.getApiKey());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream();){
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                String content;
                Map delta;
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                Map response = (Map)objectMapper.readValue(data, Map.class);
                List choices = (List)response.get("choices");
                if (choices == null || choices.isEmpty() || (delta = (Map)((Map)choices.get(0)).get("delta")) == null || !delta.containsKey("content") || (content = (String)delta.get("content")) == null || content.isEmpty()) continue;
                fullText.append(content);
                this.writeSseData(outputStream, content);
                if (!this.clientDisconnected) continue;
                log.info("\u3010\u6d41\u5f0f\u3011\u5ba2\u6237\u7aef\u65ad\u8fde\uff0cAI\u7ee7\u7eed\u751f\u6210\u81f3\u5b8c\u6210");
            }
        }
        catch (BusincessException e) {
            throw e;
        }
        finally {
            conn.disconnect();
        }
        return fullText.toString();
    }

    @Override
    public void updateChatHistory(ChatHistory history) {
        this.chatHistoryMapper.updateById(history);
    }

    @Override
    public void createChatHistory(ChatHistory history) {
        this.chatHistoryMapper.insert(history);
    }

    @Override
    @Transactional(rollbackFor={Exception.class})
    public String saveGeneratedPlan(Long userId, String type, String content) {
        if (userId == null || content == null || content.isBlank()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u6ca1\u6709\u53ef\u4fdd\u5b58\u7684\u8ba1\u5212\u5185\u5bb9");
        }
        if ("training".equals(type)) {
            this.saveGeneratedTrainingPlan(userId, content);
            return "\u8bad\u7ec3\u8ba1\u5212\u5df2\u4fdd\u5b58";
        }
        if ("diet".equals(type)) {
            this.saveGeneratedDietPlan(userId, content);
            return "\u996e\u98df\u8ba1\u5212\u5df2\u4fdd\u5b58";
        }
        throw new BusincessException(StateCode.PARAMS_ERROR, "\u4e0d\u652f\u6301\u7684\u8ba1\u5212\u7c7b\u578b");
    }

    private void saveOrUpdateChatHistory(Long userId, String userMessage, String aiResponse) {
        this.profileExtractionService.appendDirtyText(userId, userMessage);
        String emotionalState = this.detectEmotionalState(userMessage);
        ChatHistory existing = this.getChatHistory(userId);
        if (existing != null) {
            existing.setUpdateTime(new Date());
            existing.setMessageCount(existing.getMessageCount() == null ? 1 : existing.getMessageCount() + 1);
            if (emotionalState != null) {
                existing.setEmotionalState(emotionalState);
            }
            List<String> buffer = this.parseJsonArray(existing.getPendingMessages());
            buffer.add("\u7528\u6237\uff1a" + userMessage + "\n\u52a9\u624b\uff1a" + aiResponse);
            if (buffer.size() > 10) {
                buffer = new ArrayList<String>(buffer.subList(buffer.size() - 10, buffer.size()));
            }
            if (existing.getMessageCount() % 10 == 0 && !buffer.isEmpty()) {
                String allMessages = String.join((CharSequence)"\n", buffer);
                String previousSummary = existing.getSummary();
                String summary = this.generateSummary(previousSummary, allMessages);
                if (summary != null && summary.length() > 500) {
                    summary = summary.substring(0, 500);
                }
                existing.setSummary(summary);
            }
            try {
                existing.setPendingMessages(JSON_MAPPER.writeValueAsString(buffer));
            }
            catch (Exception e) {
                log.warn("\u5e8f\u5217\u5316pendingMessages\u5931\u8d25", (Throwable)e);
            }
            this.chatHistoryMapper.updateById(existing);
        } else {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setUserId(userId);
            chatHistory.setMessageCount(1);
            chatHistory.setEmotionalState(emotionalState);
            try {
                chatHistory.setPendingMessages(JSON_MAPPER.writeValueAsString(List.of("\u7528\u6237\uff1a" + userMessage + "\n\u52a9\u624b\uff1a" + aiResponse)));
            }
            catch (Exception e) {
                log.warn("\u5e8f\u5217\u5316pendingMessages\u5931\u8d25", (Throwable)e);
            }
            this.chatHistoryMapper.insert(chatHistory);
        }
    }

    private void saveGeneratedTrainingPlan(Long userId, String content) {
        List<ParsedTrainingDay> days = this.parseTrainingPlanText(content);
        if (days.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u8bad\u7ec3\u8ba1\u5212\u683c\u5f0f\u65e0\u6cd5\u8bc6\u522b");
        }
        ArrayList<String> unresolvedActions = new ArrayList<String>();
        ArrayList<SaveUserTrainingCycleRequest.CycleDayDTO> cycleDays = new ArrayList<SaveUserTrainingCycleRequest.CycleDayDTO>();
        for (int i = 0; i < 7; ++i) {
            ParsedTrainingDay day = i < days.size() ? days.get(i) : new ParsedTrainingDay(i + 1, "\u661f\u671f" + DAY_NAMES[i], "", true, List.of(), List.of(), List.of());
            SaveUserTrainingCycleRequest.CycleDayDTO dayDTO = new SaveUserTrainingCycleRequest.CycleDayDTO();
            dayDTO.setDayIndex(i + 1);
            if (!day.rest()) {
                SaveUserTrainingTemplateRequest.TrainingItemDTO dto;
                Long exerciseId;
                SaveUserTrainingTemplateRequest req = new SaveUserTrainingTemplateRequest();
                String tplName = day.muscleGroup().isBlank() ? "\u8bad\u7ec3\u65e5" + (i + 1) : "AI" + day.muscleGroup() + "\u65e5";
                req.setName(tplName);
                ArrayList<SaveUserTrainingTemplateRequest.TrainingItemDTO> items = new ArrayList<SaveUserTrainingTemplateRequest.TrainingItemDTO>();
                int sort = 0;
                for (String action : day.warmups()) {
                    exerciseId = this.resolveExerciseIdForPlan(action, "warmup", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("warmup");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                for (String action : day.trainings()) {
                    exerciseId = this.resolveExerciseIdForPlan(action, "main", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("main");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                for (String action : day.stretches()) {
                    exerciseId = this.resolveExerciseIdForPlan(action, "stretch", day.muscleGroup());
                    if (exerciseId == null) {
                        unresolvedActions.add(action);
                        continue;
                    }
                    dto = new SaveUserTrainingTemplateRequest.TrainingItemDTO();
                    dto.setSectionType("stretch");
                    dto.setExerciseId(exerciseId);
                    dto.setSortOrder(sort++);
                    items.add(dto);
                }
                if (!items.isEmpty()) {
                    req.setItems(items);
                    Long templateId = this.userTrainingTemplateService.saveTemplate(userId, req);
                    dayDTO.setTemplateId(templateId);
                }
            }
            cycleDays.add(dayDTO);
        }
        if (!unresolvedActions.isEmpty()) {
            log.warn("\u3010\u4fdd\u5b58\u8bad\u7ec3\u8ba1\u5212\u3011\u4ee5\u4e0b\u52a8\u4f5c\u672a\u547d\u4e2d\u52a8\u4f5c\u5e93\uff0c\u5df2\u8df3\u8fc7\uff1a{}", (Object)unresolvedActions.stream().distinct().collect(Collectors.joining("\u3001")));
        }
        String prefix = "AI\u8bad\u7ec3\u5468\u6a21\u677f";
        int maxNum = 0;
        for (UserTrainingCycleVO c : this.userTrainingCycleService.listCycles(userId)) {
            String n = c.getName();
            if (n == null || !n.startsWith(prefix)) continue;
            try {
                maxNum = Math.max(maxNum, Integer.parseInt(n.substring(prefix.length())));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        String cycleName = prefix + (maxNum + 1);
        SaveUserTrainingCycleRequest cycleRequest = new SaveUserTrainingCycleRequest();
        cycleRequest.setName(cycleName);
        cycleRequest.setDayCount(7);
        cycleRequest.setStartDate(LocalDate.now(CN_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        cycleRequest.setActivate(true);
        cycleRequest.setDays(cycleDays);
        this.userTrainingCycleService.saveCycle(userId, cycleRequest);
    }

    private void saveGeneratedDietPlan(Long userId, String content) {
        Map<String, String> meals = this.parseDietPlanText(content);
        if (meals.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u996e\u98df\u8ba1\u5212\u683c\u5f0f\u65e0\u6cd5\u8bc6\u522b");
        }
        LinkedHashMap<String, Long> mealConfig = new LinkedHashMap<String, Long>();
        for (String mealType : PLAN_MEAL_ORDER) {
            List<ParsedFoodAmount> parsedFoods;
            String foodLine = meals.get(mealType);
            if (foodLine == null || foodLine.isBlank() || (parsedFoods = this.parseDietFoodLine(foodLine)).isEmpty()) continue;
            SaveUserDietTemplateRequest req = new SaveUserDietTemplateRequest();
            req.setName("AI" + mealType + "\u6a21\u677f");
            req.setMealType(mealType);
            ArrayList<SaveUserDietTemplateRequest.DietTemplateItemDTO> items = new ArrayList<SaveUserDietTemplateRequest.DietTemplateItemDTO>();
            int sort = 0;
            for (ParsedFoodAmount parsed : parsedFoods) {
                Long foodId = this.resolveFoodIdForPlan(userId, parsed.name(), parsed.amount(), parsed.unit());
                SaveUserDietTemplateRequest.DietTemplateItemDTO dto = new SaveUserDietTemplateRequest.DietTemplateItemDTO();
                dto.setFoodItemId(foodId);
                dto.setAmount(parsed.amount());
                dto.setUnit(parsed.unit());
                dto.setSortOrder(sort++);
                items.add(dto);
            }
            req.setItems(items);
            Long templateId = this.userDietTemplateService.saveTemplate(userId, req);
            mealConfig.put(mealType, templateId);
        }
        if (mealConfig.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u996e\u98df\u8ba1\u5212\u4e2d\u6ca1\u6709\u53ef\u4fdd\u5b58\u7684\u9910\u6b21");
        }
        SaveUserDietDayTemplateRequest dayTemplateRequest = new SaveUserDietDayTemplateRequest();
        dayTemplateRequest.setName("AI\u4e00\u65e5\u996e\u98df");
        dayTemplateRequest.setMealConfig(mealConfig);
        Long dayTemplateId = this.userDietDayTemplateService.saveDayTemplate(userId, dayTemplateRequest);
        String dietPrefix = "AI\u996e\u98df\u5468\u6a21\u677f";
        int dietMaxNum = 0;
        for (UserDietCycleVO c : this.userDietCycleService.listCycles(userId)) {
            String n = c.getName();
            if (n == null || !n.startsWith(dietPrefix)) continue;
            try {
                dietMaxNum = Math.max(dietMaxNum, Integer.parseInt(n.substring(dietPrefix.length())));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        SaveUserDietCycleRequest cycleRequest = new SaveUserDietCycleRequest();
        cycleRequest.setName(dietPrefix + (dietMaxNum + 1));
        cycleRequest.setDayCount(1);
        cycleRequest.setStartDate(LocalDate.now(CN_ZONE));
        cycleRequest.setActivate(true);
        SaveUserDietCycleRequest.CycleDayDTO day = new SaveUserDietCycleRequest.CycleDayDTO();
        day.setDayIndex(1);
        day.setDayTemplateId(dayTemplateId);
        cycleRequest.setDays(List.of(day));
        this.userDietCycleService.saveCycle(userId, cycleRequest);
    }

    private List<ParsedTrainingDay> parseTrainingPlanText(String content) {
        ArrayList<ParsedTrainingDay> result = new ArrayList<ParsedTrainingDay>();
        Pattern pattern = Pattern.compile("\u661f\u671f([\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929])(?:\uff08([^\uff09]*)\uff09)?\\s*(.*?)(?=\u661f\u671f[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929](?:\uff08[^\uff09]*\uff09)?|$)", 32);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String dayChar = matcher.group(1);
            String muscle = this.safeTrim(matcher.group(2));
            String body = this.safeTrim(matcher.group(3));
            String title = "\u661f\u671f" + dayChar + (String)(muscle.isBlank() ? "" : " \u00b7 " + muscle);
            List<String> warmups = List.of();
            List<String> trainings = List.of();
            List<String> stretches = List.of();
            boolean rest = body.contains("\u4f11\u606f");
            if (!rest) {
                warmups = this.parseActionLine(body, "\u70ed\u8eab");
                trainings = this.parseActionLine(body, "\u8bad\u7ec3");
                stretches = this.parseActionLine(body, "\u62c9\u4f38");
            }
            result.add(new ParsedTrainingDay(result.size() + 1, title, muscle, rest, warmups, trainings, stretches));
        }
        return result;
    }

    private Map<String, String> parseDietPlanText(String content) {
        LinkedHashMap<String, String> sections = new LinkedHashMap<String, String>();
        Pattern mdPattern = Pattern.compile("\\|\\s*([^|]*?(?:\u65e9\u9910|\u5348\u9910|\u665a\u9910|\u7ec3\u540e|\u52a0\u9910)[^|]*?)\\s*\\|\\s*(.*?)\\s*\\|");
        Matcher mdMatcher = mdPattern.matcher(content);
        while (mdMatcher.find()) {
            String mealCell = this.safeTrim(mdMatcher.group(1)).replaceAll("[\\p{So}\\p{Sc}]", "").trim();
            String foodCell = this.safeTrim(mdMatcher.group(2));
            String normalizedMeal = this.normalizeMealType(mealCell);
            if (normalizedMeal.isBlank() || foodCell.isBlank()) continue;
            sections.put(normalizedMeal, foodCell);
        }
        if (sections.isEmpty()) {
            String[] lines = content.split("\\r?\\n");
            String currentMeal = null;
            for (String rawLine : lines) {
                String foods;
                String line = this.safeTrim(rawLine);
                if (line.isBlank()) continue;
                String normalizedMeal = this.normalizeMealType(line);
                if (!normalizedMeal.isBlank() && line.equals(normalizedMeal)) {
                    currentMeal = normalizedMeal;
                    continue;
                }
                if (currentMeal == null || !line.startsWith("\u5403\u4ec0\u4e48")) continue;
                int idx = line.indexOf(65306);
                if (idx < 0) {
                    idx = line.indexOf(58);
                }
                String string = foods = idx >= 0 ? this.safeTrim(line.substring(idx + 1)) : "";
                if (foods.isBlank()) continue;
                sections.put(currentMeal, foods);
            }
        }
        return sections;
    }

    private List<String> parseActionLine(String body, String label) {
        Pattern stdPattern = Pattern.compile(label + "[\uff1a:](.*?)(?:\\n|$)");
        Matcher stdMatcher = stdPattern.matcher(body);
        if (stdMatcher.find()) {
            String raw = this.safeTrim(stdMatcher.group(1));
            return this.splitActions(raw);
        }
        Pattern mdPattern = Pattern.compile("\\|\\s*.*" + label + ".*\\|\\s*(.*?)\\s*\\|");
        Matcher mdMatcher = mdPattern.matcher(body);
        if (mdMatcher.find()) {
            String raw = this.safeTrim(mdMatcher.group(1));
            return this.splitActions(raw);
        }
        return List.of();
    }

    private List<String> splitActions(String raw) {
        String[] parts;
        if (raw.isBlank()) {
            return List.of();
        }
        raw = raw.replaceAll("[\uff08(][^\uff09)]*[\uff09)]", "");
        ArrayList<String> result = new ArrayList<String>();
        for (String part : parts = raw.split("[\u3001\uff0c,+\uff0b\uff1b;|]")) {
            String action = this.normalizeActionName(part);
            if (action.isBlank()) continue;
            result.add(action);
        }
        return result;
    }

    private List<ParsedFoodAmount> parseDietFoodLine(String line) {
        String[] parts;
        ArrayList<ParsedFoodAmount> result = new ArrayList<ParsedFoodAmount>();
        for (String rawPart : parts = line.split("[+\uff0b]")) {
            Matcher matcher;
            String part = this.sanitizeDietRecordText(rawPart);
            if (part.isBlank() || !(matcher = Pattern.compile("(.+?)(\\d+(?:\\.\\d+)?)(kg|g|ml|l|\u4e2a|\u7247|\u6839|\u888b|\u4efd|\u53ea|\u679a)$").matcher(part.replaceAll("\\s+", ""))).find()) continue;
            String name = this.sanitizeDietRecordText(matcher.group(1));
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
        String name = this.normalizeActionName(rawName);
        if (name.isBlank()) {
            return null;
        }
        QueryWrapper exact = new QueryWrapper();
        ((QueryWrapper)((QueryWrapper)exact.eq((Object)"isActive", (Object)1)).eq((Object)"name", (Object)name)).last("LIMIT 1");
        Exercise matched = (Exercise)this.exerciseService.getOne((Wrapper)exact, false);
        if (matched == null) {
            QueryWrapper fuzzy = new QueryWrapper();
            ((QueryWrapper)((QueryWrapper)fuzzy.eq((Object)"isActive", (Object)1)).like((Object)"name", (Object)name)).last("LIMIT 5");
            List candidates = this.exerciseService.list((Wrapper)fuzzy);
            matched = candidates.stream().filter(item -> item.getName() != null && (item.getName().contains(name) || name.contains(item.getName()))).findFirst().orElse(null);
        }
        if (matched != null) {
            return matched.getId();
        }
        return null;
    }

    private Exercise findBestExerciseForRecord(String rawName) {
        String normalizedName;
        String name = this.normalizeActionName(rawName);
        if (name.isBlank()) {
            return null;
        }
        QueryWrapper exact = new QueryWrapper();
        ((QueryWrapper)((QueryWrapper)exact.eq((Object)"isActive", (Object)1)).eq((Object)"name", (Object)name)).last("LIMIT 1");
        Exercise matched = (Exercise)this.exerciseService.getOne((Wrapper)exact, false);
        if (matched != null) {
            return matched;
        }
        QueryWrapper fuzzy = new QueryWrapper();
        ((QueryWrapper)((QueryWrapper)fuzzy.eq((Object)"isActive", (Object)1)).like((Object)"name", (Object)name)).last("LIMIT 10");
        List candidates = this.exerciseService.list((Wrapper)fuzzy);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedKeyword = this.normalizeFoodKeyword(name);
        for (Exercise item : candidates) {
            normalizedName = this.normalizeFoodKeyword(item.getName());
            if (!normalizedName.equals(normalizedKeyword)) continue;
            return item;
        }
        for (Exercise item : candidates) {
            normalizedName = this.normalizeFoodKeyword(item.getName());
            if (!normalizedName.contains(normalizedKeyword) && !normalizedKeyword.contains(normalizedName)) continue;
            return item;
        }
        return null;
    }

    private Exercise searchAndSaveExercise(String exerciseName) {
        try {
            Number n;
            Object v;
            Object tipsObj;
            Map map;
            String name;
            WebSearchHelper.SearchExtractResult result = this.webSearchHelper.searchExerciseJsonWithTiming(exerciseName);
            if (result == null || result.content() == null || result.content().isBlank()) {
                return null;
            }
            String json = result.content().trim();
            if (json.startsWith("``")) {
                json = json.replaceAll("^``(?:json)? *", "").replaceAll(" *``$", "");
            }
            if ((name = this.toCleanString((map = (Map)JSON_MAPPER.readValue(json, Map.class)).get("name"))) == null || name.isBlank()) {
                name = exerciseName;
            }
            QueryWrapper check = new QueryWrapper();
            ((QueryWrapper)((QueryWrapper)check.eq((Object)"isActive", (Object)1)).eq((Object)"name", (Object)name)).last("LIMIT 1");
            Exercise existing = (Exercise)this.exerciseService.getOne((Wrapper)check, false);
            if (existing != null) {
                return existing;
            }
            Exercise entity = new Exercise();
            entity.setName(name);
            entity.setMuscleGroup(this.toCleanString(map.getOrDefault("muscleGroup", "core")));
            entity.setEquipment(this.toCleanString(map.get("equipment")));
            entity.setDifficulty(this.toCleanString(map.get("difficulty")));
            Object stepsObj = map.get("steps");
            if (stepsObj instanceof List) {
                entity.setSteps(JSON_MAPPER.writeValueAsString(stepsObj));
            }
            if ((tipsObj = map.get("tips")) instanceof List) {
                entity.setTips(JSON_MAPPER.writeValueAsString(tipsObj));
            }
            if ((v = map.get("recommendedSets")) instanceof Number) {
                n = (Number)v;
                entity.setRecommendedSets(n.intValue());
            }
            entity.setRecommendedReps(this.toCleanString(map.get("recommendedReps")));
            v = map.get("restSeconds");
            if (v instanceof Number) {
                n = (Number)v;
                entity.setRestSeconds(n.intValue());
            }
            entity.setVideoUrl(this.toCleanString(map.get("videoUrl")));
            entity.setIsActive(1);
            entity.setSortOrder(0);
            this.exerciseService.save(entity);
            log.info("[WebSearch][Exercise] \u65b0\u52a8\u4f5c\u5165\u5e93: name={}, muscleGroup={}", (Object)name, (Object)entity.getMuscleGroup());
            return entity;
        }
        catch (Exception e) {
            log.warn("[WebSearch][Exercise] \u641c\u7d22\u5e76\u4fdd\u5b58\u52a8\u4f5c\u5931\u8d25: exerciseName={}, error={}", (Object)exerciseName, (Object)e.getMessage());
            return null;
        }
    }

    private String searchExerciseInfo(String exerciseName) {
        try {
            Object tipsObj;
            WebSearchHelper.SearchExtractResult result = this.webSearchHelper.searchExerciseJsonWithTiming(exerciseName);
            if (result == null || result.content() == null || result.content().isBlank()) {
                return null;
            }
            String json = result.content().trim();
            if (json.startsWith("``")) {
                json = json.replaceAll("^``(?:json)?\\s*", "").replaceAll("\\s*``$", "");
            }
            Map map = (Map)JSON_MAPPER.readValue(json, Map.class);
            StringBuilder sb = new StringBuilder();
            sb.append("\u52a8\u4f5c\u540d\uff1a").append(this.toCleanString(map.getOrDefault("name", exerciseName))).append("\n");
            sb.append("\u8bad\u7ec3\u90e8\u4f4d\uff1a").append(this.toCleanString(map.get("muscleGroup"))).append("\n");
            sb.append("\u6240\u9700\u5668\u68b0\uff1a").append(this.toCleanString(map.get("equipment"))).append("\n");
            sb.append("\u96be\u5ea6\uff1a").append(this.toCleanString(map.get("difficulty"))).append("\n");
            Object stepsObj = map.get("steps");
            if (stepsObj instanceof List) {
                List steps = (List)stepsObj;
                sb.append("\u52a8\u4f5c\u6b65\u9aa4\uff1a\n");
                for (int i = 0; i < steps.size(); ++i) {
                    sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
                }
            }
            if ((tipsObj = map.get("tips")) instanceof List) {
                List tips = (List)tipsObj;
                sb.append("\u6ce8\u610f\u4e8b\u9879\uff1a\n");
                for (Object tip : tips) {
                    sb.append("- ").append(tip).append("\n");
                }
            }
            return sb.toString().trim();
        }
        catch (Exception e) {
            log.warn("[WebSearch][Exercise] \u641c\u7d22\u52a8\u4f5c\u4fe1\u606f\u5931\u8d25: exerciseName={}, error={}", (Object)exerciseName, (Object)e.getMessage());
            return null;
        }
    }

    private String extractExerciseNameFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String name = query.replaceAll("(\u600e\u4e48\u505a|\u600e\u4e48\u7ec3|\u662f\u4ec0\u4e48|\u52a8\u4f5c\u8981\u9886|\u6b63\u786e\u59ff\u52bf|\u6559\u7a0b|\u65b9\u6cd5|\u8bad\u7ec3\u65b9\u6cd5|\u600e\u4e48|\u5982\u4f55|\u662f\u4ec0\u4e48\u610f\u601d|\u6709\u4ec0\u4e48\u7528|\u597d\u4e0d\u597d|\u6548\u679c|\u53ef\u4ee5\u5417|\u80fd.*\u5417).*$", "").trim();
        if (name.isBlank()) {
            return null;
        }
        if (name.length() < 2 || name.length() > 10) {
            return null;
        }
        return name;
    }

    private Long resolveFoodIdForPlan(Long userId, String rawName, BigDecimal amount, String unit) {
        String name = this.safeTrim(rawName);
        if (name.isBlank()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u996e\u98df\u8ba1\u5212\u91cc\u5b58\u5728\u7a7a\u98df\u7269\u540d");
        }
        List<FoodItem> candidates = this.foodItemService.searchVisibleFoods(userId, name);
        FoodItem matched = candidates.stream().filter(item -> item.getName() != null && item.getName().equalsIgnoreCase(name)).findFirst().orElseGet(() -> candidates.stream().filter(item -> item.getName() != null && (item.getName().contains(name) || name.contains(item.getName()))).findFirst().orElse(null));
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
        this.foodItemService.save(entity);
        return entity.getId();
    }

    private String resolveMuscleGroupKey(String muscleGroup, String fallbackName) {
        String text = (this.safeTrim(muscleGroup) + " " + this.safeTrim(fallbackName)).toLowerCase();
        if (text.contains("\u80f8")) {
            return "chest";
        }
        if (text.contains("\u80cc")) {
            return "back";
        }
        if (text.contains("\u817f")) {
            return "legs";
        }
        if (text.contains("\u80a9")) {
            return "shoulders";
        }
        if (text.contains("\u81c2") || text.contains("\u4e8c\u5934") || text.contains("\u4e09\u5934")) {
            return "arms";
        }
        if (text.contains("\u8179") || text.contains("\u6838\u5fc3")) {
            return "core";
        }
        return "core";
    }

    private String normalizeActionName(String raw) {
        String text = this.sanitizeExerciseRecordText(raw);
        text = text.replaceAll("[\uff08(].*?[\uff09)]", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(\u5206\u949f|min|\u79d2|s|\u5c0f\u65f6|h|\u7ec4|\u6b21)$", "");
        text = text.replaceAll("\u9759\u6001\u62c9\u4f38$", "\u62c9\u4f38");
        return this.safeTrim(text);
    }

    private String sanitizeExerciseRecordText(String raw) {
        String text = this.safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = this.stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:\u6211|\u4eca\u5929|\u521a\u521a|\u521a\u624d|\u521a|\u5df2\u7ecf|\u5df2\u7ecf\u5728)?(?:\u505a\u4e86|\u7ec3\u4e86|\u8dd1\u4e86|\u8d70\u4e86|\u9a91\u4e86)", "");
        text = text.replaceAll("^(?:\u8fdb\u884c|\u5b8c\u6210)(?:\u4e86)?", "");
        text = text.replaceAll("[\u3002\uff01!\uff0c,\uff1b;\u3001\\s]+$", "");
        return this.safeTrim(text);
    }

    private String sanitizeDietRecordText(String raw) {
        String text = this.safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = this.stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:\u6211|\u4eca\u5929|\u521a\u521a|\u521a\u624d|\u521a|\u5df2\u7ecf)?(?:\u65e9\u9910|\u5348\u9910|\u5348\u996d|\u665a\u9910|\u665a\u996d|\u52a0\u9910|\u591c\u5bb5|\u5bb5\u591c)?(?:\u5403\u4e86|\u559d\u4e86|\u5403\u8fc7|\u559d\u8fc7)", "");
        text = text.replaceAll("^(?:\u6211|\u4eca\u5929|\u521a\u521a|\u521a\u624d|\u521a|\u5df2\u7ecf)?(?:\u5403\u4e86|\u559d\u4e86|\u5403\u8fc7|\u559d\u8fc7)", "");
        text = text.replaceAll("^(?:\u65e9\u9910|\u5348\u9910|\u5348\u996d|\u665a\u9910|\u665a\u996d|\u52a0\u9910|\u591c\u5bb5|\u5bb5\u591c)[:\uff1a]?", "");
        text = text.replaceAll("[\u3002\uff01!\uff0c,\uff1b;\u3001\\s]+$", "");
        return this.safeTrim(text);
    }

    private String stripLeadingRecordPhrases(String raw) {
        String previous;
        String text = this.safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        do {
            previous = text;
            text = text.replaceAll("^(?:\u8bf7|\u9ebb\u70e6|\u5e2e\u5fd9)?(?:\u5e2e\u6211|\u7ed9\u6211)?(?:\u8bb0\u5f55\u4e00\u4e0b|\u8bb0\u5f55\u4e0b|\u8bb0\u5f55|\u8bb0\u4e00\u4e0b|\u8bb0\u4e0b|\u8bb0\u4e00\u7b14|\u4fdd\u5b58\u4e00\u4e0b|\u4fdd\u5b58\u4e0b)\\s*", "");
        } while (!previous.equals(text = text.replaceAll("^(?:\u8bf7|\u9ebb\u70e6|\u5e2e\u5fd9)?(?:\u628a|\u5c06)?\\s*", "")));
        return this.safeTrim(text);
    }

    private Integer extractCompletedSets(String exerciseText) {
        String text = this.safeTrim(exerciseText);
        if (text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*\u7ec4").matcher(text);
        if (matcher.find()) {
            return this.parseInteger(matcher.group(1));
        }
        return null;
    }

    private Integer extractDurationSeconds(String exerciseText) {
        String text = this.safeTrim(exerciseText);
        if (text.isBlank()) {
            return null;
        }
        Matcher hourMinuteMatcher = Pattern.compile("(\\d{1,2})\\s*(?:\u5c0f\u65f6|h|H)\\s*(\\d{1,2})?\\s*(?:\u5206\u949f|min)?").matcher(text);
        if (hourMinuteMatcher.find()) {
            int hours = Integer.parseInt(hourMinuteMatcher.group(1));
            int minutes = hourMinuteMatcher.group(2) == null ? 0 : Integer.parseInt(hourMinuteMatcher.group(2));
            return hours * 3600 + minutes * 60;
        }
        Matcher minuteMatcher = Pattern.compile("(\\d{1,4})\\s*(?:\u5206\u949f|min)").matcher(text);
        if (minuteMatcher.find()) {
            return Integer.parseInt(minuteMatcher.group(1)) * 60;
        }
        Matcher secondMatcher = Pattern.compile("(\\d{1,5})\\s*(?:\u79d2\u949f|\u79d2|s|S)").matcher(text);
        if (secondMatcher.find()) {
            return Integer.parseInt(secondMatcher.group(1));
        }
        return null;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateSummary(String userMessage, String aiResponse) {
        try {
            String prompt = "\u603b\u7ed3\u4ee5\u4e0b\u5bf9\u8bdd\uff0c\u4fdd\u7559\u9700\u6c42\u3001\u5efa\u8bae\u8981\u70b9\u3001\u540e\u7eed\u5173\u6ce8\u70b9\uff0c\u2264200\u5b57\uff0c\u53bb\u6389\u5bd2\u6684\u3002\n\u7528\u6237\uff1a" + userMessage + "\n\u52a9\u624b\uff1a" + aiResponse;
            String result = this.callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("\u751f\u6210\u5bf9\u8bdd\u603b\u7ed3\u5931\u8d25", (Throwable)e);
        }
        StringBuilder fallback = new StringBuilder("\u5bf9\u8bdd\u6458\u8981(\u81ea\u52a8\u751f\u6210)\uff1a\n");
        String[] lines = aiResponse.split("\n");
        for (int i = 0; i < lines.length && fallback.length() < 400; ++i) {
            String line = lines[i];
            if (!line.startsWith("\u7528\u6237\uff1a") && !line.startsWith("\u52a9\u624b\uff1a")) continue;
            fallback.append(line, 0, Math.min(line.length(), 60)).append("...\n");
        }
        return fallback.toString();
    }

    private boolean hasStructuredDietRecord(Long userId) {
        return userId != null && this.dietRecordService.hasRecord(userId, LocalDate.now(CN_ZONE));
    }

    private boolean hasStructuredExerciseRecord(Long userId) {
        return userId != null && this.exerciseRecordService.hasRecord(userId, LocalDate.now(CN_ZONE));
    }

    private String buildTodaySummaryExerciseInput(Long userId) {
        List<Map> sessions;
        List<Map> list = sessions = userId == null ? Collections.emptyList() : this.exerciseRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (sessions.isEmpty()) {
            return "\u65e0\u8bad\u7ec3\u8bb0\u5f55";
        }
        StringBuilder sb = new StringBuilder();
        for (Map session : sessions) {
            String sessionName = this.toCleanString(session.get("name"));
            Integer sessionDuration = this.parseInteger(session.get("durationSeconds"));
            if (!sessionName.isBlank()) {
                sb.append("\u8bad\u7ec3\uff1a").append(sessionName);
                if (sessionDuration != null && sessionDuration > 0) {
                    sb.append("\uff0c").append(Math.max(1, sessionDuration / 60)).append("\u5206\u949f");
                }
                sb.append("\n");
            }
            for (Map<String, Object> item : this.castObjectList(session.get("items"))) {
                String name = this.toCleanString(item.get("name"));
                String muscleGroup = this.toCleanString(item.get("muscleGroup"));
                Integer completedSets = this.parseInteger(item.get("completedSets"));
                Integer durationSeconds = this.parseInteger(item.get("durationSeconds"));
                if (name.isBlank()) continue;
                sb.append("- ").append(name);
                if (!muscleGroup.isBlank()) {
                    sb.append("\uff08").append(muscleGroup).append("\uff09");
                }
                if (completedSets != null) {
                    sb.append("\uff0c").append(completedSets).append("\u7ec4");
                }
                if (durationSeconds != null && durationSeconds > 0) {
                    sb.append("\uff0c").append(Math.max(1, durationSeconds / 60)).append("\u5206\u949f");
                }
                sb.append("\n");
            }
        }
        return sb.length() == 0 ? "\u65e0\u8bad\u7ec3\u8bb0\u5f55" : sb.toString().trim();
    }

    private String buildTodaySummaryDietInput(Long userId) {
        List<Map> dietRecords;
        List<Map> list = dietRecords = userId == null ? Collections.emptyList() : this.dietRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (dietRecords.isEmpty()) {
            return "\u65e0\u996e\u98df\u8bb0\u5f55";
        }
        StringBuilder sb = new StringBuilder();
        for (Map item : dietRecords) {
            String mealType = this.normalizeMealType(this.toCleanString(item.get("mealType")));
            String name = this.toCleanString(item.get("name"));
            if (name.isBlank()) continue;
            sb.append("- ");
            if (!mealType.isBlank()) {
                sb.append(mealType).append("\uff1a");
            }
            sb.append(name);
            sb.append("\n");
        }
        return sb.length() == 0 ? "\u65e0\u996e\u98df\u8bb0\u5f55" : sb.toString().trim();
    }

    private String summarizeDailyRecord(Long userId, User user, String todayPlanSection) {
        boolean hasExerciseRecord = this.hasStructuredExerciseRecord(userId);
        boolean hasDietRecord = this.hasStructuredDietRecord(userId);
        boolean noUserRecord = !hasExerciseRecord && !hasDietRecord;
        try {
            String userInfo = this.buildUserInfo(user);
            String prompt = noUserRecord ? "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u7528\u6237\u4eca\u5929\u6ca1\u6709\u8bb0\u5f55\u4efb\u4f55\u8fd0\u52a8\u548c\u996e\u98df\uff0c\u4f46\u7cfb\u7edf\u67e5\u5230\u4e86\u4eca\u5929\u7684\u8bad\u7ec3\u8ba1\u5212\u3002\n\n\u8bf7\u751f\u6210\u4e00\u4efd\u4e0e\u6570\u636e\u5e93\u5b58\u50a8\u683c\u5f0f\u4e00\u81f4\u7684\u63d0\u9192\u603b\u7ed3\u3002\n\u7eaf\u6587\u672c\u4e0d\u7528markdown\uff0c\u603b\u5b57\u6570\u9650\u5236\u5728220\u5b57\u4ee5\u5185\uff0c\u4e25\u683c\u6309\u4ee5\u4e0b\u683c\u5f0f\u8f93\u51fa\uff1a\n\u603b\u7ed3\uff1a\u4e00\u53e5\u8bdd\u6982\u62ec\u4eca\u5929\u5e94\u5b8c\u6210\u7684\u5b89\u6392\u3002\n\u5efa\u8bae\uff1a\u7ed9\u51fa1\u6761\u6700\u503c\u5f97\u6267\u884c\u7684\u5efa\u8bae\uff0c\u5e76\u81ea\u7136\u5e26\u4e0a\u201c\u4eca\u5929\u8fd8\u6ca1\u8bb0\u5f55\u54e6\u201d\u3002\n\u8bad\u7ec3\uff1a\u7b80\u8981\u5217\u51fa\u4eca\u5929\u5e94\u5b8c\u6210\u7684\u8bad\u7ec3\u5185\u5bb9\u3002\n\u996e\u98df\uff1a\u5199\u201c\u6682\u65e0\u996e\u98df\u8bb0\u5f55\u201d\u3002\n\u95ee\u9898\uff1a\u8bf4\u660e\u65e0\u6cd5\u786e\u8ba4\u662f\u5426\u6309\u8ba1\u5212\u5b8c\u6210\u3002\n\n" + (String)(userInfo.isBlank() ? "" : userInfo + "\n") + "\u3010\u4eca\u5929\u7684\u8bad\u7ec3\u8ba1\u5212\u3011\n" + (todayPlanSection == null ? "" : todayPlanSection.trim()) : "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u8bf7\u6839\u636e\u7528\u6237\u4eca\u5929\u7684\u7ed3\u6784\u5316\u8fd0\u52a8/\u996e\u98df\u8bb0\u5f55\uff0c\u751f\u6210\u4e00\u4efd\u4e0e\u6570\u636e\u5e93\u5b58\u50a8\u683c\u5f0f\u4e00\u81f4\u7684\u4eca\u65e5\u603b\u7ed3\u3002\n\n\u7eaf\u6587\u672c\u4e0d\u7528markdown\uff0c\u603b\u5b57\u6570\u9650\u5236\u5728220\u5b57\u4ee5\u5185\uff0c\u4e25\u683c\u6309\u4ee5\u4e0b\u683c\u5f0f\u8f93\u51fa\uff1a\n\u603b\u7ed3\uff1a\u4e00\u53e5\u8bdd\u6982\u62ec\u4eca\u5929\u72b6\u6001\u3002\n\u5efa\u8bae\uff1a\u7ed9\u51fa1\u6761\u6700\u503c\u5f97\u6267\u884c\u7684\u5efa\u8bae\u3002\n\u8bad\u7ec3\uff1a\u6982\u62ec\u4eca\u5929\u8bad\u7ec3\u5185\u5bb9\uff0c\u6ca1\u6709\u5219\u5199\u201c\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55\u201d\u3002\n\u996e\u98df\uff1a\u6982\u62ec\u4eca\u5929\u996e\u98df\u60c5\u51b5\uff0c\u6ca1\u6709\u5219\u5199\u201c\u6682\u65e0\u996e\u98df\u8bb0\u5f55\u201d\u3002\n\u95ee\u9898\uff1a\u5982\u65e0\u660e\u663e\u95ee\u9898\u53ef\u5199\u201c\u65e0\u201d\u3002\n\n\u91cd\u8981\u7ea6\u675f\uff1a\n1. \u53ea\u8981\u3010\u7ed3\u6784\u5316\u8bad\u7ec3\u8bb0\u5f55\u3011\u91cc\u6709\u52a8\u4f5c\u540d\uff0c\u5c31\u4e0d\u80fd\u5199\u201c\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55\u201d\u3002\n2. \u8bad\u7ec3\u8bb0\u5f55\u91cc\u6ca1\u6709\u65f6\u957f\uff0c\u4e0d\u4ee3\u8868\u6ca1\u6709\u8bad\u7ec3\uff1b\u53ea\u6709\u52a8\u4f5c\u540d\u3001\u808c\u7fa4\u3001\u7ec4\u6570\u4e5f\u5c5e\u4e8e\u6709\u6548\u8bad\u7ec3\u8bb0\u5f55\u3002\n3. \u4e0d\u8981\u56e0\u4e3a\u90e8\u5206\u8bad\u7ec3\u6ca1\u6709\u65f6\u957f\uff0c\u5c31\u5199\u201c0\u5206\u949f\u5f02\u5e38\u201d\u201c\u6570\u636e\u65e0\u6548\u201d\u201c\u6682\u65e0\u8bad\u7ec3\u8bb0\u5f55\u201d\u8fd9\u7c7b\u7ed3\u8bba\u3002\n4. \u4f18\u5148\u4f9d\u636e\u7ed3\u6784\u5316\u8bb0\u5f55\u603b\u7ed3\uff0c\u4e0d\u8981\u88ab\u65e7\u7684\u81ea\u7136\u8bed\u8a00\u6d41\u6c34\u8bef\u5bfc\u3002\n5. \u5982\u679c\u540c\u7c7b\u8bad\u7ec3\u6216\u540c\u4e00\u9910\u660e\u663e\u91cd\u590d\u51fa\u73b0\uff0c\u4f18\u5148\u505a\u5408\u5e76\u6982\u62ec\uff0c\u4e0d\u8981\u673a\u68b0\u9010\u6761\u590d\u8ff0\u3002\n6. \u5982\u679c\u4f60\u5224\u65ad\u5b58\u5728\u91cd\u590d\u8bb0\u5f55\u3001\u7591\u4f3c\u91cd\u590d\u6253\u5361\u3001\u9910\u6b21\u91cd\u590d\u7b49\u60c5\u51b5\uff0c\u53ef\u4ee5\u5728\u201c\u95ee\u9898\uff1a\u201d\u91cc\u7b80\u77ed\u6307\u51fa\uff0c\u4f46\u4e0d\u8981\u5938\u5927\u6210\u9519\u8bef\u3002\n7. \u201c\u8bad\u7ec3\uff1a\u201d\u548c\u201c\u996e\u98df\uff1a\u201d\u8981\u5199\u6210\u6982\u62ec\u540e\u7684\u81ea\u7136\u8bed\u8a00\uff0c\u4e0d\u8981\u7b80\u5355\u6284\u539f\u59cb\u5217\u8868\u3002\n\n" + (String)(userInfo.isBlank() ? "" : userInfo + "\n") + "\u3010\u7ed3\u6784\u5316\u8bad\u7ec3\u8bb0\u5f55\u3011\n" + this.buildTodaySummaryExerciseInput(userId) + "\n\n\u3010\u7ed3\u6784\u5316\u996e\u98df\u8bb0\u5f55\u3011\n" + this.buildTodaySummaryDietInput(userId);
            String result = this.callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("\u751f\u6210\u4eca\u65e5\u8bb0\u5f55\u603b\u7ed3\u5931\u8d25", (Throwable)e);
        }
        if (noUserRecord) {
            return "\u603b\u7ed3\uff1a\u4eca\u5929\u5e94\u6309\u8ba1\u5212\u5b8c\u6210\u8bad\u7ec3\u5b89\u6392\u3002\n\u5efa\u8bae\uff1a\u4eca\u5929\u8fd8\u6ca1\u8bb0\u5f55\u54e6\uff0c\u7ec3\u5b8c\u8bb0\u5f97\u53ca\u65f6\u6253\u5361\u3002\n\u8bad\u7ec3\uff1a\u8bf7\u53c2\u8003\u4eca\u65e5\u8bad\u7ec3\u8ba1\u5212\u3002\n\u996e\u98df\uff1a\u6682\u65e0\u996e\u98df\u8bb0\u5f55\u3002\n\u95ee\u9898\uff1a\u65e0\u6cd5\u786e\u8ba4\u4eca\u5929\u662f\u5426\u6309\u8ba1\u5212\u5b8c\u6210\u3002";
        }
        return "\u603b\u7ed3\uff1a\u4eca\u5929\u5df2\u6709\u8bb0\u5f55\uff0c\u4f46\u603b\u7ed3\u751f\u6210\u5931\u8d25\u3002\n\u5efa\u8bae\uff1a\u53ef\u4ee5\u7a0d\u540e\u518d\u67e5\u770b\u4e00\u6b21\u4eca\u65e5\u603b\u7ed3\u3002\n\u8bad\u7ec3\uff1a\u8bf7\u67e5\u770b\u4eca\u65e5\u539f\u59cb\u8bb0\u5f55\u3002\n\u996e\u98df\uff1a\u8bf7\u67e5\u770b\u4eca\u65e5\u539f\u59cb\u8bb0\u5f55\u3002\n\u95ee\u9898\uff1a\u6682\u65e0\u53ef\u9760\u7ed3\u6784\u5316\u603b\u7ed3\u3002";
    }

    @Override
    @Transactional(rollbackFor={Exception.class})
    public String quickSaveTodayPlan(Long userId, String type) {
        LocalDate today = LocalDate.now(CN_ZONE);
        String recordTime = LocalTime.now(CN_ZONE).format(TIME_FMT);
        if ("training".equals(type)) {
            UserTrainingCycleVO cycle = this.userTrainingCycleService.getActiveCycle(userId);
            if (cycle == null || cycle.getTodayIndex() == null || cycle.getDays() == null || cycle.getDays().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u8fd8\u6ca1\u6709\u8bad\u7ec3\u8ba1\u5212");
            }
            Map templateMap = this.userTrainingTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            int targetIndex = cycle.getTodayIndex();
            UserTrainingCycleVO.CycleDayVO day = cycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex).findFirst().orElse(null);
            if (day == null || day.getTemplateId() == null) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u4eca\u5929\u662f\u4f11\u606f\u65e5\uff0c\u6ca1\u6709\u8bad\u7ec3\u5b89\u6392");
            }
            UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u4eca\u5929\u7684\u8bad\u7ec3\u8ba1\u5212\u6ca1\u6709\u52a8\u4f5c");
            }
            DayOfWeek dow = today.getDayOfWeek();
            String note = "\u661f\u671f" + DAY_NAMES[dow.getValue() - 1] + " \u00b7 Day " + targetIndex;
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
                this.exerciseRecordService.saveRecord(userId, today, req, "chat");
            }
            return "\u5df2\u8bb0\u5f55" + (template.getName() != null ? template.getName() : "") + "\u8bad\u7ec3";
        }
        if ("diet".equals(type)) {
            UserDietCycleVO dietCycle = this.userDietCycleService.getActiveCycle(userId);
            if (dietCycle == null || dietCycle.getTodayIndex() == null || dietCycle.getDays() == null || dietCycle.getDays().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u8fd8\u6ca1\u6709\u996e\u98df\u8ba1\u5212");
            }
            int targetIndex = dietCycle.getTodayIndex();
            UserDietCycleVO.CycleDayVO day = dietCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex).findFirst().orElse(null);
            if (day == null || day.getDayTemplateId() == null) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u4eca\u5929\u6ca1\u6709\u996e\u98df\u5b89\u6392");
            }
            List<UserDietDayTemplateVO> dayTemplates = this.userDietDayTemplateService.listDayTemplates(userId);
            UserDietDayTemplateVO dayTpl = dayTemplates.stream().filter(t -> t.getId().equals(day.getDayTemplateId())).findFirst().orElse(null);
            if (dayTpl == null || dayTpl.getMealSlots() == null || dayTpl.getMealSlots().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "\u4eca\u5929\u7684\u996e\u98df\u8ba1\u5212\u6ca1\u6709\u9910\u6b21");
            }
            List<UserDietTemplateVO> dietTemplates = this.userDietTemplateService.listTemplates(userId);
            Map dtMap = dietTemplates.stream().collect(Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a));
            int savedMeals = 0;
            for (UserDietDayTemplateVO.MealSlotVO slot : dayTpl.getMealSlots()) {
                UserDietTemplateVO mealTpl;
                if (slot.getTemplateId() == null || (mealTpl = (UserDietTemplateVO)dtMap.get(slot.getTemplateId())) == null || mealTpl.getItems() == null || mealTpl.getItems().isEmpty()) continue;
                AddDietRecordRequest dietReq = new AddDietRecordRequest();
                dietReq.setTime(recordTime);
                dietReq.setMealType(slot.getMealType());
                dietReq.setName(mealTpl.getName());
                dietReq.setCalories(null);
                dietReq.setNote(null);
                dietReq.setSource("chat");
                ArrayList<DietFoodItemRequest> items = new ArrayList<DietFoodItemRequest>();
                for (UserDietTemplateVO.DietTemplateItemVO foodItem : mealTpl.getItems()) {
                    DietFoodItemRequest ir = new DietFoodItemRequest();
                    ir.setFoodItemId(foodItem.getFoodItemId());
                    ir.setAmount(foodItem.getAmount());
                    items.add(ir);
                }
                dietReq.setItems(items);
                this.appendDietRecord(userId, this.userRecordService.getByUserId(userId), dietReq);
                ++savedMeals;
            }
            return "\u5df2\u8bb0\u5f55" + savedMeals + "\u9910\u996e\u98df";
        }
        throw new BusincessException(StateCode.PARAMS_ERROR, "\u65e0\u6548\u7684\u7c7b\u578b");
    }

    @Override
    public String generateUserProfile(User user, String profileFormData) {
        try {
            UserProfile profile = this.userProfileService.getByUserId(user.getId());
            String gender = user.getGender() != null ? (user.getGender() == 0 ? "\u5973" : "\u7537") : "\u672a\u77e5";
            String userBasic = String.format("\u6027\u522b:%s, \u5e74\u9f84:%s\u5c81, \u8eab\u9ad8:%scm, \u4f53\u91cd:%skg, \u5065\u8eab\u76ee\u6807:%s", gender, user.getAge() != null ? user.getAge() : "\u672a\u586b", user.getHeight() != null ? user.getHeight() : "\u672a\u586b", user.getWeight() != null ? user.getWeight() : "\u672a\u586b", profile != null && profile.getFitnessGoal() != null ? profile.getFitnessGoal() : "\u672a\u586b");
            String oldProfile = profile != null && profile.getUserProfileText() != null && !profile.getUserProfileText().isBlank() ? profile.getUserProfileText() : "\u6682\u65e0\u753b\u50cf";
            String prompt = "\u4f60\u662f\u4e00\u4e2aAI\u5065\u8eab\u6559\u7ec3\u7684\u52a9\u624b\u3002\u8bf7\u6839\u636e\u7528\u6237\u7684\u3010\u65e7\u753b\u50cf\u3011\u3001\u3010\u57fa\u672c\u4fe1\u606f\u3011\u548c\u3010\u7528\u6237\u81ea\u586b\u4fe1\u606f\u3011\uff0c\u66f4\u65b0\u7528\u6237\u753b\u50cf\u3002\n\n\u3010\u65e7\u753b\u50cf\u3011" + oldProfile + "\n\u3010\u57fa\u672c\u4fe1\u606f\u3011" + userBasic + "\n\u3010\u7528\u6237\u81ea\u586b\u3011" + profileFormData + "\n\n\u8981\u6c42\uff1a\n- \u628a\u57fa\u672c\u4fe1\u606f\u548c\u81ea\u586b\u4fe1\u606f\u878d\u5408\u5230\u65e7\u753b\u50cf\u4e2d\uff0c\u4e0d\u8981\u5206\u70b9\u7f57\u5217\n- \u4fdd\u7559\u65e7\u753b\u50cf\u4e2d\u4ecd\u7136\u51c6\u786e\u7684\u5185\u5bb9\n- \u7528\u6237\u81ea\u586b\u7684\u4fe1\u606f\u53ef\u80fd\u8868\u8ff0\u4e0d\u6e05\uff0c\u5e2e\u4ed6\u603b\u7ed3\u5230\u4f4d\uff08\u6bd4\u5982\"\u529e\u516c\u5ba4\"\u603b\u7ed3\u4e3a\"\u4e45\u5750\u529e\u516c\"\uff09\n- \u4f24\u75c5/\u996e\u98df\u7981\u5fcc\u5fc5\u987b\u4fdd\u7559\u539f\u610f\uff0c\u4e0d\u80fd\u9057\u6f0f\n- 100\u5b57\u4ee5\u5185\uff0c\u4e0d\u5e9f\u8bdd\n- \u4e0d\u8981\u52a0\u4efb\u4f55\u524d\u7f00\uff0c\u76f4\u63a5\u8f93\u51fa\u753b\u50cf\u5185\u5bb9";
            String result = this.callAiSingle(prompt, 200, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("\u751f\u6210\u7528\u6237\u753b\u50cf\u5931\u8d25", (Throwable)e);
        }
        return profileFormData;
    }

    private String detectEmotionalState(String message) {
        String[] negative = new String[]{"\u575a\u6301\u4e0d", "\u653e\u5f03", "\u592a\u96be\u4e86", "\u7126\u8651", "\u5d29\u6e83", "\u7edd\u671b", "\u6491\u4e0d\u4f4f", "\u5fc3\u6001\u5d29"};
        String[] positive = new String[]{"\u5f00\u5fc3", "\u9ad8\u5174", "\u5174\u594b", "\u671f\u5f85", "\u8fdb\u6b65", "\u6210\u529f", "\u505a\u5230"};
        for (String word : negative) {
            if (!message.contains(word)) continue;
            return "\u4f4e\u843d";
        }
        for (String word : positive) {
            if (!message.contains(word)) continue;
            return "\u79ef\u6781";
        }
        return null;
    }

    private void appendDietRecord(Long userId, UserRecord cachedUserRecord, AddDietRecordRequest body) {
        try {
            List<DietFoodItemRequest> matchedItems;
            String today = LocalDate.now(CN_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String recordTime = this.isBlank(body.getTime()) ? LocalTime.now(CN_ZONE).format(TIME_FMT) : body.getTime();
            body.setName(this.sanitizeDietRecordText(body.getName()));
            String resolvedMealType = this.resolveDietMealTypeForRecord(this.resolveMealTypeFromText(body.getMealType(), body.getName()), recordTime);
            if (this.isBlank(body.getName()) && (body.getItems() == null || body.getItems().isEmpty())) {
                return;
            }
            LocalDate recordDate = LocalDate.parse(today);
            if (!(body.getItems() != null && !body.getItems().isEmpty() || this.isBlank(body.getName()) || (matchedItems = this.tryMatchDietItemsForRecord(userId, body.getName())).isEmpty())) {
                body.setItems(matchedItems);
            }
            if (body.getItems() != null && !body.getItems().isEmpty()) {
                MealNutrition nutrition = this.resolveMealNutrition(body.getItems(), userId);
                this.dietRecordService.saveStructuredRecord(userId, recordDate, recordTime, resolvedMealType, nutrition.summaryName(), nutrition.calories().intValue(), nutrition.protein(), nutrition.carbs(), nutrition.fat(), nutrition.fiber(), body.getNote(), this.isBlank(body.getSource()) ? "chat" : body.getSource(), nutrition.items());
                body.setName(nutrition.summaryName());
                body.setCalories(nutrition.calories().intValue());
            } else {
                this.dietRecordService.saveSimpleRecord(userId, recordDate, recordTime, resolvedMealType, body.getName().trim(), body.getCalories(), body.getNote(), this.isBlank(body.getSource()) ? "chat" : body.getSource());
            }
            User user = (User)this.userService.getById(userId);
            this.userDailyMetricService.syncDailyCalories(userId, recordDate, this.dietRecordService.listLegacyRecords(userId, recordDate), this.resolveTargetCalories(user));
        }
        catch (Exception e) {
            log.error("\u4fdd\u5b58\u996e\u98df\u8bb0\u5f55\u5931\u8d25", (Throwable)e);
        }
    }

    private MealNutrition resolveMealNutrition(List<DietFoodItemRequest> requests, Long userId) {
        List<Long> ids = requests.stream().map(DietFoodItemRequest::getFoodItemId).filter(id -> id != null && id > 0L).distinct().toList();
        if (ids.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "\u98df\u7269\u6570\u636e\u4e0d\u80fd\u4e3a\u7a7a");
        }
        HashMap<Long, FoodItem> foodMap = new HashMap<Long, FoodItem>();
        for (FoodItem foodItem : this.foodItemService.listByIds(ids)) {
            boolean visible;
            if (foodItem == null || !(visible = Integer.valueOf(1).equals(foodItem.getIsSystem()) || foodItem.getCreatedBy() != null && foodItem.getCreatedBy().equals(userId))) continue;
            foodMap.put(foodItem.getId(), foodItem);
        }
        BigDecimal calories = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbs = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        BigDecimal fiber = BigDecimal.ZERO;
        ArrayList<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        ArrayList<String> names = new ArrayList<String>();
        for (DietFoodItemRequest request : requests) {
            FoodItem foodItem = (FoodItem)foodMap.get(request.getFoodItemId());
            if (foodItem == null) {
                throw new BusincessException(StateCode.PARAMS_ERROR, "\u98df\u7269\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u9650\u4f7f\u7528");
            }
            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusincessException(StateCode.PARAMS_ERROR, "\u6444\u5165\u91cf\u5fc5\u987b\u5927\u4e8e0");
            }
            BigDecimal baseAmount = this.defaultIfZero(foodItem.getBaseAmount());
            BigDecimal ratio = amount.divide(baseAmount, 4, RoundingMode.HALF_UP);
            BigDecimal itemCalories = this.multiply(this.kjToKcal(foodItem.getCalories()), ratio);
            BigDecimal itemProtein = this.multiply(foodItem.getProtein(), ratio);
            BigDecimal itemCarbs = this.multiply(foodItem.getCarbs(), ratio);
            BigDecimal itemFat = this.multiply(foodItem.getFat(), ratio);
            BigDecimal itemFiber = this.multiply(foodItem.getFiber(), ratio);
            calories = calories.add(itemCalories);
            protein = protein.add(itemProtein);
            carbs = carbs.add(itemCarbs);
            fat = fat.add(itemFat);
            fiber = fiber.add(itemFiber);
            names.add(foodItem.getName());
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
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
        return new MealNutrition(String.join((CharSequence)"\u3001", names), calories.setScale(0, RoundingMode.HALF_UP), protein.setScale(1, RoundingMode.HALF_UP), carbs.setScale(1, RoundingMode.HALF_UP), fat.setScale(1, RoundingMode.HALF_UP), fiber.setScale(1, RoundingMode.HALF_UP), items);
    }

    private BigDecimal defaultIfZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(100L) : value;
    }

    private BigDecimal multiply(BigDecimal source, BigDecimal ratio) {
        return (source == null ? BigDecimal.ZERO : source).multiply(ratio);
    }

    private BigDecimal kjToKcal(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).divide(KJ_TO_KCAL_DIVISOR, 4, RoundingMode.HALF_UP);
    }

    private void appendExerciseRecord(Long userId, UserRecord cachedUserRecord, AddExerciseRecordRequest body) {
        try {
            Exercise matched;
            String today = LocalDate.now(CN_ZONE).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String recordTime = this.isBlank(body.getTime()) ? LocalTime.now(CN_ZONE).format(TIME_FMT) : body.getTime();
            body.setTime(recordTime);
            body.setExerciseName(this.sanitizeExerciseRecordText(body.getExerciseName()));
            if (body.getCompletedSets() == null) {
                body.setCompletedSets(this.extractCompletedSets(body.getExerciseName()));
            }
            if (body.getDurationSeconds() == null) {
                body.setDurationSeconds(this.extractDurationSeconds(body.getExerciseName()));
            }
            if (this.isBlank(body.getExerciseName())) {
                return;
            }
            if (body.getExerciseId() == null && !this.isBlank(body.getExerciseName()) && (matched = this.findBestExerciseForRecord(body.getExerciseName())) != null) {
                body.setExerciseId(matched.getId());
                body.setExerciseName(matched.getName());
                if (this.isBlank(body.getMuscleGroup())) {
                    body.setMuscleGroup(matched.getMuscleGroup());
                }
            }
            this.exerciseRecordService.saveRecord(userId, LocalDate.parse(today, DateTimeFormatter.ISO_LOCAL_DATE), body, "chat");
        }
        catch (Exception e) {
            log.error("\u4fdd\u5b58\u8fd0\u52a8\u8bb0\u5f55\u5931\u8d25", (Throwable)e);
        }
    }

    private List<Map<String, Object>> parseObjectArray(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<Map<String, Object>>();
        }
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return (List)JSON_MAPPER.readValue(fixed, (TypeReference)new TypeReference<List<Map<String, Object>>>(){});
        }
        catch (Exception e) {
            log.warn("\u89e3\u6790\u5bf9\u8c61\u6570\u7ec4\u5931\u8d25: {}", (Object)json, (Object)e);
            return new ArrayList<Map<String, Object>>();
        }
    }

    private List<Map<String, Object>> castObjectList(Object value) {
        List list;
        if (!(value instanceof List) || (list = (List)value).isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }
        ArrayList<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : list) {
            if (!(item instanceof Map)) continue;
            Map map = (Map)item;
            result.add(map);
        }
        return result;
    }

    private Double resolveTargetCalories(User user) {
        if (user == null) {
            return null;
        }
        UserProfile p = this.userProfileService.getByUserId(user.getId());
        if (p == null) {
            return null;
        }
        Double custom = p.getCustomDailyCalories();
        if (custom != null) {
            return custom;
        }
        return p.getDailyCalorieBurn();
    }

    private String resolveDietMealTypeForRecord(String mealType, String recordTime) {
        if (!this.isBlank(mealType)) {
            return this.normalizeMealType(mealType);
        }
        try {
            LocalTime time = LocalTime.parse(recordTime, TIME_FMT);
            int hour = time.getHour();
            if (hour < 10) {
                return "\u65e9\u9910";
            }
            if (hour < 14) {
                return "\u5348\u9910";
            }
            if (hour < 17) {
                return "\u52a0\u9910";
            }
            if (hour < 21) {
                return "\u665a\u9910";
            }
            return "\u52a0\u9910";
        }
        catch (Exception ignored) {
            return this.getCurrentMealType();
        }
    }

    private String resolveMealTypeFromText(String preferredMealType, String rawText) {
        String normalizedPreferred = this.normalizeLookupMealType(preferredMealType);
        if (!normalizedPreferred.isBlank()) {
            return normalizedPreferred;
        }
        String text = this.safeTrim(rawText);
        if (text.isBlank()) {
            return "";
        }
        if (text.contains("\u7ec3\u540e")) {
            return "\u7ec3\u540e\u9910";
        }
        if (text.contains("\u65e9\u9910")) {
            return "\u65e9\u9910";
        }
        if (text.contains("\u5348\u9910") || text.contains("\u5348\u996d")) {
            return "\u5348\u9910";
        }
        if (text.contains("\u665a\u9910") || text.contains("\u665a\u996d")) {
            return "\u665a\u9910";
        }
        if (text.contains("\u52a0\u9910") || text.contains("\u591c\u5bb5") || text.contains("\u5bb5\u591c")) {
            return "\u52a0\u9910";
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private TrainingPlanLookupDecision parseTrainingPlanLookupDecision(Object raw) {
        if (!(raw instanceof Map)) {
            return new TrainingPlanLookupDecision(false, "", false);
        }
        Map map = (Map)raw;
        return new TrainingPlanLookupDecision(Boolean.TRUE.equals(map.get("shouldLookup")), this.normalizeTrainingLookupTarget(this.toCleanString(map.get("targetDay"))), Boolean.TRUE.equals(map.get("viewAll")));
    }

    private PromptContextDecision parsePromptContextDecision(Object raw) {
        if (!(raw instanceof Map)) {
            return this.defaultPromptContextDecision();
        }
        Map map = (Map)raw;
        return new PromptContextDecision(Boolean.TRUE.equals(map.get("needsRag")), Boolean.TRUE.equals(map.get("includeRecentDialog")), Boolean.TRUE.equals(map.get("includeTodayRecord")), Boolean.TRUE.equals(map.get("includeYesterdaySummary")), Boolean.TRUE.equals(map.get("includeWeeklySummary")), Boolean.TRUE.equals(map.get("includeEmotionalState")), Boolean.TRUE.equals(map.get("includeTrainingPlanContext")), Boolean.TRUE.equals(map.get("includeDietPlanContext")));
    }

    private PromptContextDecision defaultPromptContextDecision() {
        return new PromptContextDecision(true, false, false, false, false, false, false, false);
    }

    private DietPlanLookupDecision parseDietPlanLookupDecision(Object raw) {
        if (!(raw instanceof Map)) {
            return new DietPlanLookupDecision(false, "", false);
        }
        Map map = (Map)raw;
        return new DietPlanLookupDecision(Boolean.TRUE.equals(map.get("shouldLookup")), this.normalizeLookupMealType(this.toCleanString(map.get("mealType"))), Boolean.TRUE.equals(map.get("viewAll")));
    }

    private ExerciseRecordDecision parseExerciseRecordDecision(Object raw, boolean continueChat) {
        Map map;
        if (!(raw instanceof Map) || !Boolean.TRUE.equals((map = (Map)raw).get("shouldRecord"))) {
            return null;
        }
        String exerciseName = this.toCleanString(map.get("exerciseName"));
        if (exerciseName.isBlank()) {
            return null;
        }
        AddExerciseRecordRequest request = new AddExerciseRecordRequest();
        request.setExerciseName(exerciseName);
        request.setMuscleGroup(this.toCleanString(map.get("muscleGroup")));
        request.setCompletedSets(this.parseInteger(map.get("completedSets")));
        request.setDurationSeconds(this.parseInteger(map.get("durationSeconds")));
        request.setNote(this.toCleanString(map.get("note")));
        request.setSource("chat");
        return new ExerciseRecordDecision(request, continueChat);
    }

    private DietRecordDecision parseDietRecordDecision(Object raw, Long userId, boolean continueChat) {
        List list;
        List rawItems;
        Map map;
        if (!(raw instanceof Map) || !Boolean.TRUE.equals((map = (Map)raw).get("shouldRecord"))) {
            return null;
        }
        String name = this.toCleanString(map.get("name"));
        String clarificationMessage = this.toCleanString(map.get("clarificationMessage"));
        Object v = map.get("items");
        List list2 = rawItems = v instanceof List ? (list = (List)v) : Collections.emptyList();
        if (rawItems.isEmpty()) {
            String fallbackMessage = clarificationMessage.isBlank() ? "\u8981\u5e2e\u4f60\u8bb0\u5f55\u996e\u98df\uff0c\u8bf7\u8865\u5145\u5177\u4f53\u98df\u7269\u540d\u548c\u514b\u91cd/\u4efd\u91cf\uff0c\u6216\u8005\u5230\u8bb0\u5f55\u996e\u98df\u754c\u9762\u76f4\u63a5\u9009\u62e9\u98df\u7269\u3002" : clarificationMessage;
            return new DietRecordDecision(null, continueChat, fallbackMessage, "");
        }
        StructuredDietParseResult structuredResult = this.tryBuildStructuredDietRequest(rawItems, userId);
        if (structuredResult.request() != null) {
            structuredResult.request().setMealType(this.normalizeMealType(this.toCleanString(map.get("mealType"))));
            structuredResult.request().setNote(this.toCleanString(map.get("note")));
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
        request.setMealType(this.normalizeMealType(this.toCleanString(map.get("mealType"))));
        request.setCalories(this.parseInteger(map.get("calories")));
        request.setNote(this.toCleanString(map.get("note")));
        request.setSource("chat");
        if (!clarificationMessage.isBlank()) {
            return new DietRecordDecision(null, continueChat, clarificationMessage, "");
        }
        return new DietRecordDecision(request, continueChat, "", "");
    }

    private StructuredDietParseResult tryBuildStructuredDietRequest(List<Map<String, Object>> rawItems, Long userId) {
        if (rawItems == null || rawItems.isEmpty()) {
            return new StructuredDietParseResult(null, "\u8981\u5e2e\u4f60\u8bb0\u5f55\u996e\u98df\uff0c\u8bf7\u8865\u5145\u5177\u4f53\u98df\u7269\u540d\u548c\u514b\u91cd/\u4efd\u91cf\u3002", "");
        }
        ArrayList<DietFoodItemRequest> resolvedItems = new ArrayList<DietFoodItemRequest>();
        ArrayList<String> resolvedNames = new ArrayList<String>();
        boolean hasIncompleteFood = false;
        for (Map<String, Object> rawItem : rawItems) {
            BigDecimal normalizedAmount;
            FoodItem foodItem;
            String action;
            String rawName = this.toCleanString(rawItem.get("name"));
            if (rawName.isBlank()) {
                return new StructuredDietParseResult(null, "\u8fd9\u6761\u996e\u98df\u91cc\u8fd8\u7f3a\u5177\u4f53\u98df\u7269\u540d\uff0c\u8865\u5145\u540e\u6211\u518d\u5e2e\u4f60\u8bb0\u5f55\u3002", "");
            }
            BigDecimal amount = this.parseBigDecimal(rawItem.get("amount"));
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "\u8981\u5e2e\u4f60\u8bb0\u5f55\u201c" + rawName + "\u201d\uff0c\u8fd8\u9700\u8981\u8865\u5145\u514b\u91cd\u6216\u4efd\u91cf\u3002", "");
            }
            BigDecimal fallbackCalories = this.parseBigDecimal(rawItem.get("calories"));
            String rawUnit = this.toCleanString(rawItem.get("unit"));
            String category = this.toCleanString(rawItem.get("category"));
            String calorieUnit = this.toCleanString(rawItem.get("calorieUnit"));
            String caloriesMode = this.toCleanString(rawItem.get("caloriesMode"));
            BigDecimal calorieBaseAmount = this.parseBigDecimal(rawItem.get("calorieBaseAmount"));
            String calorieBaseUnit = this.toCleanString(rawItem.get("calorieBaseUnit"));
            switch (action = this.normalizeDietItemAction(this.toCleanString(rawItem.get("action")))) {
                case "clarify": {
                    FoodItem foodItem2 = null;
                    break;
                }
                case "create_private_total": 
                case "create_private_per_base": {
                    FoodItem foodItem2 = this.handleAiFoodCreateAction(userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit, action, calorieBaseAmount, calorieBaseUnit);
                    break;
                }
                case "use_existing": {
                    FoodItem foodItem2 = this.handleAiFoodUseExistingAction(userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit, caloriesMode, calorieBaseAmount, calorieBaseUnit);
                    break;
                }
                default: {
                    FoodItem foodItem2 = foodItem = this.handleAiFoodUseExistingAction(userId, rawName, amount, rawUnit, category, fallbackCalories, calorieUnit, caloriesMode, calorieBaseAmount, calorieBaseUnit);
                }
            }
            if ("clarify".equals(action)) {
                return new StructuredDietParseResult(null, "\u8981\u5e2e\u4f60\u8bb0\u5f55\u201c" + rawName + "\u201d\uff0c\u8fd8\u9700\u8981\u8865\u5145\u66f4\u5b8c\u6574\u7684\u4fe1\u606f\u3002", "");
            }
            if (foodItem == null) {
                return new StructuredDietParseResult(null, "\u98df\u7269\u5e93\u6682\u65e0\u201c" + rawName + "\u201d\u7684\u6570\u636e\u3002\u5982\u679c\u60f3\u8bb0\u5f55\u70ed\u91cf\u548c\u8425\u517b\u7269\u8d28\uff0c\u53ef\u4ee5\u81ea\u884c\u4e0a\u4f20\u8fd9\u4e2a\u98df\u7269\u7684\u5177\u4f53\u6570\u503c\u54e6\u3002", "");
            }
            if (this.isIncompleteAiFood(foodItem)) {
                hasIncompleteFood = true;
            }
            if ((normalizedAmount = this.normalizeDietAmount(amount, rawUnit, foodItem.getUnit())) == null || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "\u201c" + rawName + "\u201d\u8fd9\u6761\u8fd8\u7f3a\u53ef\u6362\u7b97\u7684\u514b\u91cd/\u6beb\u5347/\u4efd\u91cf\uff0c\u8865\u5145\u540e\u6211\u518d\u5e2e\u4f60\u8bb0\u5f55\u3002", "");
            }
            DietFoodItemRequest requestItem = new DietFoodItemRequest();
            requestItem.setFoodItemId(foodItem.getId());
            requestItem.setAmount(normalizedAmount);
            resolvedItems.add(requestItem);
            resolvedNames.add(foodItem.getName());
        }
        AddDietRecordRequest request = new AddDietRecordRequest();
        request.setName(String.join((CharSequence)"\u3001", resolvedNames));
        request.setItems(resolvedItems);
        String noticeMessage = hasIncompleteFood ? "\u8fd9\u6b21\u6709\u98df\u7269\u662f\u6309\u4f60\u63d0\u4f9b\u7684\u70ed\u91cf\u548c\u5206\u91cf\u65b0\u5efa\u7684\u7b80\u5316\u6570\u636e\uff0c\u6682\u65f6\u4e0d\u80fd\u51c6\u786e\u5224\u65ad\u86cb\u767d\u8d28\u7b49\u8425\u517b\uff1b\u53ef\u4ee5\u53bb\u4e2a\u4eba\u4e2d\u5fc3\u7ba1\u7406\u81ea\u5df1\u521b\u5efa\u7684\u98df\u7269\u8865\u5168\u3002" : "";
        return new StructuredDietParseResult(request, "", noticeMessage);
    }

    private FoodItem createAiFallbackFood(Long userId, String rawName, BigDecimal amount, String rawUnit, String category, BigDecimal caloriesValue, String calorieUnit, String caloriesMode, BigDecimal calorieBaseAmount, String calorieBaseUnit) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedUnit = this.normalizeAmountUnit(rawUnit);
        if (!("g".equals(normalizedUnit) || "ml".equals(normalizedUnit) || "\u4e2a".equals(normalizedUnit) || "\u7247".equals(normalizedUnit) || "\u6839".equals(normalizedUnit) || "\u888b".equals(normalizedUnit) || "\u4efd".equals(normalizedUnit))) {
            return null;
        }
        String normalizedCalorieUnit = this.normalizeCalorieUnit(calorieUnit);
        BigDecimal storedCalories = this.normalizeStoredCalories(caloriesValue, normalizedCalorieUnit);
        if (storedCalories == null || storedCalories.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedMode = this.normalizeCaloriesMode(caloriesMode);
        BigDecimal resolvedBaseAmount = amount;
        String resolvedBaseUnit = normalizedUnit;
        if ("per_base".equals(normalizedMode)) {
            String normalizedBaseUnit = this.normalizeAmountUnit(calorieBaseUnit);
            if (calorieBaseAmount == null || calorieBaseAmount.compareTo(BigDecimal.ZERO) <= 0 || normalizedBaseUnit.isBlank()) {
                return null;
            }
            resolvedBaseAmount = calorieBaseAmount;
            resolvedBaseUnit = normalizedBaseUnit;
        }
        FoodItem foodItem = new FoodItem();
        foodItem.setName(rawName.trim());
        foodItem.setCategory(this.normalizeAiFoodCategory(category));
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
        return this.foodItemService.save(foodItem) ? foodItem : null;
    }

    private FoodItem handleAiFoodUseExistingAction(Long userId, String rawName, BigDecimal amount, String rawUnit, String category, BigDecimal caloriesValue, String calorieUnit, String caloriesMode, BigDecimal calorieBaseAmount, String calorieBaseUnit) {
        FoodItem refreshedFood;
        FoodItem foodItem = this.findBestVisibleFood(userId, rawName);
        if (foodItem != null && caloriesValue != null && caloriesValue.compareTo(BigDecimal.ZERO) > 0 && this.shouldRefreshUserFood(foodItem, userId) && (refreshedFood = this.refreshAiFallbackFood(foodItem, rawName, amount, rawUnit, category, caloriesValue, calorieUnit, caloriesMode, calorieBaseAmount, calorieBaseUnit)) != null) {
            return refreshedFood;
        }
        return foodItem;
    }

    private FoodItem handleAiFoodCreateAction(Long userId, String rawName, BigDecimal amount, String rawUnit, String category, BigDecimal caloriesValue, String calorieUnit, String action, BigDecimal calorieBaseAmount, String calorieBaseUnit) {
        FoodItem existing = this.findBestVisibleFood(userId, rawName);
        if (existing != null) {
            FoodItem refreshed;
            if (caloriesValue != null && caloriesValue.compareTo(BigDecimal.ZERO) > 0 && this.shouldRefreshUserFood(existing, userId) && (refreshed = this.refreshAiFallbackFood(existing, rawName, amount, rawUnit, category, caloriesValue, calorieUnit, "create_private_per_base".equals(action) ? "per_base" : "total", calorieBaseAmount, calorieBaseUnit)) != null) {
                return refreshed;
            }
            return existing;
        }
        if (caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return this.createAiFallbackFood(userId, rawName, amount, rawUnit, category, caloriesValue, calorieUnit, "create_private_per_base".equals(action) ? "per_base" : "total", calorieBaseAmount, calorieBaseUnit);
    }

    private FoodItem refreshAiFallbackFood(FoodItem foodItem, String rawName, BigDecimal amount, String rawUnit, String category, BigDecimal caloriesValue, String calorieUnit, String caloriesMode, BigDecimal calorieBaseAmount, String calorieBaseUnit) {
        String normalizedCalorieUnit;
        BigDecimal storedCalories;
        if (foodItem == null || caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return foodItem;
        }
        String normalizedUnit = this.normalizeAmountUnit(rawUnit);
        if (normalizedUnit.isBlank()) {
            normalizedUnit = this.normalizeAmountUnit(foodItem.getUnit());
        }
        if ((storedCalories = this.normalizeStoredCalories(caloriesValue, normalizedCalorieUnit = this.normalizeCalorieUnit(calorieUnit))) == null || storedCalories.compareTo(BigDecimal.ZERO) <= 0) {
            return foodItem;
        }
        String normalizedMode = this.normalizeCaloriesMode(caloriesMode);
        BigDecimal resolvedBaseAmount = amount;
        String resolvedBaseUnit = normalizedUnit;
        if ("per_base".equals(normalizedMode)) {
            String normalizedBaseUnit = this.normalizeAmountUnit(calorieBaseUnit);
            if (calorieBaseAmount == null || calorieBaseAmount.compareTo(BigDecimal.ZERO) <= 0 || normalizedBaseUnit.isBlank()) {
                return foodItem;
            }
            resolvedBaseAmount = calorieBaseAmount;
            resolvedBaseUnit = normalizedBaseUnit;
        }
        foodItem.setName(rawName.trim());
        foodItem.setCategory(this.normalizeAiFoodCategory(category));
        foodItem.setUnit(resolvedBaseUnit);
        foodItem.setBaseAmount(resolvedBaseAmount.setScale(2, RoundingMode.HALF_UP));
        foodItem.setCalories(storedCalories.setScale(2, RoundingMode.HALF_UP));
        this.foodItemService.updateById(foodItem);
        return (FoodItem)this.foodItemService.getById(foodItem.getId());
    }

    private boolean shouldRefreshUserFood(FoodItem foodItem, Long userId) {
        return foodItem != null && userId != null && !Integer.valueOf(1).equals(foodItem.getIsSystem()) && foodItem.getCreatedBy() != null && foodItem.getCreatedBy().equals(userId);
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
        return this.isZero(foodItem.getProtein()) && this.isZero(foodItem.getCarbs()) && this.isZero(foodItem.getFat()) && this.isZero(foodItem.getFiber());
    }

    private FoodItem findBestVisibleFood(Long userId, String rawName) {
        String keyword;
        String string = keyword = rawName == null ? "" : rawName.trim();
        if (keyword.isBlank()) {
            return null;
        }
        List candidates = this.foodItemService.searchVisibleFoods(userId, keyword);
        if ((candidates == null || candidates.isEmpty()) && ((candidates = ((LambdaQueryChainWrapper)((LambdaQueryChainWrapper)((LambdaQueryChainWrapper)((LambdaQueryChainWrapper)this.foodItemService.lambdaQuery().eq(FoodItem::getIsDelete, (Object)0)).and(q -> ((LambdaQueryWrapper)((LambdaQueryWrapper)q.eq(FoodItem::getIsSystem, (Object)1)).or()).eq(userId != null, FoodItem::getCreatedBy, (Object)userId))).orderByDesc(FoodItem::getIsSystem)).orderByAsc(FoodItem::getName)).list()) == null || candidates.isEmpty())) {
            return null;
        }
        String normalizedKeyword = this.normalizeFoodKeyword(keyword);
        for (FoodItem item : candidates) {
            if (!this.normalizeFoodKeyword(item.getName()).equals(normalizedKeyword)) continue;
            return item;
        }
        for (FoodItem item : candidates) {
            String normalizedName = this.normalizeFoodKeyword(item.getName());
            if (!normalizedName.contains(normalizedKeyword) && !normalizedKeyword.contains(normalizedName)) continue;
            return item;
        }
        for (String alias : this.expandFoodAliases(keyword)) {
            String normalizedAlias = this.normalizeFoodKeyword(alias);
            for (FoodItem item : candidates) {
                String normalizedName = this.normalizeFoodKeyword(item.getName());
                if (!normalizedName.equals(normalizedAlias) && !normalizedName.contains(normalizedAlias) && !normalizedAlias.contains(normalizedName)) continue;
                return item;
            }
        }
        return (FoodItem)candidates.get(0);
    }

    private FoodItem findStrictVisibleFood(Long userId, String rawName) {
        String keyword;
        String string = keyword = rawName == null ? "" : rawName.trim();
        if (keyword.isBlank()) {
            return null;
        }
        List<FoodItem> candidates = this.foodItemService.searchVisibleFoods(userId, keyword);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedKeyword = this.normalizeFoodKeyword(keyword);
        for (FoodItem item : candidates) {
            if (!this.normalizeFoodKeyword(item.getName()).equals(normalizedKeyword)) continue;
            return item;
        }
        for (FoodItem item : candidates) {
            String normalizedName = this.normalizeFoodKeyword(item.getName());
            if (!normalizedName.contains(normalizedKeyword) && !normalizedKeyword.contains(normalizedName)) continue;
            return item;
        }
        for (String alias : this.expandFoodAliases(keyword)) {
            String normalizedAlias = this.normalizeFoodKeyword(alias);
            for (FoodItem item : candidates) {
                String normalizedName = this.normalizeFoodKeyword(item.getName());
                if (!normalizedName.equals(normalizedAlias) && !normalizedName.contains(normalizedAlias) && !normalizedAlias.contains(normalizedName)) continue;
                return item;
            }
        }
        return null;
    }

    private List<DietFoodItemRequest> tryMatchDietItemsForRecord(Long userId, String rawDescription) {
        String[] parts;
        if (this.isBlank(rawDescription)) {
            return Collections.emptyList();
        }
        ArrayList<DietFoodItemRequest> matchedItems = new ArrayList<DietFoodItemRequest>();
        LinkedHashSet<Long> seenFoodIds = new LinkedHashSet<Long>();
        String normalizedLine = rawDescription.replace("\u4ee5\u53ca", "+").replace("\u8fd8\u6709", "+").replace("\u5e76\u4e14", "+").replace("\u7136\u540e", "+").replace("\u518d\u52a0", "+").replace("\u642d\u914d", "+").replace("\u914d", "+").replace("\u548c", "+").replace("\u3001", "+").replace("\uff0c", "+").replace(",", "+");
        for (ParsedFoodAmount parsed : this.parseDietFoodLine(normalizedLine)) {
            FoodItem foodItem = this.findStrictVisibleFood(userId, parsed.name());
            if (foodItem == null || foodItem.getId() == null || !seenFoodIds.add(foodItem.getId())) continue;
            DietFoodItemRequest request = new DietFoodItemRequest();
            request.setFoodItemId(foodItem.getId());
            request.setAmount(parsed.amount());
            matchedItems.add(request);
        }
        if (!matchedItems.isEmpty()) {
            return matchedItems;
        }
        for (String rawPart : parts = normalizedLine.split("\\+")) {
            FoodItem foodItem;
            String keyword = this.normalizeFoodRecordKeyword(rawPart);
            if (keyword.isBlank() || (foodItem = this.findStrictVisibleFood(userId, keyword)) == null || foodItem.getId() == null || !seenFoodIds.add(foodItem.getId())) continue;
            DietFoodItemRequest request = new DietFoodItemRequest();
            request.setFoodItemId(foodItem.getId());
            request.setAmount(this.defaultIfZero(foodItem.getBaseAmount()));
            matchedItems.add(request);
        }
        return matchedItems;
    }

    private String normalizeFoodRecordKeyword(String rawPart) {
        String text = this.safeTrim(rawPart);
        if (text.isBlank()) {
            return "";
        }
        text = text.replaceAll("^(\u6211|\u4eca\u5929|\u521a\u521a|\u521a\u624d|\u521a|\u65e9\u4e0a|\u4e2d\u5348|\u665a\u4e0a|\u591c\u91cc|\u591c\u5bb5|\u5bb5\u591c|\u65e9\u9910|\u5348\u9910|\u5348\u996d|\u665a\u9910|\u52a0\u9910|\u7ec3\u540e|\u559d\u4e86|\u5403\u4e86|\u559d|\u5403)+", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(kg|g|ml|l)$", "");
        text = text.replaceAll("^[\u96f6\u4e00\u4e8c\u4e24\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u534a\u51e0\u591a\u5c11\\d]+\\s*(\u4e2a|\u676f|\u7897|\u52fa|\u7247|\u5757|\u6839|\u888b|\u4efd|\u53ea|\u679a|\u4e32|\u76d2|\u74f6|\u542c|\u7f50)", "");
        text = text.replaceAll("[\u96f6\u4e00\u4e8c\u4e24\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341\u767e\u534a\u51e0\u591a\u5c11\\d]", "");
        return this.safeTrim(text);
    }

    private BigDecimal normalizeDietAmount(BigDecimal amount, String rawUnit, String targetUnit) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedTargetUnit = this.normalizeAmountUnit(targetUnit);
        String normalizedRawUnit = this.normalizeAmountUnit(rawUnit);
        if (normalizedRawUnit.isBlank()) {
            return amount;
        }
        if (normalizedRawUnit.equals(normalizedTargetUnit)) {
            return amount;
        }
        if ("kg".equals(normalizedRawUnit) && "g".equals(normalizedTargetUnit)) {
            return amount.multiply(BigDecimal.valueOf(1000L));
        }
        if ("l".equals(normalizedRawUnit) && "ml".equals(normalizedTargetUnit)) {
            return amount.multiply(BigDecimal.valueOf(1000L));
        }
        return null;
    }

    private String normalizeAmountUnit(String unit) {
        String text;
        String string = text = unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "";
        }
        return switch (text) {
            case "g", "\u514b", "\u516c\u514b", "gram", "grams" -> "g";
            case "kg", "\u516c\u65a4", "\u5343\u514b", "kilogram", "kilograms" -> "kg";
            case "ml", "\u6beb\u5347" -> "ml";
            case "l", "\u5347", "liter", "liters" -> "l";
            case "\u4e2a" -> "\u4e2a";
            case "\u7247" -> "\u7247";
            case "\u6839" -> "\u6839";
            case "\u888b" -> "\u888b";
            case "\u4efd" -> "\u4efd";
            default -> text;
        };
    }

    private String normalizeFoodKeyword(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").trim().toLowerCase(Locale.ROOT);
    }

    private List<String> expandFoodAliases(String text) {
        String raw;
        String string = raw = text == null ? "" : text.trim();
        if (raw.isBlank()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        aliases.add(raw);
        aliases.add(raw.replace("\u67d0\u67d0", "").replace("\u8fd9\u4e2a", "").replace("\u8fd9\u79cd", "").trim());
        aliases.add(raw.replace("\u5373\u98df", "").trim());
        aliases.add(raw.replace("\u53bb\u76ae", "").trim());
        aliases.add(raw.replace("\u9e21\u80f8\u8089", "\u9e21\u80f8").trim());
        aliases.add(raw.replace("\u9e21\u80f8", "\u9e21\u80f8\u8089").trim());
        aliases.add(raw.replace("\u725b\u5976", "\u4f4e\u8102\u725b\u5976").trim());
        aliases.removeIf(String::isBlank);
        return new ArrayList<String>(aliases);
    }

    private String normalizeAiFoodCategory(String category) {
        String text = this.toCleanString(category);
        if (text.isBlank()) {
            return "\u672a\u5206\u7c7b";
        }
        return switch (text) {
            case "\u78b3\u6c34", "\u86cb\u767d\u8d28", "\u8102\u80aa", "\u852c\u83dc", "\u6c34\u679c", "\u4e73\u5236\u54c1", "\u996e\u54c1", "\u96f6\u98df", "\u8865\u5242", "\u672a\u5206\u7c7b" -> text;
            default -> "\u672a\u5206\u7c7b";
        };
    }

    private String normalizeCalorieUnit(String calorieUnit) {
        String text = this.toCleanString(calorieUnit).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return "kcal";
        }
        return switch (text) {
            case "kj", "\u5343\u7126" -> "kj";
            case "kcal", "\u5361", "\u5927\u5361", "\u5361\u8def\u91cc" -> "kcal";
            default -> "kcal";
        };
    }

    private String normalizeCaloriesMode(String caloriesMode) {
        String text = this.toCleanString(caloriesMode).toLowerCase(Locale.ROOT);
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
        if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)value;
            return decimal;
        }
        if (value instanceof Number) {
            Number number = (Number)value;
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeTrainingLookupTarget(String targetDay) {
        if ("\u660e\u5929".equals(targetDay)) {
            return "\u660e\u5929";
        }
        return "\u4eca\u5929";
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "\u52a0\u9910";
        }
        if (mealType.contains("\u7ec3\u540e")) {
            return "\u7ec3\u540e\u9910";
        }
        if (mealType.contains("\u65e9\u9910")) {
            return "\u65e9\u9910";
        }
        if (mealType.contains("\u5348\u9910") || mealType.contains("\u5348\u996d")) {
            return "\u5348\u9910";
        }
        if (mealType.contains("\u665a\u9910") || mealType.contains("\u665a\u996d")) {
            return "\u665a\u9910";
        }
        return "\u52a0\u9910";
    }

    private String normalizeLookupMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "";
        }
        if (mealType.contains("\u7ec3\u540e")) {
            return "\u7ec3\u540e\u9910";
        }
        if (mealType.contains("\u65e9\u9910")) {
            return "\u65e9\u9910";
        }
        if (mealType.contains("\u5348\u9910") || mealType.contains("\u5348\u996d")) {
            return "\u5348\u9910";
        }
        if (mealType.contains("\u665a\u9910") || mealType.contains("\u665a\u996d")) {
            return "\u665a\u9910";
        }
        if (mealType.contains("\u52a0\u9910") || mealType.contains("\u591c\u5bb5") || mealType.contains("\u5bb5\u591c")) {
            return "\u52a0\u9910";
        }
        return "";
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            Number number = (Number)value;
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e) {
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
        if (user == null || this.isBlank(user.getModelPreference())) {
            return this.aiModelConfig.getDefaultModel();
        }
        try {
            Map prefs = (Map)JSON_MAPPER.readValue(user.getModelPreference(), (TypeReference)new TypeReference<Map<String, String>>(){});
            String current = (String)prefs.get("current");
            return current != null && !current.isBlank() ? current.trim() : this.aiModelConfig.getDefaultModel();
        }
        catch (Exception e) {
            return user.getModelPreference().trim();
        }
    }

    private Map<String, String> parseModelPreference(User user) {
        if (user == null || this.isBlank(user.getModelPreference())) {
            return new HashMap<String, String>();
        }
        try {
            return (Map)JSON_MAPPER.readValue(user.getModelPreference(), (TypeReference)new TypeReference<Map<String, String>>(){});
        }
        catch (Exception e) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("current", user.getModelPreference());
            return map;
        }
    }

    private String resolvePurificationModel(User user) {
        Map<String, String> prefs = this.parseModelPreference(user);
        String pm = prefs.get("purificationModel");
        return pm != null && !pm.isBlank() ? pm.trim() : this.aiModelConfig.getPurificationModel();
    }

    private String resolveChatModel(User user) {
        Map<String, String> prefs = this.parseModelPreference(user);
        String cm = prefs.get("chatModel");
        return cm != null && !cm.isBlank() ? cm.trim() : this.aiModelConfig.getChatModel();
    }

    private String resolveActiveModelName() {
        String modelName = ACTIVE_CHAT_MODEL.get();
        return this.isBlank(modelName) ? this.aiModelConfig.getDefaultModel() : modelName.trim();
    }

    private AiModelConfig.ModelProvider requireActiveProvider() {
        return this.requireProvider(this.resolveActiveModelName());
    }

    private AiModelConfig.ModelProvider requireProvider(String modelName) {
        AiModelConfig.ModelProvider provider = this.aiModelConfig.getProvider(modelName);
        if (provider != null) {
            return provider;
        }
        List<Map<String, String>> customModels = ACTIVE_CUSTOM_MODELS.get();
        if (customModels != null) {
            for (Map<String, String> cm : customModels) {
                if (!modelName.equals(cm.get("name"))) continue;
                return this.aiModelConfig.getCustomProvider(cm.get("name"), cm.get("baseUrl"), cm.get("apiKey"), cm.get("model"));
            }
        }
        throw new BusincessException(StateCode.AI_ERROR, "\u5f53\u524dAI\u6a21\u578b\u4e0d\u5b58\u5728\uff0c\u8bf7\u5207\u6362\u522b\u7684AI\u8bd5\u4e00\u8bd5");
    }

    private List<Map<String, String>> loadCustomModelsFromRedis(Long userId) {
        User dbUser = (User)this.userService.getById(userId);
        String json = dbUser.getCustomModels();
        if (json == null || json.isBlank()) {
            return new ArrayList<Map<String, String>>();
        }
        try {
            List models = (List)JSON_MAPPER.readValue(json, (TypeReference)new TypeReference<List<Map<String, String>>>(){});
            String secret = this.aiModelConfig.getCryptoSecret();
            for (Map m : models) {
                String encrypted = (String)m.get("apiKey");
                if (encrypted == null || encrypted.isBlank()) continue;
                try {
                    m.put("apiKey", CryptoUtils.decrypt(encrypted, secret));
                }
                catch (Exception exception) {}
            }
            return models;
        }
        catch (Exception e) {
            return new ArrayList<Map<String, String>>();
        }
    }

    private String normalizePlanSection(String section) {
        if (section == null) {
            return "";
        }
        return section.trim().replaceAll("\\n{3,}", "\n\n");
    }

    private String normalizeTrainingAdvice(String advice, String normalizedSection, boolean isRestDay) {
        Object text;
        Object object = text = advice == null ? "" : advice.trim();
        if (((String)text).isEmpty()) {
            return isRestDay ? "\u5efa\u8bae\uff1a\u4eca\u5929\u4ee5\u6062\u590d\u4e3a\u4e3b\uff0c\u505a\u4e00\u70b9\u8f7b\u62c9\u4f38\u548c\u6563\u6b65\u5c31\u591f\u4e86\u3002" : "\u5efa\u8bae\uff1a\u52a8\u4f5c\u8282\u594f\u7a33\u4e00\u70b9\uff0c\u5148\u628a\u70ed\u8eab\u548c\u62c9\u4f38\u505a\u5b8c\u6574\u3002";
        }
        text = ((String)text).replaceAll("^(\u4eca\u5929|\u660e\u5929)[\uff0c,:\uff1a]?", "").trim();
        String[] suggestionParts = ((String)(text = ((String)text).replaceAll("\\r", ""))).split("(?=\u5efa\u8bae[\uff1a:])");
        if (suggestionParts.length > 1) {
            String candidate = "";
            for (String part : suggestionParts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty() || trimmed.length() <= candidate.length()) continue;
                candidate = trimmed;
            }
            if (!candidate.isEmpty()) {
                text = candidate;
            }
        }
        if (!isRestDay && (((String)text).contains("\u4f11\u606f\u65e5") || ((String)text).contains("\u6062\u590d\u65e5"))) {
            return this.buildTrainingAdviceFallback(normalizedSection, false);
        }
        if (!((String)text).startsWith("\u5efa\u8bae")) {
            text = "\u5efa\u8bae\uff1a" + (String)text;
        }
        return text;
    }

    private String buildTrainingAdviceFallback(String normalizedSection, boolean isRestDay) {
        if (isRestDay) {
            return "\u5efa\u8bae\uff1a\u4ee5\u6062\u590d\u4e3a\u4e3b\uff0c\u505a\u4e00\u70b9\u8f7b\u62c9\u4f38\u548c\u6563\u6b65\u5c31\u591f\u4e86\u3002";
        }
        if (normalizedSection.contains("\u80f8")) {
            return "\u5efa\u8bae\uff1a\u7ec3\u80f8\u65f6\u91cd\u70b9\u62c9\u4f38\u80f8\u5927\u808c\u548c\u80a9\u524d\u675f\uff0c\u52a8\u4f5c\u8282\u594f\u7a33\u4e00\u70b9\uff0c\u4e0d\u8981\u6025\u7740\u4e0a\u91cd\u91cf\u3002";
        }
        if (normalizedSection.contains("\u80cc")) {
            return "\u5efa\u8bae\uff1a\u7ec3\u80cc\u65f6\u5148\u628a\u52a8\u4f5c\u8f68\u8ff9\u505a\u7a33\u5b9a\uff0c\u7ec3\u5b8c\u628a\u80cc\u9614\u808c\u548c\u4e0b\u80cc\u90e8\u62c9\u4f38\u5230\u4f4d\u3002";
        }
        if (normalizedSection.contains("\u817f")) {
            return "\u5efa\u8bae\uff1a\u7ec3\u817f\u65f6\u7ec4\u95f4\u628a\u547c\u5438\u8282\u594f\u7a33\u4f4f\uff0c\u7ec3\u5b8c\u8bb0\u5f97\u8865\u6c34\u548c\u62c9\u4f38\u3002";
        }
        if (normalizedSection.contains("\u80a9")) {
            return "\u5efa\u8bae\uff1a\u7ec3\u80a9\u65f6\u6ce8\u610f\u52a8\u4f5c\u63a7\u5236\uff0c\u907f\u514d\u501f\u529b\u8fc7\u591a\uff0c\u7ec3\u5b8c\u628a\u4e09\u89d2\u808c\u548c\u4e0a\u80cc\u653e\u677e\u5f00\u3002";
        }
        return "\u5efa\u8bae\uff1a\u52a8\u4f5c\u8282\u594f\u7a33\u4e00\u70b9\uff0c\u5148\u628a\u70ed\u8eab\u548c\u62c9\u4f38\u505a\u5b8c\u6574\u3002";
    }

    private String buildDietMealRequestInstruction(String msg) {
        String mealType = this.resolveMealType(msg);
        if (mealType == null) {
            return "6. \u5982\u679c\u7528\u6237\u6ca1\u6709\u660e\u786e\u6307\u5b9a\u67d0\u4e00\u9910\uff0c\u9ed8\u8ba4\u751f\u6210\u5168\u5929\u996e\u98df\uff08\u65e9\u9910\u3001\u5348\u9910\u3001\u665a\u9910\u3001\u52a0\u9910\u56db\u9910\uff09\n7. \u5982\u679c\u7528\u6237\u95ee\u6cd5\u6bd4\u8f83\u5bbd\u6cdb\uff0c\u5982\u201c\u63a8\u8350\u996e\u98df\u201d\u201c\u51cf\u8102\u600e\u4e48\u5403\u201d\uff0c\u4e5f\u6309\u5168\u5929\u996e\u98df\u8f93\u51fa\n8. \u8f93\u51fa\u65f6\u5fc5\u987b\u8986\u76d6\u65e9\u9910\u3001\u5348\u9910\u3001\u665a\u9910\u3001\u52a0\u9910\u56db\u9910\uff0c\u4e0d\u8981\u53ea\u7ed9\u5176\u4e2d\u4e00\u9910\n";
        }
        return "6. \u7528\u6237\u8fd9\u6b21\u660e\u786e\u53ea\u60f3\u770b\u67d0\u4e00\u9910\uff0c\u8bf7\u53ea\u751f\u6210\u3010%s\u3011\u8fd9\u4e00\u9910\uff0c\u4e0d\u8981\u8f93\u51fa\u5168\u5929\u98df\u8c31\n7. \u4e0d\u8981\u989d\u5916\u8865\u65e9\u9910/\u5348\u9910/\u665a\u9910/\u52a0\u9910\u5176\u5b83\u9910\u6b21\n8. \u6807\u9898\u548c\u6b63\u6587\u90fd\u56f4\u7ed5\u3010%s\u3011\u8fd9\u4e00\u9910\uff0c\u4e0d\u8981\u51fa\u73b0\u201c\u5168\u5929\u98df\u8c31\u201d\u201c\u4e00\u65e5\u98df\u8c31\u63a8\u8350\u201d\n".formatted(mealType, mealType);
    }

    private String sanitizeTrainingPlanOutput(String aiReply, boolean forceDefaultTrainingWeek) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        Matcher matcher = Pattern.compile("###\\s*\u661f\u671f([\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929]).*?(?=###\\s*\u661f\u671f|$)", 32).matcher(aiReply);
        ArrayList<String> sections = new ArrayList<String>();
        while (matcher.find()) {
            String block = this.safeTrim(matcher.group());
            if (block.isBlank()) continue;
            ArrayList<String> kept = new ArrayList<String>();
            for (String rawLine : block.split("\\r?\\n")) {
                String line = this.safeTrim(rawLine);
                if (line.isBlank() || !line.startsWith("###") && !line.startsWith("|") && !line.startsWith(">")) continue;
                kept.add(line);
            }
            if (!kept.isEmpty()) {
                sections.add(String.join((CharSequence)"\n", kept));
            }
            if (sections.size() < 7) continue;
            break;
        }
        if (sections.isEmpty()) {
            return aiReply;
        }
        if (forceDefaultTrainingWeek) {
            this.enforceDefaultTrainingWeek(sections);
        }
        return "### \u8bad\u7ec3\u8ba1\u5212\n\n" + String.join((CharSequence)"\n\n", sections);
    }

    private boolean shouldForceDefaultTrainingWeek(String message) {
        if (this.isBlank(message)) {
            return true;
        }
        String normalized = message.replace("\u6bcf\u5468", "");
        return !normalized.contains("\u7ec3\u4e09\u4f11\u4e00") && !normalized.contains("\u7ec3\u56db\u4f11\u4e00") && !normalized.contains("\u7ec3\u4e00\u4f11\u4e00") && !normalized.contains("\u6bcf\u5929\u90fd\u7ec3") && !normalized.contains("\u5929\u5929\u7ec3") && !normalized.contains("\u6bcf\u5468\u7ec3") && !normalized.contains("\u4e00\u5468\u7ec3") && !normalized.contains("\u5468\u672b\u4e5f\u7ec3") && !normalized.contains("\u5468\u516d\u7ec3") && !normalized.contains("\u5468\u65e5\u7ec3") && !normalized.contains("\u661f\u671f\u516d\u7ec3") && !normalized.contains("\u661f\u671f\u65e5\u7ec3") && !normalized.contains("\u5468\u51e0\u4f11") && !normalized.contains("\u661f\u671f\u51e0\u4f11");
    }

    private void enforceDefaultTrainingWeek(List<String> sections) {
        Map<String, String> forcedRestMap = Map.of("\u4e09", "### \u661f\u671f\u4e09 \u00b7 \u4f11\u606f\u65e5\n\n> \u4f11\u606f\uff0c\u53ef\u505a30\u5206\u949f\u4f4e\u5f3a\u5ea6\u6709\u6c27\uff08\u6563\u6b65/\u5feb\u8d70\uff09", "\u516d", "### \u661f\u671f\u516d \u00b7 \u4f11\u606f\u65e5\n\n> \u4f11\u606f\uff0c\u53ef\u505a30\u5206\u949f\u4f4e\u5f3a\u5ea6\u6709\u6c27\uff08\u6563\u6b65/\u5feb\u8d70\uff09", "\u65e5", "### \u661f\u671f\u65e5 \u00b7 \u4f11\u606f\u65e5\n\n> \u4f11\u606f\uff0c\u53ef\u505a30\u5206\u949f\u4f4e\u5f3a\u5ea6\u6709\u6c27\uff08\u6563\u6b65/\u5feb\u8d70\uff09", "\u5929", "### \u661f\u671f\u65e5 \u00b7 \u4f11\u606f\u65e5\n\n> \u4f11\u606f\uff0c\u53ef\u505a30\u5206\u949f\u4f4e\u5f3a\u5ea6\u6709\u6c27\uff08\u6563\u6b65/\u5feb\u8d70\uff09");
        for (int i = 0; i < sections.size(); ++i) {
            String dayKey;
            String forcedRest;
            String section = sections.get(i);
            Matcher dayMatcher = Pattern.compile("\u661f\u671f([\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u65e5\u5929])").matcher(section);
            if (!dayMatcher.find() || (forcedRest = forcedRestMap.get(dayKey = dayMatcher.group(1))) == null) continue;
            sections.set(i, forcedRest);
        }
    }

    private String sanitizeDietPlanOutput(String aiReply, String targetMealType) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        List<String> mealOrder = List.of("\u65e9\u9910", "\u5348\u9910", "\u665a\u9910", "\u52a0\u9910", "\u7ec3\u540e\u9910");
        LinkedHashMap<String, CallSite> mealMap = new LinkedHashMap<String, CallSite>();
        for (String rawLine : aiReply.split("\\r?\\n")) {
            String normalizedMeal;
            String[] parts;
            String line = this.safeTrim(rawLine);
            if (!line.startsWith("|") || line.contains("---") || (parts = line.split("\\|")).length < 3) continue;
            String mealCell = this.safeTrim(parts[1]);
            String foodCell = this.safeTrim(parts[2]);
            if (mealCell.isBlank() || foodCell.isBlank() || (normalizedMeal = this.normalizeMealType(mealCell.replaceAll("[\\p{So}\\p{Sc}]", "").trim())).isBlank()) continue;
            mealMap.put(normalizedMeal, (CallSite)((Object)(mealCell + " | " + foodCell)));
        }
        if (mealMap.isEmpty()) {
            return aiReply;
        }
        boolean singleMeal = targetMealType != null && !targetMealType.isBlank();
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append((String)(singleMeal ? targetMealType + "\u63a8\u8350" : "\u4e00\u65e5\u98df\u8c31\u63a8\u8350")).append("\n\n");
        sb.append("| \u9910\u6b21 | \u63a8\u8350\u98df\u7269 |\n|------|----------|\n");
        if (singleMeal) {
            String row = (String)mealMap.get(targetMealType);
            if (row == null) {
                return aiReply;
            }
            sb.append("| ").append(row).append(" |");
            return sb.toString().trim();
        }
        for (String mealType : mealOrder) {
            String row = (String)mealMap.get(mealType);
            if (row == null) continue;
            sb.append("| ").append(row).append(" |\n");
        }
        return sb.toString().trim();
    }

    @Override
    public Map<String, Object> recognizeFoodImage(Long userId, MultipartFile file, String type) {
        HashMap<String, Object> data;
        String foodNameEn;
        String foodName;
        long requestStartedAt = System.currentTimeMillis();
        String traceId = "vision-" + userId + "-" + requestStartedAt;
        LinkedHashMap<String, Object> debugTimings = new LinkedHashMap<String, Object>();
        debugTimings.put("traceId", traceId);
        debugTimings.put("requestStartedAtMs", requestStartedAt);
        AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
        if (visionProvider == null) {
            throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
        }
        String imageUrl = this.fileService.uploadFoodImage(file, userId);
        debugTimings.put("imageUploadedAtMs", System.currentTimeMillis());
        ArrayList<Map<String, Object>> contentParts = new ArrayList<Map<String, Object>>();
        contentParts.add(Map.of("type", "text", "text", FOOD_IDENTIFY_PROMPT));
        contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
        Map<String, Object> visionResponse = this.aiCallHelper.callVision(visionProvider, contentParts, 512, 0.0);
        if (visionResponse == null) {
            throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u8c03\u7528\u5931\u8d25");
        }
        debugTimings.put("visionCompletedAtMs", System.currentTimeMillis());
        Integer estimatedGrams = null;
        boolean amountEstimated = false;
        String visionRawContent = "";
        try {
            String cleaned;
            String content;
            List choices = (List)visionResponse.get("choices");
            Map msg = (Map)((Map)choices.get(0)).get("message");
            visionRawContent = content = ((String)msg.get("content")).trim();
            String jsonPart = cleaned = content.replaceAll("^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
            int jsonStart = cleaned.indexOf("{");
            int jsonEnd = cleaned.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonPart = cleaned.substring(jsonStart, jsonEnd + 1);
            }
            if (jsonPart.startsWith("{")) {
                String s;
                Number n;
                Map parsed = (Map)JSON_MAPPER.readValue(jsonPart, Map.class);
                foodName = this.toCleanString(parsed.get("name"));
                foodNameEn = this.toCleanString(parsed.get("nameEn"));
                Object g = parsed.get("grams");
                if (g instanceof Number && (n = (Number)g).intValue() > 0) {
                    int raw = n.intValue();
                    estimatedGrams = raw <= 5000 ? Integer.valueOf(raw) : null;
                    boolean bl = amountEstimated = estimatedGrams != null;
                    if (estimatedGrams == null) {
                        log.warn("[VisionTrace] grams \u503c\u5f02\u5e38: {}, foodName={}", (Object)raw, (Object)foodName);
                    }
                } else if (g instanceof String && !(s = (String)g).isBlank()) {
                    try {
                        String numStr = s.trim().replaceAll("[^0-9.]", "");
                        int raw = (int)Double.parseDouble(numStr);
                        estimatedGrams = raw > 0 && raw <= 5000 ? Integer.valueOf(raw) : null;
                        amountEstimated = estimatedGrams != null;
                    }
                    catch (NumberFormatException numberFormatException) {}
                }
            } else {
                foodName = cleaned;
                foodNameEn = null;
            }
        }
        catch (Exception e) {
            try {
                String cleaned;
                List choices = (List)visionResponse.get("choices");
                Map msg = (Map)((Map)choices.get(0)).get("message");
                String raw = ((String)msg.get("content")).trim();
                foodName = cleaned = raw.replaceAll("^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
                foodNameEn = null;
            }
            catch (Exception e2) {
                throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u54cd\u5e94\u89e3\u6790\u5931\u8d25");
            }
        }
        if (foodName == null || foodName.isBlank() || foodName.length() > 50) {
            foodName = "\u672a\u77e5\u98df\u7269";
        }
        log.info("[VisionTrace] traceId={}, visionRaw={}, foodName={}, estimatedGrams={}, amountEstimated={}", new Object[]{traceId, visionRawContent, foodName, estimatedGrams, amountEstimated});
        HashMap<String, BigDecimal> dbNutrition = null;
        FoodItem dbFood = this.matchFoodFromDB(userId, foodName);
        debugTimings.put("dbLookupCompletedAtMs", System.currentTimeMillis());
        if (dbFood != null) {
            HashMap<String, BigDecimal> nutrition = new HashMap<String, BigDecimal>();
            nutrition.put("calories", dbFood.getCalories());
            nutrition.put("protein", dbFood.getProtein());
            nutrition.put("carbs", dbFood.getCarbs());
            nutrition.put("fat", dbFood.getFat());
            nutrition.put("fiber", dbFood.getFiber());
            dbNutrition = nutrition;
            data = new HashMap();
            data.put("type", "food");
            data.put("imageUrl", imageUrl);
            data.put("foodName", dbFood.getName());
            data.put("confidence", 1.0);
            data.put("unit", dbFood.getUnit() != null ? dbFood.getUnit() : "g");
            data.put("suggestedAmount", estimatedGrams != null ? (Number)estimatedGrams : (Number)(dbFood.getBaseAmount() != null ? dbFood.getBaseAmount() : Integer.valueOf(100)));
            data.put("perUnitAmount", dbFood.getBaseAmount() != null ? dbFood.getBaseAmount() : Integer.valueOf(100));
            data.put("nutritionPerUnit", dbNutrition);
            data.put("dataSource", "food_database");
            data.put("amountEstimated", amountEstimated);
            debugTimings.put("backendResponseReadyAtMs", System.currentTimeMillis());
            debugTimings.put("totalElapsedMs", System.currentTimeMillis() - requestStartedAt);
            data.put("debugTimings", debugTimings);
            log.info("[VisionTrace] traceId={}, foodName={}, source=food_database, timings={}", new Object[]{traceId, dbFood.getName(), debugTimings});
            return data;
        }
        Map<String, Object> searchNutrition = this.searchFoodNutrition(foodName, foodNameEn, debugTimings);
        data = new HashMap<String, Object>();
        data.put("type", "food");
        data.put("imageUrl", imageUrl);
        data.put("foodName", foodName);
        data.put("confidence", searchNutrition != null ? 0.8 : 0.0);
        data.put("unit", "g");
        data.put("suggestedAmount", estimatedGrams != null ? estimatedGrams : 100);
        data.put("perUnitAmount", 100);
        data.put("nutritionPerUnit", searchNutrition != null ? searchNutrition.get("nutritionPerUnit") : Map.of("calories", 0, "protein", 0, "carbs", 0, "fat", 0, "fiber", 0));
        data.put("dataSource", searchNutrition != null ? searchNutrition.get("source") : "not_found");
        data.put("amountEstimated", amountEstimated);
        debugTimings.put("backendResponseReadyAtMs", System.currentTimeMillis());
        debugTimings.put("totalElapsedMs", System.currentTimeMillis() - requestStartedAt);
        data.put("debugTimings", debugTimings);
        log.info("[VisionTrace] traceId={}, foodName={}, source={}, timings={}", new Object[]{traceId, foodName, data.get("dataSource"), debugTimings});
        return data;
    }

    @Override
    public Map<String, Object> recognizeImage(Long userId, MultipartFile file, String type, String customText, boolean deepThinking) {
        if ("food".equals(type)) {
            return this.recognizeFoodImage(userId, file, "food");
        }
        return this.recognizeImagePrepare(userId, file, type, customText);
    }

    private Map<String, Object> recognizeImagePrepare(Long userId, MultipartFile file, String type, String customText) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
            if (visionProvider == null) {
                result.put("error", "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
                return result;
            }
            String visionPrompt = switch (type) {
                case "equipment" -> "\u8bc6\u522b\u56fe\u7247\u4e2d\u7684\u5065\u8eab\u5668\u68b0\uff0c\u53ea\u8fd4\u56de\u5668\u68b0\u540d\u79f0\uff0c\u4e0d\u8981\u8f93\u51fa\u5176\u4ed6\u5185\u5bb9\u3002\u5982\u679c\u56fe\u7247\u4e0d\u662f\u5065\u8eab\u5668\u68b0\uff0c\u8f93\u51fa\"\u975e\u5065\u8eab\u5668\u68b0\"\u3002";
                case "nutrition_label" -> "\u8bfb\u53d6\u56fe\u7247\u4e2d\u7684\u8425\u517b\u6210\u5206\u8868/\u6807\u7b7e\uff0c\u8fd4\u56de\u7ed3\u6784\u5316JSON\u3002\u683c\u5f0f\uff1a{\"name\":\"\u98df\u7269\u540d\",\"calories\":\"\u6570\u503c\",\"unit\":\"kJ\u6216kcal\",\"protein\":\"\u6570\u503cg\",\"carbs\":\"\u6570\u503cg\",\"fat\":\"\u6570\u503cg\",\"fiber\":\"\u6570\u503cg\"}\u3002\u53ea\u8f93\u51faJSON\uff0c\u4e0d\u8981\u5176\u4ed6\u5185\u5bb9\u3002";
                case "form_check" -> "\u5206\u6790\u56fe\u7247\u4e2d\u4eba\u7269\u7684\u52a8\u4f5c\u59ff\u52bf\uff0c\u8fd4\u56deJSON\uff1a{\"exercise\":\"\u52a8\u4f5c\u540d\",\"score\":85,\"issues\":[\"\u95ee\u98981\",\"\u95ee\u98982\"],\"advice\":[\"\u5efa\u8bae1\",\"\u5efa\u8bae2\"]}\u3002\u53ea\u8f93\u51faJSON\u3002";
                default -> "\u7b80\u8981\u63cf\u8ff0\u56fe\u7247\u5185\u5bb9\uff0c50\u5b57\u4ee5\u5185\u3002";
            };
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", visionPrompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 512, 0.0);
            String visionText = this.extractVisionText(visionRes);
            log.info("[RecognizePrepare] userId={}, type={}, visionResult={}, elapsed={}ms", new Object[]{userId, type, visionText, System.currentTimeMillis() - start});
            if (visionText == null || visionText.isBlank()) {
                result.put("error", "\u672a\u8bc6\u522b\u5230\u6709\u6548\u5185\u5bb9");
                return result;
            }
            result.put("imageUrl", imageUrl);
            result.put("equipmentName", visionText);
            if ("equipment".equals(type)) {
                String rawSearch = this.webSearchHelper.searchEquipmentInfo(visionText);
                result.put("rawData", rawSearch);
                log.info("[RecognizePrepare] userId={}, searchDone={}, elapsed={}ms", new Object[]{userId, rawSearch != null, System.currentTimeMillis() - start});
            }
        }
        catch (Exception e) {
            log.error("[RecognizePrepare] userId={}, type={}, error={}", new Object[]{userId, type, e.getMessage(), e});
            result.put("error", "\u8bc6\u522b\u5931\u8d25");
        }
        log.info("[RecognizePrepare] userId={}, type={}, totalElapsed={}ms", new Object[]{userId, type, System.currentTimeMillis() - start});
        return result;
    }

    @Override
    public void recognizeSummaryStream(Long userId, String type, String equipmentName, String rawData, boolean deepThinking, OutputStream outputStream) {
        long start = System.currentTimeMillis();
        try {
            String reply;
            int maxTokens;
            String aiPrompt;
            String systemPrompt;
            User user = (User)this.userService.getById(userId);
            String userChatModel = user != null ? this.resolveChatModel(user) : this.aiModelConfig.getChatModel();
            AiModelConfig.ModelProvider summaryProvider = this.aiModelConfig.getByModelName(userChatModel, true);
            if ("equipment".equals(type)) {
                if (deepThinking) {
                    systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u62e5\u670910\u5e74\u7ecf\u9a8c\u7684\u4e13\u4e1a\u5065\u8eab\u6559\u7ec3\uff0c\u64c5\u957f\u8fd0\u52a8\u89e3\u5256\u5b66\u548c\u8bad\u7ec3\u79d1\u5b66\u6307\u5bfc\u3002";
                    aiPrompt = "\u7528\u6237\u4e0a\u4f20\u4e86\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u8bc6\u522b\u4e3a\u300c" + equipmentName + "\u300d\u3002\u4ee5\u4e0b\u662f\u8054\u7f51\u641c\u7d22\u7684\u8d44\u6599\uff1a\n\n" + (rawData != null && !rawData.isBlank() ? rawData : "\uff08\u672a\u641c\u5230\u76f8\u5173\u8d44\u6599\uff09") + "\n\n\u8bf7\u7ed9\u51fa\u4e13\u4e1a\u7684\u5668\u68b0\u6307\u5bfc\uff1a\u5668\u68b0\u6982\u8ff0\u3001\u8bad\u7ec3\u90e8\u4f4d\uff08\u89e3\u5256\u5b66\u89d2\u5ea6\uff09\u3001\u8be6\u7ec6\u4f7f\u7528\u6b65\u9aa4\uff08\u542b\u547c\u5438\u8282\u594f\u548c\u53d1\u529b\u987a\u5e8f\uff09\u3001\u5e38\u89c1\u9519\u8bef\u4e0e\u7ea0\u6b63\u3001\u8bad\u7ec3\u5efa\u8bae\uff08\u7ec4\u6570/\u6b21\u6570/\u642d\u914d\uff09\u3002";
                    maxTokens = 2048;
                    double temperature = 0.6;
                } else {
                    systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u64c5\u957f\u7528\u7b80\u6d01\u8bed\u8a00\u89e3\u7b54\u5065\u8eab\u95ee\u9898\u3002";
                    aiPrompt = "\u7528\u6237\u4e0a\u4f20\u4e86\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u8bc6\u522b\u4e3a\u300c" + equipmentName + "\u300d\u3002\u4ee5\u4e0b\u662f\u641c\u7d22\u5230\u7684\u8d44\u6599\uff1a\n\n" + (rawData != null && !rawData.isBlank() ? rawData : "\u672a\u641c\u5230\u8d44\u6599") + "\n\n\u8bf7\u6574\u7406\u6210\u7b80\u6d01\u7684\u5668\u68b0\u4ecb\u7ecd\uff08\u8bad\u7ec3\u90e8\u4f4d\u3001\u4f7f\u7528\u65b9\u6cd5\u3001\u6ce8\u610f\u4e8b\u9879\uff09\uff0c\u4e0d\u8d85\u8fc7200\u5b57\u3002";
                    maxTokens = 512;
                    double temperature = 0.3;
                }
            } else if ("nutrition_label".equals(type)) {
                systemPrompt = "\u4f60\u662f\u8425\u517b\u6807\u7b7e\u89e3\u8bfb\u52a9\u624b\u3002";
                aiPrompt = "\u56fe\u7247\u8425\u517b\u6210\u5206\u8868OCR\u7ed3\u679c\uff1a" + (equipmentName != null ? equipmentName : "\u65e0\u6570\u636e") + "\n\n\u8bf7\u6574\u7406\u6210\u6613\u8bfb\u683c\u5f0f\uff0c\u8bf4\u660e\u6bcf\u4efd\u7684\u70ed\u91cf\u548c\u5404\u8425\u517b\u7d20\u542b\u91cf\u3002";
                maxTokens = 512;
                double temperature = 0.3;
            } else if ("form_check".equals(type)) {
                systemPrompt = "\u4f60\u662f\u4e13\u4e1a\u5065\u8eab\u6559\u7ec3\uff0c\u64c5\u957f\u52a8\u4f5c\u5206\u6790\u548c\u59ff\u52bf\u7ea0\u6b63\u3002";
                aiPrompt = "\u56fe\u7247\u4e2d\u4eba\u7269\u7684\u59ff\u52bf\u5206\u6790\u7ed3\u679c\uff1a" + (equipmentName != null ? equipmentName : "\u65e0\u6570\u636e") + "\n\n\u8bf7\u7ed9\u51fa\u8bc4\u5206\u3001\u95ee\u9898\u6307\u51fa\u548c\u7ea0\u6b63\u5efa\u8bae\uff0c\u8bed\u6c14\u9f13\u52b1\u3002";
                maxTokens = 512;
                double temperature = 0.3;
            } else {
                systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002";
                aiPrompt = "\u56fe\u7247\u8bc6\u522b\u7ed3\u679c\uff1a" + (equipmentName != null ? equipmentName : "\u65e0\u6570\u636e") + "\n\n\u8bf7\u7b80\u8981\u56de\u590d\uff0c\u4e0d\u8d85\u8fc7100\u5b57\u3002";
                maxTokens = 256;
                double temperature = 0.3;
            }
            long aiStart = System.currentTimeMillis();
            try {
                reply = this.callAiApiStreamWithProvider(systemPrompt, aiPrompt, outputStream, summaryProvider, maxTokens);
            }
            catch (Exception e) {
                log.error("[RecognizeSummary] AI\u6d41\u5f0f\u8c03\u7528\u5931\u8d25", (Throwable)e);
                reply = null;
            }
            long aiElapsed = System.currentTimeMillis() - aiStart;
            log.info("[RecognizeSummary] userId={}, type={}, deepThinking={}, model={}, elapsed={}ms, replyLen={}", new Object[]{userId, type, deepThinking, summaryProvider.getModel(), aiElapsed, reply != null ? Integer.valueOf(reply.length()) : "null"});
        }
        catch (Exception e) {
            log.error("[RecognizeSummary] userId={}, type={}, error={}", new Object[]{userId, type, e.getMessage(), e});
            try {
                this.writeSseEvent(outputStream, "error", "{\"message\":\"\u751f\u6210\u5931\u8d25\"}");
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        log.info("[RecognizeSummary] userId={}, type={}, totalElapsed={}ms", new Object[]{userId, type, System.currentTimeMillis() - start});
    }

    private void writeSseEvent(OutputStream outputStream, String event, String data) {
        try {
            outputStream.write(("event:" + event + "\ndata:" + data + "\n\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }
        catch (Exception e) {
            log.warn("[SSE] writeEvent failed: {}", (Object)e.getMessage());
        }
    }

    private void streamTextGradually(OutputStream outputStream, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        int chunkSize = 8;
        for (int i = 0; i < content.length(); i += chunkSize) {
            try {
                int end = Math.min(content.length(), i + chunkSize);
                String chunk = content.substring(i, end);
                String escaped = chunk.replace("\n", "\\n");
                outputStream.write(("data:" + escaped + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                if (end >= content.length()) continue;
                Thread.sleep(30L);
                continue;
            }
            catch (Exception e) {
                break;
            }
        }
    }

    private Map<String, Object> handleEquipmentRecognition(Long userId, MultipartFile file, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            double temperature;
            int maxTokens;
            Object aiPrompt;
            String systemPrompt;
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
            }
            String visionPrompt = "\u8bc6\u522b\u56fe\u7247\u4e2d\u7684\u5065\u8eab\u5668\u68b0\uff0c\u53ea\u8fd4\u56de\u5668\u68b0\u540d\u79f0\uff0c\u4e0d\u8981\u8f93\u51fa\u5176\u4ed6\u5185\u5bb9\u3002\u53ea\u8f93\u51fa\u540d\u79f0\uff0c\u4e0d\u8981\u8f93\u51fa\u4efb\u4f55\u89e3\u91ca\u6216JSON\u3002\u5982\u679c\u56fe\u7247\u4e0d\u662f\u5065\u8eab\u5668\u68b0\uff0c\u8f93\u51fa\"\u975e\u5065\u8eab\u5668\u68b0\"\u3002";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", visionPrompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 256, 0.0);
            String equipmentName = this.extractVisionText(visionRes);
            if (equipmentName == null || equipmentName.isBlank() || "\u975e\u5065\u8eab\u5668\u68b0".equals(equipmentName)) {
                result.put("textReply", "\u672a\u8bc6\u522b\u5230\u5065\u8eab\u5668\u68b0");
                return result;
            }
            log.info("[EquipmentRecognition] userId={}, visionResult={}, elapsed={}ms", new Object[]{userId, equipmentName, System.currentTimeMillis() - start});
            String rawSearch = this.webSearchHelper.searchEquipmentInfo(equipmentName);
            log.info("[EquipmentRecognition] userId={}, searchDone={}, elapsed={}ms", new Object[]{userId, rawSearch != null ? "success" : "null", System.currentTimeMillis() - start});
            if (deepThinking) {
                systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u4e00\u4f4d\u62e5\u670910\u5e74\u7ecf\u9a8c\u7684\u4e13\u4e1a\u5065\u8eab\u6559\u7ec3\u3002\u4f60\u5bf9\u5404\u7c7b\u5065\u8eab\u5668\u68b0\u4e86\u5982\u6307\u638c\uff0c\n\u80fd\u591f\u4ece\u8fd0\u52a8\u89e3\u5256\u5b66\u3001\u751f\u7269\u529b\u5b66\u548c\u8bad\u7ec3\u79d1\u5b66\u7684\u89d2\u5ea6\u7ed9\u51fa\u4e13\u4e1a\u7684\u6307\u5bfc\u5efa\u8bae\u3002\n\u4f60\u5584\u4e8e\u7528\u901a\u4fd7\u6613\u61c2\u4f46\u4e13\u4e1a\u51c6\u786e\u7684\u8bed\u8a00\u89e3\u91ca\u5668\u68b0\u4f7f\u7528\u65b9\u6cd5\uff0c\u5e76\u7ed9\u51fa\u9488\u5bf9\u6027\u7684\u8bad\u7ec3\u5efa\u8bae\u3002\n";
                aiPrompt = rawSearch != null && !rawSearch.isBlank() ? "\u7528\u6237\u4e0a\u4f20\u4e86\u4e00\u5f20\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u89c6\u89c9\u6a21\u578b\u8bc6\u522b\u4e3a\u300c%s\u300d\u3002\n\u4ee5\u4e0b\u662f\u8054\u7f51\u641c\u7d22\u5230\u7684\u539f\u59cb\u8d44\u6599\uff1a\n\n%s\n\n\u8bf7\u57fa\u4e8e\u4ee5\u4e0a\u8d44\u6599\u548c\u4f60\u7684\u4e13\u4e1a\u77e5\u8bc6\uff0c\u5b8c\u6210\u4ee5\u4e0b\u4efb\u52a1\uff1a\n\n## 1. \u5668\u68b0\u6982\u8ff0\n\u7b80\u8981\u4ecb\u7ecd\u8be5\u5668\u68b0\u7684\u57fa\u672c\u4fe1\u606f\u3001\u9002\u7528\u4eba\u7fa4\u3001\u8bad\u7ec3\u6548\u679c\u3002\n\n## 2. \u7cbe\u51c6\u8bad\u7ec3\u90e8\u4f4d\n\u4ece\u89e3\u5256\u5b66\u89d2\u5ea6\u5206\u6790\u8be5\u5668\u68b0\u4e3b\u8981\u953b\u70bc\u7684\u808c\u7fa4\uff08\u533a\u5206\u4e3b\u52a8\u808c\u3001\u534f\u540c\u808c\u3001\u7a33\u5b9a\u808c\uff09\uff0c\n\u5e76\u8bf4\u660e\u4e0d\u540c\u63e1\u59ff/\u7ad9\u59ff/\u5ea7\u6905\u4f4d\u7f6e\u5bf9\u76ee\u6807\u808c\u7fa4\u7684\u5f71\u54cd\u3002\n\n## 3. \u8be6\u7ec6\u4f7f\u7528\u6b65\u9aa4\n\u5206\u6b65\u9aa4\u8bf4\u660e\u6b63\u786e\u7684\u4f7f\u7528\u65b9\u6cd5\uff0c\u5305\u62ec\uff1a\n- \u521d\u59cb\u8c03\u8282\uff08\u5ea7\u6905\u9ad8\u5ea6\u3001\u9760\u80cc\u89d2\u5ea6\u3001\u914d\u91cd\u9009\u62e9\uff09\n- \u52a8\u4f5c\u6267\u884c\uff08\u547c\u5438\u8282\u594f\u3001\u53d1\u529b\u987a\u5e8f\u3001\u9876\u5cf0\u6536\u7f29\uff09\n- \u8fd8\u539f\u63a7\u5236\uff08\u79bb\u5fc3\u9636\u6bb5\u6ce8\u610f\u4e8b\u9879\uff09\n\n## 4. \u5e38\u89c1\u9519\u8bef\u4e0e\u7ea0\u6b63\n\u5217\u4e3e3-5\u4e2a\u65b0\u624b\u6700\u5e38\u72af\u7684\u9519\u8bef\u52a8\u4f5c\uff0c\u5e76\u7ed9\u51fa\u7ea0\u6b63\u65b9\u6cd5\u3002\n\n## 5. \u8bad\u7ec3\u5efa\u8bae\n\u63a8\u8350\u9002\u5408\u7684\u7ec4\u6570\u3001\u6b21\u6570\u3001\u4f11\u606f\u65f6\u95f4\uff0c\u4ee5\u53ca\u5982\u4f55\u4e0e\u5176\u4ed6\u52a8\u4f5c\u642d\u914d\u3002\n".formatted(equipmentName, rawSearch) : "\u7528\u6237\u4e0a\u4f20\u4e86\u4e00\u5f20\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u8bc6\u522b\u4e3a\u300c%s\u300d\uff0c\u4f46\u8054\u7f51\u641c\u7d22\u672a\u67e5\u5230\u76f8\u5173\u8d44\u6599\u3002\n\u8bf7\u5b8c\u5168\u4f9d\u9760\u4f60\u7684\u4e13\u4e1a\u77e5\u8bc6\uff0c\u7ed9\u51fa\u8be6\u5c3d\u7684\u5668\u68b0\u4ecb\u7ecd\u548c\u4f7f\u7528\u6307\u5bfc\u3002\n\n\u8981\u6c42\u8986\u76d6\uff1a\u5668\u68b0\u6982\u8ff0\u3001\u8bad\u7ec3\u90e8\u4f4d\uff08\u89e3\u5256\u5b66\u89d2\u5ea6\uff09\u3001\u8be6\u7ec6\u4f7f\u7528\u6b65\u9aa4\u3001\u5e38\u89c1\u9519\u8bef\u4e0e\u7ea0\u6b63\u3001\u8bad\u7ec3\u5efa\u8bae\u3002\n\u5982\u679c\u5bf9\u8be5\u5668\u68b0\u4e0d\u592a\u786e\u5b9a\uff0c\u8bda\u5b9e\u8bf4\u660e\u5e76\u5efa\u8bae\u7528\u6237\u5728B\u7ad9\u641c\u7d22\u8be5\u5668\u68b0\u7684\u6559\u7a0b\u89c6\u9891\u3002\n".formatted(equipmentName);
                maxTokens = 2048;
                temperature = 0.6;
            } else {
                systemPrompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\uff0c\u64c5\u957f\u7528\u7b80\u6d01\u6613\u61c2\u7684\u8bed\u8a00\u89e3\u7b54\u5065\u8eab\u76f8\u5173\u95ee\u9898\u3002";
                aiPrompt = rawSearch != null && !rawSearch.isBlank() ? "\u7528\u6237\u4e0a\u4f20\u4e86\u4e00\u5f20\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u8bc6\u522b\u4e3a\u300c" + equipmentName + "\u300d\u3002\u4ee5\u4e0b\u662f\u8054\u7f51\u641c\u7d22\u5230\u7684\u8d44\u6599\uff0c\u8bf7\u6574\u7406\u6210\u7b80\u6d01\u7684\u5668\u68b0\u4ecb\u7ecd\uff1a\n\n" + rawSearch + "\n\n\u8981\u6c42\uff1a\u53ea\u4fdd\u7559\u6838\u5fc3\u7684\u8bad\u7ec3\u90e8\u4f4d\u3001\u4f7f\u7528\u65b9\u6cd5\u3001\u6ce8\u610f\u4e8b\u9879\uff0c\u4e0d\u8d85\u8fc7200\u5b57\u3002" : "\u7528\u6237\u4e0a\u4f20\u4e86\u4e00\u5f20\u5065\u8eab\u5668\u68b0\u56fe\u7247\uff0c\u8bc6\u522b\u4e3a\u300c" + equipmentName + "\u300d\uff0c\u8054\u7f51\u672a\u641c\u5230\u8d44\u6599\u3002\u8bf7\u6839\u636e\u77e5\u8bc6\u7b80\u8981\u4ecb\u7ecd\u8be5\u5668\u68b0\uff0c\u4e0d\u8d85\u8fc7150\u5b57\u3002\u5982\u679c\u4e0d\u786e\u5b9a\uff0c\u5efa\u8bae\u7528\u6237\u5728B\u7ad9\u641c\u7d22\u6559\u7a0b\u3002";
                maxTokens = 512;
                temperature = 0.3;
            }
            String userChatModel = this.resolveChatModel((User)this.userService.getById(userId));
            AiModelConfig.ModelProvider summaryProvider = this.aiModelConfig.getByModelName(userChatModel, true);
            long sumStart = System.currentTimeMillis();
            String reply = this.aiCallHelper.callText(summaryProvider, systemPrompt, (String)aiPrompt, maxTokens, temperature);
            long sumElapsed = System.currentTimeMillis() - sumStart;
            log.info("[EquipmentRecognition] userId={}, deepThinking={}, model={}, maxTokens={}, temperature={}, elapsed={}ms, replyLen={}", new Object[]{userId, deepThinking, summaryProvider.getModel(), maxTokens, temperature, sumElapsed, reply != null ? Integer.valueOf(reply.length()) : "null"});
            log.info("[EquipmentRecognition] userId={}, replyPreview={}", (Object)userId, reply != null ? (reply.length() > 300 ? reply.substring(0, 300) + "..." : reply) : "null");
            result.put("textReply", reply);
            result.put("equipmentName", equipmentName);
            result.put("imageUrl", imageUrl);
        }
        catch (Exception e) {
            log.error("\u5668\u68b0\u8bc6\u522b\u5931\u8d25", (Throwable)e);
            result.put("textReply", "\u5668\u68b0\u8bc6\u522b\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        }
        log.info("[EquipmentRecognition] userId={}, totalElapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleNutritionLabelRecognition(Long userId, MultipartFile file, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
            }
            String prompt = "\u8bfb\u53d6\u56fe\u7247\u4e2d\u7684\u8425\u517b\u6210\u5206\u8868/\u6807\u7b7e\uff0c\u8fd4\u56de\u7ed3\u6784\u5316 JSON\u3002\n\u53ea\u8f93\u51fa JSON\uff0c\u4e0d\u8981 markdown\uff0c\u4e0d\u8981\u89e3\u91ca\uff0c\u4e0d\u8981\u4ee3\u7801\u5757\u3002\n\u683c\u5f0f\uff1a{\"name\":\"\u98df\u7269\u540d\",\"calories\":\"\u6570\u503c\",\"caloriesUnit\":\"kJ\u6216kcal\",\"protein\":\"\u6570\u503cg\",\"carbs\":\"\u6570\u503cg\",\"fat\":\"\u6570\u503cg\",\"fiber\":\"\u6570\u503cg\",\"sodium\":\"\u6570\u503cg\"}\n\u89c4\u5219\uff1a\n1. \u4ed4\u7ec6\u8bfb\u53d6\u6807\u7b7e\u4e0a\u7684\u6bcf\u4e00\u9879\u6570\u636e\uff0c\u5305\u62ec\u70ed\u91cf\u3001\u86cb\u767d\u8d28\u3001\u78b3\u6c34\u5316\u5408\u7269\u3001\u8102\u80aa\u3001\u81b3\u98df\u7ea4\u7ef4\u3001\u94a0\u7b49\u3002\n2. caloriesUnit \u4fdd\u7559\u539f\u59cb\u5355\u4f4d\uff08kJ \u6216 kcal\uff09\u3002\n3. \u5982\u679c\u56fe\u7247\u4e0d\u662f\u8425\u517b\u6210\u5206\u8868\uff0c\u8fd4\u56de {\"error\":\"not_nutrition_label\"}\n";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 512, 0.0);
            String content = this.extractVisionRaw(visionRes);
            if (content == null) {
                result.put("textReply", "\u6807\u7b7e\u8bc6\u522b\u5931\u8d25");
                return result;
            }
            String cleaned = content.replaceAll("^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
            int js = cleaned.indexOf("{");
            int je = cleaned.lastIndexOf("}");
            if (js < 0 || je < js) {
                result.put("textReply", "\u6807\u7b7e\u8bc6\u522b\u5931\u8d25");
                return result;
            }
            String jsonPart = cleaned.substring(js, je + 1);
            Map parsed = (Map)JSON_MAPPER.readValue(jsonPart, Map.class);
            if (parsed.containsKey("error")) {
                result.put("textReply", "\u56fe\u7247\u4e2d\u672a\u68c0\u6d4b\u5230\u8425\u517b\u6210\u5206\u8868");
                return result;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\u8425\u517b\u6807\u7b7e\u8bfb\u53d6\u7ed3\u679c\uff1a\n");
            sb.append("\u98df\u7269\uff1a").append(this.toCleanString(parsed.get("name"))).append("\n");
            sb.append("\u70ed\u91cf\uff1a").append(this.toCleanString(parsed.get("calories"))).append(" ").append(this.toCleanString(parsed.get("caloriesUnit"))).append("\n");
            sb.append("\u86cb\u767d\u8d28\uff1a").append(this.toCleanString(parsed.get("protein"))).append("\n");
            sb.append("\u78b3\u6c34\u5316\u5408\u7269\uff1a").append(this.toCleanString(parsed.get("carbs"))).append("\n");
            sb.append("\u8102\u80aa\uff1a").append(this.toCleanString(parsed.get("fat"))).append("\n");
            if (parsed.get("fiber") != null) {
                sb.append("\u81b3\u98df\u7ea4\u7ef4\uff1a").append(this.toCleanString(parsed.get("fiber"))).append("\n");
            }
            if (parsed.get("sodium") != null) {
                sb.append("\u94a0\uff1a").append(this.toCleanString(parsed.get("sodium"))).append("\n");
            }
            result.put("textReply", sb.toString());
            result.put("imageUrl", imageUrl);
            result.put("labelData", parsed);
        }
        catch (Exception e) {
            log.error("\u8425\u517b\u6807\u7b7e\u8bc6\u522b\u5931\u8d25", (Throwable)e);
            result.put("textReply", "\u6807\u7b7e\u8bc6\u522b\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        }
        log.info("[NutritionLabelRecognition] userId={}, elapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleFormCheckRecognition(Long userId, MultipartFile file, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
            }
            String prompt = "\u4f60\u662f\u4e13\u4e1a\u5065\u8eab\u6559\u7ec3\u3002\u5206\u6790\u56fe\u7247\u4e2d\u4eba\u7269\u7684\u5065\u8eab\u52a8\u4f5c\u59ff\u52bf\u3002\n\u8bf7\u7ed9\u51fa\u4ee5\u4e0b\u5185\u5bb9\uff1a\n1. \u8bc6\u522b\u51fa\u6b63\u5728\u6267\u884c\u7684\u52a8\u4f5c\u540d\u79f0\n2. \u8bc4\u4f30\u59ff\u52bf\u662f\u5426\u6b63\u786e\uff08\u63091-10\u5206\uff09\n3. \u5982\u679c\u6709\u95ee\u9898\uff0c\u6307\u51fa\u5177\u4f53\u54ea\u91cc\u4e0d\u5bf9\n4. \u7ed9\u51fa\u7ea0\u6b63\u5efa\u8bae\n\u7528\u7b80\u6d01\u7684\u4e2d\u6587\u56de\u7b54\u3002\u5982\u679c\u56fe\u7247\u4e2d\u6ca1\u6709\u4eba\u6216\u6ca1\u6709\u5728\u505a\u8fd0\u52a8\uff0c\u56de\u590d\"\u672a\u68c0\u6d4b\u5230\u5065\u8eab\u52a8\u4f5c\"\u3002\n";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 1024, 0.3);
            String content = this.extractVisionRaw(visionRes);
            if (content == null || content.isBlank()) {
                result.put("textReply", "\u59ff\u52bf\u5206\u6790\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
                return result;
            }
            result.put("textReply", content);
            result.put("imageUrl", imageUrl);
        }
        catch (Exception e) {
            log.error("\u59ff\u52bf\u7ea0\u9519\u5931\u8d25", (Throwable)e);
            result.put("textReply", "\u59ff\u52bf\u5206\u6790\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        }
        log.info("[FormCheckRecognition] userId={}, elapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleOtherImageRecognition(Long userId, MultipartFile file, String customText, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.aiModelConfig.getVisionProvider();
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "\u89c6\u89c9\u6a21\u578b\u672a\u914d\u7f6e");
            }
            String prompt = "\u4f60\u662f\u5065\u8eab\u52a9\u624bTatan\u3002\u7528\u6237\u4e0a\u4f20\u4e86\u4e00\u5f20\u56fe\u7247\u5e76\u9644\u5e26\u4e86\u63cf\u8ff0\uff1a\"%s\"\n\u8bf7\u6839\u636e\u56fe\u7247\u5185\u5bb9\u548c\u7528\u6237\u63cf\u8ff0\uff0c\u5224\u65ad\u662f\u5426\u80fd\u63d0\u4f9b\u4e0e\u5065\u8eab\u3001\u996e\u98df\u76f8\u5173\u7684\u5e2e\u52a9\u3002\n\u5982\u679c\u53ef\u4ee5\uff0c\u7ed9\u51fa\u76f8\u5173\u5efa\u8bae\uff08\u5982\u98df\u7269\u8425\u517b\u4f30\u7b97\u3001\u5668\u68b0\u4f7f\u7528\u3001\u8bad\u7ec3\u5efa\u8bae\u7b49\uff09\u3002\n\u5982\u679c\u4e0e\u5065\u8eab\u996e\u98df\u5b8c\u5168\u65e0\u5173\uff0c\u56de\u590d\"\u6211\u65e0\u6cd5\u5904\u7406\u8fd9\u4e2a\u95ee\u9898\"\u3002\n\u7528\u7b80\u6d01\u7684\u4e2d\u6587\u56de\u7b54\u3002\n".formatted(customText != null ? customText : "");
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 1024, 0.3);
            String content = this.extractVisionRaw(visionRes);
            if (content == null || content.isBlank()) {
                result.put("textReply", "\u6211\u65e0\u6cd5\u5904\u7406\u8fd9\u4e2a\u95ee\u9898");
                return result;
            }
            result.put("textReply", content);
            result.put("imageUrl", imageUrl);
        }
        catch (Exception e) {
            log.error("other\u56fe\u7247\u8bc6\u522b\u5931\u8d25", (Throwable)e);
            result.put("textReply", "\u8bc6\u522b\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5");
        }
        log.info("[OtherImageRecognition] userId={}, elapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private String extractVisionText(Map<String, Object> visionResponse) {
        try {
            List choices = (List)visionResponse.get("choices");
            Map msg = (Map)((Map)choices.get(0)).get("message");
            String content = ((String)msg.get("content")).trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
            }
            return content;
        }
        catch (Exception e) {
            return null;
        }
    }

    private String extractVisionRaw(Map<String, Object> visionResponse) {
        try {
            List choices = (List)visionResponse.get("choices");
            Map msg = (Map)((Map)choices.get(0)).get("message");
            return ((String)msg.get("content")).trim();
        }
        catch (Exception e) {
            return null;
        }
    }

    private FoodItem matchFoodFromDB(Long userId, String foodName) {
        List<FoodItem> candidates = this.foodItemService.searchVisibleFoods(userId, foodName);
        if (candidates.isEmpty()) {
            return null;
        }
        for (FoodItem item : candidates) {
            if (!item.getName().equalsIgnoreCase(foodName)) continue;
            return item;
        }
        for (FoodItem item : candidates) {
            if (!item.getName().contains(foodName) && !foodName.contains(item.getName())) continue;
            return item;
        }
        return null;
    }

    private Map<String, Object> searchFoodNutrition(String foodName, String foodNameEn, Map<String, Object> debugTimings) {
        try {
            Map result;
            Map nutrition;
            String content;
            debugTimings.put("webSearchStartedAtMs", System.currentTimeMillis());
            String usdaQuery = foodNameEn != null && !foodNameEn.isBlank() ? foodNameEn : foodName;
            WebSearchHelper.SearchExtractResult searchResult = this.webSearchHelper.searchFoodNutritionJsonWithTiming(foodName, usdaQuery);
            debugTimings.put("webSearchCompletedAtMs", System.currentTimeMillis());
            if (searchResult != null) {
                debugTimings.put("webSearchElapsedMs", searchResult.elapsedMs());
            }
            String string = content = searchResult != null ? searchResult.content() : null;
            if (content == null || content.isBlank()) {
                return null;
            }
            String json = content.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            if ((nutrition = (Map)(result = (Map)JSON_MAPPER.readValue(json, Map.class)).get("nutritionPerUnit")) == null) {
                return null;
            }
            double cal = ((Number)nutrition.getOrDefault("calories", 0)).doubleValue();
            double fat = ((Number)nutrition.getOrDefault("fat", 0)).doubleValue();
            cal = Math.round(cal * 4.184);
            nutrition.put("calories", cal);
            if (fat > 0.0 && fat * 9.0 * 4.184 > cal * 1.5) {
                log.warn("\u98df\u7269\u8425\u517b\u6570\u636e\u6821\u9a8c\u5931\u8d25\uff08\u70ed\u91cf\u4e0e\u8102\u80aa\u4e0d\u5339\u914d\uff09: foodName={}, calories={}kJ, fat={}g", new Object[]{foodName, cal, fat});
            }
            debugTimings.put("nutritionParsedAtMs", System.currentTimeMillis());
            return result;
        }
        catch (Exception e) {
            log.warn("\u98df\u7269\u8425\u517b\u641c\u7d22\u5931\u8d25: foodName={}, error={}", (Object)foodName, (Object)e.getMessage());
            debugTimings.put("nutritionSearchFailedAtMs", System.currentTimeMillis());
            debugTimings.put("nutritionSearchError", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor={Exception.class})
    public boolean saveRecognizedFood(Long userId, SaveRecognizedFoodRequest request) {
        boolean isNew;
        BigDecimal perUnit = request.getPerUnitAmount() != null ? request.getPerUnitAmount() : BigDecimal.valueOf(100L);
        BigDecimal actualAmount = request.getActualAmount();
        BigDecimal factor = actualAmount.divide(perUnit, 6, RoundingMode.HALF_UP);
        FoodItem foodItem = (FoodItem)this.foodItemService.getOne((Wrapper)((QueryWrapper)((QueryWrapper)((QueryWrapper)new QueryWrapper().eq((Object)"name", (Object)request.getFoodName())).eq((Object)"createdBy", (Object)userId)).eq((Object)"isDelete", (Object)0)).last("LIMIT 1"));
        boolean bl = isNew = foodItem == null;
        if (isNew) {
            foodItem = new FoodItem();
            foodItem.setName(request.getFoodName());
            foodItem.setImageUrl(request.getImageUrl());
            foodItem.setCreatedBy(userId);
            foodItem.setIsSystem(0);
        }
        foodItem.setUnit(request.getUnit());
        foodItem.setBaseAmount(perUnit);
        foodItem.setCalories(request.getCalories());
        foodItem.setProtein(request.getProtein());
        foodItem.setCarbs(request.getCarbs());
        foodItem.setFat(request.getFat());
        foodItem.setFiber(request.getFiber());
        if (foodItem.getImageUrl() == null && request.getImageUrl() != null) {
            foodItem.setImageUrl(request.getImageUrl());
        }
        this.foodItemService.saveOrUpdate(foodItem);
        int totalCalKcal = request.getCalories().multiply(factor).divide(KJ_TO_KCAL_DIVISOR, 0, RoundingMode.HALF_UP).intValue();
        ArrayList<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("foodItemId", foodItem.getId());
        item.put("name", request.getFoodName());
        item.put("unit", request.getUnit());
        item.put("amount", actualAmount);
        item.put("calories", request.getCalories().multiply(factor).divide(KJ_TO_KCAL_DIVISOR, 2, RoundingMode.HALF_UP));
        item.put("protein", request.getProtein().multiply(factor));
        item.put("carbs", request.getCarbs().multiply(factor));
        item.put("fat", request.getFat().multiply(factor));
        item.put("fiber", request.getFiber().multiply(factor));
        items.add(item);
        this.dietRecordService.saveStructuredRecord(userId, LocalDate.now(CN_ZONE), LocalTime.now(CN_ZONE).format(TIME_FMT), request.getMealType(), request.getFoodName(), totalCalKcal, request.getProtein().multiply(factor), request.getCarbs().multiply(factor), request.getFat().multiply(factor), request.getFiber().multiply(factor), null, "image_recognition", items);
        this.userDailyMetricService.syncDailyCalories(userId, LocalDate.now(CN_ZONE), items, null);
        return true;
    }

    private record DirectRouteResult(String replyType, String toolData, String systemPrompt, boolean showRecordButton) {
        DirectRouteResult(String replyType, String toolData, String systemPrompt) {
            this(replyType, toolData, systemPrompt, true);
        }
    }

    private record ClassifyResult(String intent, Map<String, Object> params, boolean complete, String clarify) {
    }

    private record ToolCallResult(String replyType, String extra) {
    }

    private record PromptContextDecision(boolean needsRag, boolean includeRecentDialog, boolean includeTodayRecord, boolean includeYesterdaySummary, boolean includeWeeklySummary, boolean includeEmotionalState, boolean includeTrainingPlanContext, boolean includeDietPlanContext) {
    }

    private record MessageRoutingDecision(boolean preferGeneralAnswer, boolean continueChatAfterRecord, PromptContextDecision promptContext, String recordLookupIntent, String planGenerationIntent, TrainingPlanLookupDecision trainingPlanLookup, DietPlanLookupDecision dietPlanLookup, ExerciseRecordDecision exerciseRecord, DietRecordDecision dietRecord) {
    }

    private record TrainingPlanLookupDecision(boolean shouldLookup, String targetDay, boolean viewAll) {
    }

    private record DietPlanLookupDecision(boolean shouldLookup, String mealType, boolean viewAll) {
    }

    private record ExerciseRecordDecision(AddExerciseRecordRequest request, boolean continueChat) {
    }

    private record DietRecordDecision(AddDietRecordRequest request, boolean continueChat, String clarificationMessage, String noticeMessage) {
    }

    private record TimePattern(Pattern pattern, Function<Matcher, LocalDate> resolver) {
    }

    private record PlanFilterConditions(String equipment, String foodCategory) {
    }

    private record ParsedTrainingDay(int dayIndex, String title, String muscleGroup, boolean rest, List<String> warmups, List<String> trainings, List<String> stretches) {
    }

    private record ParsedFoodAmount(String name, BigDecimal amount, String unit) {
    }

    private record MealNutrition(String summaryName, BigDecimal calories, BigDecimal protein, BigDecimal carbs, BigDecimal fat, BigDecimal fiber, List<Map<String, Object>> items) {
    }

    private record StructuredDietParseResult(AddDietRecordRequest request, String clarificationMessage, String noticeMessage) {
    }

    private record TrainingPlanTarget(String label, DayOfWeek dayOfWeek) {
    }

    private record AiIntentSaveResult(String replyType, String replyText) {
    }
}
