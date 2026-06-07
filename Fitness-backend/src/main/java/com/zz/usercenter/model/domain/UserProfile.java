package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@TableName("user_profile")
@Data
public class UserProfile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fitnessGoal;

    private String activityLevel;

    private Double activityFactor;

    private Double dailyCalorieBurn;

    private Double customDailyCalories;

    private Double targetWeight;

    private String experienceLevel;

    private String preferredEquipment;

    private Integer weeklyTrainingDays;

    private Integer trainingDuration;

    private String occupation;

    private String personality;

    private String medicalHistory;

    private String dietPreference;

    private String trainingPreference;

    private String userProfileText;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
