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
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Base64;
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
    private static final Pattern DIET_SINGLE_MEAL_PATTERN = Pattern.compile("(早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜|练后餐)");
    private static final List<String> PLAN_MEAL_ORDER = List.of("早餐", "练后餐", "午餐", "加餐", "晚餐");
    private static final List<String> PLAN_MUSCLE_GROUP_ORDER = List.of("chest", "back", "shoulders", "arms", "legs", "core");
    private static final BigDecimal KJ_TO_KCAL_DIVISOR = new BigDecimal("4.184");
    private static final ThreadLocal<String> ACTIVE_CHAT_MODEL = new ThreadLocal();
    private static final ThreadLocal<Map<String, WebClient>> CACHED_WEB_CLIENTS = ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<String> CACHED_TRAINING_PLAN_TEXT = new ThreadLocal();
    private static final ThreadLocal<List<Map<String, String>>> ACTIVE_CUSTOM_MODELS = new ThreadLocal();
    private volatile boolean clientDisconnected = false;
    private static final Map<String, Integer> WEEKDAY_MAP = Map.of("一", 1, "二", 2, "三", 3, "四", 4, "五", 5, "六", 6, "日", 7, "天", 7);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final String INTENT_CLASSIFY_PROMPT = "你是健身助手的意图分析模块。分析用户消息，严格只返回一个JSON对象，不要返回任何其他文字。\n\n可用意图：\n1. save_diet: 用户在陈述\"已经吃了/喝了什么\"或\"请帮我记录饮食\"。参数: food(必填,仅保留食物内容), meal(餐次)\n2. save_exercise: 用户在陈述\"已经做了什么运动\"或\"请帮我记录训练\"。参数: exercises(必填,数组,每个元素含name(动作名,必填),sets(组数,可选),duration(分钟数,可选))\n3. save_weight: 用户报体重数字。参数: weight_kg(必填,数字,斤需换算成kg), note(可选备注)\n4. query_training: 查训练。\"练什么/该练什么/训练安排\"=计划安排，\"练了什么/运动记录\"=已完成的记录。参数: query_type(\"已完成的记录\"/\"计划安排\",必填), target_day(\"今天\"/\"明天\"/\"全部\",默认今天)\n5. query_diet: 查饮食。\"吃了什么/饮食记录\"=已完成的记录，\"吃什么/食谱\"=计划安排。参数: query_type(必填), meal_type(可选)\n6. query_weight: 查体重。\"最近体重/体重趋势\"等。参数: range(\"今天\"/\"最近7天\"/\"最近30天\",默认最近7天)\n7. get_today_records: 查今天综合记录。无参数\n8. get_week_records: 查本周记录。无参数\n9. get_history_record: 查某天记录。参数: date_description(必填)\n10. search_knowledge: 健身专业问题(训练原理/营养/补剂/器械/伤病/康复/减脂/增肌等)。参数: query(必填)\n11. generate_training_plan: 生成/调整训练计划。参数: user_request, adjust_existing(bool)\n12. generate_diet_plan: 生成饮食推荐/食谱。参数: user_request, meal_scope(\"全天\"/\"单餐\")\n13. delete_record: 用户想删除/撤回已记录的内容。参数: type(\"exercise\"/\"diet\",必填), target(具体描述如\"深蹲\"/\"午餐\",可选,默认最后一条)\n14. update_weight: 用户在修正之前报错的体重(语境含\"不对/错了/改成/应该是/修改\")。参数: weight_kg(必填,数字)\n15. other: 纯闲聊/打招呼/不匹配以上任何意图\n16. query_weather: 用户询问天气情况。参数: query(可选,如\"明天天气\"\"这周天气\")\n17. set_daily_calories: 用户想设置每日摄入热量目标。参数: daily_calories(必填,数字,单位kcal)\n18. set_target_weight: 用户想设置目标体重。参数: target_weight(必填,数字,单位kg,斤需换算成kg)\n19. report_training_issue: 用户反馈训练中的身体不适或困难。参数: issue_description(必填,问题描述), affected_exercise(可选,出问题的动作)\n20. report_diet_issue: 用户反馈饮食不足或饥饿。参数: issue_description(必填,问题描述), meal_context(可选,哪一餐/什么时候)\n21. update_location: 用户告知自己所在的城市/位置。参数: city(必填,城市名,如\"广州\"\"深圳\"\"上海\")\n\n判断规则：\n- \"我吃了XX\"/\"记录一下吃了XX\" → save_diet（提取食物，不要带\"我吃了/帮我记录\"前缀）\n- \"我练了XX\"/\"记录一下练了XX\" → save_exercise（拆分为exercises数组，每个动作一个元素，提取组数和时长）\n- \"体重70kg\"/\"称了一下65.5\"/\"今天体重140斤\" → save_weight（斤要除以2转kg）\n- \"不对，体重是72\"/\"体重改成72\"/\"应该是73kg不是74\" → update_weight（修正体重，斤要除以2转kg）\n- \"把XX删了\"/\"撤回XX\"/\"删除记录\"/\"没吃那个\" → delete_record\n- \"今天练什么\"/\"该练什么\"/\"训练安排\" → query_training(query_type=计划安排)\n- \"今天练了什么\"/\"运动记录\" → query_training(query_type=已完成的记录)\n- \"今天吃什么\"/\"食谱推荐\" → query_diet(query_type=计划安排)\n- \"今天吃了什么\"/\"饮食记录\" → query_diet(query_type=已完成的记录)\n- \"最近体重\"/\"体重趋势\"/\"瘦了多少\" → query_weight\n- \"今天做了什么\"/\"打卡总结\" → get_today_records\n- \"这周做了什么\"/\"本周总结\" → get_week_records\n- \"昨天练了什么\"/\"上周三的记录\" → get_history_record\n- \"帮我制定训练计划\" → generate_training_plan\n- \"推荐今天的食谱\" → generate_diet_plan\n- \"筋膜枪有用吗\"/\"蛋白粉怎么喝\"/\"深蹲膝盖痛\" → search_knowledge\n- \"今天天气\"/\"明天下不下雨\"/\"这周天气怎么样\"/\"外面冷不冷\" → query_weather\n- \"每天吃2000千卡\"/\"目标热量1800\"/\"每日摄入设为2200kcal\"/\"我想每天吃1500大卡\" → set_daily_calories\n- \"目标体重65kg\"/\"想减到140斤\"/\"我要练到70公斤\" → set_target_weight\n- \"我在广州\"/\"我在深圳\"/\"坐标上海\"/\"我在武汉\" → update_location\n- \"你好\"/\"谢谢\"/闲聊 → other\n\n参数规则：\n- 不要编造克重、热量、组数、时长；只抽取用户明确说过的内容\n- weight_kg 统一为kg单位（用户说斤要除以2）\n- exercises数组中如果用户只提了一个动作也要用数组格式\n- 不要输出额外文字，只返回JSON\n\n示例：\n\"我晚上吃了两个全麦面包和三个鸡蛋\" → {\"intent\":\"save_diet\",\"food\":\"两个全麦面包和三个鸡蛋\",\"meal\":\"晚餐\"}\n\"帮我记录一下我练了深蹲5组\" → {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"深蹲\",\"sets\":5}]}\n\"我练了深蹲4组15分钟还有卧推3组10分钟\" → {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"深蹲\",\"sets\":4,\"duration\":15},{\"name\":\"卧推\",\"sets\":3,\"duration\":10}]}\n\"跑步30分钟\" → {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"跑步\",\"duration\":30}]}\n\"今天体重70.5kg\" → {\"intent\":\"save_weight\",\"weight_kg\":70.5}\n\"称了一下140斤\" → {\"intent\":\"save_weight\",\"weight_kg\":70.0}\n\"不对，我体重是72\" → {\"intent\":\"update_weight\",\"weight_kg\":72.0}\n\"体重改成72kg\" → {\"intent\":\"update_weight\",\"weight_kg\":72.0}\n\"把刚才的深蹲删了\" → {\"intent\":\"delete_record\",\"type\":\"exercise\",\"target\":\"深蹲\"}\n\"撤回午餐\" → {\"intent\":\"delete_record\",\"type\":\"diet\",\"target\":\"午餐\"}\n\"删除最后一条训练记录\" → {\"intent\":\"delete_record\",\"type\":\"exercise\"}\n\"今天该练什么\" → {\"intent\":\"query_training\",\"query_type\":\"计划安排\",\"target_day\":\"今天\",\"complete\":true}\n\"最近体重变化大吗\" → {\"intent\":\"query_weight\",\"range\":\"最近7天\",\"complete\":true}\n\"深蹲膝盖痛怎么办\" → {\"intent\":\"search_knowledge\",\"query\":\"深蹲膝盖痛怎么办\",\"complete\":true}\n\"今天天气怎么样\" → {\"intent\":\"query_weather\",\"complete\":true}\n\"明天下雨吗\" → {\"intent\":\"query_weather\",\"query\":\"明天天气\",\"complete\":true}\n\"每天吃2000千卡\" → {\"intent\":\"set_daily_calories\",\"daily_calories\":2000,\"complete\":true}\n\"目标热量设为1800\" → {\"intent\":\"set_daily_calories\",\"daily_calories\":1800,\"complete\":true}\n\"每日摄入1500大卡\" → {\"intent\":\"set_daily_calories\",\"daily_calories\":1500,\"complete\":true}\n\"目标体重65kg\" → {\"intent\":\"set_target_weight\",\"target_weight\":65.0,\"complete\":true}\n\"想减到140斤\" → {\"intent\":\"set_target_weight\",\"target_weight\":70.0,\"complete\":true}\n\"你好\" → {\"intent\":\"other\",\"complete\":true}\n\"帮我制定计划\" → {\"intent\":\"generate_training_plan\",\"user_request\":\"帮我制定计划\",\"complete\":false,\"clarify\":\"好的，帮你制定训练计划！需要确认：健身目标是什么？每周能练几天？\"}\n\"帮我制定减脂计划每周4天\" → {\"intent\":\"generate_training_plan\",\"user_request\":\"制定减脂计划每周4天\",\"complete\":true}\n\"我晚上吃了两个全麦面包和三个鸡蛋\" → {\"intent\":\"save_diet\",\"food\":\"两个全麦面包和三个鸡蛋\",\"meal\":\"晚餐\",\"complete\":true}\n\"跑步30分钟\" → {\"intent\":\"save_exercise\",\"exercises\":[{\"name\":\"跑步\",\"duration\":30}],\"complete\":true}\n\"我吃了\" → {\"intent\":\"save_diet\",\"complete\":false,\"clarify\":\"吃了什么食物呢？\"}\n\"我练了\" → {\"intent\":\"save_exercise\",\"complete\":false,\"clarify\":\"做了什么运动呢？\"}\n\"深蹲膝盖痛做不了\" → {\"intent\":\"report_training_issue\",\"issue_description\":\"深蹲时膝盖痛，无法完成\",\"affected_exercise\":\"深蹲\",\"complete\":true}\n\"卧推太重了肩膀扛不住\" → {\"intent\":\"report_training_issue\",\"issue_description\":\"卧推重量太大，肩膀不舒服\",\"affected_exercise\":\"卧推\",\"complete\":true}\n\"吃不饱老是饿\" → {\"intent\":\"report_diet_issue\",\"issue_description\":\"总是吃不饱，感觉饿\",\"complete\":true}\n\"热量不够头晕\" → {\"intent\":\"report_diet_issue\",\"issue_description\":\"摄入热量不足，出现头晕\",\"complete\":true}\n\n补充输出字段：\n- complete(bool): 必填参数是否齐全。\n  save_weight/update_weight/query_*/delete_*/get_today_records/get_week_records/get_history_record/search_knowledge/set_daily_calories/set_target_weight 时一律为true。\n  save_diet 有food时为true，food为空或缺失时为false，追问\"吃了什么？\"。\n  save_exercise 有exercises时为true，exercises为空或缺失时为false，追问\"做了什么运动？\"。\n  generate_* 有user_request时为true，缺user_request时为false。\n  report_training_issue/report_diet_issue 有issue_description时为true。\n  other 时为true。\n- clarify(string): 仅complete=false时填写，向用户追问缺失的信息，简洁一句话，不超过50字。complete=true时为null。\n";
    private static final String AUDIT_CHECK_PROMPT = "你是信息审计模块。对比【用户消息】和【已提取参数】，检查用户消息中是否还有被遗漏的相关信息。\n\n【用户消息】\n%s\n\n【已提取参数】\n%s\n\n【当前意图】%s\n可提取参数列表：%s\n\n只关注明确出现在用户消息中的信息，不要推断。\n如果有遗漏，返回 {\"missed\":{\"field\":\"值\"}}\n没有遗漏返回 {\"missed\":null}\n只返回JSON，不要其他文字。\n";
    private static final Map<String, String> CITY_EN_MAP = Map.ofEntries(Map.entry("北京", "Beijing"), Map.entry("上海", "Shanghai"), Map.entry("广州", "Guangzhou"), Map.entry("深圳", "Shenzhen"), Map.entry("成都", "Chengdu"), Map.entry("杭州", "Hangzhou"), Map.entry("武汉", "Wuhan"), Map.entry("南京", "Nanjing"), Map.entry("重庆", "Chongqing"), Map.entry("天津", "Tianjin"), Map.entry("西安", "Xi'an"), Map.entry("苏州", "Suzhou"), Map.entry("长沙", "Changsha"), Map.entry("郑州", "Zhengzhou"), Map.entry("东莞", "Dongguan"), Map.entry("青岛", "Qingdao"), Map.entry("沈阳", "Shenyang"), Map.entry("宁波", "Ningbo"), Map.entry("昆明", "Kunming"), Map.entry("大连", "Dalian"), Map.entry("厦门", "Xiamen"), Map.entry("福州", "Fuzhou"), Map.entry("无锡", "Wuxi"), Map.entry("合肥", "Hefei"), Map.entry("哈尔滨", "Harbin"), Map.entry("济南", "Jinan"), Map.entry("佛山", "Foshan"), Map.entry("长春", "Changchun"), Map.entry("温州", "Wenzhou"), Map.entry("石家庄", "Shijiazhuang"), Map.entry("南宁", "Nanning"), Map.entry("贵阳", "Guiyang"), Map.entry("南昌", "Nanchang"), Map.entry("海口", "Haikou"), Map.entry("兰州", "Lanzhou"), Map.entry("太原", "Taiyuan"), Map.entry("银川", "Yinchuan"), Map.entry("西宁", "Xining"), Map.entry("呼和浩特", "Hohhot"), Map.entry("拉萨", "Lhasa"), Map.entry("乌鲁木齐", "Urumqi"), Map.entry("珠海", "Zhuhai"), Map.entry("中山", "Zhongshan"), Map.entry("惠州", "Huizhou"), Map.entry("江门", "Jiangmen"), Map.entry("汕头", "Shantou"), Map.entry("徐州", "Xuzhou"), Map.entry("常州", "Changzhou"), Map.entry("烟台", "Yantai"), Map.entry("漳州", "Zhangzhou"), Map.entry("保定", "Baoding"), Map.entry("邯郸", "Handan"), Map.entry("洛阳", "Luoyang"), Map.entry("嘉兴", "Jiaxing"), Map.entry("绍兴", "Shaoxing"), Map.entry("台州", "Taizhou"), Map.entry("金华", "Jinhua"), Map.entry("潍坊", "Weifang"), Map.entry("淄博", "Zibo"), Map.entry("临沂", "Linyi"), Map.entry("威海", "Weihai"), Map.entry("济宁", "Jining"), Map.entry("德州", "Dezhou"), Map.entry("襄阳", "Xiangyang"), Map.entry("宜昌", "Yichang"), Map.entry("岳阳", "Yueyang"), Map.entry("株洲", "Zhuzhou"), Map.entry("衡阳", "Hengyang"), Map.entry("柳州", "Liuzhou"), Map.entry("桂林", "Guilin"), Map.entry("绵阳", "Mianyang"), Map.entry("宜宾", "Yibin"), Map.entry("大理", "Dali"), Map.entry("丽江", "Lijiang"), Map.entry("三亚", "Sanya"), Map.entry("香港", "Hong Kong"), Map.entry("澳门", "Macau"), Map.entry("台北", "Taipei"));
    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal();
    private final ThreadLocal<OutputStream> outputStreamLocal = new ThreadLocal();
    private final ThreadLocal<StringBuilder> resultHolderLocal = new ThreadLocal();
    private static final Pattern WEEKDAY_PATTERN = Pattern.compile("(这周|本周|上周|上星期|这星期|星期)([一二三四五六日天])");
    private static final List<TimePattern> TIME_PATTERNS = List.of(new TimePattern(Pattern.compile("昨天|昨日|前一天"), m -> LocalDate.now(CN_ZONE).minusDays(1L)), new TimePattern(WEEKDAY_PATTERN, m -> {
        int v = WEEKDAY_MAP.get(m.group(2));
        int weekOffset = "上周".equals(m.group(1)) || "上星期".equals(m.group(1)) ? 1 : 0;
        return LocalDate.now(CN_ZONE).minusWeeks(weekOffset).with(DayOfWeek.of(v));
    }), new TimePattern(Pattern.compile("(\\d{1,2})[月.](\\d{1,2})[号日]?"), m -> {
        int month = Integer.parseInt(m.group(1));
        int day = Integer.parseInt(m.group(2));
        return LocalDate.of(LocalDate.now(CN_ZONE).getYear(), month, day);
    }));
    private static final String[] DAY_NAMES = new String[]{"一", "二", "三", "四", "五", "六", "日"};
    private static final String TRAINING_PLAN_GEN_PROMPT = "你是健身助手Tatan，请根据用户信息生成或调整一周训练计划。\n\n规则：\n1. 输出完整7天，从星期一到星期日；休息日也写出来\n2. 默认训练节奏必须固定为：星期一训练、星期二训练、星期三休息、星期四训练、星期五训练、星期六休息、星期日休息；只有当用户明确提出别的节奏（如练三休一、每天都练、每周只练4天、周末也练）时，才允许覆盖默认节奏\n3. 先确定当天训练的主肌群，再从下方该肌群对应的”训练动作”里选训练动作；严禁跨肌群乱选\n3.5. 带有[收藏]标记的动作是用户收藏的，优先选用但也兼顾动作多样性，不要全部只选收藏动作\n4. 热身必须从当天训练主肌群对应的”热身动作”里选；严禁把全身热身、其他肌群热身、正式训练动作拿来充当热身\n5. 如果某天写的是”背部”，那么热身和训练动作都必须优先来自背部对应列表；胸部、腿部、肩部、手臂、核心同理\n6. 动作名必须逐字来自下方给定动作库，不能改写、不能自己发明、不能拼接库里不存在的动作\n6.5. 严格遵守用户器械约束：如果用户画像/偏好中明确要求徒手、无器械、只有哑铃等，绝不能推荐不符合约束的动作；动作库中不符合约束的视为不可用\n7. 每天分成三个部分：热身、训练、拉伸\n8. 每天训练动作 4-5 个，不写组数次数，不写解释\n9. 同肌群尽量错开，避免连续两天重复同一主肌群\n10. 如果有【用户当前已有的训练计划】，优先按用户反馈做局部调整；没有就直接新生成\n11. 必须使用 Markdown 表格格式输出，严格按下方示例格式，不要输出任何非表格内容\n12. 严禁输出”注：””说明：””循环开始””参考上周””若需调整””以上计划”等附加内容\n13. 星期标题用 ### 三级标题，训练日用 Markdown 表格展示，休息日用 > 引用格式\n14. 星期标题只能写成”星期X · 肌群”或”星期X · 休息日”\n15. 训练表格有4列：| 阶段 | 内容 |\n16. 训练行只能列动作，不要补括号解释，不要补原因，不要补恢复建议\n17. 在你正式输出前，先逐天自检：检查”标题肌群、热身动作、训练动作”三者是否属于同一肌群；如果不一致，说明结果不合格，必须重排后再输出\n18. 在你正式输出前，再自检默认模式的星期安排是否严格等于：一二训练、三休息、四五训练、六日休息；只要任何一天不符合，说明结果不合格，必须重排后再输出\n19. 默认模式下严禁输出周六训练、周日训练、连续3天训练，除非用户明确提出这类要求\n20. 每天之间用空行分隔，除表格和标题外不要输出任何额外文字\n\n{existingPlan}\n\n输出格式（严格遵循）：\n### 星期一 · 胸部\n\n| 阶段 | 内容 |\n|------|------|\n| 🔥 热身 | 开合跳 |\n| 💪 训练 | 杠铃卧推、上斜哑铃卧推、蝴蝶机夹胸、双杠臂屈伸 |\n| 🧘 拉伸 | 手臂交叉伸展 |\n\n### 星期二 · 背部\n\n| 阶段 | 内容 |\n|------|------|\n| 🔥 热身 | 高抬腿 |\n| 💪 训练 | 引体向上、杠铃划船、高位下拉、坐姿划船 |\n| 🧘 拉伸 | 手臂环绕 |\n\n### 星期三 · 休息日\n\n> 休息，可做30分钟低强度有氧（散步/快走）\n\n### 星期四 · 腿部\n...（以此类推，每天用相同格式，拉伸动作也必须从动作库选）\n\n{userInfo}\n{exerciseCatalog}\n";
    private static final String DIET_PLAN_GEN_PROMPT = "你是健身助手Tatan，请根据用户信息和食物营养参考生成饮食推荐。\n\n规则：\n1. 优先从下方【食物营养参考】里选食物；带[我的食物]标记的是用户自己上传的，优先考虑选用，但也兼顾多样性\n1.5. 严格遵守用户饮食约束：如果用户画像/偏好中明确说明不会烹饪、只能吃即食/外卖等，严禁推荐需要复杂烹饪的食物；食物库中不符合约束的视为不可用\n2. 先判断用户要的是全天饮食还是某一餐\n{mealRequestInstruction}\n3. 每种食物必须标注具体克数（如“鸡胸肉150g”），克数根据用户目标热量合理分配\n4. 根据用户的目标热量和三大宏量比例（蛋白质30%、碳水40%、脂肪30%）计算每餐用量。\n【关键】营养素计算公式：实际营养素(g) = 推荐克数 ÷ 食物基准克数 × 营养参考表中该营养素值。\n例：鸡胸肉基准100g含蛋白质31g，推荐150g则蛋白质=150÷100×31=46.5g。\n严禁把食物克数直接当作营养素克数！必须按公式换算后再累加。\n4.5. 【目标量已预计算，直接使用】以下目标量由系统计算，严禁AI自行计算，必须原样填入营养估算表的目标量列：\n{targetNutrients}\n4.6. 【营养精度校验】输出前必须自检：把每餐每种食物的碳水量逐一累加，确保碳水总摄入量等于目标碳水总量（目标热量×40%÷4kcal/g）。如果总和不等于目标量，必须调整食物克数直到一致。蛋白质和脂肪同理校验。\n5. 每餐用 Markdown 表格展示\n6. 必须使用 Markdown 表格格式输出，严格按下方示例格式\n7. 严禁输出“注：”“说明：”“建议：”“可替换”“可调整”“如果没有”之类附加文字\n8. 除表格和标题及汇总外，不要输出任何其他段落\n\n输出格式（严格遵循）：\n\n### 一日食谱推荐\n\n| 餐次 | 推荐食物 |\n|------|----------|\n| 🌅 早餐 | 即食鸡胸肉100g + 水煮蛋2个(约100g) + 无糖豆浆300ml |\n| 🌞 午餐 | 去皮鸡腿150g + 糙米饭150g + 清炒西兰花200g |\n| 🌙 晚餐 | 三文鱼120g + 红薯200g + 蒜蓉菠菜200g |\n| 🍌 加餐 | 牛奶250ml + 香蕉1根(约120g) |\n\n### 今日营养估算\n\n| 营养素 | 摄入量 | 目标量 | 评价 |\n|--------|--------|--------|------|\n| 热量 | 约1850kcal | 2000kcal | 合理 |\n| 蛋白质 | 约110g | 150g | 偏低，建议加餐补充 |\n| 碳水化合物 | 约220g | 200g | 合理 |\n| 脂肪 | 约55g | 67g | 合理 |\n\n【食物营养参考】\n{foodKnowledge}\n\n{userInfo}\n";
    private static final String REPORT_TRAINING_ISSUE_PROMPT = "你是健身助手Tatan。用户在训练中遇到了身体不适或困难，需要你分析问题并生成改进后的训练计划。\n\n分析要求：\n1. 结合用户伤病历史，判断反馈的动作是否适合该用户\n2. 检查当前计划中该动作的重量/组数是否合理（对比训练水平）\n3. 如果有伤病，推荐替代动作（从动作库中选择）\n\n输出格式：\n第一部分：2-4句话的分析，说明问题原因和改进方向（纯文本，不用markdown）\n第二部分：完整的改进后7天训练计划（必须用以下标准markdown格式）\n\n### 星期一 · 肌群\n热身：开合跳3组15个\n正式训练：动作名（X组，X次）、动作名（X组，X次）\n拉伸：动作名1分钟\n\n### 星期二 · 休息\n\n（以此类推覆盖7天，休息日也要写明\"休息\"，拉伸动作必须从动作库选）\n\n注意：\n- 必须使用标准markdown格式，### 标题格式为\"### 星期X · 肌群\"\n- 动作名称必须从下方动作库中选，不要自己编造\n- 训练计划部分前面不要输出任何额外说明文字\n";
    private static final String REPORT_DIET_ISSUE_PROMPT = "你是健身助手Tatan。用户反馈饮食不足或饥饿，需要你分析热量摄入情况并生成改进后的饮食计划。\n\n分析要求：\n1. 计算热量缺口：用户目标热量 vs 近期实际摄入\n2. 检查宏量营养素是否合理（蛋白质是否充足、碳水是否太低）\n3. 结合训练消耗判断是否需要额外补充\n\n输出格式：\n第一部分：2-4句话的分析，说明热量缺口和营养素问题（纯文本，不用markdown）\n第二部分：改进后的全天饮食计划（必须用以下标准markdown表格格式）\n\n### 改进食谱推荐\n\n| 餐次 | 推荐食物 |\n|------|----------|\n| 早餐 | 食物A 150g + 食物B 200g |\n| 午餐 | 食物C 150g + 食物D 100g |\n| 晚餐 | 食物E 120g + 食物F 200g |\n| 加餐 | 食物G 250ml |\n\n注意：\n- 每种食物必须标注具体克数\n- 食物名称必须从下方食物营养参考中选\n- 饮食计划表格前面不要输出任何额外说明文字\n";
    private static final String GENERAL_SYSTEM_PROMPT = "你是健身助手Tatan，风格简洁亲切。\n\n回复规则：\n1. 健身专业问题（训练、饮食、营养、减脂、增肌、伤病、康复、补剂、器械等）\n   → 依据上下文中【知识库检索结果】回答；如果没有知识库结果，只能回复：知识库暂无该知识，我无法回答\n\n2. 日常闲聊、问候、情感倾诉、通用话题（天气、心情、电影、自我介绍等）\n   → 像朋友一样自由回答，风格轻松，≤100字\n\n3. 非健身的专业请求（写代码、翻译、做题、写作等）\n   → 回复：我主要擅长健身方面的问题，关于[用户提到的领域]建议咨询专业工具哦。有健身问题随时问我~\n\n情绪感知规则：\n- 如果上下文中用户情绪状态为\"低落\"，回复时主动关心和鼓励，语气温暖，适当提出放松建议\n- 如果用户情绪状态为\"积极\"，给予肯定和激励，语气热情\n- 如果没有情绪标注，正常回复即可\n\n回复格式：\n- 纯文本，不用markdown\n- 第一行用 emoji 开头\n";
    private static final String GENERAL_SYSTEM_PROMPT_WITH_KNOWLEDGE = "你是健身助手Tatan，风格简洁亲切。上下文中已提供【知识库检索结果】，请依据它回答健身专业问题。\n\n回复规则：\n1. 健身专业问题 → 依据【知识库检索结果】回答，优先使用知识库内容\n2. 日常闲聊 → 像朋友一样自由回答，风格轻松，≤100字\n3. 非健身专业请求 → 回复：我主要擅长健身方面的问题，建议咨询专业工具哦\n\n情绪感知规则：\n- 用户情绪\"低落\"→主动关心鼓励，语气温暖\n- 用户情绪\"积极\"→给予肯定激励，语气热情\n- 没有情绪标注 → 正常回复\n\n回复格式：纯文本，不用markdown，第一行用emoji开头\n";
    private static final String RAG_SYSTEM_PROMPT = "你是健身助手Tatan。你现在处于严格 RAG 回答模式，只能依据下方【知识库检索结果】回答。\n\n【硬性规则】\n1. 先判断知识库条目是否能直接回答用户当前问题。\n2. “直接相关”必须满足：知识条目里的主题、动作/食物/营养/训练目标/伤病场景，与用户核心问题一致，并且能支持主要结论。\n3. 只是同属健身领域、只提到相近肌群、只出现泛泛训练原则，都不算直接相关。\n4. 如果没有直接相关知识条，或检索结果明显跑题，必须只回复：知识库暂无该知识，我无法回答\n5. 如果知识条直接相关，只能使用知识库中的内容回答；严禁补充常识、经验、推测、外部知识或自创建议。\n6. 如果知识条只覆盖了部分问题，只回答覆盖到的部分，并说明“知识库只覆盖到以上内容”。\n7. 不要因为用户画像、今日记录、历史上下文而扩展知识库没有提供的专业结论。\n8. 纯文本，不用 markdown，不输出判断过程。\n\n【相关性示例】\n- 用户问“胸肌训练怎么做”，知识库有“胸部训练原则” → 直接相关，可以回答。\n- 用户问“筋膜枪有用吗”，知识库只有“手臂训练原则”“胸部训练原则” → 不直接相关，只回复固定暂无内容。\n- 用户问“蛋白质怎么补”，知识库有“增肌期蛋白质摄入量” → 直接相关，可以回答。\n- 用户问“膝盖痛能不能深蹲”，知识库只有“腿部训练动作列表” → 不直接相关，只回复固定暂无内容。\n\n【知识相关时的回复格式】\n- 第一句直接回答结论。\n- 后面用 2-4 点说明知识库支持的依据。\n- 最后一行可以给 1 条实用建议，但必须来自知识库内容。\n- 总字数不超过 260 字。\n\n【知识库检索结果】\n{knowledge}\n";
    private static final String FOOD_IDENTIFY_PROMPT = "你是食物识别助手。识别图片中的食物并预估分量。\n严格只返回一行 JSON，不要 markdown、不要解释、不要代码块。\n格式：{\"name\":\"食物名\",\"nameEn\":\"English name\",\"grams\":预估克数}\n规则：\n1. name：只写中文食物名称，多种食物用顿号分隔，如\"鸡胸肉、西兰花、米饭\"\n2. nameEn：对应英文名称，多种食物用逗号分隔，如\"chicken breast, broccoli, cooked rice\"。如果是营养标签图片无法判断英文，填 null\n3. grams 只填一个纯数字（整数），代表图片中食物的总克数，禁止填写任何文字说明。\n   严格根据图片中食物的视觉大小判断，禁止随意编造数值。\n   请对照常见实物参考：\n   - 一个普通汉堡约200g\n   - 一碗米饭约150g、一盘米饭约250g\n   - 一个鸡蛋约50g\n   - 一块鸡胸肉约150g\n   - 一块牛排约200g\n   - 一份沙拉约150g\n   - 一个苹果约200g、一根香蕉约120g\n   - 一杯牛奶约250ml\n   - 一个馒头约100g、一碗面条约250g\n   - 一块披萨(标准切片)约100g\n   - 仅供参考，必须根据图片中食物的实际大小比例进行调整\n4. 如果图片中无法判断分量大小，grams 填 null\n5. 如果图片是营养成分表标签，从标签上读取食物名称，grams 填 null\n";

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
                log.info("【流程】quickMatch命中, 耗时{}ms", (Object)(System.currentTimeMillis() - t0));
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
            log.info("【流程】初始化完成(DB+Redis), 耗时{}ms", (Object)(t1 - t0));
            CompletableFuture<String> purificationFuture = this.profileExtractionService.purifyDirtyText(userId);
            DirectRouteResult direct = this.directRoute(message, userId, user, userRecord);
            if (direct != null) {
                log.info("【流程】directRoute命中, 耗时{}ms", (Object)(System.currentTimeMillis() - t0));
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
                this.writeSseStatus(outputStream, "正在分析意图...");
            }
            catch (Exception exception) {
                // empty catch block
            }
            long t2 = System.currentTimeMillis();
            ClassifyResult classifyResult = this.classifyAndAudit(userId, message, user, history, pendingContext, userProfile);
            long t3 = System.currentTimeMillis();
            log.info("【流程】classifyAndAudit耗时{}ms, 结果={}", (Object)(t3 - t2), (Object)classifyResult);
            if (classifyResult == null) {
                String fullResponse2;
                log.warn("【流程】classifyAndAudit返回null, 走闲聊兜底");
                try {
                    this.writeSseStatus(outputStream, "正在回复...");
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
                log.info("【流程】追问分支: intent={}, clarify={}", (Object)intent, (Object)clarify);
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
                log.info("【流程】闲聊分支(other), 耗时{}ms", (Object)(System.currentTimeMillis() - t0));
                try {
                    this.writeSseStatus(outputStream, "正在回复...");
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
            log.info("【流程】auditCheck耗时{}ms", (Object)(t5 - t4));
            params = this.autoFillFromDB(intent, params, userId);
            String purifiedText = this.profileExtractionService.getPurifiedText(userId);
            this.outputStreamLocal.set(outputStream);
            this.resultHolderLocal.set(resultHolder);
            try {
                String statusHint = intent.startsWith("generate_") ? "正在生成计划..." : (intent.startsWith("query_") || intent.startsWith("get_") || intent.equals("search_knowledge") ? "正在查询数据..." : "正在处理...");
                try {
                    this.writeSseStatus(outputStream, statusHint);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                long t6 = System.currentTimeMillis();
                ToolCallResult result = this.dispatchIntent(userId, message, user, history, userRecord, purifiedText, outputStream, resultHolder, intent, params);
                long t7 = System.currentTimeMillis();
                log.info("【流程】dispatchIntent({})耗时{}ms, result={}", new Object[]{intent, t7 - t6, result});
                if (result != null) {
                    String fullResponse4;
                    if (replyTypeHolder != null) {
                        replyTypeHolder.set(result.replyType());
                    }
                    if (!(fullResponse4 = resultHolder.toString()).isBlank()) {
                        this.saveOrUpdateChatHistory(userId, message, fullResponse4);
                    }
                    log.info("【流程】总耗时{}ms, replyType={}", (Object)(System.currentTimeMillis() - t0), (Object)result.replyType());
                    return;
                }
            }
            finally {
                this.outputStreamLocal.remove();
                this.resultHolderLocal.remove();
            }
            log.warn("【流程】dispatchIntent返回null, 走闲聊兜底, intent={}", (Object)intent);
            try {
                this.writeSseStatus(outputStream, "正在回复...");
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
            log.info("【流程】总耗时{}ms, replyType=general_chat(fallback)", (Object)(System.currentTimeMillis() - t0));
        }
        catch (Exception e) {
            log.error("SSE流式对话异常", (Throwable)e);
            try {
                this.writeSseDataGradually(outputStream, "不好意思，AI 调用失败，请联系工作人员或稍等片刻再试。");
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
        log.info("【意图分发】userId={}, intent={}", (Object)userId, (Object)intent);
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
                log.warn("【意图分发】未知intent: {}", (Object)intent);
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
        if (msg.length() <= 6 && Pattern.compile(".*(你好|hi|hello|嗨|hey|在吗).*").matcher(msg).matches()) {
            return "你好！我是你的智能健身助手 Tatan，有什么可以帮你的吗？";
        }
        if (Pattern.compile(".*(你是谁|你叫什么|介绍.*自己|你能做什么).*").matcher(msg).matches()) {
            return "我是 Tatan 智能健身助手，可以帮你：\n1. 回答健身相关问题\n2. 制定个性化训练计划\n3. 提供饮食建议\n4. 聊聊健身心得，给你打气加油！";
        }
        if (msg.length() <= 6 && Pattern.compile(".*(谢谢|感谢|多谢|thanks).*").matcher(msg).matches()) {
            return "不客气！有任何健身问题随时问我，我会一直在这里陪你。";
        }
        return null;
    }

    private DirectRouteResult directRoute(String message, Long userId, User user, UserRecord userRecord) {
        String msg = message.toLowerCase();
        if (msg.equals("今天做了什么") || msg.equals("今天记录了什么") || msg.equals("打卡总结")) {
            String data = this.buildTodayRecordReply(user, userRecord);
            String toolData = data != null && !data.isBlank() ? data : "暂无记录";
            return new DirectRouteResult("today_exercise_record", toolData, "你是健身助手Tatan。根据下方工具返回的今日记录数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整呈现所有训练和饮食数据，不能省略任何记录。");
        }
        if (msg.equals("这周做了什么") || msg.equals("本周总结") || msg.equals("这周总结")) {
            String data = this.buildWeekRecordReply(user, userRecord);
            String toolData = data != null && !data.isBlank() ? data : "暂无记录";
            return new DirectRouteResult("general_chat", toolData, "你是健身助手Tatan。根据下方工具返回的本周记录数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整呈现所有数据，不能省略任何一天的记录。");
        }
        return null;
    }

    private String streamSummarizeWithToolData(Long userId, User user, String userMessage, String toolData, String systemPrompt, OutputStream outputStream, StringBuilder resultHolder) {
        try {
            AiModelConfig.ModelProvider provider = this.requireProvider(this.resolvePurificationModel(user));
            String userContent = "用户问：" + userMessage + "\n\n工具返回数据：\n" + toolData;
            String summarized = this.callAiApiStreamWithProvider(systemPrompt, userContent, outputStream, provider, 1024);
            resultHolder.append(summarized);
            return summarized;
        }
        catch (Exception e) {
            log.error("streamSummarizeWithToolData异常", (Throwable)e);
            String fallback = "数据获取成功，但AI总结失败，请稍后重试。";
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
                userMsg.append("【对话历史】\n");
                if (hasSummary) {
                    userMsg.append("长期摘要：\n").append(summary.trim()).append("\n\n");
                }
                if (hasRecentMessages) {
                    int start = Math.max(0, recentMessages.size() - 10);
                    userMsg.append("最近对话（最多5轮）：\n");
                    for (int i = start; i < recentMessages.size(); ++i) {
                        String item = recentMessages.get(i);
                        if (item == null || item.isBlank()) continue;
                        userMsg.append(item.trim()).append("\n");
                    }
                    userMsg.append("\n");
                }
            }
            if (pendingContext != null && !pendingContext.isBlank()) {
                userMsg.append("【待补全信息】上次追问了：\n").append(pendingContext).append("\n");
            }
            if (userProfile != null && userProfile.getUserProfileText() != null && !userProfile.getUserProfileText().isBlank()) {
                userMsg.append("【用户画像摘要】\n").append(userProfile.getUserProfileText()).append("\n");
            }
            userMsg.append("【用户最新消息】\n").append(message);
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
                log.warn("【分类+审计】API返回null");
                return null;
            }
            List choices = (List)response.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("【分类+审计】choices为空, response={}", (Object)response);
                return null;
            }
            Map msg = (Map)((Map)choices.get(0)).get("message");
            String string = content = msg != null ? (String)msg.get("content") : null;
            if (content == null || content.isBlank()) {
                log.warn("【分类+审计】content为空, msg={}", (Object)msg);
                return null;
            }
            String json = content.trim();
            log.info("【分类+审计】AI原始返回: {}", (Object)json);
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            try {
                intentMap = (Map)JSON_MAPPER.readValue(json, Map.class);
            }
            catch (Exception e) {
                log.warn("【分类+审计】JSON解析失败: {}", (Object)json);
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
            log.info("【分类+审计】userId={}, intent={}, complete={}, params={}", new Object[]{userId, intent, complete, params.keySet()});
            return new ClassifyResult(intent, params, complete, clarify);
        }
        catch (Exception e) {
            log.warn("【分类+审计】异常", (Throwable)e);
            return null;
        }
    }

    private Map<String, Object> auditCheck(String intent, Map<String, Object> params, String message, User user) {
        if ("other".equals(intent)) {
            return params;
        }
        String extractableParams = switch (intent) {
            case "save_diet" -> "food(必填), meal(可选)";
            case "save_exercise" -> "exercises(必填数组, 每项含name/sets/duration)";
            case "save_weight" -> "weight_kg(必填), note(可选)";
            case "query_training" -> "query_type(必填), target_day(可选)";
            case "query_diet" -> "query_type(必填), meal_type(可选)";
            case "query_weight" -> "range(可选)";
            case "get_today_records", "get_week_records" -> "无参数";
            case "get_history_record" -> "date_description(必填)";
            case "search_knowledge" -> "query(必填)";
            case "generate_training_plan" -> "user_request(必填), fitness_goal(可选), days_per_week(可选), equipment(可选), medical_history(可选)";
            case "generate_diet_plan" -> "user_request(必填), meal_scope(可选)";
            case "report_training_issue" -> "issue_description(必填), affected_exercise(可选)";
            case "report_diet_issue" -> "issue_description(必填), meal_context(可选)";
            case "update_location" -> "city(必填)";
            case "delete_record" -> "type(必填), target(可选)";
            case "update_weight" -> "weight_kg(必填)";
            default -> "无参数";
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
                Map<String, Object> missed = (Map<String, Object>)missedObj;
                for (Map.Entry<String, Object> entry : missed.entrySet()) {
                    String key = entry.getKey();
                    if (params.containsKey(key)) continue;
                    params.put(key, entry.getValue());
                    log.info("【审计补漏】补入: {}={}", (Object)key, entry.getValue());
                }
            }
            return params;
        }
        catch (Exception e) {
            log.warn("【审计检查】异常，返回原params", (Throwable)e);
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
            this.stringRedisTemplate.opsForValue().set(key, JSON_MAPPER.writeValueAsString(pending), 5L, TimeUnit.MINUTES);
            log.info("【追问保存】userId={}, intent={}", (Object)userId, (Object)intent);
        }
        catch (Exception e) {
            log.warn("【追问保存】Redis写入失败", (Throwable)e);
        }
    }

    private void clearPendingIntent(Long userId) {
        try {
            this.stringRedisTemplate.delete("chat:pending_intent:" + userId);
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
            String reply = "好的，已记录" + mealLabel + "：" + food;
            String macroSummary = this.buildMacroSummaryText(userId, LocalDate.now(CN_ZONE));
            if (!macroSummary.isEmpty()) {
                reply = reply + "\n" + macroSummary;
            }
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("today_diet_record", "");
        }
        catch (Exception e) {
            log.error("handleSaveDietIntent异常", (Throwable)e);
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
            String sessionName = String.join((CharSequence)"、", names);
            this.exerciseRecordService.saveStructuredRecord(userId, today, recordTime, sessionName, totalDuration > 0 ? Integer.valueOf(totalDuration) : null, null, sessionName, "chat", items);
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            String reply = "好的，已记录运动：" + sessionName;
            if (todayBurned > 0) {
                reply = reply + String.format("（今日累计消耗约%d大卡）", todayBurned);
            }
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("today_exercise_record", "");
        }
        catch (Exception e) {
            log.error("handleSaveExerciseIntent异常", (Throwable)e);
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
                    log.warn("{}: 无法解析weight_kg={}", (Object)(isUpdate ? "update_weight" : "save_weight"), weightObj);
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
            String reply = isUpdate ? "好的，已修正今日体重为：" + weightStr + "kg" : "好的，已记录今日体重：" + weightStr + "kg";
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("{}异常", (Object)(isUpdate ? "handleUpdateWeightIntent" : "handleSaveWeightIntent"), (Object)e);
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
                String reply = "每日热量目标应在 500~10000 kcal 之间，请确认后重新设置";
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
            Object reply = String.format("已将每日摄入热量目标设为 %.0fkcal", calories);
            try {
                User user = (User)this.userService.getById(userId);
                if (user != null && user.getWeight() != null) {
                    reply = (String)reply + String.format("（当前体重%.1fkg", user.getWeight());
                    if (profile.getTargetWeight() != null && profile.getTargetWeight() > 0.0) {
                        double diff = user.getWeight() - profile.getTargetWeight();
                        reply = (String)reply + String.format("，距目标%.1fkg还差%.1fkg", profile.getTargetWeight(), Math.abs(diff));
                    }
                    reply = (String)reply + "）";
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
            log.error("handleSetDailyCaloriesIntent异常", (Throwable)e);
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
                String reply = "目标体重应在 30~300 kg 之间，请确认后重新设置";
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
            Object reply = String.format("已将目标体重设为 %.1fkg", targetWeight);
            try {
                User user = (User)this.userService.getById(userId);
                if (user != null && user.getWeight() != null) {
                    double diff = user.getWeight() - targetWeight;
                    reply = Math.abs(diff) > 0.1 ? (String)reply + String.format("，当前%.1fkg，%s%.1fkg", user.getWeight(), diff > 0.0 ? "还需减" : "还需增", Math.abs(diff)) : (String)reply + "，当前体重已接近目标，继续保持！";
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            reply = (String)reply + "，加油！";
            this.writeSseDataGradually(this.outputStreamLocal.get(), (String)reply);
            this.resultHolderLocal.get().append((String)reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleSetTargetWeightIntent异常", (Throwable)e);
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
            if (city.endsWith("市")) {
                city = city.substring(0, city.length() - 1);
            }
            if ((user = (User)this.userService.getById(userId)) == null) {
                return null;
            }
            user.setCity(city);
            user.setCityEn(CITY_EN_MAP.getOrDefault(city, ""));
            this.userService.updateById(user);
            log.info("update_location: userId={}, city={}, cityEn={}", new Object[]{userId, city, user.getCityEn()});
            String reply = "好的，已更新你的位置为" + city + "，以后天气提醒会按" + city + "的天气来推送。";
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleUpdateLocationIntent异常", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleReportTrainingIssueIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> params) {
        try {
            String dietPlan;
            LocalDate today = LocalDate.now(CN_ZONE);
            LocalDate yesterday = today.minusDays(1L);
            StringBuilder context = new StringBuilder();
            context.append("用户问题：").append(this.toCleanString(params.get("issue_description")));
            String affectedExercise = this.toCleanString(params.get("affected_exercise"));
            if (!affectedExercise.isBlank()) {
                context.append("，涉及动作：").append(affectedExercise);
            }
            context.append("\n\n");
            context.append(this.buildUserInfo(user)).append("\n");
            String trainingPlan = this.buildTrainingPlanText(userId);
            if (trainingPlan != null && !trainingPlan.isBlank()) {
                context.append("【当前训练计划】\n").append(trainingPlan).append("\n\n");
            }
            if ((dietPlan = this.buildDietPlanText(userId)) != null && !dietPlan.isBlank()) {
                context.append("【当前饮食计划】\n").append(dietPlan).append("\n\n");
            }
            Map<String, Object> todayMacro = this.dietRecordService.getDayMacroSummary(userId, today);
            Map<String, Object> yesterdayMacro = this.dietRecordService.getDayMacroSummary(userId, yesterday);
            context.append("【近期饮食数据】\n");
            context.append("今日摄入：热量").append(todayMacro.get("calories")).append("kcal，蛋白质").append(todayMacro.get("protein")).append("g，碳水").append(todayMacro.get("carbs")).append("g，脂肪").append(todayMacro.get("fat")).append("g\n");
            context.append("昨日摄入：热量").append(yesterdayMacro.get("calories")).append("kcal，蛋白质").append(yesterdayMacro.get("protein")).append("g，碳水").append(yesterdayMacro.get("carbs")).append("g，脂肪").append(yesterdayMacro.get("fat")).append("g\n\n");
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            int yesterdayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, yesterday);
            context.append("【近期训练消耗】\n今日：").append(todayBurned).append("大卡，昨日：").append(yesterdayBurned).append("大卡\n\n");
            String exerciseCatalog = this.buildExerciseCatalog(userId, null);
            context.append("【动作库】\n").append(exerciseCatalog).append("\n\n");
            context.append("用户的具体要求：").append(message);
            String aiReply = this.callAiApiStream(REPORT_TRAINING_ISSUE_PROMPT, context.toString(), outputStream, null, 1500);
            if (!this.isBlank(aiReply)) {
                resultHolder.append(aiReply);
            }
            return new ToolCallResult("training_plan", "training");
        }
        catch (Exception e) {
            log.error("handleReportTrainingIssueIntent异常", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleReportDietIssueIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> params) {
        try {
            String dietPlan;
            LocalDate today = LocalDate.now(CN_ZONE);
            LocalDate yesterday = today.minusDays(1L);
            StringBuilder context = new StringBuilder();
            context.append("用户问题：").append(this.toCleanString(params.get("issue_description")));
            String mealContext = this.toCleanString(params.get("meal_context"));
            if (!mealContext.isBlank()) {
                context.append("，相关餐次：").append(mealContext);
            }
            context.append("\n\n");
            context.append(this.buildUserInfo(user)).append("\n");
            String trainingPlan = this.buildTrainingPlanText(userId);
            if (trainingPlan != null && !trainingPlan.isBlank()) {
                context.append("【当前训练计划】\n").append(trainingPlan).append("\n\n");
            }
            if ((dietPlan = this.buildDietPlanText(userId)) != null && !dietPlan.isBlank()) {
                context.append("【当前饮食计划】\n").append(dietPlan).append("\n\n");
            }
            Map<String, Object> todayMacro = this.dietRecordService.getDayMacroSummary(userId, today);
            Map<String, Object> yesterdayMacro = this.dietRecordService.getDayMacroSummary(userId, yesterday);
            context.append("【近期饮食数据】\n");
            context.append("今日摄入：热量").append(todayMacro.get("calories")).append("kcal，蛋白质").append(todayMacro.get("protein")).append("g，碳水").append(todayMacro.get("carbs")).append("g，脂肪").append(todayMacro.get("fat")).append("g\n");
            context.append("昨日摄入：热量").append(yesterdayMacro.get("calories")).append("kcal，蛋白质").append(yesterdayMacro.get("protein")).append("g，碳水").append(yesterdayMacro.get("carbs")).append("g，脂肪").append(yesterdayMacro.get("fat")).append("g\n\n");
            int todayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
            int yesterdayBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, yesterday);
            context.append("【近期训练消耗】\n今日：").append(todayBurned).append("大卡，昨日：").append(yesterdayBurned).append("大卡\n\n");
            String foodCatalog = this.buildFoodCatalog(userId, null);
            context.append("【食物营养参考】\n").append(foodCatalog).append("\n\n");
            context.append("用户的具体要求：").append(message);
            String aiReply = this.callAiApiStream(REPORT_DIET_ISSUE_PROMPT, context.toString(), outputStream, null, 1500);
            if (!this.isBlank(aiReply)) {
                resultHolder.append(aiReply);
            }
            return new ToolCallResult("diet_plan", "diet");
        }
        catch (Exception e) {
            log.error("handleReportDietIssueIntent异常", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleQueryWeatherIntent(User user, String message, OutputStream outputStream) {
        try {
            String weatherContext = this.getWeatherContextForUser(user);
            String systemPrompt = "你是健身助手Tatan。用户在询问天气情况，请结合天气给出运动建议。如果天气恶劣建议室内训练，天气好可以鼓励户外运动。回复简洁友好，100字以内。";
            Object userMsg = message;
            userMsg = weatherContext != null ? (String)userMsg + "\n\n" + weatherContext : (String)userMsg + "\n\n（天气数据获取失败，请根据常识回答）";
            String reply = this.callAiApiStream(systemPrompt, (String)userMsg, outputStream, null, 256);
            return new ToolCallResult("query_weather", "");
        }
        catch (Exception e) {
            log.error("查询天气失败", (Throwable)e);
            return null;
        }
    }

    private ToolCallResult handleDeleteRecordIntent(Long userId, UserRecord userRecord, Map<String, Object> intentMap) {
        try {
            boolean deleted;
            int index;
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
                    String reply = "没有找到" + (target != null ? "包含\"" + target + "\"的" : "") + "训练记录";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
                    this.resultHolderLocal.get().append(reply);
                    return new ToolCallResult("general_chat", "");
                }
                deleted = this.exerciseRecordService.deleteTodayRecord(userId, today, index);
            } else {
                List<DietRecord> records = this.dietRecordService.listByUserAndDate(userId, today);
                index = this.resolveDeleteIndex(records, target);
                if (index < 0 || index >= records.size()) {
                    String reply = "没有找到" + (target != null ? "包含\"" + target + "\"的" : "") + "饮食记录";
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
            String typeName = "exercise".equals(type) ? "训练" : "饮食";
            String reply = deleted ? "好的，已删除" + typeName + "记录" : "删除失败，可能记录已不存在";
            this.writeSseDataGradually(this.outputStreamLocal.get(), reply);
            this.resultHolderLocal.get().append(reply);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleDeleteRecordIntent异常", (Throwable)e);
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
        String queryType = this.toCleanString(intentMap.getOrDefault("query_type", "已完成的记录"));
        String targetDay = this.toCleanString(intentMap.getOrDefault("target_day", "今天"));
        if ("计划安排".equals(queryType)) {
            boolean isRestDay;
            String data;
            String replyType = "today_training_plan";
            if ("全部".equals(targetDay)) {
                data = this.buildTrainingPlanText(userId);
            } else {
                int offset = "明天".equals(targetDay) ? 1 : 0;
                data = this.buildTrainingPlanDaySection(userId, offset);
            }
            if (data == null || data.isBlank()) {
                data = "暂无训练安排";
            }
            boolean bl = isRestDay = data.contains("休息日") || data.contains("休息");
            if (isRestDay) {
                replyType = "today_rest_day";
            }
            String prompt = "你是健身助手Tatan。根据下方数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整列出所有内容。";
            String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, "");
        }
        String replyType = "today_exercise_record";
        String data = this.buildTodayExerciseReply(userId);
        if (data == null || data.isBlank()) {
            data = "今天暂无训练记录";
        }
        String prompt = "你是健身助手Tatan。根据下方数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整列出每个动作。";
        String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
        return new ToolCallResult(replyType, "");
    }

    private ToolCallResult handleQueryDietIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        String queryType = this.toCleanString(intentMap.getOrDefault("query_type", "已完成的记录"));
        String mealType = this.toCleanString(intentMap.getOrDefault("meal_type", "全部"));
        if ("计划安排".equals(queryType)) {
            String section;
            String replyType = "today_diet_plan";
            String planText = this.buildDietPlanText(userId);
            Object data = planText == null || planText.isBlank() ? "还没有饮食计划" : ("全部".equals(mealType) ? planText : ((section = this.extractDietMealSection(planText, mealType)) != null && !section.isBlank() ? section : "当前饮食计划里没有" + mealType + "推荐"));
            String prompt = "你是健身助手Tatan。根据下方数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整列出每个餐次。";
            String summarized = this.streamSummarizeWithToolData(userId, user, message, (String)data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, "");
        }
        String replyType = "today_diet_record";
        String data = this.buildTodayDietReply(userId);
        if (data == null || data.isBlank()) {
            data = "今天暂无饮食记录";
        }
        String prompt = "你是健身助手Tatan。根据下方数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整列出每个餐次和食物。";
        String summarized = this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
        return new ToolCallResult(replyType, "");
    }

    private ToolCallResult handleQueryWeightIntent(Long userId, String message, User user, ChatHistory history, UserRecord userRecord, String purifiedText, OutputStream outputStream, StringBuilder resultHolder, Map<String, Object> intentMap) {
        String data;
        String range = this.toCleanString(intentMap.getOrDefault("range", "最近7天"));
        LocalDate endDate = LocalDate.now(CN_ZONE);
        int days = switch (range) {
            case "今天" -> 0;
            case "最近30天" -> 29;
            default -> 6;
        };
        LocalDate startDate = endDate.minusDays(days);
        List<UserWeightRecord> records = this.userWeightRecordService.listByUserAndDateRange(userId, startDate, endDate);
        if (records == null || records.isEmpty()) {
            data = "暂无体重记录";
        } else {
            StringBuilder ws = new StringBuilder("体重记录（").append(range).append("）：\n");
            for (UserWeightRecord r : records) {
                ws.append(r.getRecordDate()).append("：").append(r.getWeight()).append("kg\n");
            }
            data = ws.toString().trim();
        }
        String prompt = "你是健身助手Tatan。根据下方体重数据，简洁亲切地回答用户。纯文本，不用markdown。如果有变化趋势可以适当总结。";
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
                data = "暂无记录";
            }
            String prompt = "你是健身助手Tatan。根据下方数据，简洁亲切地回答用户。纯文本，不用markdown。必须完整列出所有记录。";
            this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult(replyType, savePlanType);
        }
        catch (Exception e) {
            log.error("handleQueryDataIntent异常", (Throwable)e);
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
                    String data2 = "知识库暂无该知识，我无法回答";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), data2);
                    this.resultHolderLocal.get().append(data2);
                    return new ToolCallResult("general_chat", "");
                }
                String exerciseInfo = this.searchExerciseInfo(exerciseName);
                if (exerciseInfo != null && !exerciseInfo.isBlank()) {
                    data = exerciseInfo;
                } else {
                    String data3 = "知识库暂无该知识，我无法回答";
                    this.writeSseDataGradually(this.outputStreamLocal.get(), data3);
                    this.resultHolderLocal.get().append(data3);
                    return new ToolCallResult("general_chat", "");
                }
            }
            String prompt = "你是健身助手Tatan。根据下方知识库内容，简洁专业地回答用户的健身问题。纯文本，不用markdown。如果知识库内容不足以回答，回复：知识库暂无该知识，我无法回答。";
            this.streamSummarizeWithToolData(userId, user, message, data, prompt, outputStream, resultHolder);
            return new ToolCallResult("general_chat", "");
        }
        catch (Exception e) {
            log.error("handleSearchKnowledgeIntent异常", (Throwable)e);
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
            log.warn("[Weather] 获取天气上下文失败: {}", (Object)e.getMessage());
            return null;
        }
    }

    private String getWeatherContextAsync() {
        try {
            String ip = CLIENT_IP.get();
            return this.weatherHelper.buildWeatherContextByIp(ip);
        }
        catch (Exception e) {
            log.warn("[Weather] 获取天气上下文失败: {}", (Object)e.getMessage());
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
                log.info("[Weather] 已保存用户[{}]城市: {} / {}", new Object[]{userId, city, cityEn});
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
            boolean hasFitnessKeyword = msgLower.contains("练") || msgLower.contains("吃") || msgLower.contains("记录") || msgLower.contains("计划") || msgLower.contains("食谱") || msgLower.contains("运动") || msgLower.contains("饮食") || msgLower.contains("体重") || msgLower.contains("热量") || msgLower.contains("卡路里") || msgLower.contains("打卡") || msgLower.contains("训练") || msgLower.contains("减脂") || msgLower.contains("增肌") || msgLower.contains("蛋白") || msgLower.contains("碳水") || msgLower.contains("脂肪");
            String knowledgeResult = hasFitnessKeyword ? this.retrieveKnowledgeDirectly(message) : null;
            AiModelConfig.ModelProvider provider = this.requireActiveProvider();
            PromptContextDecision ctx = new PromptContextDecision(false, true, false, false, false, true, false, false);
            Object contextMsg = this.buildContextAwareUserMessage(message, history, userRecord, user, null, ctx);
            if (purifiedText != null && !purifiedText.isBlank()) {
                contextMsg = (String)contextMsg + "\n【用户近期画像信息】\n" + purifiedText + "\n";
            }
            String systemPrompt = GENERAL_SYSTEM_PROMPT;
            if (knowledgeResult != null && !knowledgeResult.isBlank()) {
                contextMsg = (String)contextMsg + "\n【知识库检索结果】\n" + knowledgeResult + "\n";
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
            log.error("通用对话异常", (Throwable)e);
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
                extraNote = "用户的具体要求：" + userRequest;
            }
            return this.handlePlanGeneration(message, user, history, outputStream, resultHolder, userRecord, routingDecision, extraNote);
        }
        catch (Exception e) {
            log.error("计划生成信号工具执行失败", (Throwable)e);
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
            return data != null && !data.isBlank() ? data : "暂无记录";
        }
        if ("get_week_records".equals(toolName)) {
            String data = this.buildWeekRecordReply(user, userRecord);
            return data != null && !data.isBlank() ? data : "暂无记录";
        }
        return null;
    }

    private String fetchHistoryRecord(UserRecord userRecord, LocalDate target) {
        LocalDate today = LocalDate.now(CN_ZONE);
        if (target.isAfter(today)) {
            return "该日期还没到哦~";
        }
        if (ChronoUnit.DAYS.between(target, today) > 14L) {
            return "不好意思，该日期数据已清除或未记录";
        }
        if (target.equals(today.minusDays(1L))) {
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
        return this.searchReviewByDate(userRecord, target);
    }

    private String searchReviewByDate(UserRecord userRecord, LocalDate target) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日");
        String dateKey = "【" + fmt.format(target);
        List<String> reviews = this.parseJsonArray(userRecord.getWeeklyReviews());
        for (String review : reviews) {
            if (!review.replaceAll("\\s+", "").contains(dateKey)) continue;
            if (review.contains("暂无记录")) {
                return fmt.format(target) + "暂无记录内容";
            }
            return review;
        }
        return fmt.format(target) + "暂无记录内容";
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
            log.warn("解析JSON数组失败: {}", (Object)json, (Object)e);
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
                minimalContext.append("【系统说明】\n").append(extraSystemNote).append("\n");
            }
            minimalContext.append("【当前用户消息】\n").append(currentMessage);
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
            context.append("【历史上下文】\n");
            if (hasSummary) {
                context.append("长期摘要：\n").append(summary.trim()).append("\n\n");
            }
            if (hasRecentMessages) {
                int start = Math.max(0, recentMessages.size() - 10);
                context.append("最近原文对话（最多5轮）：\n");
                for (int i = start; i < recentMessages.size(); ++i) {
                    String item = recentMessages.get(i);
                    if (item == null || item.isBlank()) continue;
                    context.append(item.trim()).append("\n");
                }
                context.append("\n");
            }
        }
        if (contextDecision.includeTrainingPlanContext() && user != null && (trainingPlanText = this.buildTrainingPlanText(user.getId())) != null && !trainingPlanText.isBlank()) {
            context.append("【当前训练计划】\n").append(trainingPlanText.trim()).append("\n\n");
        }
        if (contextDecision.includeDietPlanContext() && user != null && (dietPlanText = this.buildDietPlanText(user.getId())) != null && !dietPlanText.isBlank()) {
            context.append("【当前饮食计划】\n").append(dietPlanText.trim()).append("\n\n");
        }
        if (contextDecision.includeTodayRecord() && user != null) {
            String exRecords = this.exerciseRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));
            String dietRecords = this.dietRecordService.getLegacyRecordJson(user.getId(), LocalDate.now(CN_ZONE));
            if (exRecords != null && !exRecords.isBlank() || dietRecords != null && !dietRecords.isBlank()) {
                context.append("【用户今日记录】\n");
                if (exRecords != null && !exRecords.isBlank()) {
                    context.append("运动记录：\n").append(exRecords.trim()).append("\n\n");
                }
                if (dietRecords != null && !dietRecords.isBlank()) {
                    context.append("饮食记录：\n").append(dietRecords.trim()).append("\n\n");
                }
            }
        }
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
        if (contextDecision.includeEmotionalState() && history != null && history.getEmotionalState() != null && !history.getEmotionalState().isBlank()) {
            context.append("【用户当前情绪状态：").append(history.getEmotionalState()).append("】\n");
        }
        if (extraSystemNote != null && !extraSystemNote.isBlank()) {
            context.append("【系统说明】\n").append(extraSystemNote).append("\n");
        }
        context.append("【当前用户消息】\n").append(currentMessage);
        return context.toString();
    }

    private String buildTodayRecordReply(User user, UserRecord userRecord) {
        String daySection;
        boolean hasRecord;
        if (user == null) {
            return "暂无今日记录";
        }
        boolean bl = hasRecord = this.hasStructuredDietRecord(user.getId()) || this.hasStructuredExerciseRecord(user.getId());
        if (hasRecord) {
            String aiSummary = this.summarizeDailyRecord(user.getId(), user, null);
            return aiSummary != null ? aiSummary : "今天暂无记录，运动或饮食后告诉我，我帮你记下来~";
        }
        String trainingPlanText = this.buildTrainingPlanText(user.getId());
        if (trainingPlanText != null && !trainingPlanText.isBlank() && (daySection = this.buildTrainingPlanDaySection(user.getId(), 0)) != null) {
            String aiSummary = this.summarizeDailyRecord(user.getId(), user, daySection);
            return aiSummary != null ? aiSummary : "今天暂无记录，这是今天的训练安排：\n" + daySection;
        }
        return "今天暂无记录，运动或饮食后告诉我，我帮你记下来~";
    }

    private String buildTodayDietReply(Long userId) {
        if (userId == null) {
            return "今天暂无饮食记录。";
        }
        LocalDate today = LocalDate.now(CN_ZONE);
        List<Map<String, Object>> dietRecords = this.dietRecordService.listLegacyRecords(userId, today);
        if (dietRecords.isEmpty()) {
            return "今天暂无饮食记录。";
        }
        StringBuilder sb = new StringBuilder("【今日饮食记录】\n");
        for (Map<String, Object> item : dietRecords) {
            String mealType = this.normalizeMealType(this.toCleanString(item.get("mealType")));
            String name = this.toCleanString(item.get("name"));
            String calories = item.get("calories") == null ? "" : String.valueOf(item.get("calories")) + "kcal";
            String protein = item.get("protein") != null ? String.format("%.0fg", ((Number)item.get("protein")).doubleValue()) : "";
            String time = this.toCleanString(item.get("time"));
            sb.append("餐次：").append(mealType.isBlank() ? "未分类" : mealType).append("｜").append(calories.isBlank() ? "-" : calories);
            if (!protein.isEmpty()) {
                sb.append("｜蛋白质").append(protein);
            }
            if (!time.isBlank()) {
                sb.append("｜").append(time);
            }
            sb.append("\n");
            sb.append("食物：").append(name.isBlank() ? "未命名食物" : name).append("\n");
        }
        String macroSummary = this.buildMacroSummaryText(userId, today);
        if (!macroSummary.isEmpty()) {
            sb.append("\n").append(macroSummary);
        }
        return sb.toString().trim();
    }

    private String buildTodayExerciseReply(Long userId) {
        if (userId == null) {
            return "今天暂无训练记录。";
        }
        LocalDate today = LocalDate.now(CN_ZONE);
        List<Map<String, Object>> sessions = this.exerciseRecordService.listLegacyRecords(userId, today);
        if (sessions.isEmpty()) {
            return "今天暂无训练记录。";
        }
        StringBuilder sb = new StringBuilder("【今日训练记录】\n");
        for (Map<String, Object> session : sessions) {
            String sessionName = this.toCleanString(session.get("name"));
            Integer sessionDuration = this.parseInteger(session.get("durationSeconds"));
            Integer sessionCalories = this.parseInteger(session.get("caloriesBurned"));
            List<Map<String, Object>> items = this.castObjectList(session.get("items"));
            if (sessionName.isBlank() && items.isEmpty()) continue;
            sb.append("训练：").append(sessionName.isBlank() ? "未命名训练" : sessionName);
            if (sessionDuration != null && sessionDuration > 0) {
                sb.append("｜").append(Math.max(1, sessionDuration / 60)).append("分钟");
            }
            if (sessionCalories != null && sessionCalories > 0) {
                sb.append("｜消耗").append(sessionCalories).append("大卡");
            }
            sb.append("｜").append(items.size()).append("个动作").append("\n");
            for (Map<String, Object> item : items) {
                String name = this.toCleanString(item.get("name"));
                String muscleGroup = this.toCleanString(item.get("muscleGroup"));
                Integer durationSeconds = this.parseInteger(item.get("durationSeconds"));
                Integer completedSets = this.parseInteger(item.get("completedSets"));
                if (name.isBlank()) continue;
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
        int totalBurned = this.exerciseRecordService.getDayTotalCaloriesBurned(userId, today);
        if (totalBurned > 0) {
            sb.append("\n今日累计消耗：").append(totalBurned).append("大卡");
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
        sb.append(String.format("今日已摄入：热量%dkcal，蛋白质%.0fg，碳水%.0fg，脂肪%.0fg", calories, protein, carbs, fat));
        try {
            UserProfile profile = this.userProfileService.getByUserId(userId);
            if (profile != null) {
                double target = 0.0;
                double d = profile.getCustomDailyCalories() != null && profile.getCustomDailyCalories() > 0.0 ? profile.getCustomDailyCalories() : (target = profile.getDailyCalorieBurn() != null ? profile.getDailyCalorieBurn() : 0.0);
                if (target > 0.0) {
                    int remaining = (int)Math.round(target - (double)calories);
                    sb.append(String.format("（目标%.0fkcal，%s%.0fkcal）", target, remaining >= 0 ? "还差" : "超出", Math.abs(remaining)));
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
            return "暂无本周记录";
        }
        if (userRecord != null && userRecord.getWeeklyReviews() != null && !userRecord.getWeeklyReviews().isBlank()) {
            List<String> reviews = this.parseJsonArray(userRecord.getWeeklyReviews());
            if (reviews.isEmpty()) {
                return "本周暂无记录";
            }
            StringBuilder sb = new StringBuilder("【本周记录】\n");
            for (String review : reviews) {
                sb.append(review).append("\n\n");
            }
            return sb.toString().trim();
        }
        return "本周暂无记录，运动或饮食后告诉我，我帮你记下来~";
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
            log.error("AI文本调用失败", (Throwable)e);
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
            log.error("AI单消息调用失败", (Throwable)e);
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
            case "午饭" -> "午餐";
            case "晚饭" -> "晚餐";
            case "夜宵", "宵夜" -> "加餐";
            default -> meal;
        };
    }

    private String getCurrentMealType() {
        int hour = LocalTime.now(CN_ZONE).getHour();
        if (hour < 10) {
            return "早餐";
        }
        if (hour < 14) {
            return "午餐";
        }
        if (hour < 17) {
            return "加餐";
        }
        if (hour < 21) {
            return "晚餐";
        }
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
        UserTrainingCycleVO activeCycle = this.userTrainingCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map templateMap = this.userTrainingTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("训练计划\n");
        for (int i = 1; i <= activeCycle.getDayCount(); ++i) {
            int dayIndex = i;
            UserTrainingCycleVO.CycleDayVO day = activeCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex).findFirst().orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("星期").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("：");
            if (day == null || day.getTemplateId() == null) {
                sb.append("休息\n");
                continue;
            }
            UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                sb.append(day.getTemplateName() == null ? "休息" : day.getTemplateName()).append("\n");
                continue;
            }
            sb.append(template.getName()).append("\n");
            this.appendTrainingSection(sb, "热身", "warmup", template.getItems());
            this.appendTrainingSection(sb, "正式训练", "main", template.getItems());
            this.appendTrainingSection(sb, "拉伸", "stretch", template.getItems());
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
        String title = "星期" + DAY_NAMES[targetDayOfWeek.getValue() - 1];
        if (day == null || day.getTemplateId() == null) {
            return title + "（休息日）\n休息";
        }
        UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return title + "（休息日）\n休息";
        }
        String body = this.buildTrainingTemplateBody(template.getItems());
        String muscleLabel = this.inferTrainingDayMuscleLabel(template);
        StringBuilder sb = new StringBuilder(title).append("（").append(muscleLabel.isBlank() ? this.safeTrim(template.getName()) : muscleLabel).append("）");
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
        sb.append(label).append("：");
        ArrayList<String> parts = new ArrayList<String>();
        for (UserTrainingTemplateVO.TrainingItemVO item2 : sectionItems) {
            StringBuilder part = new StringBuilder(this.safeTrim(item2.getExerciseName()));
            ArrayList<String> detailParts = new ArrayList<>();
            if (item2.getRecommendedSets() != null) {
                detailParts.add(item2.getRecommendedSets() + "组");
            }
            if (item2.getRecommendedReps() != null && !item2.getRecommendedReps().isBlank()) {
                detailParts.add(item2.getRecommendedReps());
            }
            if (!detailParts.isEmpty()) {
                part.append("（").append(String.join("，", detailParts)).append("）");
            }
            parts.add(part.toString());
        }
        sb.append(String.join((CharSequence)"、", parts)).append("\n");
    }

    private String buildTrainingTemplateBody(List<UserTrainingTemplateVO.TrainingItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        this.appendTrainingSection(sb, "热身", "warmup", items);
        this.appendTrainingSection(sb, "正式训练", "main", items);
        this.appendTrainingSection(sb, "拉伸", "stretch", items);
        return sb.toString().trim();
    }

    private String inferTrainingDayMuscleLabel(UserTrainingTemplateVO template) {
        if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
            return "";
        }
        Map<String, Long> groupCount = template.getItems().stream().map(UserTrainingTemplateVO.TrainingItemVO::getMuscleGroup).map(this::normalizePlanMuscleGroup).filter(group -> !this.isBlank(group)).collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        if (groupCount.isEmpty()) {
            return "";
        }
        String dominantGroup = groupCount.entrySet().stream().max(Map.Entry.<String, Long>comparingByValue()).map(Map.Entry::getKey).orElse("");
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
        UserDietCycleVO activeCycle = this.userDietCycleService.getActiveCycle(userId);
        if (activeCycle == null || activeCycle.getDays() == null || activeCycle.getDays().isEmpty()) {
            return null;
        }
        Map dayTemplateMap = this.userDietDayTemplateService.listDayTemplates(userId).stream().collect(Collectors.toMap(UserDietDayTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map mealTemplateMap = this.userDietTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        int todayIndex = activeCycle.getTodayIndex() == null ? 1 : activeCycle.getTodayIndex();
        LocalDate today = LocalDate.now(CN_ZONE);
        StringBuilder sb = new StringBuilder("一日食谱推荐\n");
        for (int i = 1; i <= activeCycle.getDayCount(); ++i) {
            int dayIndex = i;
            UserDietCycleVO.CycleDayVO day = activeCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == dayIndex).findFirst().orElse(null);
            int offsetFromToday = dayIndex - todayIndex;
            DayOfWeek targetDayOfWeek = today.plusDays(offsetFromToday).getDayOfWeek();
            sb.append("星期").append(DAY_NAMES[targetDayOfWeek.getValue() - 1]).append("：");
            if (day == null || day.getDayTemplateId() == null) {
                sb.append("未安排\n");
                continue;
            }
            UserDietDayTemplateVO dayTemplate = (UserDietDayTemplateVO)dayTemplateMap.get(day.getDayTemplateId());
            sb.append(dayTemplate == null ? "未安排" : dayTemplate.getName()).append("\n");
            if (dayTemplate == null || dayTemplate.getMealSlots() == null) continue;
            for (String mealType : PLAN_MEAL_ORDER) {
                UserDietTemplateVO mealTemplate;
                UserDietDayTemplateVO.MealSlotVO slot = dayTemplate.getMealSlots().stream().filter(meal -> mealType.equals(this.normalizeMealType(meal.getMealType()))).findFirst().orElse(null);
                if (slot == null || slot.getTemplateId() == null || (mealTemplate = (UserDietTemplateVO)mealTemplateMap.get(slot.getTemplateId())) == null || mealTemplate.getItems() == null || mealTemplate.getItems().isEmpty()) continue;
                sb.append(mealType).append("：").append(mealTemplate.getName()).append("\n");
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
            if (line.isBlank() || "一日食谱推荐".equals(line)) continue;
            if (line.startsWith("星期")) {
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
        String todayKey = "星期" + DAY_NAMES[LocalDate.now(CN_ZONE).getDayOfWeek().getValue() - 1];
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
        Pattern p = Pattern.compile("(?:星期" + dayName + "|周" + dayName + ")[：:\\s]*(.*?)(?=(?:星期[一二三四五六日]|周[一二三四五六日])|$)", 32);
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
                        if (!line.startsWith("用户：")) continue;
                        recentUserMsgs.append(line.substring(3)).append("\n");
                    }
                }
            }
            String historyContext = recentUserMsgs.length() > 0 ? "【近期对话记录（用户消息）】\n" + String.valueOf(recentUserMsgs) + "\n" : "";
            String prompt = "training_plan".equals(planIntent) ? "根据用户消息、近期对话和用户信息，提取训练计划的器械筛选条件。\n\n" + historyContext + "用户信息：" + userInfo + "\n\n用户消息：" + userMessage + "\n\n请严格按以下JSON格式返回，不要输出任何其他内容：\n{\"equipment\": \"器械条件\"}\n\nequipment填写规则：\n1. 如果用户明确说徒手/无器械/自重训练，填 \"徒手\"\n2. 如果用户说只有某种器械（如只有哑铃、只有弹力带），填该器械名称（逗号分隔）\n3. 如果用户说不能使用某种器械（如没有杠铃、去不了健身房），从用户画像器械中排除不可用的，填剩余可用器械\n4. 如果用户没提到器械限制，填 \"null\"（直接填null三个字母），将使用用户画像中的器械设置\n5. 注意用户画像中的训练水平（新手/中级/高级），筛选时要配合其水平\n6. 不要自己编造器械，只根据用户消息和用户信息推断" : "根据用户消息、近期对话和用户信息，提取饮食计划的食物筛选条件。\n\n" + historyContext + "用户信息：" + userInfo + "\n\n用户消息：" + userMessage + "\n\n请严格按以下JSON格式返回，不要输出任何其他内容：\n{\"foodCategory\": \"分类条件\"}\n\nfoodCategory填写规则：\n1. 如果用户说不会做饭/不想做饭/只能吃即食，填 \"即食\"\n2. 如果用户提到饮食偏好（如减脂、低碳水、高蛋白），填对应的食物分类偏好\n3. 如果用户没提到饮食限制，填 \"null\"（直接填null三个字母）\n4. 只根据用户消息推断，不要自己编造";
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
                log.info("AI提取筛选条件: planIntent={}, equipment={}, foodCategory={}", new Object[]{planIntent, equipment, foodCategory});
                return new PlanFilterConditions(equipment, foodCategory);
            }
        }
        catch (Exception e) {
            log.warn("提取计划筛选条件失败，使用默认值: {}", (Object)e.getMessage());
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
            contextAwareMessage = message + "\n\n当前已有训练计划参考：\n" + trainingPlanText;
        }
        String string = planIntent = routingDecision == null ? "none" : routingDecision.planGenerationIntent();
        if (planIntent.isBlank() || "none".equals(planIntent)) {
            return null;
        }
        PlanFilterConditions filterConditions = this.extractPlanFilterConditions(planIntent, message, user, history);
        if ("diet_plan".equals(planIntent)) {
            String foodKnowledge = this.buildFoodCatalog(user != null ? user.getId() : null, filterConditions.foodCategory());
            String mealRequestInstruction = this.buildDietMealRequestInstruction(msg);
            // 后端预计算目标营养素，避免AI算错
            Double targetCal = this.resolveTargetCalories(user);
            String targetNutrients;
            if (targetCal != null && targetCal > 0) {
                int tProtein = (int) Math.round(targetCal * 0.30 / 4);
                int tCarbs = (int) Math.round(targetCal * 0.40 / 4);
                int tFat = (int) Math.round(targetCal * 0.30 / 9);
                targetNutrients = String.format("目标热量: %.0fkcal, 目标蛋白质: %dg, 目标碳水: %dg, 目标脂肪: %dg", targetCal, tProtein, tCarbs, tFat);
            } else {
                targetNutrients = "（用户未设置目标热量，按2000kcal估算）目标热量: 2000kcal, 目标蛋白质: 150g, 目标碳水: 200g, 目标脂肪: 67g";
            }
            String prompt = DIET_PLAN_GEN_PROMPT.replace("{userInfo}", userInfo).replace("{foodKnowledge}", foodKnowledge != null ? foodKnowledge : "无").replace("{mealRequestInstruction}", mealRequestInstruction).replace("{targetNutrients}", targetNutrients);
            String aiReply = this.callAiApiStream(prompt, (String)contextAwareMessage, outputStream, null, 900);
            return this.sanitizeDietPlanOutput(aiReply, this.resolveMealType(msg));
        }
        if ("training_plan".equals(planIntent)) {
            String trainingPlanText2;
            String exerciseCatalog = this.buildExerciseCatalog(user != null ? user.getId() : null, filterConditions.equipment());
            String weatherContext = this.getWeatherContextAsync();
            if (weatherContext != null) {
                contextAwareMessage = (String)contextAwareMessage + "\n\n" + weatherContext + "\n请参考天气情况合理安排室内/室外训练。";
            }
            Object existingPlan = "";
            if (promptContext.includeTrainingPlanContext() && user != null && (trainingPlanText2 = this.buildTrainingPlanText(user.getId())) != null && !trainingPlanText2.isBlank()) {
                existingPlan = "\n【用户当前已有的训练计划（用户反馈时在此基础上调整）】\n" + trainingPlanText2 + "\n";
            }
            String prompt = TRAINING_PLAN_GEN_PROMPT.replace("{userInfo}", userInfo).replace("{exerciseCatalog}", exerciseCatalog).replace("{existingPlan}", (CharSequence)existingPlan);
            String aiReply = this.callAiApiStream(prompt, (String)contextAwareMessage, outputStream, null, 1200);
            return this.sanitizeTrainingPlanOutput(aiReply, this.shouldForceDefaultTrainingWeek(msg));
        }
        return null;
    }

    private void securityCheck(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "消息不能为空");
        }
        if (message.length() > 2000) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "消息长度不能超过2000字");
        }
        if (Pattern.compile("<script.*?>.*?</script>", 2).matcher(message).find()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "输入包含非法内容");
        }
        if (Pattern.compile("(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|ALTER|CREATE)\\b.*\\b(FROM|INTO|TABLE|DATABASE|WHERE)\\b)", 2).matcher(message).find()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "输入包含非法内容");
        }
    }

    private String retrieveKnowledgeDirectly(String userMessage) {
        try {
            List results = this.vectorStore.similaritySearch(SearchRequest.builder().query(userMessage).topK(3).similarityThreshold(0.35).build());
            if (results == null || results.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object obj : results) {
                Document doc = (Document) obj;
                sb.append(doc.getText()).append("\n");
            }
            return sb.toString().trim();
        }
        catch (Exception e) {
            log.warn("RAG 检索失败: {}", (Object)e.getMessage());
            return null;
        }
    }

    private String buildUserInfo(User user) {
        UserProfile p;
        if (user == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n【用户】" + user.getUsername() + "|");
        String gender = user.getGender() != null ? (user.getGender() == 0 ? "女" : "男") : "未知";
        sb.append(gender);
        if (user.getAge() != null) {
            sb.append("|").append(user.getAge()).append("岁");
        }
        if (user.getHeight() != null) {
            sb.append("|").append(user.getHeight()).append("cm");
        }
        if (user.getWeight() != null) {
            sb.append("|").append(user.getWeight()).append("kg");
        }
        if ((p = this.userProfileService.getByUserId(user.getId())) != null) {
            if (p.getFitnessGoal() != null) {
                sb.append("|目标:").append(p.getFitnessGoal());
            }
            if (p.getTargetWeight() != null) {
                sb.append("|目标体重:").append(p.getTargetWeight()).append("kg");
            }
            if (p.getActivityLevel() != null) {
                sb.append("|活动水平:").append(p.getActivityLevel());
            }
            if (p.getCustomDailyCalories() != null) {
                sb.append("|目标热量:").append(p.getCustomDailyCalories().intValue()).append("kcal");
            }
            if (p.getExperienceLevel() != null) {
                sb.append("|训练水平:").append(p.getExperienceLevel());
            }
            if (p.getPreferredEquipment() != null) {
                sb.append("|器械:").append(p.getPreferredEquipment());
            }
            if (p.getWeeklyTrainingDays() != null) {
                sb.append("|每周练").append(p.getWeeklyTrainingDays()).append("天");
            }
            if (p.getTrainingDuration() != null) {
                sb.append("|每次").append(p.getTrainingDuration()).append("分钟");
            }
            if (p.getOccupation() != null) {
                sb.append("|职业:").append(p.getOccupation());
            }
            if (p.getPersonality() != null) {
                sb.append("|性格:").append(p.getPersonality());
            }
            if (p.getMedicalHistory() != null) {
                sb.append("|伤病:").append(p.getMedicalHistory());
            }
            if (p.getDietPreference() != null) {
                sb.append("|饮食:").append(p.getDietPreference());
            }
            if (p.getTrainingPreference() != null) {
                sb.append("|训练偏好:").append(p.getTrainingPreference());
            }
            if (p.getUserProfileText() != null && !p.getUserProfileText().isBlank()) {
                sb.append("|画像:").append(p.getUserProfileText());
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
            return "（动作库为空）";
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
        Map<String, String> groupMap = Map.of("chest", "胸部", "back", "背部", "core", "核心", "arms", "手臂", "legs", "腿部", "shoulders", "肩部");
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
                Object wName = favIds.contains(exercise.getId()) ? exercise.getName() + "[收藏]" : exercise.getName();
                this.addDistinctLimited((List)warmupByGroup.get(groupKey), (String)wName, 4);
                continue;
            }
            if (this.isStretchExercise(exercise.getName())) continue;
            Object mName = favIds.contains(exercise.getId()) ? exercise.getName() + "[收藏]" : exercise.getName();
            this.addDistinctLimited((List)mainByGroup.get(groupKey), (String)mName, 6);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【分部位训练动作库-训练动作只能从对应部位中选】\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List actions = (List)mainByGroup.get(group);
            if (actions == null || actions.isEmpty()) continue;
            sb.append(groupMap.getOrDefault(group, group)).append("训练动作：").append(String.join((CharSequence)"、", actions)).append("\n");
        }
        sb.append("\n【分部位热身动作-热身只能从当天训练部位对应的热身动作中选】\n");
        for (String group : PLAN_MUSCLE_GROUP_ORDER) {
            List warmups = (List)warmupByGroup.get(group);
            if (warmups == null || warmups.isEmpty()) continue;
            sb.append(groupMap.getOrDefault(group, group)).append("热身动作：").append(String.join((CharSequence)"、", warmups)).append("\n");
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
        return !this.isBlank(name) && name.contains("热身");
    }

    private boolean isStretchExercise(String name) {
        return !this.isBlank(name) && name.contains("拉伸");
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
            return "无";
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
            for (String cat : effectiveCategory.split("[,，]")) {
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
        sb.append("【食物营养参考】名称|分类|基准|热量(kcal)|蛋白质(g)|碳水(g)|脂肪(g)\n");
        for (FoodItem food : selected) {
            boolean isMine = food.getCreatedBy() != null && food.getCreatedBy().equals(userId) && !Integer.valueOf(1).equals(food.getIsSystem());
            Object name = isMine ? this.safeTrim(food.getName()) + "[我的食物]" : this.safeTrim(food.getName());
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
            log.error("流式调用AI接口失败", (Throwable)e);
            throw new BusincessException(StateCode.AI_ERROR, "当前AI调用失败，请切换别的AI试一试");
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
                log.info("【流式】客户端断连，AI继续生成至完成");
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
            throw new BusincessException(StateCode.PARAMS_ERROR, "没有可保存的计划内容");
        }
        if ("training".equals(type)) {
            this.saveGeneratedTrainingPlan(userId, content);
            return "训练计划已保存";
        }
        if ("diet".equals(type)) {
            this.saveGeneratedDietPlan(userId, content);
            return "饮食计划已保存";
        }
        throw new BusincessException(StateCode.PARAMS_ERROR, "不支持的计划类型");
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
            buffer.add("用户：" + userMessage + "\n助手：" + aiResponse);
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
                log.warn("序列化pendingMessages失败", (Throwable)e);
            }
            this.chatHistoryMapper.updateById(existing);
        } else {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setUserId(userId);
            chatHistory.setMessageCount(1);
            chatHistory.setEmotionalState(emotionalState);
            try {
                chatHistory.setPendingMessages(JSON_MAPPER.writeValueAsString(List.of("用户：" + userMessage + "\n助手：" + aiResponse)));
            }
            catch (Exception e) {
                log.warn("序列化pendingMessages失败", (Throwable)e);
            }
            this.chatHistoryMapper.insert(chatHistory);
        }
    }

    private void saveGeneratedTrainingPlan(Long userId, String content) {
        List<ParsedTrainingDay> days = this.parseTrainingPlanText(content);
        if (days.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "训练计划格式无法识别");
        }
        ArrayList<String> unresolvedActions = new ArrayList<String>();
        ArrayList<SaveUserTrainingCycleRequest.CycleDayDTO> cycleDays = new ArrayList<SaveUserTrainingCycleRequest.CycleDayDTO>();
        for (int i = 0; i < 7; ++i) {
            ParsedTrainingDay day = i < days.size() ? days.get(i) : new ParsedTrainingDay(i + 1, "星期" + DAY_NAMES[i], "", true, List.of(), List.of(), List.of());
            SaveUserTrainingCycleRequest.CycleDayDTO dayDTO = new SaveUserTrainingCycleRequest.CycleDayDTO();
            dayDTO.setDayIndex(i + 1);
            if (!day.rest()) {
                SaveUserTrainingTemplateRequest.TrainingItemDTO dto;
                Long exerciseId;
                SaveUserTrainingTemplateRequest req = new SaveUserTrainingTemplateRequest();
                String tplName = day.muscleGroup().isBlank() ? "训练日" + (i + 1) : "AI" + day.muscleGroup() + "日";
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
            log.warn("【保存训练计划】以下动作未命中动作库，已跳过：{}", (Object)unresolvedActions.stream().distinct().collect(Collectors.joining("、")));
        }
        String prefix = "AI训练周模板";
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
            throw new BusincessException(StateCode.PARAMS_ERROR, "饮食计划格式无法识别");
        }
        LinkedHashMap<String, Long> mealConfig = new LinkedHashMap<String, Long>();
        for (String mealType : PLAN_MEAL_ORDER) {
            List<ParsedFoodAmount> parsedFoods;
            String foodLine = meals.get(mealType);
            if (foodLine == null || foodLine.isBlank() || (parsedFoods = this.parseDietFoodLine(foodLine)).isEmpty()) continue;
            SaveUserDietTemplateRequest req = new SaveUserDietTemplateRequest();
            req.setName("AI" + mealType + "模板");
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
            throw new BusincessException(StateCode.PARAMS_ERROR, "饮食计划中没有可保存的餐次");
        }
        SaveUserDietDayTemplateRequest dayTemplateRequest = new SaveUserDietDayTemplateRequest();
        dayTemplateRequest.setName("AI一日饮食");
        dayTemplateRequest.setMealConfig(mealConfig);
        Long dayTemplateId = this.userDietDayTemplateService.saveDayTemplate(userId, dayTemplateRequest);
        String dietPrefix = "AI饮食周模板";
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
        Pattern pattern = Pattern.compile("星期([一二三四五六日天])(?:（([^）]*)）)?\\s*(.*?)(?=星期[一二三四五六日天](?:（[^）]*）)?|$)", 32);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String dayChar = matcher.group(1);
            String muscle = this.safeTrim(matcher.group(2));
            String body = this.safeTrim(matcher.group(3));
            String title = "星期" + dayChar + (String)(muscle.isBlank() ? "" : " · " + muscle);
            List<String> warmups = List.of();
            List<String> trainings = List.of();
            List<String> stretches = List.of();
            boolean rest = body.contains("休息");
            if (!rest) {
                warmups = this.parseActionLine(body, "热身");
                trainings = this.parseActionLine(body, "训练");
                stretches = this.parseActionLine(body, "拉伸");
            }
            result.add(new ParsedTrainingDay(result.size() + 1, title, muscle, rest, warmups, trainings, stretches));
        }
        return result;
    }

    private Map<String, String> parseDietPlanText(String content) {
        LinkedHashMap<String, String> sections = new LinkedHashMap<String, String>();
        Pattern mdPattern = Pattern.compile("\\|\\s*([^|]*?(?:早餐|午餐|晚餐|练后|加餐)[^|]*?)\\s*\\|\\s*(.*?)\\s*\\|");
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
                if (currentMeal == null || !line.startsWith("吃什么")) continue;
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
        Pattern stdPattern = Pattern.compile(label + "[：:](.*?)(?:\\n|$)");
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
        raw = raw.replaceAll("[（(][^）)]*[）)]", "");
        ArrayList<String> result = new ArrayList<String>();
        for (String part : parts = raw.split("[、，,+＋；;|]")) {
            String action = this.normalizeActionName(part);
            if (action.isBlank()) continue;
            result.add(action);
        }
        return result;
    }

    private List<ParsedFoodAmount> parseDietFoodLine(String line) {
        String[] parts;
        ArrayList<ParsedFoodAmount> result = new ArrayList<ParsedFoodAmount>();
        for (String rawPart : parts = line.split("[+＋]")) {
            Matcher matcher;
            String part = this.sanitizeDietRecordText(rawPart);
            if (part.isBlank() || !(matcher = Pattern.compile("(.+?)(\\d+(?:\\.\\d+)?)(kg|g|ml|l|个|片|根|袋|份|只|枚)$").matcher(part.replaceAll("\\s+", ""))).find()) continue;
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
        QueryWrapper<Exercise> exact = new QueryWrapper<>();
        exact.eq("isActive", 1).eq("name", name).last("LIMIT 1");
        Exercise matched = this.exerciseService.getOne(exact, false);
        if (matched == null) {
            QueryWrapper<Exercise> fuzzy = new QueryWrapper<>();
            fuzzy.eq("isActive", 1).like("name", name).last("LIMIT 5");
            List<Exercise> candidates = this.exerciseService.list(fuzzy);
            matched = candidates.stream().filter(item -> item.getName() != null && (item.getName().contains(name) || name.contains(item.getName()))).findFirst().orElse(null);
        }
        if (matched != null) {
            return matched.getId();
        }
        return null;
    }

    private Exercise findBestExerciseForRecord(String rawName) {
        String name = this.normalizeActionName(rawName);
        if (name.isBlank()) {
            return null;
        }
        QueryWrapper<Exercise> exact = new QueryWrapper<>();
        exact.eq("isActive", 1).eq("name", name).last("LIMIT 1");
        Exercise matched = this.exerciseService.getOne(exact, false);
        if (matched != null) {
            return matched;
        }
        QueryWrapper<Exercise> fuzzy = new QueryWrapper<>();
        fuzzy.eq("isActive", 1).like("name", name).last("LIMIT 10");
        List<Exercise> candidates = this.exerciseService.list(fuzzy);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String normalizedKeyword = this.normalizeFoodKeyword(name);
        for (Exercise item : candidates) {
            if (item.getName() == null) continue;
            String normalizedName = this.normalizeFoodKeyword(item.getName());
            if (!normalizedName.equals(normalizedKeyword)) continue;
            return item;
        }
        for (Exercise item2 : candidates) {
            if (item2.getName() == null) continue;
            String normalizedName2 = this.normalizeFoodKeyword(item2.getName());
            if (!normalizedName2.contains(normalizedKeyword) && !normalizedKeyword.contains(normalizedName2)) continue;
            return item2;
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
            log.info("[WebSearch][Exercise] 新动作入库: name={}, muscleGroup={}", (Object)name, (Object)entity.getMuscleGroup());
            return entity;
        }
        catch (Exception e) {
            log.warn("[WebSearch][Exercise] 搜索并保存动作失败: exerciseName={}, error={}", (Object)exerciseName, (Object)e.getMessage());
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
            sb.append("动作名：").append(this.toCleanString(map.getOrDefault("name", exerciseName))).append("\n");
            sb.append("训练部位：").append(this.toCleanString(map.get("muscleGroup"))).append("\n");
            sb.append("所需器械：").append(this.toCleanString(map.get("equipment"))).append("\n");
            sb.append("难度：").append(this.toCleanString(map.get("difficulty"))).append("\n");
            Object stepsObj = map.get("steps");
            if (stepsObj instanceof List) {
                List steps = (List)stepsObj;
                sb.append("动作步骤：\n");
                for (int i = 0; i < steps.size(); ++i) {
                    sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
                }
            }
            if ((tipsObj = map.get("tips")) instanceof List) {
                List tips = (List)tipsObj;
                sb.append("注意事项：\n");
                for (Object tip : tips) {
                    sb.append("- ").append(tip).append("\n");
                }
            }
            return sb.toString().trim();
        }
        catch (Exception e) {
            log.warn("[WebSearch][Exercise] 搜索动作信息失败: exerciseName={}, error={}", (Object)exerciseName, (Object)e.getMessage());
            return null;
        }
    }

    private String extractExerciseNameFromQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String name = query.replaceAll("(怎么做|怎么练|是什么|动作要领|正确姿势|教程|方法|训练方法|怎么|如何|是什么意思|有什么用|好不好|效果|可以吗|能.*吗).*$", "").trim();
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
            throw new BusincessException(StateCode.PARAMS_ERROR, "饮食计划里存在空食物名");
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
        if (text.contains("胸")) {
            return "chest";
        }
        if (text.contains("背")) {
            return "back";
        }
        if (text.contains("腿")) {
            return "legs";
        }
        if (text.contains("肩")) {
            return "shoulders";
        }
        if (text.contains("臂") || text.contains("二头") || text.contains("三头")) {
            return "arms";
        }
        if (text.contains("腹") || text.contains("核心")) {
            return "core";
        }
        return "core";
    }

    private String normalizeActionName(String raw) {
        String text = this.sanitizeExerciseRecordText(raw);
        text = text.replaceAll("[（(].*?[）)]", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(分钟|min|秒|s|小时|h|组|次)$", "");
        text = text.replaceAll("静态拉伸$", "拉伸");
        return this.safeTrim(text);
    }

    private String sanitizeExerciseRecordText(String raw) {
        String text = this.safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = this.stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经|已经在)?(?:做了|练了|跑了|走了|骑了)", "");
        text = text.replaceAll("^(?:进行|完成)(?:了)?", "");
        text = text.replaceAll("[。！!，,；;、\\s]+$", "");
        return this.safeTrim(text);
    }

    private String sanitizeDietRecordText(String raw) {
        String text = this.safeTrim(raw);
        if (text.isBlank()) {
            return "";
        }
        text = this.stripLeadingRecordPhrases(text);
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经)?(?:早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜)?(?:吃了|喝了|吃过|喝过)", "");
        text = text.replaceAll("^(?:我|今天|刚刚|刚才|刚|已经)?(?:吃了|喝了|吃过|喝过)", "");
        text = text.replaceAll("^(?:早餐|午餐|午饭|晚餐|晚饭|加餐|夜宵|宵夜)[:：]?", "");
        text = text.replaceAll("[。！!，,；;、\\s]+$", "");
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
            text = text.replaceAll("^(?:请|麻烦|帮忙)?(?:帮我|给我)?(?:记录一下|记录下|记录|记一下|记下|记一笔|保存一下|保存下)\\s*", "");
        } while (!previous.equals(text = text.replaceAll("^(?:请|麻烦|帮忙)?(?:把|将)?\\s*", "")));
        return this.safeTrim(text);
    }

    private Integer extractCompletedSets(String exerciseText) {
        String text = this.safeTrim(exerciseText);
        if (text.isBlank()) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*组").matcher(text);
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

    private String generateSummary(String userMessage, String aiResponse) {
        try {
            String prompt = "总结以下对话，保留需求、建议要点、后续关注点，≤200字，去掉寒暄。\n用户：" + userMessage + "\n助手：" + aiResponse;
            String result = this.callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("生成对话总结失败", (Throwable)e);
        }
        StringBuilder fallback = new StringBuilder("对话摘要(自动生成)：\n");
        String[] lines = aiResponse.split("\n");
        for (int i = 0; i < lines.length && fallback.length() < 400; ++i) {
            String line = lines[i];
            if (!line.startsWith("用户：") && !line.startsWith("助手：")) continue;
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
        List<Map<String, Object>> sessions;
        List<Map<String, Object>> list = sessions = userId == null ? Collections.emptyList() : this.exerciseRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (sessions.isEmpty()) {
            return "无训练记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> session : sessions) {
            String sessionName = this.toCleanString(session.get("name"));
            Integer sessionDuration = this.parseInteger(session.get("durationSeconds"));
            if (!sessionName.isBlank()) {
                sb.append("训练：").append(sessionName);
                if (sessionDuration != null && sessionDuration > 0) {
                    sb.append("，").append(Math.max(1, sessionDuration / 60)).append("分钟");
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
        List<Map<String, Object>> dietRecords;
        List<Map<String, Object>> list = dietRecords = userId == null ? Collections.emptyList() : this.dietRecordService.listLegacyRecords(userId, LocalDate.now(CN_ZONE));
        if (dietRecords.isEmpty()) {
            return "无饮食记录";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : dietRecords) {
            String mealType = this.normalizeMealType(this.toCleanString(item.get("mealType")));
            String name = this.toCleanString(item.get("name"));
            if (name.isBlank()) continue;
            sb.append("- ");
            if (!mealType.isBlank()) {
                sb.append(mealType).append("：");
            }
            sb.append(name);
            sb.append("\n");
        }
        return sb.length() == 0 ? "无饮食记录" : sb.toString().trim();
    }

    private String summarizeDailyRecord(Long userId, User user, String todayPlanSection) {
        boolean hasExerciseRecord = this.hasStructuredExerciseRecord(userId);
        boolean hasDietRecord = this.hasStructuredDietRecord(userId);
        boolean noUserRecord = !hasExerciseRecord && !hasDietRecord;
        try {
            String userInfo = this.buildUserInfo(user);
            String prompt = noUserRecord ? "你是健身助手Tatan。用户今天没有记录任何运动和饮食，但系统查到了今天的训练计划。\n\n请生成一份与数据库存储格式一致的提醒总结。\n纯文本不用markdown，总字数限制在220字以内，严格按以下格式输出：\n总结：一句话概括今天应完成的安排。\n建议：给出1条最值得执行的建议，并自然带上“今天还没记录哦”。\n训练：简要列出今天应完成的训练内容。\n饮食：写“暂无饮食记录”。\n问题：说明无法确认是否按计划完成。\n\n" + (String)(userInfo.isBlank() ? "" : userInfo + "\n") + "【今天的训练计划】\n" + (todayPlanSection == null ? "" : todayPlanSection.trim()) : "你是健身助手Tatan。请根据用户今天的结构化运动/饮食记录，生成一份与数据库存储格式一致的今日总结。\n\n纯文本不用markdown，总字数限制在220字以内，严格按以下格式输出：\n总结：一句话概括今天状态。\n建议：给出1条最值得执行的建议。\n训练：概括今天训练内容，没有则写“暂无训练记录”。\n饮食：概括今天饮食情况，没有则写“暂无饮食记录”。\n问题：如无明显问题可写“无”。\n\n重要约束：\n1. 只要【结构化训练记录】里有动作名，就不能写“暂无训练记录”。\n2. 训练记录里没有时长，不代表没有训练；只有动作名、肌群、组数也属于有效训练记录。\n3. 不要因为部分训练没有时长，就写“0分钟异常”“数据无效”“暂无训练记录”这类结论。\n4. 优先依据结构化记录总结，不要被旧的自然语言流水误导。\n5. 如果同类训练或同一餐明显重复出现，优先做合并概括，不要机械逐条复述。\n6. 如果你判断存在重复记录、疑似重复打卡、餐次重复等情况，可以在“问题：”里简短指出，但不要夸大成错误。\n7. “训练：”和“饮食：”要写成概括后的自然语言，不要简单抄原始列表。\n\n" + (String)(userInfo.isBlank() ? "" : userInfo + "\n") + "【结构化训练记录】\n" + this.buildTodaySummaryExerciseInput(userId) + "\n\n【结构化饮食记录】\n" + this.buildTodaySummaryDietInput(userId);
            String result = this.callAiSingle(prompt, 300, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("生成今日记录总结失败", (Throwable)e);
        }
        if (noUserRecord) {
            return "总结：今天应按计划完成训练安排。\n建议：今天还没记录哦，练完记得及时打卡。\n训练：请参考今日训练计划。\n饮食：暂无饮食记录。\n问题：无法确认今天是否按计划完成。";
        }
        return "总结：今天已有记录，但总结生成失败。\n建议：可以稍后再查看一次今日总结。\n训练：请查看今日原始记录。\n饮食：请查看今日原始记录。\n问题：暂无可靠结构化总结。";
    }

    @Override
    @Transactional(rollbackFor={Exception.class})
    public String quickSaveTodayPlan(Long userId, String type) {
        LocalDate today = LocalDate.now(CN_ZONE);
        String recordTime = LocalTime.now(CN_ZONE).format(TIME_FMT);
        if ("training".equals(type)) {
            UserTrainingCycleVO cycle = this.userTrainingCycleService.getActiveCycle(userId);
            if (cycle == null || cycle.getTodayIndex() == null || cycle.getDays() == null || cycle.getDays().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "还没有训练计划");
            }
            Map templateMap = this.userTrainingTemplateService.listTemplates(userId).stream().collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            int targetIndex = cycle.getTodayIndex();
            UserTrainingCycleVO.CycleDayVO day = cycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex).findFirst().orElse(null);
            if (day == null || day.getTemplateId() == null) {
                throw new BusincessException(StateCode.NULL_ERROR, "今天是休息日，没有训练安排");
            }
            UserTrainingTemplateVO template = (UserTrainingTemplateVO)templateMap.get(day.getTemplateId());
            if (template == null || template.getItems() == null || template.getItems().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "今天的训练计划没有动作");
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
                this.exerciseRecordService.saveRecord(userId, today, req, "chat");
            }
            return "已记录" + (template.getName() != null ? template.getName() : "") + "训练";
        }
        if ("diet".equals(type)) {
            UserDietCycleVO dietCycle = this.userDietCycleService.getActiveCycle(userId);
            if (dietCycle == null || dietCycle.getTodayIndex() == null || dietCycle.getDays() == null || dietCycle.getDays().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "还没有饮食计划");
            }
            int targetIndex = dietCycle.getTodayIndex();
            UserDietCycleVO.CycleDayVO day = dietCycle.getDays().stream().filter(d -> d.getDayIndex() != null && d.getDayIndex() == targetIndex).findFirst().orElse(null);
            if (day == null || day.getDayTemplateId() == null) {
                throw new BusincessException(StateCode.NULL_ERROR, "今天没有饮食安排");
            }
            List<UserDietDayTemplateVO> dayTemplates = this.userDietDayTemplateService.listDayTemplates(userId);
            UserDietDayTemplateVO dayTpl = dayTemplates.stream().filter(t -> t.getId().equals(day.getDayTemplateId())).findFirst().orElse(null);
            if (dayTpl == null || dayTpl.getMealSlots() == null || dayTpl.getMealSlots().isEmpty()) {
                throw new BusincessException(StateCode.NULL_ERROR, "今天的饮食计划没有餐次");
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
            return "已记录" + savedMeals + "餐饮食";
        }
        throw new BusincessException(StateCode.PARAMS_ERROR, "无效的类型");
    }

    @Override
    public String generateUserProfile(User user, String profileFormData) {
        try {
            UserProfile profile = this.userProfileService.getByUserId(user.getId());
            String gender = user.getGender() != null ? (user.getGender() == 0 ? "女" : "男") : "未知";
            String userBasic = String.format("性别:%s, 年龄:%s岁, 身高:%scm, 体重:%skg, 健身目标:%s", gender, user.getAge() != null ? user.getAge() : "未填", user.getHeight() != null ? user.getHeight() : "未填", user.getWeight() != null ? user.getWeight() : "未填", profile != null && profile.getFitnessGoal() != null ? profile.getFitnessGoal() : "未填");
            String oldProfile = profile != null && profile.getUserProfileText() != null && !profile.getUserProfileText().isBlank() ? profile.getUserProfileText() : "暂无画像";
            String prompt = "你是一个AI健身教练的助手。请根据用户的【旧画像】、【基本信息】和【用户自填信息】，更新用户画像。\n\n【旧画像】" + oldProfile + "\n【基本信息】" + userBasic + "\n【用户自填】" + profileFormData + "\n\n要求：\n- 把基本信息和自填信息融合到旧画像中，不要分点罗列\n- 保留旧画像中仍然准确的内容\n- 用户自填的信息可能表述不清，帮他总结到位（比如\"办公室\"总结为\"久坐办公\"）\n- 伤病/饮食禁忌必须保留原意，不能遗漏\n- 100字以内，不废话\n- 不要加任何前缀，直接输出画像内容";
            String result = this.callAiSingle(prompt, 200, 0.3);
            if (result != null && !result.isBlank()) {
                return result.trim();
            }
        }
        catch (Exception e) {
            log.error("生成用户画像失败", (Throwable)e);
        }
        return profileFormData;
    }

    private String detectEmotionalState(String message) {
        String[] negative = new String[]{"坚持不", "放弃", "太难了", "焦虑", "崩溃", "绝望", "撑不住", "心态崩"};
        String[] positive = new String[]{"开心", "高兴", "兴奋", "期待", "进步", "成功", "做到"};
        for (String word : negative) {
            if (!message.contains(word)) continue;
            return "低落";
        }
        for (String word : positive) {
            if (!message.contains(word)) continue;
            return "积极";
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
            log.error("保存饮食记录失败", (Throwable)e);
        }
    }

    private MealNutrition resolveMealNutrition(List<DietFoodItemRequest> requests, Long userId) {
        List<Long> ids = requests.stream().map(DietFoodItemRequest::getFoodItemId).filter(id -> id != null && id > 0L).distinct().toList();
        if (ids.isEmpty()) {
            throw new BusincessException(StateCode.PARAMS_ERROR, "食物数据不能为空");
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
                throw new BusincessException(StateCode.PARAMS_ERROR, "食物不存在或无权限使用");
            }
            BigDecimal amount = request.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusincessException(StateCode.PARAMS_ERROR, "摄入量必须大于0");
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
        return new MealNutrition(String.join((CharSequence)"、", names), calories.setScale(0, RoundingMode.HALF_UP), protein.setScale(1, RoundingMode.HALF_UP), carbs.setScale(1, RoundingMode.HALF_UP), fat.setScale(1, RoundingMode.HALF_UP), fiber.setScale(1, RoundingMode.HALF_UP), items);
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
            log.error("保存运动记录失败", (Throwable)e);
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
            log.warn("解析对象数组失败: {}", (Object)json, (Object)e);
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
                return "早餐";
            }
            if (hour < 14) {
                return "午餐";
            }
            if (hour < 17) {
                return "加餐";
            }
            if (hour < 21) {
                return "晚餐";
            }
            return "加餐";
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
        if (text.contains("练后")) {
            return "练后餐";
        }
        if (text.contains("早餐")) {
            return "早餐";
        }
        if (text.contains("午餐") || text.contains("午饭")) {
            return "午餐";
        }
        if (text.contains("晚餐") || text.contains("晚饭")) {
            return "晚餐";
        }
        if (text.contains("加餐") || text.contains("夜宵") || text.contains("宵夜")) {
            return "加餐";
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
            String fallbackMessage = clarificationMessage.isBlank() ? "要帮你记录饮食，请补充具体食物名和克重/份量，或者到记录饮食界面直接选择食物。" : clarificationMessage;
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
            return new StructuredDietParseResult(null, "要帮你记录饮食，请补充具体食物名和克重/份量。", "");
        }
        ArrayList<DietFoodItemRequest> resolvedItems = new ArrayList<DietFoodItemRequest>();
        ArrayList<String> resolvedNames = new ArrayList<String>();
        boolean hasIncompleteFood = false;
        for (Map<String, Object> rawItem : rawItems) {
            BigDecimal normalizedAmount;
            FoodItem foodItem = null;
            String action;
            String rawName = this.toCleanString(rawItem.get("name"));
            if (rawName.isBlank()) {
                return new StructuredDietParseResult(null, "这条饮食里还缺具体食物名，补充后我再帮你记录。", "");
            }
            BigDecimal amount = this.parseBigDecimal(rawItem.get("amount"));
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "要帮你记录“" + rawName + "”，还需要补充克重或份量。", "");
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
                return new StructuredDietParseResult(null, "要帮你记录“" + rawName + "”，还需要补充更完整的信息。", "");
            }
            if (foodItem == null) {
                return new StructuredDietParseResult(null, "食物库暂无“" + rawName + "”的数据。如果想记录热量和营养物质，可以自行上传这个食物的具体数值哦。", "");
            }
            if (this.isIncompleteAiFood(foodItem)) {
                hasIncompleteFood = true;
            }
            if ((normalizedAmount = this.normalizeDietAmount(amount, rawUnit, foodItem.getUnit())) == null || normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return new StructuredDietParseResult(null, "“" + rawName + "”这条还缺可换算的克重/毫升/份量，补充后我再帮你记录。", "");
            }
            DietFoodItemRequest requestItem = new DietFoodItemRequest();
            requestItem.setFoodItemId(foodItem.getId());
            requestItem.setAmount(normalizedAmount);
            resolvedItems.add(requestItem);
            resolvedNames.add(foodItem.getName());
        }
        AddDietRecordRequest request = new AddDietRecordRequest();
        request.setName(String.join((CharSequence)"、", resolvedNames));
        request.setItems(resolvedItems);
        String noticeMessage = hasIncompleteFood ? "这次有食物是按你提供的热量和分量新建的简化数据，暂时不能准确判断蛋白质等营养；可以去个人中心管理自己创建的食物补全。" : "";
        return new StructuredDietParseResult(request, "", noticeMessage);
    }

    private FoodItem createAiFallbackFood(Long userId, String rawName, BigDecimal amount, String rawUnit, String category, BigDecimal caloriesValue, String calorieUnit, String caloriesMode, BigDecimal calorieBaseAmount, String calorieBaseUnit) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || caloriesValue == null || caloriesValue.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        String normalizedUnit = this.normalizeAmountUnit(rawUnit);
        if (!("g".equals(normalizedUnit) || "ml".equals(normalizedUnit) || "个".equals(normalizedUnit) || "片".equals(normalizedUnit) || "根".equals(normalizedUnit) || "袋".equals(normalizedUnit) || "份".equals(normalizedUnit))) {
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
        List<FoodItem> candidates = this.foodItemService.searchVisibleFoods(userId, keyword);
        if ((candidates == null || candidates.isEmpty()) && ((candidates = this.foodItemService.lambdaQuery()
                .eq(FoodItem::getIsDelete, 0)
                .and(q -> q.eq(FoodItem::getIsSystem, 1).or().eq(userId != null, FoodItem::getCreatedBy, userId))
                .orderByDesc(FoodItem::getIsSystem)
                .orderByAsc(FoodItem::getName)
                .list()) == null || candidates.isEmpty())) {
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
        String normalizedLine = rawDescription.replace("以及", "+").replace("还有", "+").replace("并且", "+").replace("然后", "+").replace("再加", "+").replace("搭配", "+").replace("配", "+").replace("和", "+").replace("、", "+").replace("，", "+").replace(",", "+");
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
        text = text.replaceAll("^(我|今天|刚刚|刚才|刚|早上|中午|晚上|夜里|夜宵|宵夜|早餐|午餐|午饭|晚餐|加餐|练后|喝了|吃了|喝|吃)+", "");
        text = text.replaceAll("\\d+(?:\\.\\d+)?\\s*(kg|g|ml|l)$", "");
        text = text.replaceAll("^[零一二两三四五六七八九十百半几多少\\d]+\\s*(个|杯|碗|勺|片|块|根|袋|份|只|枚|串|盒|瓶|听|罐)", "");
        text = text.replaceAll("[零一二两三四五六七八九十百半几多少\\d]", "");
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
        String raw;
        String string = raw = text == null ? "" : text.trim();
        if (raw.isBlank()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        aliases.add(raw);
        aliases.add(raw.replace("某某", "").replace("这个", "").replace("这种", "").trim());
        aliases.add(raw.replace("即食", "").trim());
        aliases.add(raw.replace("去皮", "").trim());
        aliases.add(raw.replace("鸡胸肉", "鸡胸").trim());
        aliases.add(raw.replace("鸡胸", "鸡胸肉").trim());
        aliases.add(raw.replace("牛奶", "低脂牛奶").trim());
        aliases.removeIf(String::isBlank);
        return new ArrayList<String>(aliases);
    }

    private String normalizeAiFoodCategory(String category) {
        String text = this.toCleanString(category);
        if (text.isBlank()) {
            return "未分类";
        }
        return switch (text) {
            case "碳水", "蛋白质", "脂肪", "蔬菜", "水果", "乳制品", "饮品", "零食", "补剂", "未分类" -> text;
            default -> "未分类";
        };
    }

    private String normalizeCalorieUnit(String calorieUnit) {
        String text = this.toCleanString(calorieUnit).toLowerCase(Locale.ROOT);
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
        if ("明天".equals(targetDay)) {
            return "明天";
        }
        return "今天";
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "加餐";
        }
        if (mealType.contains("练后")) {
            return "练后餐";
        }
        if (mealType.contains("早餐")) {
            return "早餐";
        }
        if (mealType.contains("午餐") || mealType.contains("午饭")) {
            return "午餐";
        }
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) {
            return "晚餐";
        }
        return "加餐";
    }

    private String normalizeLookupMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "";
        }
        if (mealType.contains("练后")) {
            return "练后餐";
        }
        if (mealType.contains("早餐")) {
            return "早餐";
        }
        if (mealType.contains("午餐") || mealType.contains("午饭")) {
            return "午餐";
        }
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) {
            return "晚餐";
        }
        if (mealType.contains("加餐") || mealType.contains("夜宵") || mealType.contains("宵夜")) {
            return "加餐";
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
            return this.aiModelConfig.getConversationModel();
        }
        try {
            Map prefs = (Map)JSON_MAPPER.readValue(user.getModelPreference(), (TypeReference)new TypeReference<Map<String, String>>(){});
            String current = (String)prefs.get("current");
            return current != null && !current.isBlank() ? current.trim() : this.aiModelConfig.getConversationModel();
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

    private AiModelConfig.ModelProvider resolveVisionProvider(Long userId) {
        try {
            User user = (User) this.userService.getById(userId);
            if (user != null) {
                Map<String, String> prefs = this.parseModelPreference(user);
                String vm = prefs.get("visionModel");
                if (vm != null && !vm.isBlank()) {
                    AiModelConfig.ModelProvider p = this.aiModelConfig.getProvider(vm.trim());
                    if (p != null) return p;
                }
            }
        } catch (Exception e) {
            log.warn("[VisionProvider] resolve user preference failed: {}", e.getMessage());
        }
        return this.aiModelConfig.getVisionProvider();
    }

    private String resolveActiveModelName() {
        String modelName = ACTIVE_CHAT_MODEL.get();
        return this.isBlank(modelName) ? this.aiModelConfig.getConversationModel() : modelName.trim();
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
        throw new BusincessException(StateCode.AI_ERROR, "当前AI模型不存在，请切换别的AI试一试");
    }

    private List<Map<String, String>> loadCustomModelsFromRedis(Long userId) {
        User dbUser = (User)this.userService.getById(userId);
        String json = dbUser.getCustomModels();
        if (json == null || json.isBlank()) {
            return new ArrayList<Map<String, String>>();
        }
        try {
            List<Map<String, String>> models = JSON_MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>(){});
            String secret = this.aiModelConfig.getCryptoSecret();
            for (Map<String, String> m : models) {
                String encrypted = m.get("apiKey");
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
        String text = advice == null ? "" : advice.trim();
        if (text.isEmpty()) {
            return isRestDay ? "建议：今天以恢复为主，做一点轻拉伸和散步就够了。" : "建议：动作节奏稳一点，先把热身和拉伸做完整。";
        }
        text = text.replaceAll("^(今天|明天)[\\uff0c,:\\uff1a]?", "").trim();
        text = text.replaceAll("\\r", "");
        String[] suggestionParts = text.split("(?=建议[\\uff1a:])");
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
        if (!isRestDay && (text.contains("休息日") || text.contains("恢复日"))) {
            return this.buildTrainingAdviceFallback(normalizedSection, false);
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
        String mealType = this.resolveMealType(msg);
        if (mealType == null) {
            return "6. 如果用户没有明确指定某一餐，默认生成全天饮食（早餐、午餐、晚餐、加餐四餐）\n7. 如果用户问法比较宽泛，如“推荐饮食”“减脂怎么吃”，也按全天饮食输出\n8. 输出时必须覆盖早餐、午餐、晚餐、加餐四餐，不要只给其中一餐\n";
        }
        return "6. 用户这次明确只想看某一餐，请只生成【%s】这一餐，不要输出全天食谱\n7. 不要额外补早餐/午餐/晚餐/加餐其它餐次\n8. 标题和正文都围绕【%s】这一餐，不要出现“全天食谱”“一日食谱推荐”\n".formatted(mealType, mealType);
    }

    private String sanitizeTrainingPlanOutput(String aiReply, boolean forceDefaultTrainingWeek) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        Matcher matcher = Pattern.compile("###\\s*星期([一二三四五六日天]).*?(?=###\\s*星期|$)", 32).matcher(aiReply);
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
        return "### 训练计划\n\n" + String.join((CharSequence)"\n\n", sections);
    }

    private boolean shouldForceDefaultTrainingWeek(String message) {
        if (this.isBlank(message)) {
            return true;
        }
        String normalized = message.replace("每周", "");
        return !normalized.contains("练三休一") && !normalized.contains("练四休一") && !normalized.contains("练一休一") && !normalized.contains("每天都练") && !normalized.contains("天天练") && !normalized.contains("每周练") && !normalized.contains("一周练") && !normalized.contains("周末也练") && !normalized.contains("周六练") && !normalized.contains("周日练") && !normalized.contains("星期六练") && !normalized.contains("星期日练") && !normalized.contains("周几休") && !normalized.contains("星期几休");
    }

    private void enforceDefaultTrainingWeek(List<String> sections) {
        Map<String, String> forcedRestMap = Map.of("三", "### 星期三 · 休息日\n\n> 休息，可做30分钟低强度有氧（散步/快走）", "六", "### 星期六 · 休息日\n\n> 休息，可做30分钟低强度有氧（散步/快走）", "日", "### 星期日 · 休息日\n\n> 休息，可做30分钟低强度有氧（散步/快走）", "天", "### 星期日 · 休息日\n\n> 休息，可做30分钟低强度有氧（散步/快走）");
        for (int i = 0; i < sections.size(); ++i) {
            String dayKey;
            String forcedRest;
            String section = sections.get(i);
            Matcher dayMatcher = Pattern.compile("星期([一二三四五六日天])").matcher(section);
            if (!dayMatcher.find() || (forcedRest = forcedRestMap.get(dayKey = dayMatcher.group(1))) == null) continue;
            sections.set(i, forcedRest);
        }
    }

    private String sanitizeDietPlanOutput(String aiReply, String targetMealType) {
        if (aiReply == null || aiReply.isBlank()) {
            return aiReply;
        }
        List<String> mealOrder = List.of("早餐", "午餐", "晚餐", "加餐", "练后餐");
        LinkedHashMap<String, String> mealMap = new LinkedHashMap<>();
        for (String rawLine : aiReply.split("\\r?\\n")) {
            String normalizedMeal;
            String[] parts;
            String line = this.safeTrim(rawLine);
            if (!line.startsWith("|") || line.contains("---") || (parts = line.split("\\|")).length < 3) continue;
            String mealCell = this.safeTrim(parts[1]);
            String foodCell = this.safeTrim(parts[2]);
            if (mealCell.isBlank() || foodCell.isBlank() || (normalizedMeal = this.normalizeMealType(mealCell.replaceAll("[\\p{So}\\p{Sc}]", "").trim())).isBlank()) continue;
            mealMap.put(normalizedMeal, mealCell + " | " + foodCell);
        }
        if (mealMap.isEmpty()) {
            return aiReply;
        }
        boolean singleMeal = targetMealType != null && !targetMealType.isBlank();
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(singleMeal ? targetMealType + "推荐" : "一日食谱推荐").append("\n\n");
        sb.append("| 餐次 | 推荐食物 |\n|------|----------|\n");
        if (singleMeal) {
            String row = mealMap.get(targetMealType);
            if (row == null) {
                return aiReply;
            }
            sb.append("| ").append(row).append(" |");
            return sb.toString().trim();
        }
        for (String mealType : mealOrder) {
            String row = mealMap.get(mealType);
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
        AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
        if (visionProvider == null) {
            throw new BusincessException(StateCode.AI_ERROR, "视觉模型未配置");
        }
        String imageUrl = this.fileService.uploadFoodImage(file, userId);
        debugTimings.put("imageUploadedAtMs", System.currentTimeMillis());
        ArrayList<Map<String, Object>> contentParts = new ArrayList<Map<String, Object>>();
        contentParts.add(Map.of("type", "text", "text", FOOD_IDENTIFY_PROMPT));
        contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
        Map<String, Object> visionResponse = this.aiCallHelper.callVision(visionProvider, contentParts, 512, 0.0);
        if (visionResponse == null) {
            throw new BusincessException(StateCode.AI_ERROR, "视觉模型调用失败");
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
                        log.warn("[VisionTrace] grams 值异常: {}, foodName={}", (Object)raw, (Object)foodName);
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
                throw new BusincessException(StateCode.AI_ERROR, "视觉模型响应解析失败");
            }
        }
        if (foodName == null || foodName.isBlank() || foodName.length() > 50) {
            foodName = "未知食物";
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

    /**
     * 图片预处理：缩放到最长边1024px + 轻微增强对比度，返回base64 data URI
     */
    private String preprocessImageToDataUri(byte[] imageBytes, String contentType) {
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
            BufferedImage img = ImageIO.read(bais);
            if (img == null) return null;
            int w = img.getWidth(), h = img.getHeight();
            int maxDim = 1024;
            if (w > maxDim || h > maxDim) {
                double scale = (double) maxDim / Math.max(w, h);
                int nw = (int)(w * scale), nh = (int)(h * scale);
                BufferedImage resized = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.drawImage(img, 0, 0, nw, nh, null);
                g.dispose();
                img = resized;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/jpeg;base64," + base64;
        } catch (Exception e) {
            log.warn("[ImagePreprocess] failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> recognizeImagePrepare(Long userId, MultipartFile file, String type, String customText) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
            if (visionProvider == null) {
                result.put("error", "视觉模型未配置");
                return result;
            }
            String visionPrompt = switch (type) {
                case "equipment" -> "识别图片中的健身器械。只从以下列表中选择匹配的器械名称（可多个，用/分隔）：杠铃、哑铃、龙门架、蝴蝶机、史密斯机、卧推架、深蹲架、上斜凳、下斜凳、平凳、推胸机、推肩机、高位下拉器、划船机、腿举机、腿屈伸机、腿弯举机、哈克深蹲机、倒蹬机、提踵机、髋外展机、卷腹机、弯举机、牧师椅、单杠、双杠、罗马椅、绳索、弹力带、壶铃、战绳、药球、TRX、跳绳、瑜伽垫、无器械。规则：1.只返回器械名称，不要输出其他内容；2.不确定就跳过，不要猜测；3.如果图片不是健身器械，输出\"非健身器械\"。";
                case "nutrition_label" -> "读取图片中的营养成分表/标签，返回结构化JSON。格式：{\"name\":\"食物名\",\"calories\":\"数值\",\"unit\":\"kJ或kcal\",\"protein\":\"数值g\",\"carbs\":\"数值g\",\"fat\":\"数值g\",\"fiber\":\"数值g\"}。只输出JSON，不要其他内容。";
                case "form_check" -> "分析图片中人物的动作姿势，返回JSON：{\"exercise\":\"动作名\",\"score\":85,\"issues\":[\"问题1\",\"问题2\"],\"advice\":[\"建议1\",\"建议2\"]}。只输出JSON。";
                default -> "简要描述图片内容，50字以内。";
            };
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", visionPrompt));
            // 图片预处理：缩放+增强，用base64 data URI发给视觉模型（COS URL仅用于前端展示）
                    String visionImageUrl = imageUrl;
                    try {
                        byte[] imgBytes = file.getBytes();
                        String dataUri = this.preprocessImageToDataUri(imgBytes, file.getContentType());
                        if (dataUri != null) visionImageUrl = dataUri;
                    } catch (Exception e) {
                        log.warn("[RecognizePrepare] preprocess failed, fallback to COS URL: {}", e.getMessage());
                    }
                    parts.add(Map.of("type", "image_url", "image_url", Map.of("url", visionImageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 512, 0.0);
            String visionText = this.extractVisionText(visionRes);
            log.info("[RecognizePrepare] userId={}, type={}, visionResult={}, elapsed={}ms", new Object[]{userId, type, visionText, System.currentTimeMillis() - start});
            if (visionText == null || visionText.isBlank()) {
                result.put("error", "未识别到有效内容");
                return result;
            }
            result.put("imageUrl", imageUrl);
            result.put("equipmentName", visionText);
            if ("equipment".equals(type)) {
                // 视觉模型判断为非健身器械，直接返回不搜索
                if ("非健身器械".equals(visionText)) {
                    result.put("textReply", "未识别到健身器械");
                    return result;
                }
                // AI可能返回"蝴蝶机/夹胸机"，按/或／拆分，取第一个作为主名称
                String[] nameParts = visionText.split("[/／]");
                String primaryName = nameParts[0].trim();
                if (primaryName.isBlank()) {
                    primaryName = visionText;
                }
                if (nameParts.length > 1) {
                    log.info("[RecognizePrepare] userId={}, 器械名拆分: '{}' -> '{}'", userId, visionText, primaryName);
                }
                result.put("equipmentName", primaryName);
                // 联网搜索资料
                String rawSearch = this.webSearchHelper.searchEquipmentInfo(primaryName);
                result.put("rawData", rawSearch);
                // B站和抖音搜索链接
                String encodedName = java.net.URLEncoder.encode(primaryName + " 使用教程", java.nio.charset.StandardCharsets.UTF_8);
                result.put("bilibiliUrl", "https://search.bilibili.com/all?keyword=" + encodedName);
                result.put("douyinUrl", "https://www.douyin.com/search/" + encodedName);
                // 匹配动作库中用到该器械的动作
                List<Exercise> matchedExercises = this.exerciseService.searchByEquipment(primaryName);
                if (matchedExercises != null && !matchedExercises.isEmpty()) {
                    result.put("matchedExercises", matchedExercises.stream()
                        .map(e -> Map.of("name", e.getName(), "muscleGroup", e.getMuscleGroup() != null ? e.getMuscleGroup() : ""))
                        .toList());
                }
                log.info("[RecognizePrepare] userId={}, searchDone={}, matched={}, elapsed={}ms", new Object[]{userId, rawSearch != null, matchedExercises != null ? matchedExercises.size() : 0, System.currentTimeMillis() - start});
            }
        }
        catch (Exception e) {
            log.error("[RecognizePrepare] userId={}, type={}, error={}", new Object[]{userId, type, e.getMessage(), e});
            result.put("error", "识别失败");
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
                    systemPrompt = "你是健身助手Tatan，拥有10年经验的专业健身教练，擅长运动解剖学和训练科学指导。";
                    aiPrompt = "用户上传了健身器械图片，识别为「" + equipmentName + "」。以下是联网搜索的资料：\n\n" + (rawData != null && !rawData.isBlank() ? rawData : "（未搜到相关资料）") + "\n\n请给出专业的器械指导：器械概述、训练部位（解剖学角度）、详细使用步骤（含呼吸节奏和发力顺序）、常见错误与纠正、训练建议（组数/次数/搭配）。";
                    maxTokens = 2048;
                    double temperature = 0.6;
                } else {
                    systemPrompt = "你是健身助手Tatan，擅长用简洁语言解答健身问题。";
                    aiPrompt = "用户上传了健身器械图片，识别为「" + equipmentName + "」。以下是搜索到的资料：\n\n" + (rawData != null && !rawData.isBlank() ? rawData : "未搜到资料") + "\n\n请整理成简洁的器械介绍（训练部位、使用方法、注意事项），不超过200字。";
                    maxTokens = 512;
                    double temperature = 0.3;
                }
            } else if ("nutrition_label".equals(type)) {
                systemPrompt = "你是营养标签解读助手。";
                aiPrompt = "图片营养成分表OCR结果：" + (equipmentName != null ? equipmentName : "无数据") + "\n\n请整理成易读格式，说明每份的热量和各营养素含量。";
                maxTokens = 512;
                double temperature = 0.3;
            } else if ("form_check".equals(type)) {
                systemPrompt = "你是专业健身教练，擅长动作分析和姿势纠正。";
                aiPrompt = "图片中人物的姿势分析结果：" + (equipmentName != null ? equipmentName : "无数据") + "\n\n请给出评分、问题指出和纠正建议，语气鼓励。";
                maxTokens = 512;
                double temperature = 0.3;
            } else {
                systemPrompt = "你是健身助手Tatan。";
                aiPrompt = "图片识别结果：" + (equipmentName != null ? equipmentName : "无数据") + "\n\n请简要回复，不超过100字。";
                maxTokens = 256;
                double temperature = 0.3;
            }
            long aiStart = System.currentTimeMillis();
            try {
                reply = this.callAiApiStreamWithProvider(systemPrompt, aiPrompt, outputStream, summaryProvider, maxTokens);
            }
            catch (Exception e) {
                log.error("[RecognizeSummary] AI流式调用失败", (Throwable)e);
                reply = null;
            }
            long aiElapsed = System.currentTimeMillis() - aiStart;
            log.info("[RecognizeSummary] userId={}, type={}, deepThinking={}, model={}, elapsed={}ms, replyLen={}", new Object[]{userId, type, deepThinking, summaryProvider.getModel(), aiElapsed, reply != null ? Integer.valueOf(reply.length()) : "null"});
        }
        catch (Exception e) {
            log.error("[RecognizeSummary] userId={}, type={}, error={}", new Object[]{userId, type, e.getMessage(), e});
            try {
                this.writeSseEvent(outputStream, "error", "{\"message\":\"生成失败\"}");
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
            AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "视觉模型未配置");
            }
            String visionPrompt = "识别图片中的健身器械，只返回器械名称，不要输出其他内容。只输出名称，不要输出任何解释或JSON。如果图片不是健身器械，输出\"非健身器械\"。";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", visionPrompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 256, 0.0);
            String equipmentName = this.extractVisionText(visionRes);
            if (equipmentName == null || equipmentName.isBlank() || "非健身器械".equals(equipmentName)) {
                result.put("textReply", "未识别到健身器械");
                return result;
            }
            // AI可能返回"蝴蝶机/夹胸机"，按/或／拆分，取第一个作为主名称
            String[] nameParts = equipmentName.split("[/／]");
            String primaryEquipmentName = nameParts[0].trim();
            if (primaryEquipmentName.isBlank()) {
                primaryEquipmentName = equipmentName;
            }
            if (nameParts.length > 1) {
                log.info("[EquipmentRecognition] userId={}, 器械名拆分: '{}' -> '{}'", userId, equipmentName, primaryEquipmentName);
            }
            equipmentName = primaryEquipmentName;
            log.info("[EquipmentRecognition] userId={}, visionResult={}, elapsed={}ms", new Object[]{userId, equipmentName, System.currentTimeMillis() - start});
            String rawSearch = this.webSearchHelper.searchEquipmentInfo(equipmentName);
            log.info("[EquipmentRecognition] userId={}, searchDone={}, elapsed={}ms", new Object[]{userId, rawSearch != null ? "success" : "null", System.currentTimeMillis() - start});
            if (deepThinking) {
                systemPrompt = "你是健身助手Tatan，一位拥有10年经验的专业健身教练。你对各类健身器械了如指掌，\n能够从运动解剖学、生物力学和训练科学的角度给出专业的指导建议。\n你善于用通俗易懂但专业准确的语言解释器械使用方法，并给出针对性的训练建议。\n";
                aiPrompt = rawSearch != null && !rawSearch.isBlank() ? "用户上传了一张健身器械图片，视觉模型识别为「%s」。\n以下是联网搜索到的原始资料：\n\n%s\n\n请基于以上资料和你的专业知识，完成以下任务：\n\n## 1. 器械概述\n简要介绍该器械的基本信息、适用人群、训练效果。\n\n## 2. 精准训练部位\n从解剖学角度分析该器械主要锻炼的肌群（区分主动肌、协同肌、稳定肌），\n并说明不同握姿/站姿/座椅位置对目标肌群的影响。\n\n## 3. 详细使用步骤\n分步骤说明正确的使用方法，包括：\n- 初始调节（座椅高度、靠背角度、配重选择）\n- 动作执行（呼吸节奏、发力顺序、顶峰收缩）\n- 还原控制（离心阶段注意事项）\n\n## 4. 常见错误与纠正\n列举3-5个新手最常犯的错误动作，并给出纠正方法。\n\n## 5. 训练建议\n推荐适合的组数、次数、休息时间，以及如何与其他动作搭配。\n".formatted(equipmentName, rawSearch) : "用户上传了一张健身器械图片，识别为「%s」，但联网搜索未查到相关资料。\n请完全依靠你的专业知识，给出详尽的器械介绍和使用指导。\n\n要求覆盖：器械概述、训练部位（解剖学角度）、详细使用步骤、常见错误与纠正、训练建议。\n如果对该器械不太确定，诚实说明并建议用户在B站搜索该器械的教程视频。\n".formatted(equipmentName);
                maxTokens = 2048;
                temperature = 0.6;
            } else {
                systemPrompt = "你是健身助手Tatan，擅长用简洁易懂的语言解答健身相关问题。";
                aiPrompt = rawSearch != null && !rawSearch.isBlank() ? "用户上传了一张健身器械图片，识别为「" + equipmentName + "」。以下是联网搜索到的资料，请整理成简洁的器械介绍：\n\n" + rawSearch + "\n\n要求：只保留核心的训练部位、使用方法、注意事项，不超过200字。" : "用户上传了一张健身器械图片，识别为「" + equipmentName + "」，联网未搜到资料。请根据知识简要介绍该器械，不超过150字。如果不确定，建议用户在B站搜索教程。";
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
            // 生成B站和抖音搜索链接（固定URL格式，无需联网）
            String encodedName = java.net.URLEncoder.encode(equipmentName + " 使用教程", java.nio.charset.StandardCharsets.UTF_8);
            result.put("bilibiliUrl", "https://search.bilibili.com/all?keyword=" + encodedName);
            result.put("douyinUrl", "https://www.douyin.com/search/" + encodedName);
            // 匹配动作库中用到该器械的动作
            List<Exercise> matchedExercises = this.exerciseService.searchByEquipment(equipmentName);
            if (matchedExercises != null && !matchedExercises.isEmpty()) {
                result.put("matchedExercises", matchedExercises.stream()
                    .map(e -> Map.of("name", e.getName(), "muscleGroup", e.getMuscleGroup() != null ? e.getMuscleGroup() : ""))
                    .toList());
            }
        }
        catch (Exception e) {
            log.error("器械识别失败", (Throwable)e);
            result.put("textReply", "器械识别失败，请重试");
        }
        log.info("[EquipmentRecognition] userId={}, totalElapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleNutritionLabelRecognition(Long userId, MultipartFile file, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "视觉模型未配置");
            }
            String prompt = "读取图片中的营养成分表/标签，返回结构化 JSON。\n只输出 JSON，不要 markdown，不要解释，不要代码块。\n格式：{\"name\":\"食物名\",\"calories\":\"数值\",\"caloriesUnit\":\"kJ或kcal\",\"protein\":\"数值g\",\"carbs\":\"数值g\",\"fat\":\"数值g\",\"fiber\":\"数值g\",\"sodium\":\"数值g\"}\n规则：\n1. 仔细读取标签上的每一项数据，包括热量、蛋白质、碳水化合物、脂肪、膳食纤维、钠等。\n2. caloriesUnit 保留原始单位（kJ 或 kcal）。\n3. 如果图片不是营养成分表，返回 {\"error\":\"not_nutrition_label\"}\n";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 512, 0.0);
            String content = this.extractVisionRaw(visionRes);
            if (content == null) {
                result.put("textReply", "标签识别失败");
                return result;
            }
            String cleaned = content.replaceAll("^\\s*```(?:json)?\\s*", "").replaceAll("\\s*```\\s*$", "").trim();
            int js = cleaned.indexOf("{");
            int je = cleaned.lastIndexOf("}");
            if (js < 0 || je < js) {
                result.put("textReply", "标签识别失败");
                return result;
            }
            String jsonPart = cleaned.substring(js, je + 1);
            Map parsed = (Map)JSON_MAPPER.readValue(jsonPart, Map.class);
            if (parsed.containsKey("error")) {
                result.put("textReply", "图片中未检测到营养成分表");
                return result;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("营养标签读取结果：\n");
            sb.append("食物：").append(this.toCleanString(parsed.get("name"))).append("\n");
            sb.append("热量：").append(this.toCleanString(parsed.get("calories"))).append(" ").append(this.toCleanString(parsed.get("caloriesUnit"))).append("\n");
            sb.append("蛋白质：").append(this.toCleanString(parsed.get("protein"))).append("\n");
            sb.append("碳水化合物：").append(this.toCleanString(parsed.get("carbs"))).append("\n");
            sb.append("脂肪：").append(this.toCleanString(parsed.get("fat"))).append("\n");
            if (parsed.get("fiber") != null) {
                sb.append("膳食纤维：").append(this.toCleanString(parsed.get("fiber"))).append("\n");
            }
            if (parsed.get("sodium") != null) {
                sb.append("钠：").append(this.toCleanString(parsed.get("sodium"))).append("\n");
            }
            result.put("textReply", sb.toString());
            result.put("imageUrl", imageUrl);
            result.put("labelData", parsed);
        }
        catch (Exception e) {
            log.error("营养标签识别失败", (Throwable)e);
            result.put("textReply", "标签识别失败，请重试");
        }
        log.info("[NutritionLabelRecognition] userId={}, elapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleFormCheckRecognition(Long userId, MultipartFile file, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "视觉模型未配置");
            }
            String prompt = "你是专业健身教练。分析图片中人物的健身动作姿势。\n请给出以下内容：\n1. 识别出正在执行的动作名称\n2. 评估姿势是否正确（按1-10分）\n3. 如果有问题，指出具体哪里不对\n4. 给出纠正建议\n用简洁的中文回答。如果图片中没有人或没有在做运动，回复\"未检测到健身动作\"。\n";
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 1024, 0.3);
            String content = this.extractVisionRaw(visionRes);
            if (content == null || content.isBlank()) {
                result.put("textReply", "姿势分析失败，请重试");
                return result;
            }
            result.put("textReply", content);
            result.put("imageUrl", imageUrl);
        }
        catch (Exception e) {
            log.error("姿势纠错失败", (Throwable)e);
            result.put("textReply", "姿势分析失败，请重试");
        }
        log.info("[FormCheckRecognition] userId={}, elapsed={}ms", (Object)userId, (Object)(System.currentTimeMillis() - start));
        return result;
    }

    private Map<String, Object> handleOtherImageRecognition(Long userId, MultipartFile file, String customText, boolean deepThinking) {
        long start = System.currentTimeMillis();
        HashMap<String, Object> result = new HashMap<String, Object>();
        try {
            String imageUrl = this.fileService.uploadFoodImage(file, userId);
            AiModelConfig.ModelProvider visionProvider = this.resolveVisionProvider(userId);
            if (visionProvider == null) {
                throw new BusincessException(StateCode.AI_ERROR, "视觉模型未配置");
            }
            String prompt = "你是健身助手Tatan。用户上传了一张图片并附带了描述：\"%s\"\n请根据图片内容和用户描述，判断是否能提供与健身、饮食相关的帮助。\n如果可以，给出相关建议（如食物营养估算、器械使用、训练建议等）。\n如果与健身饮食完全无关，回复\"我无法处理这个问题\"。\n用简洁的中文回答。\n".formatted(customText != null ? customText : "");
            ArrayList<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(Map.of("type", "text", "text", prompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)));
            Map<String, Object> visionRes = this.aiCallHelper.callVision(visionProvider, parts, 1024, 0.3);
            String content = this.extractVisionRaw(visionRes);
            if (content == null || content.isBlank()) {
                result.put("textReply", "我无法处理这个问题");
                return result;
            }
            result.put("textReply", content);
            result.put("imageUrl", imageUrl);
        }
        catch (Exception e) {
            log.error("other图片识别失败", (Throwable)e);
            result.put("textReply", "识别失败，请重试");
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
                log.warn("食物营养数据校验失败（热量与脂肪不匹配）: foodName={}, calories={}kJ, fat={}g", new Object[]{foodName, cal, fat});
            }
            debugTimings.put("nutritionParsedAtMs", System.currentTimeMillis());
            return result;
        }
        catch (Exception e) {
            log.warn("食物营养搜索失败: foodName={}, error={}", (Object)foodName, (Object)e.getMessage());
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
