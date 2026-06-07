package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 聊天请求
 */
@Data
public class ChatRequest implements Serializable {


    @Serial
    private static final long serialVersionUID = -2732157919277108022L;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 是否开启深度思索（true=聪明模型，false=聊天模型）
     */
    private boolean deepThinking = false;
}
