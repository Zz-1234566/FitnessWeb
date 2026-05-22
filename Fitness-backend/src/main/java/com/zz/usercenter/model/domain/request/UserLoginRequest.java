package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author zhouzhou
 */

@Data
public class UserLoginRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = -2732157919277108022L;

    /**
     * 记录请求体的所有请求对象
     */
    private String userAccount;

    private String userPassword;
}
