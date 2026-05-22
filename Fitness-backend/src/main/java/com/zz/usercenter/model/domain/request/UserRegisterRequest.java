package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author zhouzhou
 */
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3864840947077010363L;

    /**
     * 记录请求体的所有请求对象
     */
    private String userAccount;

    private String userPassword;

    private String checkPassword;

    private String username;

    private int gender;

    private String captcha;

}
