package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.UserRecord;

public interface UserRecordService extends IService<UserRecord> {

    UserRecord getByUserId(Long userId);

    boolean saveOrUpdateByUserId(UserRecord userRecord);
}
