package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户健康记录聚合表
 */
@TableName(value = "user_record")
@Data
public class UserRecord implements Serializable {

    /**
     * 用户ID，与user表1:1
     */
    @TableId
    private Long userId;

    /**
     * 昨日AI总结
     */
    private String yesterdaySummary;

    /**
     * 每日复盘数组JSON
     */
    private String weeklyReviews;

    /**
     * 周度AI总结
     */
    private String weeklySummary;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
