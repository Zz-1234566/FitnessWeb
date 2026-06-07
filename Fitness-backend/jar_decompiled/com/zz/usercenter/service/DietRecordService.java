/*
 * Decompiled with CFR 0.152.
 */
package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.DietRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DietRecordService {
    public Long saveStructuredRecord(Long var1, LocalDate var2, String var3, String var4, String var5, Integer var6, BigDecimal var7, BigDecimal var8, BigDecimal var9, BigDecimal var10, String var11, String var12, List<Map<String, Object>> var13);

    public boolean updateStructuredRecord(Long var1, LocalDate var2, int var3, String var4, String var5, String var6, Integer var7, BigDecimal var8, BigDecimal var9, BigDecimal var10, BigDecimal var11, String var12, String var13, List<Map<String, Object>> var14);

    public boolean deleteTodayRecord(Long var1, LocalDate var2, int var3);

    public Long saveSimpleRecord(Long var1, LocalDate var2, String var3, String var4, String var5, Integer var6, String var7, String var8);

    public List<DietRecord> listByUserAndDate(Long var1, LocalDate var2);

    public List<Map<String, Object>> listLegacyRecords(Long var1, LocalDate var2);

    public String getLegacyRecordJson(Long var1, LocalDate var2);

    public boolean hasRecord(Long var1, LocalDate var2);

    public Map<String, Object> getDayMacroSummary(Long var1, LocalDate var2);
}
