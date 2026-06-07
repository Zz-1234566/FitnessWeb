/*
 * Decompiled with CFR 0.152.
 */
package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.ExerciseSession;
import com.zz.usercenter.model.domain.request.AddExerciseRecordRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExerciseRecordService {
    public Long saveRecord(Long var1, LocalDate var2, AddExerciseRecordRequest var3, String var4);

    public Long saveStructuredRecord(Long var1, LocalDate var2, String var3, String var4, Integer var5, Integer var6, String var7, String var8, List<Map<String, Object>> var9);

    public boolean updateStructuredRecord(Long var1, LocalDate var2, int var3, String var4, String var5, Integer var6, Integer var7, String var8, String var9, List<Map<String, Object>> var10);

    public boolean deleteTodayRecord(Long var1, LocalDate var2, int var3);

    public List<ExerciseSession> listByUserAndDate(Long var1, LocalDate var2);

    public List<ExerciseSession> listByUserAndDateRange(Long var1, LocalDate var2, LocalDate var3);

    public Map<LocalDate, Integer> sumCaloriesByUserAndDateRange(Long var1, LocalDate var2, LocalDate var3);

    public List<Map<String, Object>> listLegacyRecords(Long var1, LocalDate var2);

    public String getLegacyRecordJson(Long var1, LocalDate var2);

    public boolean hasRecord(Long var1, LocalDate var2);

    public int getDayTotalCaloriesBurned(Long var1, LocalDate var2);
}
