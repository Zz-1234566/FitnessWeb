package com.zz.usercenter.exception;


import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static com.zz.usercenter.common.StateCode.*;


/**
 *  全局异常处理器，捕获所有未被捕获的异常并返回统一的错误响应
 *
 * @author zhouzhou
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常处理器，捕获所有业务异常并返回统一的错误响应
     *
     * @param e 运行时异常
     * @return 返回异常信息
     */
    @ExceptionHandler(BusincessException.class)
    public BaseResponse busincessExceptionHandler(BusincessException e){
        log.error("busincessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 处理运行时异常，返回系统错误响应
     * @param e 运行时异常
     * @return 返回异常信息
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e){
        log.error("runtimeException: ", e);
        return ResultUtils.error(SYSTEM_ERROR, e.getMessage(), "");
    }



}
