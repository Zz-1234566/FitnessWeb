package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("exercise_session")
public class ExerciseSession implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("recordDate")
    private LocalDate recordDate;

    private Integer durationSeconds;

    @TableField("caloriesBurned")
    private Integer caloriesBurned;

    private String note;

    @TableLogic
    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
