package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.ExerciseSession;
import com.zz.usercenter.model.domain.request.AddExerciseRecordRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExerciseRecordService {

    Long saveRecord(Long userId, LocalDate recordDate, AddExerciseRecordRequest body, String defaultSource);

    Long saveStructuredRecord(Long userId,
                              LocalDate recordDate,
                              String recordTime,
                              String name,
                              Integer durationSeconds,
                              Integer caloriesBurned,
                              String note,
                              String source,
                              List<Map<String, Object>> items);

    boolean updateStructuredRecord(Long userId,
                                   LocalDate recordDate,
                                   int index,
                                   String recordTime,
                                   String name,
                                   Integer durationSeconds,
                                   Integer caloriesBurned,
                                   String note,
                                   String source,
                                   List<Map<String, Object>> items);

    boolean deleteTodayRecord(Long userId, LocalDate recordDate, int index);

    List<ExerciseSession> listByUserAndDate(Long userId, LocalDate recordDate);

    List<ExerciseSession> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    Map<LocalDate, Integer> sumCaloriesByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> listLegacyRecords(Long userId, LocalDate recordDate);

    String getLegacyRecordJson(Long userId, LocalDate recordDate);

    boolean hasRecord(Long userId, LocalDate recordDate);
}
