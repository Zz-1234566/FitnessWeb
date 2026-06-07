package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.UserProfile;

public interface UserProfileService extends IService<UserProfile> {

    UserProfile getByUserId(Long userId);

    void saveOrUpdate(Long userId, UserProfile profile);
}
