package com.zz.usercenter.model.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 训练动作表
 *
 * @author zhangzheng
 */
@Data
public class Exercise implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 动作名称 */
    private String name;

    /** 所属肌群 */
    private String muscleGroup;

    /** 器械类型 */
    private String equipment;

    /** 难度 */
    private String difficulty;

    /** 训练步骤（JSON数组） */
    private String steps;

    /** 注意事项（JSON数组） */
    private String tips;

    /** 推荐组数 */
    private Integer recommendedSets;

    /** 推荐次数 */
    private String recommendedReps;

    /** 组间休息时间（秒） */
    private Integer restSeconds;

    /** 视频URL */
    private String videoUrl;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 是否启用 */
    private Integer isActive;
}