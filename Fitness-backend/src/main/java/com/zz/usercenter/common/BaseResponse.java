package com.zz.usercenter.common;

import lombok.Data;

import java.io.Serializable;


/**
 * 通用返回类
 *
 * @author zhouzhou
 */
@Data
public class BaseResponse<T> implements Serializable {
    // 状态码
    private int code;

    // 数据体
    private T data;

    // 操作信息
    private String message;

    //
    private String description;

    // 构造函数
    public BaseResponse(int code, T data, String meseage, String description) {
        this.code = code;
        this.data = data;
        this.message = meseage;
        this.description = description;
    }

    // 少部分参数的构造函数
    public BaseResponse(int code, T data) {
        // 利用自己的构造函数设置值 避免代码臃肿
        this(code, data, "", "");
    }

    // 返回状态码的构造参数
    public BaseResponse(StateCode stateCode){
        this(stateCode.getCode(), null, stateCode.getMessage(), stateCode.getDescription());
    }

    public BaseResponse(int code, String message, String description) {
        this(code, null, message, description);
    }
}