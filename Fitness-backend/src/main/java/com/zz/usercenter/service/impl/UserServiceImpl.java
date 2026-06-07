package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.UserMapper;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.UserProfile;
import com.zz.usercenter.service.UserService;
import com.zz.usercenter.service.UserProfileService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.zz.usercenter.common.StateCode.*;
import static com.zz.usercenter.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
* @author zhouzhou
* @description 针对表【user】的数据库操作Service实现
* @createDate 2026-03-25 19:20:27
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    //注入Mapper 让代码一眼就能看出是业务逻辑最终是靠哪个数据访问对象完成的
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserProfileService userProfileService;

    /**
     * 盐值，混淆密码
     */
   private static final String SALT = "Zz";
   private static final Pattern INVALID_ACCOUNT_PATTERN = Pattern.compile("\\pP|\\pS|\\s+");



    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword, String username) {
        userAccount = StringUtils.trim(userAccount);

        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusincessException(PARAMS_ERROR, "账号、密码或确认密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusincessException(PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusincessException(PARAMS_ERROR, "账号密码过短");
        }

        //账户不能包含特殊字符
        Matcher matcher = INVALID_ACCOUNT_PATTERN.matcher(userAccount);
        if (matcher.find()) {
            throw new BusincessException(PARAMS_ERROR, "账号不能包含特殊字符");
        }

        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusincessException(PARAMS_ERROR, "两次密码不一致");
        }

        //账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusincessException(PARAMS_ERROR, "账号已存在");
        }

        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //插入数据，用户名默认为账号，性别默认0
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUsername(StringUtils.isBlank(username) ? userAccount : username.trim());
        user.setGender(0);
        boolean saveResult;
        try {
            saveResult = this.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusincessException(PARAMS_ERROR, "账号已存在");
        }
        if(!saveResult){
            throw new BusincessException(SYSTEM_ERROR, "注册失败，数据库保存失败");
        }
        return user.getId();
    }


    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        userAccount = StringUtils.trim(userAccount);

        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusincessException(PARAMS_ERROR, "账号，密码或者检查密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusincessException(PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusincessException(PARAMS_ERROR, "账号密码过短");
        }

        //账户不能包含特殊字符
        Matcher matcher = INVALID_ACCOUNT_PATTERN.matcher(userAccount);
        if (matcher.find()) {
            throw new BusincessException(PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusincessException(NULL_ERROR, "用户不存在或密码错误");
        }

        // 3.用户脱敏 -- 避免返回敏感信息
        User safetyUser = getSafetyUser(user);

        // 4.记录用户的登录态（存脱敏对象，避免密码哈希暴露在Session中）
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     *
     * @param originUser 取到的未脱敏用户信息
     * @return 脱敏用户信息
     */
    @Override
    public User getSafetyUser(User originUser){
        if ( originUser == null ) {
            throw new BusincessException(NOT_LOGIN, "用户未登录");
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setAge(originUser.getAge());
        safetyUser.setHeight(originUser.getHeight());
        safetyUser.setWeight(originUser.getWeight());
        safetyUser.setFavoritesExercises(originUser.getFavoritesExercises());
        safetyUser.setModelPreference(originUser.getModelPreference());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setCity(originUser.getCity());
        safetyUser.setCityEn(originUser.getCityEn());
        UserProfile p = userProfileService.getByUserId(originUser.getId());
        if (p != null) {
            safetyUser.setFitnessGoal(p.getFitnessGoal());
            safetyUser.setActivityLevel(p.getActivityLevel());
            safetyUser.setActivityFactor(p.getActivityFactor());
            safetyUser.setDailyCalorieBurn(p.getDailyCalorieBurn());
            safetyUser.setCustomDailyCalories(p.getCustomDailyCalories());
            safetyUser.setTargetWeight(p.getTargetWeight());
            safetyUser.setExperienceLevel(p.getExperienceLevel());
            safetyUser.setPreferredEquipment(p.getPreferredEquipment());
            safetyUser.setWeeklyTrainingDays(p.getWeeklyTrainingDays());
            safetyUser.setTrainingDuration(p.getTrainingDuration());
            safetyUser.setOccupation(p.getOccupation());
            safetyUser.setPersonality(p.getPersonality());
            safetyUser.setMedicalHistory(p.getMedicalHistory());
            safetyUser.setDietPreference(p.getDietPreference());
            safetyUser.setTrainingPreference(p.getTrainingPreference());
            safetyUser.setUserProfile(p.getUserProfileText());
        }

        return safetyUser;
    }

    /**
     * 用户注销
     *
     * @param request 前端对象
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }
}




