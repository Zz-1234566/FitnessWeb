package com.zz.usercenter.exception;


import com.zz.usercenter.common.StateCode;

/**
 * 自定义异常类
 *
 * @author zhouzhou
 */
public class BusincessException extends RuntimeException{



    private final int code;

    private final String description;


    public BusincessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusincessException(StateCode stateCode, String description){
        super(stateCode.getMessage());
        this.code = stateCode.getCode();
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

}
