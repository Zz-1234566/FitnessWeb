package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户类
 *
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 密码
     */
    private String userPassword;


    /**
     * 身高(cm)
     */
    private Double height;

    /**
     * 体重(kg)
     */
    private Double weight;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 活动等级：sedentary/light/moderate/active/very_active
     */
    @TableField("activityLevel")
    private String activityLevel;

    /**
     * 活动系数
     */
    @TableField("activityFactor")
    private Double activityFactor;

    /**
     * 日消耗热量(kcal)
     */
    @TableField("dailyCalorieBurn")
    private Double dailyCalorieBurn;

    /**
     * 用户自定义每日目标热量(kcal)
     */
    @TableField("customDailyCalories")
    private Double customDailyCalories;

    /**
     * 用户自定义目标体重(kg)
     */
    @TableField("targetWeight")
    private Double targetWeight;

    /**
     * 健身目标
     */
    private String fitnessGoal;

    /**
     * AI用户画像（性格/职业/训练频率/病史等，由前端画像表单自动生成）
     */
    private String userProfile;

    /**
     * 状态: 0 - 正常
     */
    private Integer userStatus;

    /**
     * 用户角色: 0 - 普通用户 1 - 管理员
     */
    private Integer userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 收藏动作ID（JSON数组，如[1,3,7]）
     */
    private String favoritesExercises;

    /**
     * 通知状态 JSON，按 noticeKey 存 read / cleared
     */
    private String notificationStates;

    /**
     * 通知已读状态(JSON对象)
     */
    @TableField(exist = false)
    private String noticeReadState;

    /**
     * 用户选择的AI模型
     */
    private String modelPreference;

    /**
     * 训练水平：beginner/intermediate/advanced
     */
    private String experienceLevel;

    /**
     * 可用器械，逗号分隔，如"哑铃,杠铃"
     */
    private String preferredEquipment;

    /**
     * 是否删除
     */
    //标识该属性是Mybatis的逻辑删除字段
    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 6075451864949341862L;
}
