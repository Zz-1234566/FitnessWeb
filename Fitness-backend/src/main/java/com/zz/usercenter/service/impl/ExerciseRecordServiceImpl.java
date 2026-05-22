package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.mapper.ExerciseSessionItemMapper;
import com.zz.usercenter.mapper.ExerciseSessionMapper;
import com.zz.usercenter.model.domain.ExerciseSession;
import com.zz.usercenter.model.domain.ExerciseSessionItem;
import com.zz.usercenter.model.domain.request.AddExerciseRecordRequest;
import com.zz.usercenter.service.ExerciseRecordService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExerciseRecordServiceImpl implements ExerciseRecordService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Resource
    private ExerciseSessionMapper exerciseSessionMapper;

    @Resource
    private ExerciseSessionItemMapper exerciseSessionItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRecord(Long userId, LocalDate recordDate, AddExerciseRecordRequest body, String defaultSource) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("exerciseId", body.getExerciseId());
        item.put("name", StringUtils.trimToEmpty(body.getExerciseName()));
        item.put("muscleGroup", body.getMuscleGroup());
        item.put("completedSets", body.getCompletedSets());
        item.put("durationSeconds", body.getDurationSeconds());
        item.put("note", body.getNote());
        return saveStructuredRecord(
                userId,
                recordDate,
                StringUtils.defaultIfBlank(body.getTime(), LocalTime.now().format(TIME_FMT)),
                StringUtils.trimToEmpty(body.getExerciseName()),
                body.getDurationSeconds(),
                body.getCaloriesBurned(),
                body.getNote(),
                StringUtils.defaultIfBlank(body.getSource(), defaultSource),
                List.of(item)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveStructuredRecord(Long userId,
                                     LocalDate recordDate,
                                     String recordTime,
                                     String name,
                                     Integer durationSeconds,
                                     Integer caloriesBurned,
                                     String note,
                                     String source,
                                     List<Map<String, Object>> items) {
        ExerciseSession session = new ExerciseSession();
        session.setUserId(userId);
        session.setRecordDate(recordDate);
        session.setDurationSeconds(durationSeconds);
        session.setCaloriesBurned(caloriesBurned);
        session.setNote(note);
        exerciseSessionMapper.insert(session);
        replaceItems(session.getId(), items);
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStructuredRecord(Long userId,
                                          LocalDate recordDate,
                                          int index,
                                          String recordTime,
                                          String name,
                                          Integer durationSeconds,
                                          Integer caloriesBurned,
                                          String note,
                                          String source,
                                          List<Map<String, Object>> items) {
        ExerciseSession existing = getTodayRecordByIndex(userId, recordDate, index);
        if (existing == null) {
            return false;
        }
        existing.setDurationSeconds(durationSeconds);
        existing.setCaloriesBurned(caloriesBurned);
        existing.setNote(note);
        exerciseSessionMapper.updateById(existing);
        replaceItems(existing.getId(), items);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTodayRecord(Long userId, LocalDate recordDate, int index) {
        ExerciseSession existing = getTodayRecordByIndex(userId, recordDate, index);
        if (existing == null) {
            return false;
        }
        exerciseSessionItemMapper.delete(new QueryWrapper<ExerciseSessionItem>().eq("sessionId", existing.getId()));
        return exerciseSessionMapper.deleteById(existing.getId()) > 0;
    }

    @Override
    public List<ExerciseSession> listByUserAndDate(Long userId, LocalDate recordDate) {
        if (userId == null || recordDate == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ExerciseSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExerciseSession::getUserId, userId)
                .eq(ExerciseSession::getRecordDate, recordDate)
                .orderByAsc(ExerciseSession::getCreateTime)
                .orderByAsc(ExerciseSession::getId);
        return exerciseSessionMapper.selectList(wrapper);
    }

    @Override
    public List<ExerciseSession> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ExerciseSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExerciseSession::getUserId, userId)
                .ge(ExerciseSession::getRecordDate, startDate)
                .le(ExerciseSession::getRecordDate, endDate);
        return exerciseSessionMapper.selectList(wrapper);
    }

    @Override
    public Map<LocalDate, Integer> sumCaloriesByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null) {
            return Collections.emptyMap();
        }
        QueryWrapper<ExerciseSession> wrapper = new QueryWrapper<>();
        wrapper.select("recordDate", "caloriesBurned")
                .eq("userId", userId)
                .eq("isDelete", 0)
                .ge("recordDate", startDate)
                .le("recordDate", endDate)
                .isNotNull("caloriesBurned");

        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : exerciseSessionMapper.selectMaps(wrapper)) {
            Object dateValue = row.get("recordDate");
            Object caloriesValue = row.get("caloriesBurned");
            if (dateValue == null || caloriesValue == null) {
                continue;
            }
            LocalDate date = dateValue instanceof LocalDate
                    ? (LocalDate) dateValue
                    : LocalDate.parse(String.valueOf(dateValue));
            Integer calories = toInteger(caloriesValue);
            if (date == null || calories == null) {
                continue;
            }
            result.merge(date, calories, Integer::sum);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listLegacyRecords(Long userId, LocalDate recordDate) {
        List<ExerciseSession> sessions = listByUserAndDate(userId, recordDate);
        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = sessions.stream().map(ExerciseSession::getId).toList();
        LambdaQueryWrapper<ExerciseSessionItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(ExerciseSessionItem::getSessionId, ids)
                .orderByAsc(ExerciseSessionItem::getSessionId)
                .orderByAsc(ExerciseSessionItem::getSortOrder)
                .orderByAsc(ExerciseSessionItem::getId);
        Map<Long, List<ExerciseSessionItem>> itemMap = exerciseSessionItemMapper.selectList(itemWrapper)
                .stream()
                .collect(Collectors.groupingBy(ExerciseSessionItem::getSessionId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ExerciseSession session : sessions) {
            List<Map<String, Object>> legacyItems = buildLegacyItems(itemMap.get(session.getId()));
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("time", formatSessionTime(session));
            record.put("name", buildSessionName(legacyItems));
            record.put("durationSeconds", session.getDurationSeconds());
            record.put("caloriesBurned", session.getCaloriesBurned());
            record.put("note", session.getNote());
            record.put("items", legacyItems);
            result.add(record);
        }
        return result;
    }

    private String formatSessionTime(ExerciseSession session) {
        if (session == null || session.getCreateTime() == null) {
            return LocalTime.now().format(TIME_FMT);
        }
        return session.getCreateTime().toInstant()
                .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                .toLocalTime()
                .format(TIME_FMT);
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

    private ExerciseSession getTodayRecordByIndex(Long userId, LocalDate recordDate, int index) {
        List<ExerciseSession> sessions = listByUserAndDate(userId, recordDate);
        if (index < 0 || index >= sessions.size()) {
            return null;
        }
        return sessions.get(index);
    }

    private void replaceItems(Long sessionId, List<Map<String, Object>> items) {
        exerciseSessionItemMapper.delete(new QueryWrapper<ExerciseSessionItem>().eq("sessionId", sessionId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int sortOrder = 0;
        for (Map<String, Object> item : items) {
            ExerciseSessionItem entity = new ExerciseSessionItem();
            entity.setSessionId(sessionId);
            entity.setExerciseId(toLong(item.get("exerciseId")));
            entity.setName(toStringValue(item.get("name")));
            entity.setMuscleGroup(toStringValue(item.get("muscleGroup")));
            entity.setCompletedSets(toInteger(item.get("completedSets")));
            entity.setDurationSeconds(toInteger(item.get("durationSeconds")));
            entity.setNote(toStringValue(item.get("note")));
            entity.setSortOrder(sortOrder++);
            exerciseSessionItemMapper.insert(entity);
        }
    }

    private List<Map<String, Object>> buildLegacyItems(List<ExerciseSessionItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExerciseSessionItem item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("exerciseId", item.getExerciseId());
            map.put("name", item.getName());
            map.put("muscleGroup", item.getMuscleGroup());
            map.put("completedSets", item.getCompletedSets());
            map.put("durationSeconds", item.getDurationSeconds());
            map.put("note", item.getNote());
            result.add(map);
        }
        return result;
    }

    private String buildSessionName(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return "训练记录";
        }
        List<String> names = items.stream()
                .map(item -> toStringValue(item.get("name")))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        return names.isEmpty() ? "训练记录" : String.join("、", names);
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

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return null;
    }

    private String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
