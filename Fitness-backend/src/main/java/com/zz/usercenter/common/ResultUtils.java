package com.zz.usercenter.common;


/**
 * 返回工具类
 *
 * @author zhouzhou
 */
public class ResultUtils {


    /**
     *  成功
     *
     * @param data 返回前端数据
     * @return 通用返回类 -- 状态码 -- 数据 -- 信息 -- 详细描述
     * @param <T> 泛型
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok", "");
    }

    /**
     * 失败
     *
     * @param stateCode 状态码
     * @return 通用返回类 -- 状态码 -- 数据 -- 状态码信息 -- 状态码详细描述
     */
    public static  BaseResponse error(StateCode stateCode) {
        return new BaseResponse(stateCode);
    }

    /**
     * 失败
     *
     * @param stateCode 状态码
     * @return 通用返回类 -- 状态码 -- 数据 -- 状态码信息 -- 状态码详细描述
     */
    public static  BaseResponse error(StateCode stateCode, String message, String description) {
        return new BaseResponse(stateCode.getCode(), message, description);
    }


    /**
     * 失败
     *
     * @param stateCode 状态码
     * @return 通用返回类 -- 状态码 -- 数据 -- 状态码信息 -- 状态码详细描述
     */
    public static  BaseResponse error(StateCode stateCode, String description) {
        return new BaseResponse(stateCode.getCode(), stateCode.getMessage(), description);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static  BaseResponse error(int code, String message, String description) {
        return new BaseResponse(code, message, description);
    }



}
