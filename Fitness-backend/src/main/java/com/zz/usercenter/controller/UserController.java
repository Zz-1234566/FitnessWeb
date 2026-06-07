package com.zz.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zz.usercenter.model.domain.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wf.captcha.SpecCaptcha;
import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.Exercise;
import com.zz.usercenter.model.domain.ExerciseSession;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.model.domain.UserDailyMetric;
import com.zz.usercenter.model.domain.UserWeightRecord;
import com.zz.usercenter.model.domain.request.CaptchaVO;
import com.zz.usercenter.model.domain.request.UpdatePasswordRequest;
import com.zz.usercenter.model.domain.request.UpdateUserRequest;
import com.zz.usercenter.model.domain.request.UserLoginRequest;
import com.zz.usercenter.model.domain.request.UserRegisterRequest;
import com.zz.usercenter.model.domain.vo.UserProgressTrendVO;
import com.zz.usercenter.service.ChatService;
import com.zz.usercenter.service.UserDailyMetricService;
import com.zz.usercenter.service.ExerciseRecordService;
import com.zz.usercenter.service.ExerciseService;
import com.zz.usercenter.service.FileService;
import com.zz.usercenter.service.UserService;
import com.zz.usercenter.service.UserRecordService;
import com.zz.usercenter.service.UserWeightRecordService;
import com.zz.usercenter.util.CalorieCalculator;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.NO_AUTH;
import static com.zz.usercenter.common.StateCode.NOT_LOGIN;
import static com.zz.usercenter.common.StateCode.NULL_ERROR;
import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;
import static com.zz.usercenter.common.StateCode.SYSTEM_ERROR;
import static com.zz.usercenter.constant.UserConstant.ADMIN_ROLE;
import static com.zz.usercenter.constant.UserConstant.CAPTCHA_KEY;
import static com.zz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    @Resource
    private ExerciseService exerciseService;

    @Resource
    private ChatService chatService;

    @Resource
    private UserRecordService userRecordService;

    @Resource
    private UserWeightRecordService userWeightRecordService;

    @Resource
    private UserDailyMetricService userDailyMetricService;

    @Resource
    private ExerciseRecordService exerciseRecordService;

    @Resource
    private com.zz.usercenter.service.UserProfileService userProfileService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime SUMMARY_PUSH_TIME = LocalTime.of(8, 0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("M月d日");
    private static final String[] WEEKDAYS = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
    private static final Pattern DISPLAY_DATE_PATTERN = Pattern.compile("(\\d{1,2})月(\\d{1,2})日");
    private static final int CARD_PREVIEW_LIMIT = 56;
    private static final String SUMMARY_NOTICE_READ_STATE = "SUMMARY_NOTICE_READ_STATE";

    private final String SALT = "Zz";

    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest,
                                           HttpServletRequest request) {
        if (userRegisterRequest == null) {
            throw new BusincessException(PARAMS_ERROR, "注册请求为空");
        }

        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String username = userRegisterRequest.getUsername();
        String captcha = userRegisterRequest.getCaptcha();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, captcha)) {
            throw new BusincessException(PARAMS_ERROR, "请求参数为空");
        }

        Object captchaObj = request.getSession().getAttribute(CAPTCHA_KEY);
        if (captchaObj == null) {
            throw new BusincessException(PARAMS_ERROR, "验证码为空");
        }

        String sessionCaptcha = captchaObj.toString().toLowerCase();
        if (!sessionCaptcha.equals(captcha.toLowerCase())) {
            request.getSession().removeAttribute(CAPTCHA_KEY);
            throw new BusincessException(PARAMS_ERROR, "验证码错误");
        }

        request.getSession().removeAttribute(CAPTCHA_KEY);
        return ResultUtils.success(userService.userRegister(userAccount, userPassword, checkPassword, username));
    }

    @PostMapping("login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                        HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusincessException(PARAMS_ERROR, "请求参数为空");
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusincessException(PARAMS_ERROR, "账号或密码为空");
        }

        return ResultUtils.success(userService.userLogin(userAccount, userPassword, request));
    }

    @PostMapping("logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusincessException(NOT_LOGIN, "用户未登录");
        }
        return ResultUtils.success(userService.userLogout(request));
    }

    @GetMapping("current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = requireLogin(request);
        User user = userService.getById(currentUser.getId());
        if (user == null) {
            throw new BusincessException(NULL_ERROR, "用户不存在");
        }
        return ResultUtils.success(userService.getSafetyUser(user));
    }

    @GetMapping("/summary-notification")
    public BaseResponse<Map<String, Object>> getSummaryNotification(HttpServletRequest request) {
        User currentUser = requireLogin(request);
        User dbUser = userService.getById(currentUser.getId());
        if (dbUser == null) {
            throw new BusincessException(NULL_ERROR, "用户不存在");
        }

        LocalDate today = LocalDate.now(CN_ZONE);
        LocalTime now = LocalTime.now(CN_ZONE);
        List<Map<String, Object>> cards = buildActiveNoticeCards(dbUser, today, now);
        if (cards.isEmpty()) {
            return ResultUtils.success(null);
        }

        Map<String, Map<String, Boolean>> notificationStates = parseNotificationStates(dbUser.getNotificationStates());
        notificationStates = pruneNotificationStates(notificationStates, cards);

        List<Map<String, Object>> visibleCards = new ArrayList<>();
        int unreadCount = 0;
        for (Map<String, Object> card : cards) {
            String cardId = String.valueOf(card.get("id"));
            if (getNotificationFlag(notificationStates, cardId, "cleared")) {
                continue;
            }
            boolean read = getNotificationFlag(notificationStates, cardId, "read");
            card.put("read", read);
            visibleCards.add(card);
            if (!read) {
                unreadCount++;
            }
        }
        persistNotificationStates(currentUser.getId(), notificationStates);
        if (visibleCards.isEmpty()) {
            return ResultUtils.success(null);
        }

        Map<String, Object> result = new HashMap<>();
        String noticeType = resolveNoticeType(visibleCards);
        result.put("id", buildNoticeId("summary", visibleCards));
        result.put("type", noticeType);
        result.put("title", resolveNoticeTitle(noticeType));
        result.put("cards", visibleCards);
        result.put("pushDate", String.valueOf(visibleCards.get(0).get("date")));
        // 根据卡片类型动态决定推送显示时间
        String pushTime;
        if ("evening".equals(noticeType) || noticeType.contains("evening")) {
            pushTime = "19:50";
        } else if ("morning".equals(noticeType)) {
            pushTime = "07:50";
        } else {
            pushTime = "07:50";
        }
        result.put("pushTime", pushTime);
        result.put("unreadCount", unreadCount);
        result.put("hasUnread", unreadCount > 0);
        return ResultUtils.success(result);
    }

    @PostMapping("/summary-notification/read")
    public BaseResponse<Boolean> markSummaryNotificationRead(@RequestBody Map<String, String> body,
                                                             HttpServletRequest request) {
        User currentUser = requireLogin(request);
        String noticeId = body.get("noticeId");
        if (StringUtils.isBlank(noticeId)) {
            throw new BusincessException(PARAMS_ERROR, "noticeId不能为空");
        }
        updateNotificationState(currentUser.getId(), noticeId, true, null);
        return ResultUtils.success(true);
    }

    @PostMapping("/summary-notification/read-all")
    public BaseResponse<Boolean> markAllSummaryNotificationsRead(HttpServletRequest request) {
        User currentUser = requireLogin(request);
        User dbUser = userService.getById(currentUser.getId());
        if (dbUser == null) {
            throw new BusincessException(NULL_ERROR, "用户不存在");
        }

        LocalDate today = LocalDate.now(CN_ZONE);
        LocalTime now = LocalTime.now(CN_ZONE);
        List<Map<String, Object>> cards = buildActiveNoticeCards(dbUser, today, now);
        if (!cards.isEmpty()) {
            Map<String, Map<String, Boolean>> notificationStates = parseNotificationStates(dbUser.getNotificationStates());
            for (Map<String, Object> card : cards) {
                String noticeId = String.valueOf(card.get("id"));
                Map<String, Boolean> state = notificationStates.computeIfAbsent(noticeId, key -> new HashMap<>());
                state.put("read", true);
            }
            persistNotificationStates(currentUser.getId(), notificationStates);
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/summary-notification/status")
    public BaseResponse<Boolean> updateSummaryNotificationStatus(@RequestBody Map<String, Object> body,
                                                                 HttpServletRequest request) {
        User currentUser = requireLogin(request);
        String noticeId = body.get("noticeId") == null ? null : String.valueOf(body.get("noticeId"));
        if (StringUtils.isBlank(noticeId)) {
            throw new BusincessException(PARAMS_ERROR, "noticeId不能为空");
        }

        Boolean read = body.containsKey("read") ? Boolean.valueOf(String.valueOf(body.get("read"))) : null;
        Boolean cleared = body.containsKey("cleared") ? Boolean.valueOf(String.valueOf(body.get("cleared"))) : null;
        if (read == null && cleared == null) {
            throw new BusincessException(PARAMS_ERROR, "状态不能为空");
        }

        updateNotificationState(currentUser.getId(), noticeId, read, cleared);
        return ResultUtils.success(true);
    }

    @GetMapping("search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusincessException(NO_AUTH, "无权限");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username", username);
        }

        List<User> result = userService.list(queryWrapper).stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toList());
        return ResultUtils.success(result);
    }

    @PostMapping("delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusincessException(NO_AUTH, "无权限");
        }
        if (id <= 0) {
            throw new BusincessException(PARAMS_ERROR, "请求参数错误");
        }
        return ResultUtils.success(userService.removeById(id));
    }

    @PostMapping("delete/batch")
    public BaseResponse<Boolean> batchDeleteUsers(@RequestBody List<Long> ids, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusincessException(NO_AUTH, "无权限");
        }
        if (ids == null || ids.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "请求参数错误");
        }
        return ResultUtils.success(userService.removeByIds(ids));
    }

    @GetMapping("/captcha")
    public BaseResponse<CaptchaVO> getCaptcha(HttpServletRequest request) throws IOException {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        request.getSession().setAttribute(CAPTCHA_KEY, captcha.text().toLowerCase());
        return ResultUtils.success(new CaptchaVO(captcha.toBase64()));
    }

    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        User currentUser = requireLogin(request);
        String url = fileService.uploadAvatar(file, currentUser.getId());

        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setAvatarUrl(url);
        userService.updateById(updateUser);
        currentUser.setAvatarUrl(url);

        return ResultUtils.success(url);
    }

    @PostMapping("/update")
    public BaseResponse<User> updateUser(@RequestBody UpdateUserRequest updateRequest,
                                         HttpServletRequest request) {
        User currentUser = requireLogin(request);

        // 基础信息写 User 表
        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setUsername(updateRequest.getUsername());
        updateUser.setGender(updateRequest.getGender());
        updateUser.setHeight(updateRequest.getHeight());
        updateUser.setWeight(updateRequest.getWeight());
        updateUser.setAge(updateRequest.getAge());
        updateUser.setCity(updateRequest.getCity());
        updateUser.setCityEn(updateRequest.getCityEn());
        userService.updateById(updateUser);
        UserProfile dbProfile = userProfileService.getByUserId(currentUser.getId());
        String resolvedActivityLevel = updateRequest.getActivityLevel() != null ? updateRequest.getActivityLevel()
                : (dbProfile != null ? dbProfile.getActivityLevel() : null);
        Double resolvedActivityFactor = CalorieCalculator.resolveActivityFactor(resolvedActivityLevel);
        Integer gender = updateRequest.getGender() != null ? updateRequest.getGender()
                : (currentUser.getGender() != null ? currentUser.getGender() : null);
        Double height = updateRequest.getHeight() != null ? updateRequest.getHeight()
                : (currentUser.getHeight() != null ? currentUser.getHeight() : null);
        Double weight = updateRequest.getWeight() != null ? updateRequest.getWeight()
                : (currentUser.getWeight() != null ? currentUser.getWeight() : null);
        Integer age = updateRequest.getAge() != null ? updateRequest.getAge()
                : (currentUser.getAge() != null ? currentUser.getAge() : null);

        UserProfile profile = new UserProfile();
        profile.setUserId(currentUser.getId());
        profile.setActivityLevel(resolvedActivityLevel);
        profile.setActivityFactor(resolvedActivityFactor);
        profile.setDailyCalorieBurn(CalorieCalculator.calculateDailyCalorieBurn(
                gender, height, weight, age, resolvedActivityFactor, resolvedActivityLevel));
        profile.setCustomDailyCalories(updateRequest.getCustomDailyCalories() != null && updateRequest.getCustomDailyCalories() <= 0
                ? null : (updateRequest.getCustomDailyCalories() != null ? updateRequest.getCustomDailyCalories() : (dbProfile != null ? dbProfile.getCustomDailyCalories() : null)));
        profile.setTargetWeight(updateRequest.getTargetWeight() != null && updateRequest.getTargetWeight() <= 0
                ? null : (updateRequest.getTargetWeight() != null ? updateRequest.getTargetWeight() : (dbProfile != null ? dbProfile.getTargetWeight() : null)));
        profile.setFitnessGoal(updateRequest.getFitnessGoal() != null ? updateRequest.getFitnessGoal() : (dbProfile != null ? dbProfile.getFitnessGoal() : null));
        profile.setExperienceLevel(updateRequest.getExperienceLevel() != null ? updateRequest.getExperienceLevel() : (dbProfile != null ? dbProfile.getExperienceLevel() : null));
        profile.setPreferredEquipment(updateRequest.getPreferredEquipment() != null ? updateRequest.getPreferredEquipment() : (dbProfile != null ? dbProfile.getPreferredEquipment() : null));
        profile.setUserProfileText(updateRequest.getUserProfile() != null ? updateRequest.getUserProfile() : (dbProfile != null ? dbProfile.getUserProfileText() : null));
        profile.setWeeklyTrainingDays(updateRequest.getWeeklyTrainingDays() != null ? updateRequest.getWeeklyTrainingDays() : (dbProfile != null ? dbProfile.getWeeklyTrainingDays() : null));
        profile.setTrainingDuration(updateRequest.getTrainingDuration() != null ? updateRequest.getTrainingDuration() : (dbProfile != null ? dbProfile.getTrainingDuration() : null));
        profile.setOccupation(updateRequest.getOccupation() != null ? updateRequest.getOccupation() : (dbProfile != null ? dbProfile.getOccupation() : null));
        profile.setPersonality(updateRequest.getPersonality() != null ? updateRequest.getPersonality() : (dbProfile != null ? dbProfile.getPersonality() : null));
        profile.setMedicalHistory(updateRequest.getMedicalHistory() != null ? updateRequest.getMedicalHistory() : (dbProfile != null ? dbProfile.getMedicalHistory() : null));
        profile.setDietPreference(updateRequest.getDietPreference() != null ? updateRequest.getDietPreference() : (dbProfile != null ? dbProfile.getDietPreference() : null));
        profile.setTrainingPreference(updateRequest.getTrainingPreference() != null ? updateRequest.getTrainingPreference() : (dbProfile != null ? dbProfile.getTrainingPreference() : null));
        userProfileService.saveOrUpdate(currentUser.getId(), profile);

        userWeightRecordService.saveOrUpdateTodayWeight(currentUser.getId(), updateRequest.getWeight());
        userDailyMetricService.syncTodayTarget(currentUser);

        return ResultUtils.success(userService.getSafetyUser(userService.getById(currentUser.getId())));
    }

    @PostMapping("/generate-profile")
    public BaseResponse<String> generateProfile(@RequestBody Map<String, String> body,
                                                HttpServletRequest request) {
        User currentUser = requireLogin(request);
        User dbUser = userService.getById(currentUser.getId());
        String profileFormData = body.get("profileData");
        if (StringUtils.isBlank(profileFormData)) {
            throw new BusincessException(PARAMS_ERROR, "画像数据为空");
        }
        return ResultUtils.success(chatService.generateUserProfile(dbUser, profileFormData));
    }

    @PostMapping("/admin/update")
    public BaseResponse<User> adminUpdateUser(@RequestBody UpdateUserRequest updateRequest,
                                              HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusincessException(NO_AUTH, "无权限");
        }
        if (updateRequest.getId() == null) {
            throw new BusincessException(PARAMS_ERROR, "用户ID不能为空");
        }
        User dbUser = userService.getById(updateRequest.getId());
        if (dbUser == null) {
            throw new BusincessException(NULL_ERROR, "用户不存在");
        }
        User updateUser = new User();
        updateUser.setId(updateRequest.getId());
        updateUser.setUsername(updateRequest.getUsername());
        updateUser.setGender(updateRequest.getGender());
        updateUser.setHeight(updateRequest.getHeight());
        updateUser.setWeight(updateRequest.getWeight());
        updateUser.setAge(updateRequest.getAge());
        updateUser.setCity(updateRequest.getCity());
        updateUser.setCityEn(updateRequest.getCityEn());
        userService.updateById(updateUser);

        UserProfile dbProfile = userProfileService.getByUserId(dbUser.getId());
        String resolvedActivityLevel = updateRequest.getActivityLevel() != null ? updateRequest.getActivityLevel()
                : (dbProfile != null ? dbProfile.getActivityLevel() : null);
        Integer gender = updateRequest.getGender() != null ? updateRequest.getGender() : dbUser.getGender();
        Double height = updateRequest.getHeight() != null ? updateRequest.getHeight() : dbUser.getHeight();
        Double weight = updateRequest.getWeight() != null ? updateRequest.getWeight() : dbUser.getWeight();
        Integer age = updateRequest.getAge() != null ? updateRequest.getAge() : dbUser.getAge();

        UserProfile profile = new UserProfile();
        profile.setUserId(dbUser.getId());
        profile.setActivityLevel(resolvedActivityLevel);
        profile.setActivityFactor(CalorieCalculator.resolveActivityFactor(resolvedActivityLevel));
        profile.setDailyCalorieBurn(CalorieCalculator.calculateDailyCalorieBurn(
                gender, height, weight, age, profile.getActivityFactor(), resolvedActivityLevel));
        profile.setCustomDailyCalories(updateRequest.getCustomDailyCalories() != null && updateRequest.getCustomDailyCalories() <= 0
                ? null : (updateRequest.getCustomDailyCalories() != null ? updateRequest.getCustomDailyCalories() : (dbProfile != null ? dbProfile.getCustomDailyCalories() : null)));
        profile.setTargetWeight(updateRequest.getTargetWeight() != null && updateRequest.getTargetWeight() <= 0
                ? null : (updateRequest.getTargetWeight() != null ? updateRequest.getTargetWeight() : (dbProfile != null ? dbProfile.getTargetWeight() : null)));
        profile.setFitnessGoal(updateRequest.getFitnessGoal() != null ? updateRequest.getFitnessGoal() : (dbProfile != null ? dbProfile.getFitnessGoal() : null));
        profile.setExperienceLevel(updateRequest.getExperienceLevel() != null ? updateRequest.getExperienceLevel() : (dbProfile != null ? dbProfile.getExperienceLevel() : null));
        profile.setPreferredEquipment(updateRequest.getPreferredEquipment() != null ? updateRequest.getPreferredEquipment() : (dbProfile != null ? dbProfile.getPreferredEquipment() : null));
        profile.setUserProfileText(updateRequest.getUserProfile() != null ? updateRequest.getUserProfile() : (dbProfile != null ? dbProfile.getUserProfileText() : null));
        profile.setWeeklyTrainingDays(updateRequest.getWeeklyTrainingDays() != null ? updateRequest.getWeeklyTrainingDays() : (dbProfile != null ? dbProfile.getWeeklyTrainingDays() : null));
        profile.setTrainingDuration(updateRequest.getTrainingDuration() != null ? updateRequest.getTrainingDuration() : (dbProfile != null ? dbProfile.getTrainingDuration() : null));
        profile.setOccupation(updateRequest.getOccupation() != null ? updateRequest.getOccupation() : (dbProfile != null ? dbProfile.getOccupation() : null));
        profile.setPersonality(updateRequest.getPersonality() != null ? updateRequest.getPersonality() : (dbProfile != null ? dbProfile.getPersonality() : null));
        profile.setMedicalHistory(updateRequest.getMedicalHistory() != null ? updateRequest.getMedicalHistory() : (dbProfile != null ? dbProfile.getMedicalHistory() : null));
        profile.setDietPreference(updateRequest.getDietPreference() != null ? updateRequest.getDietPreference() : (dbProfile != null ? dbProfile.getDietPreference() : null));
        profile.setTrainingPreference(updateRequest.getTrainingPreference() != null ? updateRequest.getTrainingPreference() : (dbProfile != null ? dbProfile.getTrainingPreference() : null));
        userProfileService.saveOrUpdate(dbUser.getId(), profile);

        userWeightRecordService.saveOrUpdateTodayWeight(updateRequest.getId(), updateRequest.getWeight());
        userDailyMetricService.syncTodayTarget(dbUser);

        return ResultUtils.success(userService.getSafetyUser(userService.getById(updateRequest.getId())));
    }

    @GetMapping("/progress-trend")
    public BaseResponse<UserProgressTrendVO> getProgressTrend(
            @RequestParam(value = "range", defaultValue = "week") String range,
            HttpServletRequest request) {
        User currentUser = requireLogin(request);
        LocalDate endDate = LocalDate.now(CN_ZONE);

        List<UserWeightRecord> weightRecords;
        List<UserDailyMetric> calorieMetrics;

        LocalDate startDate = switch (range) {
            case "year" -> endDate.minusDays(364);
            case "month" -> endDate.minusDays(29);
            default -> endDate.minusDays(6);
        };

        weightRecords = userWeightRecordService.listByUserAndDateRange(currentUser.getId(), startDate, endDate);
        calorieMetrics = userDailyMetricService.listByUserAndDateRange(currentUser.getId(), startDate, endDate);
        Map<LocalDate, Double> weightMap = weightRecords.stream()
                .collect(Collectors.toMap(UserWeightRecord::getRecordDate, item -> item.getWeight().doubleValue(), (a, b) -> b));
        Map<LocalDate, UserDailyMetric> calorieMap = calorieMetrics.stream()
                .collect(Collectors.toMap(UserDailyMetric::getRecordDate, item -> item, (a, b) -> b));
        Map<LocalDate, Integer> exerciseCalorieMap =
                exerciseRecordService.sumCaloriesByUserAndDateRange(currentUser.getId(), startDate, endDate);

        List<UserProgressTrendVO.TrendPoint> points = new ArrayList<>();
        Double firstWeight = null;
        Double lastWeight = null;
        double totalCalorieBalance = 0D;
        boolean hasCalorieBalance = false;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Double weight = weightMap.get(date);
            // 未称重的日期沿用前一天的体重
            if (weight == null) {
                weight = lastWeight;
            }
            UserDailyMetric metric = calorieMap.get(date);
            Integer exerciseCal = exerciseCalorieMap.getOrDefault(date, 0);
            if (weightMap.containsKey(date)) {
                if (firstWeight == null) {
                    firstWeight = weight;
                }
                lastWeight = weight;
            } else if (weight != null) {
                // 沿用的值也更新 lastWeight，确保后续天能继续沿用
                if (firstWeight == null) {
                    firstWeight = weight;
                }
            }
            if (metric != null && metric.getCalorieBalance() != null) {
                totalCalorieBalance += metric.getCalorieBalance();
                hasCalorieBalance = true;
            }
            points.add(new UserProgressTrendVO.TrendPoint(
                    date.toString(),
                    (date.getMonthValue()) + "/" + date.getDayOfMonth(),
                    weight,
                    metric == null ? null : metric.getIntakeCalories(),
                    metric == null ? null : metric.getTargetCalories(),
                    metric == null ? null : metric.getCalorieBalance(),
                    exerciseCal > 0 ? exerciseCal : null
            ));
        }

        UserProgressTrendVO result = new UserProgressTrendVO();
        Double latestOverallWeight = userWeightRecordService.getLatestWeightAtOrBefore(currentUser.getId(), endDate);
        result.setPoints(points);
        result.setLatestWeight(latestOverallWeight != null
                ? latestOverallWeight
                : (currentUser.getWeight() == null ? null : currentUser.getWeight().doubleValue()));
        result.setWeeklyWeightChange(firstWeight == null || lastWeight == null ? null : Math.round((lastWeight - firstWeight) * 100.0) / 100.0);
        result.setWeeklyCalorieBalance(hasCalorieBalance ? Math.round(totalCalorieBalance * 100.0) / 100.0 : null);
        UserProfile profile = userProfileService.getByUserId(currentUser.getId());
        result.setDailyCalorieBurn(profile != null ? profile.getDailyCalorieBurn() : null);
        result.setCustomDailyCalories(profile != null ? profile.getCustomDailyCalories() : null);
        return ResultUtils.success(result);
    }

    private Double getLatestWeight(List<UserWeightRecord> records, LocalDate endDate) {
        return records.stream()
                .filter(r -> !r.getRecordDate().isAfter(endDate))
                .max(Comparator.comparing(UserWeightRecord::getRecordDate))
                .map(r -> r.getWeight().doubleValue())
                .orElse(null);
    }

    private UserProgressTrendVO buildYearlyTrend(List<UserWeightRecord> weightRecords,
                                                   List<UserDailyMetric> calorieMetrics,
                                                   LocalDate startDate, LocalDate endDate) {
        Map<Integer, List<Double>> monthlyWeights = new LinkedHashMap<>();
        Map<Integer, List<Double>> monthlyBalances = new LinkedHashMap<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int monthKey = date.getYear() * 100 + date.getMonthValue();
            monthlyWeights.computeIfAbsent(monthKey, k -> new ArrayList<>());
            monthlyBalances.computeIfAbsent(monthKey, k -> new ArrayList<>());
        }

        for (UserWeightRecord r : weightRecords) {
            int monthKey = r.getRecordDate().getYear() * 100 + r.getRecordDate().getMonthValue();
            List<Double> list = monthlyWeights.get(monthKey);
            if (list != null) list.add(r.getWeight().doubleValue());
        }

        for (UserDailyMetric m : calorieMetrics) {
            int monthKey = m.getRecordDate().getYear() * 100 + m.getRecordDate().getMonthValue();
            List<Double> list = monthlyBalances.get(monthKey);
            if (list != null && m.getCalorieBalance() != null) {
                list.add(m.getCalorieBalance());
            }
        }

        List<UserProgressTrendVO.TrendPoint> points = new ArrayList<>();
        Double firstWeight = null;
        Double lastWeight = null;

        for (Map.Entry<Integer, List<Double>> entry : monthlyWeights.entrySet()) {
            List<Double> weights = entry.getValue();
            if (weights.isEmpty()) {
                int m = entry.getKey() % 100;
                points.add(new UserProgressTrendVO.TrendPoint(null, m + "月", null, null, null, null, null));
                continue;
            }
            double avg = weights.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double rounded = Math.round(avg * 10.0) / 10.0;
            if (firstWeight == null) firstWeight = rounded;
            lastWeight = rounded;
            int m = entry.getKey() % 100;
            List<Double> balances = monthlyBalances.getOrDefault(entry.getKey(), List.of());
            Double balance = balances.isEmpty() ? null : Math.round(balances.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0;
            points.add(new UserProgressTrendVO.TrendPoint(null, m + "月", rounded, null, null, balance, null));
        }

        UserProgressTrendVO result = new UserProgressTrendVO();
        result.setPoints(points);
        result.setWeeklyWeightChange(firstWeight == null || lastWeight == null ? null : Math.round((lastWeight - firstWeight) * 100.0) / 100.0);
        return result;
    }

    @PostMapping("/updatePassword")
    public BaseResponse<Boolean> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest,
                                                HttpServletRequest request) {
        User currentUser = requireLogin(request);
        String newPassword = updatePasswordRequest.getNewPassword();
        String checkPassword = updatePasswordRequest.getCheckPassword();
        String captcha = updatePasswordRequest.getCaptcha();

        if (newPassword == null || checkPassword == null || captcha == null) {
            throw new BusincessException(PARAMS_ERROR, "参数不能为空");
        }
        if (!newPassword.equals(checkPassword)) {
            throw new BusincessException(PARAMS_ERROR, "两次输入的密码不一致");
        }
        if (newPassword.length() < 8) {
            throw new BusincessException(PARAMS_ERROR, "密码不能少于8位");
        }

        Object captchaObj = request.getSession().getAttribute(CAPTCHA_KEY);
        if (captchaObj == null) {
            throw new BusincessException(PARAMS_ERROR, "验证码已过期，请重新获取");
        }
        String sessionCaptcha = captchaObj.toString().toLowerCase();
        if (!sessionCaptcha.equals(captcha.toLowerCase())) {
            request.getSession().removeAttribute(CAPTCHA_KEY);
            throw new BusincessException(PARAMS_ERROR, "验证码错误");
        }
        request.getSession().removeAttribute(CAPTCHA_KEY);

        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setUserPassword(DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes()));
        userService.updateById(updateUser);
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return ResultUtils.success(true);
    }

    @PostMapping("/favorite/toggle")
    public BaseResponse<List<Long>> toggleFavorite(@RequestBody Map<String, Long> body,
                                                   HttpServletRequest request) {
        User currentUser = requireLogin(request);
        Long exerciseId = body.get("exerciseId");
        if (exerciseId == null || exerciseId <= 0) {
            throw new BusincessException(PARAMS_ERROR, "参数错误");
        }

        User dbUser = userService.getById(currentUser.getId());
        List<Long> favoriteIds = parseFavorites(dbUser.getFavoritesExercises());
        if (favoriteIds.contains(exerciseId)) {
            favoriteIds.remove(exerciseId);
        } else {
            favoriteIds.add(exerciseId);
        }

        String favoritesJson;
        try {
            favoritesJson = OBJECT_MAPPER.writeValueAsString(favoriteIds);
        } catch (JsonProcessingException e) {
            throw new BusincessException(SYSTEM_ERROR, "收藏数据保存失败");
        }

        User updateUser = new User();
        updateUser.setId(currentUser.getId());
        updateUser.setFavoritesExercises(favoritesJson);
        userService.updateById(updateUser);
        currentUser.setFavoritesExercises(favoritesJson);

        return ResultUtils.success(favoriteIds);
    }

    @GetMapping("/favorite/list")
    public BaseResponse<List<Exercise>> getFavoriteList(HttpServletRequest request) {
        User currentUser = requireLogin(request);
        User dbUser = userService.getById(currentUser.getId());
        List<Long> favoriteIds = parseFavorites(dbUser.getFavoritesExercises());
        if (favoriteIds.isEmpty()) {
            return ResultUtils.success(Collections.emptyList());
        }
        return ResultUtils.success(exerciseService.getByIds(favoriteIds));
    }

    private User requireLogin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusincessException(NOT_LOGIN, "未登录，请先登录");
        }
        return currentUser;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    private List<Map<String, Object>> buildActiveNoticeCards(User user, LocalDate today, LocalTime now) {
        LocalDate effectiveDate = now.isBefore(SUMMARY_PUSH_TIME) ? today.minusDays(1) : today;
        LocalDate weekStart = effectiveDate.with(DayOfWeek.MONDAY);
        UserRecord userRecord = getUserRecord(user.getId());

        List<Map<String, Object>> cards = new ArrayList<>();
        Map<String, Object> weeklyCard = buildActiveWeeklyNoticeCard(userRecord, weekStart);
        if (weeklyCard != null) {
            cards.add(weeklyCard);
        }
        cards.addAll(buildActiveDailyNoticeCards(userRecord, weekStart, effectiveDate, today));
        // 晚间主动提醒（20:00后显示当天提醒）
        if (now.isAfter(LocalTime.of(20, 0)) || now.equals(LocalTime.of(20, 0))) {
            Map<String, Object> eveningCard = buildEveningReminderCard(userRecord, today);
            if (eveningCard != null) {
                cards.add(eveningCard);
            }
        }
        // 早间天气+训练计划提醒（当天有效）
        Map<String, Object> morningCard = buildMorningReminderCard(userRecord, today);
        if (morningCard != null) {
            cards.add(morningCard);
        }
        cards.sort(Comparator.comparing((Map<String, Object> card) -> String.valueOf(card.get("date"))).reversed());
        return cards;
    }

    private List<Map<String, Object>> buildActiveDailyNoticeCards(UserRecord userRecord,
                                                                  LocalDate weekStart,
                                                                  LocalDate effectiveDate,
                                                                  LocalDate today) {
        List<Map<String, Object>> cards = parseStringArray(userRecord == null ? null : userRecord.getWeeklyReviews()).stream()
                .map(review -> parseDailyReviewCard(review, today))
                .filter(card -> isCardInRange(card, weekStart, effectiveDate))
                .sorted(Comparator.comparing((Map<String, Object> card) -> String.valueOf(card.get("date"))).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        if (cards.isEmpty() && userRecord != null && StringUtils.isNotBlank(userRecord.getYesterdaySummary())) {
            LocalDate fallbackDate = today.minusDays(1);
            Map<String, Object> card = buildSummaryCard(
                    "daily-" + fallbackDate.format(DATE_FMT),
                    "daily",
                    formatDateDisplay(fallbackDate),
                    "Daily",
                    buildPreview(userRecord.getYesterdaySummary()),
                    userRecord.getYesterdaySummary()
            );
            card.put("date", fallbackDate.format(DATE_FMT));
            cards.add(card);
        }
        return cards;
    }

    private Map<String, Object> buildActiveWeeklyNoticeCard(UserRecord userRecord, LocalDate weekStart) {
        if (userRecord == null || StringUtils.isBlank(userRecord.getWeeklySummary())) {
            return null;
        }

        LocalDate summaryWeekStart = weekStart.minusWeeks(1);
        LocalDate summaryWeekEnd = summaryWeekStart.plusDays(6);
        Map<String, Object> card = buildSummaryCard(
                "weekly-" + summaryWeekStart.format(DATE_FMT),
                "weekly",
                DISPLAY_DATE_FMT.format(summaryWeekStart) + " - " + DISPLAY_DATE_FMT.format(summaryWeekEnd),
                "Weekly",
                buildPreview(userRecord.getWeeklySummary()),
                userRecord.getWeeklySummary()
        );
        card.put("date", summaryWeekStart.format(DATE_FMT));
        return card;
    }

    private boolean isCardInRange(Map<String, Object> card, LocalDate start, LocalDate end) {
        if (card == null) {
            return false;
        }
        String dateText = String.valueOf(card.get("date"));
        if (StringUtils.isBlank(dateText)) {
            return false;
        }
        LocalDate cardDate = LocalDate.parse(dateText, DATE_FMT);
        return !cardDate.isBefore(start) && !cardDate.isAfter(end);
    }

    private List<Map<String, Object>> buildNoticeCards(User user, LocalDate today) {
        UserRecord userRecord = getUserRecord(user.getId());
        List<Map<String, Object>> cards = new ArrayList<>();
        cards.addAll(buildDailyNoticeCards(userRecord, today));
        cards.addAll(buildWeeklyNoticeCards(userRecord, today));
        cards.sort(Comparator.comparing((Map<String, Object> card) -> String.valueOf(card.get("date"))).reversed());
        return cards;
    }

    private List<Map<String, Object>> buildDailyNoticeCards(UserRecord userRecord, LocalDate today) {
        List<Map<String, Object>> cards = parseStringArray(userRecord == null ? null : userRecord.getWeeklyReviews()).stream()
                .map(review -> parseDailyReviewCard(review, today))
                .filter(card -> card != null && isCurrentWeekCard(card, today))
                .sorted(Comparator.comparing((Map<String, Object> card) -> String.valueOf(card.get("date"))).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        if (cards.isEmpty() && userRecord != null && StringUtils.isNotBlank(userRecord.getYesterdaySummary())) {
            LocalDate date = today.minusDays(1);
            Map<String, Object> card = buildSummaryCard(
                    "daily-" + date.format(DATE_FMT),
                    "daily",
                    formatDateDisplay(date),
                    "日总结",
                    buildPreview(userRecord.getYesterdaySummary()),
                    userRecord.getYesterdaySummary()
            );
            card.put("date", date.format(DATE_FMT));
            cards.add(card);
        }
        return cards;
    }

    private List<Map<String, Object>> buildWeeklyNoticeCards(UserRecord userRecord, LocalDate today) {
        if (userRecord == null || StringUtils.isBlank(userRecord.getWeeklySummary())) {
            return Collections.emptyList();
        }

        LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastSunday = today.minusWeeks(1).with(DayOfWeek.SUNDAY);
        Map<String, Object> card = buildSummaryCard(
                "weekly-" + lastMonday.format(DATE_FMT) + "_" + lastSunday.format(DATE_FMT),
                "weekly",
                DISPLAY_DATE_FMT.format(lastMonday) + " - " + DISPLAY_DATE_FMT.format(lastSunday),
                "Weekly",
                buildPreview(userRecord.getWeeklySummary()),
                userRecord.getWeeklySummary()
        );
        card.put("date", lastSunday.format(DATE_FMT));
        return List.of(card);
    }

    private UserRecord getUserRecord(Long userId) {
        return userRecordService.getByUserId(userId);
    }

    private Map<String, Object> parseDailyReviewCard(String review, LocalDate today) {
        if (StringUtils.isBlank(review)) {
            return null;
        }

        String trimmed = review.trim();
        String title = "本周记录";
        String content = trimmed;

        if (trimmed.startsWith("【")) {
            int end = trimmed.indexOf("】");
            if (end > 1) {
                title = trimmed.substring(1, end);
                content = trimmed.substring(end + 1).trim();
            }
        }

        if (StringUtils.isBlank(content)) {
            content = "暂无记录";
        }

        LocalDate cardDate = inferDateFromTitle(title, today);
        String dateKey = cardDate != null ? cardDate.format(DATE_FMT) : today.format(DATE_FMT);
        Map<String, Object> card = buildSummaryCard(
                "daily-" + dateKey,
                "daily",
                title,
                "日总结",
                buildPreview(content),
                content
        );
        card.put("date", dateKey);
        return card;
    }

    private Map<String, Object> buildSummaryCard(String id,
                                                 String type,
                                                 String title,
                                                 String subtitle,
                                                 String preview,
                                                 String content) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", id);
        card.put("type", type);
        card.put("title", title);
        card.put("subtitle", subtitle);
        card.put("preview", preview);
        card.put("content", content);
        return card;
    }

    private Map<String, Object> buildEveningReminderCard(UserRecord userRecord, LocalDate today) {
        if (userRecord == null || StringUtils.isBlank(userRecord.getEveningReminder())) {
            return null;
        }
        Map<String, Object> card = buildSummaryCard(
                "evening-" + today.format(DATE_FMT),
                "evening",
                DISPLAY_DATE_FMT.format(today) + " 晚间提醒",
                "Evening",
                buildPreview(userRecord.getEveningReminder()),
                userRecord.getEveningReminder()
        );
        card.put("date", today.format(DATE_FMT));
        return card;
    }

    private Map<String, Object> buildMorningReminderCard(UserRecord userRecord, LocalDate today) {
        if (userRecord == null || StringUtils.isBlank(userRecord.getMorningReminder())) {
            return null;
        }
        Map<String, Object> card = buildSummaryCard(
                "morning-" + today.format(DATE_FMT),
                "morning",
                DISPLAY_DATE_FMT.format(today) + " 早间提醒",
                "Morning",
                buildPreview(userRecord.getMorningReminder()),
                userRecord.getMorningReminder()
        );
        card.put("date", today.format(DATE_FMT));
        return card;
    }

    private String buildNoticeId(String type, List<Map<String, Object>> cards) {
        if (cards.isEmpty()) {
            return type + "-empty";
        }
        return type + "-" + cards.get(0).get("id") + "-" + cards.get(cards.size() - 1).get("id");
    }

    private String resolveNoticeType(List<Map<String, Object>> cards) {
        boolean hasDaily = cards.stream().anyMatch(card -> "daily".equals(card.get("type")));
        boolean hasWeekly = cards.stream().anyMatch(card -> "weekly".equals(card.get("type")));
        boolean hasEvening = cards.stream().anyMatch(card -> "evening".equals(card.get("type")));
        if (hasDaily && hasWeekly && hasEvening) {
            return "mixed-evening";
        }
        if (hasDaily && hasWeekly) {
            return "mixed";
        }
        if (hasEvening && hasDaily) {
            return "daily-evening";
        }
        if (hasEvening && hasWeekly) {
            return "weekly-evening";
        }
        if (hasEvening) {
            return "evening";
        }
        return hasWeekly ? "weekly" : "daily";
    }

    private String resolveNoticeTitle(String type) {
        if ("mixed-evening".equals(type)) {
            return "总结、日报与晚间提醒";
        }
        if ("daily-evening".equals(type)) {
            return "日报与晚间提醒";
        }
        if ("weekly-evening".equals(type)) {
            return "周总结与晚间提醒";
        }
        if ("evening".equals(type)) {
            return "晚间提醒";
        }
        if ("mixed".equals(type)) {
            return "上周总结与本周日报";
        }
        if ("weekly".equals(type)) {
            return "上周总结";
        }
        return "本周日报";
    }

    private LocalDate inferDateFromTitle(String title, LocalDate today) {
        Matcher matcher = DISPLAY_DATE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return null;
        }

        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        LocalDate candidate = LocalDate.of(today.getYear(), month, day);
        if (candidate.isAfter(today.plusDays(1))) {
            candidate = candidate.minusYears(1);
        }
        return candidate;
    }

    private boolean isCurrentWeekCard(Map<String, Object> card, LocalDate today) {
        String dateText = String.valueOf(card.get("date"));
        if (StringUtils.isBlank(dateText)) {
            return false;
        }
        LocalDate cardDate = LocalDate.parse(dateText, DATE_FMT);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        return !cardDate.isBefore(weekStart) && !cardDate.isAfter(weekEnd);
    }

    private String formatDateDisplay(LocalDate date) {
        return DISPLAY_DATE_FMT.format(date) + " " + WEEKDAYS[date.getDayOfWeek().getValue()];
    }

    private String buildPreview(String content) {
        String normalized = content.replace("\r", "").replace("\n", " ").trim();
        if (normalized.length() <= CARD_PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, CARD_PREVIEW_LIMIT) + "...";
    }

    private Map<String, Map<String, Boolean>> parseNotificationStates(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Map<String, Boolean>>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private Map<String, Map<String, Boolean>> pruneNotificationStates(Map<String, Map<String, Boolean>> states,
                                                                      List<Map<String, Object>> cards) {
        if (states.isEmpty()) {
            return states;
        }
        Set<String> activeKeys = cards.stream()
                .map(card -> String.valueOf(card.get("id")))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        states.entrySet().removeIf(entry -> !activeKeys.contains(entry.getKey()));
        return states;
    }

    private boolean getNotificationFlag(Map<String, Map<String, Boolean>> states,
                                        String noticeId,
                                        String field) {
        Map<String, Boolean> state = states.get(noticeId);
        return state != null && Boolean.TRUE.equals(state.get(field));
    }

    private void updateNotificationState(Long userId, String noticeId, Boolean read, Boolean cleared) {
        User dbUser = userService.getById(userId);
        if (dbUser == null) {
            throw new BusincessException(NULL_ERROR, "用户不存在");
        }

        Map<String, Map<String, Boolean>> states = parseNotificationStates(dbUser.getNotificationStates());
        Map<String, Boolean> state = states.computeIfAbsent(noticeId, key -> new HashMap<>());
        if (read != null) {
            state.put("read", read);
        }
        if (cleared != null) {
            state.put("cleared", cleared);
        }
        persistNotificationStates(userId, states);
    }

    private void persistNotificationStates(Long userId, Map<String, Map<String, Boolean>> states) {
        try {
            User updateUser = new User();
            updateUser.setId(userId);
            updateUser.setNotificationStates(OBJECT_MAPPER.writeValueAsString(states));
            userService.updateById(updateUser);
        } catch (JsonProcessingException e) {
            throw new BusincessException(SYSTEM_ERROR, "通知状态保存失败");
        }
    }

    private Map<String, Boolean> parseReadState(String json) {
        if (StringUtils.isBlank(json)) {
            return new HashMap<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Boolean>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private String readNoticeState(HttpServletRequest request) {
        Object state = request.getSession().getAttribute(SUMMARY_NOTICE_READ_STATE);
        return state == null ? null : String.valueOf(state);
    }

    private void saveReadState(HttpServletRequest request, List<String> noticeIds) {
        Map<String, Boolean> readState = parseReadState(readNoticeState(request));
        for (String noticeId : noticeIds) {
            if (StringUtils.isNotBlank(noticeId)) {
                readState.put(noticeId, true);
            }
        }

        try {
            request.getSession().setAttribute(SUMMARY_NOTICE_READ_STATE, OBJECT_MAPPER.writeValueAsString(readState));
        } catch (JsonProcessingException e) {
            throw new BusincessException(SYSTEM_ERROR, "通知已读状态保存失败");
        }
    }

    private List<Long> parseFavorites(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseStringArray(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            String fixed = json.replace("\n", "\\n").replace("\r", "");
            return OBJECT_MAPPER.readValue(fixed, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
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
}
