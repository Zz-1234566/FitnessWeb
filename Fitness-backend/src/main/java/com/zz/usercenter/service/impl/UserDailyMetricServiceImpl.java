package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.UserDailyMetricMapper;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserDailyMetric;
import com.zz.usercenter.service.UserDailyMetricService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserDailyMetricServiceImpl extends ServiceImpl<UserDailyMetricMapper, UserDailyMetric>
        implements UserDailyMetricService {

    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public void syncDailyCalories(Long userId, LocalDate recordDate, List<Map<String, Object>> dietRecords, Double targetCalories) {
        if (userId == null || recordDate == null) {
            return;
        }

        int intakeCalories = 0;
        if (dietRecords != null) {
            for (Map<String, Object> item : dietRecords) {
                Object raw = item == null ? null : item.get("calories");
                if (raw instanceof Number number) {
                    intakeCalories += number.intValue();
                    continue;
                }
                if (raw instanceof String text) {
                    try {
                        intakeCalories += Integer.parseInt(text.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        UserDailyMetric metric = getOrCreate(userId, recordDate);
        metric.setIntakeCalories(intakeCalories);
        metric.setTargetCalories(targetCalories);
        metric.setCalorieBalance(targetCalories == null ? null : Math.round((intakeCalories - targetCalories) * 100.0) / 100.0);
        saveOrUpdate(metric);
    }

    @Override
    public void syncTodayTarget(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        Double targetCalories = resolveTargetCalories(user);
        UserDailyMetric metric = getOrCreate(user.getId(), LocalDate.now(CN_ZONE));
        metric.setTargetCalories(targetCalories);
        Integer intake = metric.getIntakeCalories();
        metric.setCalorieBalance(intake == null || targetCalories == null
                ? null
                : Math.round((intake - targetCalories) * 100.0) / 100.0);
        saveOrUpdate(metric);
    }

    @Override
    public List<UserDailyMetric> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(UserDailyMetric::getUserId, userId)
                .ge(UserDailyMetric::getRecordDate, startDate)
                .le(UserDailyMetric::getRecordDate, endDate)
                .orderByAsc(UserDailyMetric::getRecordDate)
                .list();
    }

    private UserDailyMetric getOrCreate(Long userId, LocalDate recordDate) {
        UserDailyMetric existing = lambdaQuery()
                .eq(UserDailyMetric::getUserId, userId)
                .eq(UserDailyMetric::getRecordDate, recordDate)
                .one();
        if (existing != null) {
            return existing;
        }
        UserDailyMetric created = new UserDailyMetric();
        created.setUserId(userId);
        created.setRecordDate(recordDate);
        return created;
    }

    private Double resolveTargetCalories(User user) {
        if (user == null) {
            return null;
        }
        return user.getCustomDailyCalories() != null
                ? user.getCustomDailyCalories()
                : user.getDailyCalorieBurn();
    }
}
