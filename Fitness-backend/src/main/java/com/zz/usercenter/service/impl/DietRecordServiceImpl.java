package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.mapper.DietRecordItemMapper;
import com.zz.usercenter.mapper.DietRecordMapper;
import com.zz.usercenter.model.domain.DietRecord;
import com.zz.usercenter.model.domain.DietRecordItem;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.service.DietRecordService;
import com.zz.usercenter.service.FoodItemService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DietRecordServiceImpl implements DietRecordService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private DietRecordMapper dietRecordMapper;

    @Resource
    private DietRecordItemMapper dietRecordItemMapper;

    @Resource
    private FoodItemService foodItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStructuredRecord(Long userId,
                                     LocalDate recordDate,
                                     String recordTime,
                                     String mealType,
                                     String name,
                                     Integer calories,
                                     BigDecimal protein,
                                     BigDecimal carbs,
                                     BigDecimal fat,
                                     BigDecimal fiber,
                                     String note,
                                     String source,
                                     List<Map<String, Object>> items) {
        DietRecord record = buildRecord(userId, recordDate, recordTime, mealType, name, calories, protein, carbs, fat, fiber, note, source);
        dietRecordMapper.insert(record);
        replaceItems(record.getId(), items);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStructuredRecord(Long userId,
                                          LocalDate recordDate,
                                          int index,
                                          String recordTime,
                                          String mealType,
                                          String name,
                                          Integer calories,
                                          BigDecimal protein,
                                          BigDecimal carbs,
                                          BigDecimal fat,
                                          BigDecimal fiber,
                                          String note,
                                          String source,
                                          List<Map<String, Object>> items) {
        DietRecord existing = getTodayRecordByIndex(userId, recordDate, index);
        if (existing == null) {
            return false;
        }
        existing.setRecordTime(recordTime);
        existing.setMealType(mealType);
        existing.setName(name);
        existing.setCalories(calories);
        existing.setProtein(protein);
        existing.setCarbs(carbs);
        existing.setFat(fat);
        existing.setFiber(fiber);
        existing.setNote(note);
        existing.setSource(source);
        dietRecordMapper.updateById(existing);
        replaceItems(existing.getId(), items);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTodayRecord(Long userId, LocalDate recordDate, int index) {
        DietRecord existing = getTodayRecordByIndex(userId, recordDate, index);
        if (existing == null) {
            return false;
        }
        dietRecordItemMapper.delete(new QueryWrapper<DietRecordItem>().eq("dietRecordId", existing.getId()));
        return dietRecordMapper.deleteById(existing.getId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSimpleRecord(Long userId,
                                 LocalDate recordDate,
                                 String recordTime,
                                 String mealType,
                                 String name,
                                 Integer calories,
                                 String note,
                                 String source) {
        DietRecord record = buildRecord(userId, recordDate, recordTime, mealType, name, calories, null, null, null, null, note, source);
        dietRecordMapper.insert(record);
        return record.getId();
    }

    @Override
    public List<DietRecord> listByUserAndDate(Long userId, LocalDate recordDate) {
        if (userId == null || recordDate == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DietRecord::getUserId, userId)
                .eq(DietRecord::getRecordDate, recordDate)
                .orderByAsc(DietRecord::getRecordTime)
                .orderByAsc(DietRecord::getId);
        return dietRecordMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> listLegacyRecords(Long userId, LocalDate recordDate) {
        List<DietRecord> records = listByUserAndDate(userId, recordDate);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = records.stream().map(DietRecord::getId).toList();
        LambdaQueryWrapper<DietRecordItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(DietRecordItem::getDietRecordId, ids)
                .orderByAsc(DietRecordItem::getDietRecordId)
                .orderByAsc(DietRecordItem::getSortOrder)
                .orderByAsc(DietRecordItem::getId);
        Map<Long, List<DietRecordItem>> itemMap = dietRecordItemMapper.selectList(itemWrapper)
                .stream()
                .collect(Collectors.groupingBy(DietRecordItem::getDietRecordId));
        Map<Long, String> foodImageMap = buildFoodImageMap(itemMap);

        List<Map<String, Object>> result = new ArrayList<>();
        for (DietRecord record : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("time", record.getRecordTime());
            item.put("name", record.getName());
            item.put("mealType", record.getMealType());
            item.put("calories", record.getCalories());
            item.put("protein", toDouble(record.getProtein()));
            item.put("carbs", toDouble(record.getCarbs()));
            item.put("fat", toDouble(record.getFat()));
            item.put("fiber", toDouble(record.getFiber()));
            item.put("note", record.getNote());
            item.put("source", record.getSource());
            item.put("items", buildLegacyItems(itemMap.get(record.getId()), foodImageMap));
            result.add(item);
        }
        return result;
    }

    @Override
    public String getLegacyRecordJson(Long userId, LocalDate recordDate) {
        try {
            return OBJECT_MAPPER.writeValueAsString(listLegacyRecords(userId, recordDate));
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public boolean hasRecord(Long userId, LocalDate recordDate) {
        return !listByUserAndDate(userId, recordDate).isEmpty();
    }

    @Override
    public Map<String, Object> getDayMacroSummary(Long userId, LocalDate recordDate) {
        Map<String, Object> summary = new LinkedHashMap<>();
        double totalCalories = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0, totalFiber = 0;
        for (DietRecord record : listByUserAndDate(userId, recordDate)) {
            if (record.getCalories() != null) totalCalories += record.getCalories();
            if (record.getProtein() != null) totalProtein += record.getProtein().doubleValue();
            if (record.getCarbs() != null) totalCarbs += record.getCarbs().doubleValue();
            if (record.getFat() != null) totalFat += record.getFat().doubleValue();
            if (record.getFiber() != null) totalFiber += record.getFiber().doubleValue();
        }
        summary.put("calories", (int) Math.round(totalCalories));
        summary.put("protein", Math.round(totalProtein * 10) / 10.0);
        summary.put("carbs", Math.round(totalCarbs * 10) / 10.0);
        summary.put("fat", Math.round(totalFat * 10) / 10.0);
        summary.put("fiber", Math.round(totalFiber * 10) / 10.0);
        summary.put("hasRecord", totalCalories > 0 || totalProtein > 0);
        return summary;
    }

    private DietRecord getTodayRecordByIndex(Long userId, LocalDate recordDate, int index) {
        List<DietRecord> records = listByUserAndDate(userId, recordDate);
        if (index < 0 || index >= records.size()) {
            return null;
        }
        return records.get(index);
    }

    private DietRecord buildRecord(Long userId,
                                   LocalDate recordDate,
                                   String recordTime,
                                   String mealType,
                                   String name,
                                   Integer calories,
                                   BigDecimal protein,
                                   BigDecimal carbs,
                                   BigDecimal fat,
                                   BigDecimal fiber,
                                   String note,
                                   String source) {
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setRecordDate(recordDate);
        record.setRecordTime(recordTime);
        record.setMealType(mealType);
        record.setName(name);
        record.setCalories(calories);
        record.setProtein(protein);
        record.setCarbs(carbs);
        record.setFat(fat);
        record.setFiber(fiber);
        record.setNote(note);
        record.setSource(source);
        return record;
    }

    private void replaceItems(Long dietRecordId, List<Map<String, Object>> items) {
        dietRecordItemMapper.delete(new QueryWrapper<DietRecordItem>().eq("dietRecordId", dietRecordId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int sortOrder = 0;
        for (Map<String, Object> item : items) {
            DietRecordItem entity = new DietRecordItem();
            entity.setDietRecordId(dietRecordId);
            entity.setFoodItemId(toLong(item.get("foodItemId")));
            entity.setName(toStringValue(item.get("name")));
            entity.setUnit(toStringValue(item.get("unit")));
            entity.setAmount(toBigDecimal(item.get("amount")));
            entity.setCalories(toBigDecimal(item.get("calories")));
            entity.setProtein(toBigDecimal(item.get("protein")));
            entity.setCarbs(toBigDecimal(item.get("carbs")));
            entity.setFat(toBigDecimal(item.get("fat")));
            entity.setFiber(toBigDecimal(item.get("fiber")));
            entity.setSortOrder(sortOrder++);
            dietRecordItemMapper.insert(entity);
        }
    }

    private List<Map<String, Object>> buildLegacyItems(List<DietRecordItem> items, Map<Long, String> foodImageMap) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (DietRecordItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("foodItemId", item.getFoodItemId());
            map.put("name", item.getName());
            map.put("imageUrl", item.getFoodItemId() == null ? null : foodImageMap.get(item.getFoodItemId()));
            map.put("unit", item.getUnit());
            map.put("amount", item.getAmount());
            map.put("calories", item.getCalories());
            map.put("protein", item.getProtein());
            map.put("carbs", item.getCarbs());
            map.put("fat", item.getFat());
            map.put("fiber", item.getFiber());
            result.add(map);
        }
        return result;
    }

    private Map<Long, String> buildFoodImageMap(Map<Long, List<DietRecordItem>> itemMap) {
        if (itemMap == null || itemMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> foodIds = itemMap.values().stream()
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .map(DietRecordItem::getFoodItemId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (foodIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> imageMap = new LinkedHashMap<>();
        for (FoodItem foodItem : foodItemService.listByIds(foodIds)) {
            if (foodItem == null || foodItem.getId() == null) {
                continue;
            }
            String imageUrl = foodItem.getImageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            imageMap.putIfAbsent(foodItem.getId(), imageUrl);
        }
        return imageMap;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            return new BigDecimal(text.trim());
        }
        return null;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
