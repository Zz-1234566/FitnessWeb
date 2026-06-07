package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.UserProfileMapper;
import com.zz.usercenter.model.domain.UserProfile;
import com.zz.usercenter.service.UserProfileService;
import org.springframework.stereotype.Service;

@Service
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    @Override
    public UserProfile getByUserId(Long userId) {
        return getOne(new QueryWrapper<UserProfile>().eq("userId", userId));
    }

    @Override
    public void saveOrUpdate(Long userId, UserProfile profile) {
        UserProfile existing = getByUserId(userId);
        if (existing == null) {
            profile.setUserId(userId);
            save(profile);
        } else {
            profile.setId(existing.getId());
            updateById(profile);
        }
    }
}
