package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("diet_record")
public class DietRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("recordDate")
    private LocalDate recordDate;

    @TableField("recordTime")
    private String recordTime;

    private String mealType;

    private String name;

    @TableField("totalCalories")
    private Integer calories;

    @TableField("totalProtein")
    private BigDecimal protein;

    @TableField("totalCarbs")
    private BigDecimal carbs;

    @TableField("totalFat")
    private BigDecimal fat;

    @TableField("totalFiber")
    private BigDecimal fiber;

    private String note;

    private String source;

    @TableLogic
    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
