package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("user_daily_metric")
public class UserDailyMetric implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("recordDate")
    private LocalDate recordDate;

    @TableField("intakeCalories")
    private Integer intakeCalories;

    @TableField("targetCalories")
    private Double targetCalories;

    @TableField("calorieBalance")
    private Double calorieBalance;

    @TableField("createTime")
    private Date createTime;

    @TableField("updateTime")
    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
