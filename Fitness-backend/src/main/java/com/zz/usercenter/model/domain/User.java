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
    /** 用户模型偏好配置（JSON: {current, purificationModel, chatModel, whisperModel, visionModel}） */
    private String modelPreference;

    /** 用户所在城市（中文，展示用） */
    private String city;

    /** 用户所在城市（英文，天气API查询用） */
    private String cityEn;

    /**
     * 用户自定义模型配置（JSON数组，apiKey 已 AES 加密）
     */
    @TableField("customModels")
    private String customModels;

    // ========== 以下字段来自 UserProfile，仅用于接口返回透传 ==========

    @TableField(exist = false)
    private String fitnessGoal;

    @TableField(exist = false)
    private String activityLevel;

    @TableField(exist = false)
    private Double customDailyCalories;

    @TableField(exist = false)
    private Double targetWeight;

    @TableField(exist = false)
    private Double activityFactor;

    @TableField(exist = false)
    private Double dailyCalorieBurn;

    @TableField(exist = false)
    private String experienceLevel;

    @TableField(exist = false)
    private String preferredEquipment;

    @TableField(exist = false)
    private Integer weeklyTrainingDays;

    @TableField(exist = false)
    private Integer trainingDuration;

    @TableField(exist = false)
    private String occupation;

    @TableField(exist = false)
    private String personality;

    @TableField(exist = false)
    private String medicalHistory;

    @TableField(exist = false)
    private String dietPreference;

    @TableField(exist = false)
    private String trainingPreference;

    @TableField(exist = false)
    private String userProfile;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 6075451864949341862L;
}
