package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.UserWeightRecordMapper;
import com.zz.usercenter.model.domain.UserWeightRecord;
import com.zz.usercenter.service.UserWeightRecordService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
public class UserWeightRecordServiceImpl extends ServiceImpl<UserWeightRecordMapper, UserWeightRecord>
        implements UserWeightRecordService {

    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public void saveOrUpdateTodayWeight(Long userId, Double weight) {
        if (userId == null || weight == null) {
            return;
        }

        LocalDate today = LocalDate.now(CN_ZONE);
        UserWeightRecord existing = lambdaQuery()
                .eq(UserWeightRecord::getUserId, userId)
                .eq(UserWeightRecord::getRecordDate, today)
                .one();

        if (existing == null) {
            UserWeightRecord record = new UserWeightRecord();
            record.setUserId(userId);
            record.setWeight(BigDecimal.valueOf(weight));
            record.setRecordDate(today);
            save(record);
            return;
        }

        existing.setWeight(BigDecimal.valueOf(weight));
        updateById(existing);
    }

    @Override
    public List<UserWeightRecord> listByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (userId == null || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Collections.emptyList();
        }

        return lambdaQuery()
                .eq(UserWeightRecord::getUserId, userId)
                .ge(UserWeightRecord::getRecordDate, startDate)
                .le(UserWeightRecord::getRecordDate, endDate)
                .orderByAsc(UserWeightRecord::getRecordDate)
                .list();
    }

    @Override
    public Double getLatestWeightAtOrBefore(Long userId, LocalDate endDate) {
        if (userId == null || endDate == null) {
            return null;
        }

        UserWeightRecord latest = lambdaQuery()
                .eq(UserWeightRecord::getUserId, userId)
                .le(UserWeightRecord::getRecordDate, endDate)
                .orderByDesc(UserWeightRecord::getRecordDate)
                .last("limit 1")
                .one();
        return latest == null || latest.getWeight() == null ? null : latest.getWeight().doubleValue();
    }
}
