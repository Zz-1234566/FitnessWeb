package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("exercise_session_item")
public class ExerciseSessionItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("sessionId")
    private Long sessionId;

    private Long exerciseId;

    private String name;

    private String muscleGroup;

    private Integer completedSets;

    private Integer durationSeconds;

    private String note;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
