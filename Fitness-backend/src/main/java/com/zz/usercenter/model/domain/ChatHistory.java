package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI对话记录表
 *
 * @author zhouzhou
 */
@TableName("chat_history")
@Data
public class ChatHistory implements Serializable {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（唯一，一个用户只有一条记录）
     */
    private Long userId;

    /**
     * AI对本次对话的总结（保留关键信息，过滤寒暄和废话）
     */
    private String summary;

    /**
     * 用户当前情绪状态（正常/低落/焦虑/疲惫/兴奋等）
     */
    private String emotionalState;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 消息计数器（每5次生成一次总结）
     */
    private Integer messageCount;

    /**
     * 待总结的对话缓冲（JSON数组，每5轮总结后清空，防止重启丢失）
     */
    private String pendingMessages;

}
