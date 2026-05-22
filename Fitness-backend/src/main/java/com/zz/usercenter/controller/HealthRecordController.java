package com.zz.usercenter.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.model.domain.request.AddDietRecordRequest;
import com.zz.usercenter.model.domain.request.AddExerciseRecordRequest;
import com.zz.usercenter.model.domain.request.DietFoodItemRequest;
import com.zz.usercenter.service.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.zz.usercenter.common.StateCode.NOT_LOGIN;
import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@RestController
@RequestMapping("/record")
public class HealthRecordController {

    @Resource
    private UserService userService;

    @Resource
    private UserRecordService userRecordService;

    @Resource
    private UserDailyMetricService userDailyMetricService;

    @Resource
    private FoodItemService foodItemService;

    @Resource
    private DietRecordService dietRecordService;

    @Resource
    private ExerciseRecordService exerciseRecordService;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final BigDecimal KJ_TO_KCAL_DIVISOR = new BigDecimal("4.184");

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录后继续操作");
        }
        return (User) obj;
    }

    @PostMapping("/add")
    public BaseResponse<Boolean> addRecord(@RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        throw new BusincessException(PARAMS_ERROR, "通用记录入口已废弃，请使用训练或饮食记录接口");
    }

    @PostMapping("/exercise")
    public BaseResponse<Boolean> addExerciseRecord(@RequestBody AddExerciseRecordRequest body,
                                                   HttpServletRequest request) {
        User user = getLoginUser(request);
        if (body == null || StringUtils.isBlank(body.getExerciseName())) {
            throw new BusincessException(PARAMS_ERROR, "动作名称不能为空");
        }

        String recordTime = StringUtils.defaultIfBlank(body.getTime(), LocalTime.now().format(TIME_FMT));
        body.setTime(recordTime);
        exerciseRecordService.saveRecord(user.getId(), LocalDate.now(), body, "manual");
        return ResultUtils.success(true);
    }

    @PostMapping("/exercise/structured")
    public BaseResponse<Boolean> addStructuredExerciseRecord(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        User user = getLoginUser(request);
        Map<String, Object> record = body == null ? null : objectMapper.convertValue(body.get("record"), new TypeReference<Map<String, Object>>() {});
        if (record == null) {
            throw new BusincessException(PARAMS_ERROR, "训练记录不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = record.get("items") instanceof List<?> list
                ? list.stream().map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {})).toList()
                : new ArrayList<>();
        if (items.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "请至少保留一个训练动作");
        }

        String recordTime = StringUtils.defaultIfBlank((String) record.get("time"), LocalTime.now().format(TIME_FMT));
        String name = StringUtils.trimToEmpty((String) record.get("name"));
        Integer durationSeconds = parseInteger(record.get("durationSeconds"));
        Integer caloriesBurned = parseInteger(record.get("caloriesBurned"));
        String note = StringUtils.trimToNull((String) record.get("note"));
        String source = StringUtils.defaultIfBlank((String) record.get("source"), "manual");

        exerciseRecordService.saveStructuredRecord(
                user.getId(),
                LocalDate.now(),
                recordTime,
                name,
                durationSeconds,
                caloriesBurned,
                note,
                source,
                items
        );
        return ResultUtils.success(true);
    }

    @PostMapping("/diet")
    public BaseResponse<Boolean> addDietRecord(@RequestBody AddDietRecordRequest body,
                                               HttpServletRequest request) {
        User user = getLoginUser(request);
        if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "请选择食物");
        }

        String recordTime = StringUtils.defaultIfBlank(body.getTime(), LocalTime.now().format(TIME_FMT));
        MealNutrition nutrition = resolveMealNutrition(body.getItems(), user.getId());
        dietRecordService.saveStructuredRecord(
                user.getId(),
                LocalDate.now(),
                recordTime,
                body.getMealType(),
                nutrition.summaryName,
                nutrition.calories.intValue(),
                nutrition.protein,
                nutrition.carbs,
                nutrition.fat,
                nutrition.fiber,
                body.getNote(),
                StringUtils.defaultIfBlank(body.getSource(), "manual"),
                nutrition.items
        );
        body.setName(nutrition.summaryName);
        body.setCalories(nutrition.calories.intValue());
        userDailyMetricService.syncDailyCalories(
                user.getId(),
                LocalDate.now(),
                dietRecordService.listLegacyRecords(user.getId(), LocalDate.now()),
                resolveTargetCalories(user)
        );
        return ResultUtils.success(true);
    }

    @PostMapping("/exercise/delete")
    public BaseResponse<Boolean> deleteExerciseRecord(@RequestBody Map<String, Integer> body,
                                                      HttpServletRequest request) {
        User user = getLoginUser(request);
        int index = resolveRecordIndex(body);
        String today = LocalDate.now().format(DATE_FMT);
        LocalDate recordDate = LocalDate.parse(today, DATE_FMT);
        List<Map<String, Object>> exerciseRecords = exerciseRecordService.listLegacyRecords(user.getId(), recordDate);
        if (index < 0 || index >= exerciseRecords.size()) {
            throw new BusincessException(PARAMS_ERROR, "训练记录不存在");
        }
        exerciseRecordService.deleteTodayRecord(user.getId(), recordDate, index);
        return ResultUtils.success(true);
    }

    @PostMapping("/diet/delete")
    public BaseResponse<Boolean> deleteDietRecord(@RequestBody Map<String, Integer> body,
                                                  HttpServletRequest request) {
        User user = getLoginUser(request);
        int index = resolveRecordIndex(body);
        String today = LocalDate.now().format(DATE_FMT);
        LocalDate recordDate = LocalDate.parse(today, DATE_FMT);
        List<Map<String, Object>> dietRecords = dietRecordService.listLegacyRecords(user.getId(), recordDate);
        if (index < 0 || index >= dietRecords.size()) {
            throw new BusincessException(PARAMS_ERROR, "饮食记录不存在");
        }
        dietRecordService.deleteTodayRecord(user.getId(), recordDate, index);
        userDailyMetricService.syncDailyCalories(
                user.getId(),
                recordDate,
                dietRecordService.listLegacyRecords(user.getId(), recordDate),
                resolveTargetCalories(user)
        );
        return ResultUtils.success(true);
    }

    @PostMapping("/diet/update")
    public BaseResponse<Boolean> updateDietRecord(@RequestBody Map<String, Object> body,
                                                  HttpServletRequest request) {
        User user = getLoginUser(request);
        Integer indexValue = body == null ? null : parseInteger(body.get("index"));
        if (indexValue == null) {
            throw new BusincessException(PARAMS_ERROR, "记录索引不能为空");
        }
        AddDietRecordRequest record = objectMapper.convertValue(body.get("record"), AddDietRecordRequest.class);
        if (record == null || record.getItems() == null || record.getItems().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "请选择食物");
        }

        String today = LocalDate.now().format(DATE_FMT);
        LocalDate recordDate = LocalDate.parse(today, DATE_FMT);
        List<Map<String, Object>> dietRecords = dietRecordService.listLegacyRecords(user.getId(), recordDate);
        if (indexValue < 0 || indexValue >= dietRecords.size()) {
            throw new BusincessException(PARAMS_ERROR, "饮食记录不存在");
        }

        String recordTime = StringUtils.defaultIfBlank(record.getTime(), LocalTime.now().format(TIME_FMT));
        MealNutrition nutrition = resolveMealNutrition(record.getItems(), user.getId());
        dietRecordService.updateStructuredRecord(
                user.getId(),
                recordDate,
                indexValue,
                recordTime,
                record.getMealType(),
                nutrition.summaryName,
                nutrition.calories.intValue(),
                nutrition.protein,
                nutrition.carbs,
                nutrition.fat,
                nutrition.fiber,
                record.getNote(),
                StringUtils.defaultIfBlank(record.getSource(), "manual"),
                nutrition.items
        );
        userDailyMetricService.syncDailyCalories(
                user.getId(),
                recordDate,
                dietRecordService.listLegacyRecords(user.getId(), recordDate),
                resolveTargetCalories(user)
        );
        return ResultUtils.success(true);
    }

    @PostMapping("/exercise/update")
    public BaseResponse<Boolean> updateExerciseRecord(@RequestBody Map<String, Object> body,
                                                      HttpServletRequest request) {
        User user = getLoginUser(request);
        Integer indexValue = body == null ? null : parseInteger(body.get("index"));
        if (indexValue == null) {
            throw new BusincessException(PARAMS_ERROR, "记录索引不能为空");
        }
        Map<String, Object> record = body == null ? null : objectMapper.convertValue(body.get("record"), new TypeReference<Map<String, Object>>() {});
        if (record == null) {
            throw new BusincessException(PARAMS_ERROR, "训练记录不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = record.get("items") instanceof List<?> list
                ? list.stream().map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {})).toList()
                : new ArrayList<>();
        if (items.isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "请至少保留一个训练动作");
        }

        String today = LocalDate.now().format(DATE_FMT);
        LocalDate recordDate = LocalDate.parse(today, DATE_FMT);
        List<Map<String, Object>> exerciseRecords = exerciseRecordService.listLegacyRecords(user.getId(), recordDate);
        if (indexValue < 0 || indexValue >= exerciseRecords.size()) {
            throw new BusincessException(PARAMS_ERROR, "训练记录不存在");
        }

        String recordTime = StringUtils.defaultIfBlank((String) record.get("time"), LocalTime.now().format(TIME_FMT));
        String name = StringUtils.trimToEmpty((String) record.get("name"));
        Integer durationSeconds = parseInteger(record.get("durationSeconds"));
        Integer caloriesBurned = parseInteger(record.get("caloriesBurned"));
        String note = StringUtils.trimToNull((String) record.get("note"));
        String source = StringUtils.defaultIfBlank((String) record.get("source"), "manual");

        exerciseRecordService.updateStructuredRecord(
                user.getId(),
                recordDate,
                indexValue,
                recordTime,
                name,
                durationSeconds,
                caloriesBurned,
                note,
                source,
                items
        );
        return ResultUtils.success(true);
    }

    @GetMapping("/foods")
    public BaseResponse<List<FoodItem>> searchFoods(@RequestParam(value = "keyword", required = false) String keyword,
                                                    HttpServletRequest request) {
        User user = getLoginUser(request);
        return ResultUtils.success(foodItemService.searchVisibleFoods(user.getId(), keyword));
    }

    /**
     * 获取当日记录
     */
    @GetMapping("/today")
    public BaseResponse<UserRecord> getTodayRecord(HttpServletRequest request) {
        User user = getLoginUser(request);
        UserRecord dbUserRecord = userRecordService.getByUserId(user.getId());

        UserRecord result = new UserRecord();
        result.setUserId(user.getId());
        if (dbUserRecord != null) {
            result.setYesterdaySummary(dbUserRecord.getYesterdaySummary());
            result.setWeeklyReviews(dbUserRecord.getWeeklyReviews());
            result.setWeeklySummary(dbUserRecord.getWeeklySummary());
        }

        return ResultUtils.success(result);
    }

    /**
     * 获取当日训练记录列表
     */
    @GetMapping("/exercise/today")
    public BaseResponse<List<Map<String, Object>>> getTodayExerciseRecords(HttpServletRequest request) {
        User user = getLoginUser(request);
        List<Map<String, Object>> records = exerciseRecordService.listLegacyRecords(user.getId(), LocalDate.now());
        return ResultUtils.success(records);
    }

    /**
     * 获取当日饮食记录列表
     */
    @GetMapping("/diet/today")
    public BaseResponse<List<Map<String, Object>>> getTodayDietRecords(HttpServletRequest request) {
        User user = getLoginUser(request);
        List<Map<String, Object>> records = dietRecordService.listLegacyRecords(user.getId(), LocalDate.now());
        return ResultUtils.success(records);
    }

    private Double resolveTargetCalories(User user) {
        if (user == null) {
            return null;
        }
        return user.getCustomDailyCalories() != null
                ? user.getCustomDailyCalories()
                : user.getDailyCalorieBurn();
    }

    private int resolveRecordIndex(Map<String, Integer> body) {
        if (body == null || body.get("index") == null) {
            throw new BusincessException(PARAMS_ERROR, "记录索引不能为空");
        }
        return body.get("index");
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private record MealNutrition(
            String summaryName,
            BigDecimal calories,
            BigDecimal protein,
            BigDecimal carbs,
            BigDecimal fat,
            BigDecimal fiber,
            List<Map<String, Object>> items
    ) {}

}
