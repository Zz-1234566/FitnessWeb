package com.zz.usercenter.common;


/**
 * 全局错误码
 *
 * @author zhouzhou
 */
public enum StateCode {

    SUCCESS(0, "ok", "成功"),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无权限", ""),
    SYSTEM_ERROR(50000, "系统内部异常", ""),
    AI_ERROR(50100, "AI异常", ""),
    RECORD_NOT_FOUND(40400, "记录不存在", ""),
    RECORD_ALREADY_EXISTS(40401, "记录已存在", "");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码详细信息
     */
    private final String description;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    StateCode(int code, String message, String description) {
        this.description = description;
        this.message = message;
        this.code = code;
    }

}
