package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.User;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务
 *
* @author zhouzhou
*/
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String username);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     *
     * @param originUser 取到的未脱敏用户信息
     * @return 脱敏用户信息
     */
    User getSafetyUser(User originUser);


    /**
     * 用户注销
     *
     * @param request 前端对象
     * @return
     */
    int userLogout(HttpServletRequest request);

}