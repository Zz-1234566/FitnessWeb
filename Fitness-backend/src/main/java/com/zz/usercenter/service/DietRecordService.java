package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.DietRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DietRecordService {

    Long saveStructuredRecord(Long userId,
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
                              List<Map<String, Object>> items);

    boolean updateStructuredRecord(Long userId,
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
                                   List<Map<String, Object>> items);

    boolean deleteTodayRecord(Long userId, LocalDate recordDate, int index);

    Long saveSimpleRecord(Long userId,
                          LocalDate recordDate,
                          String recordTime,
                          String mealType,
                          String name,
                          Integer calories,
                          String note,
                          String source);

    List<DietRecord> listByUserAndDate(Long userId, LocalDate recordDate);

    List<Map<String, Object>> listLegacyRecords(Long userId, LocalDate recordDate);

    String getLegacyRecordJson(Long userId, LocalDate recordDate);

    boolean hasRecord(Long userId, LocalDate recordDate);

    /**
     * 获取指定日期的宏量营养素汇总
     */
    Map<String, Object> getDayMacroSummary(Long userId, LocalDate recordDate);

    /** 获取指定记录的最大 sortOrder */
    int getMaxSortOrder(Long dietRecordId);

    /** 追加 items 到已有记录 */
    void appendItems(Long dietRecordId, List<Map<String, Object>> items);

    /** 更新记录 */
    boolean updateRecord(DietRecord record);
}
