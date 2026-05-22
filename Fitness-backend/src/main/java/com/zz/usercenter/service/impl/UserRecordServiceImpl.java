package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.UserRecordMapper;
import com.zz.usercenter.model.domain.UserRecord;
import com.zz.usercenter.service.UserRecordService;
import org.springframework.stereotype.Service;

@Service
public class UserRecordServiceImpl extends ServiceImpl<UserRecordMapper, UserRecord>
        implements UserRecordService {

    @Override
    public UserRecord getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getById(userId);
    }

    @Override
    public boolean saveOrUpdateByUserId(UserRecord userRecord) {
        if (userRecord == null || userRecord.getUserId() == null) {
            return false;
        }
        return saveOrUpdate(userRecord);
    }
}
