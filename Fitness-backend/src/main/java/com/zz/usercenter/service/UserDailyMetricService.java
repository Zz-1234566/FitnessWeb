package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserDailyMetric;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface UserDailyMetricService extends IService<UserDailyMetric> {

    void syncDailyCalories(Long userId, LocalDate recordDate, List<Map<String, Object>> dietRecords, Double targetCalories);

    void syncTodayTarget(User user);

    List<UserDailyMetric> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
