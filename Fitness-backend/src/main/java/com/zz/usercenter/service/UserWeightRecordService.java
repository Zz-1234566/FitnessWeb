package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.UserWeightRecord;

import java.time.LocalDate;
import java.util.List;

public interface UserWeightRecordService extends IService<UserWeightRecord> {

    void saveOrUpdateTodayWeight(Long userId, Double weight);

    List<UserWeightRecord> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    Double getLatestWeightAtOrBefore(Long userId, LocalDate endDate);
}
