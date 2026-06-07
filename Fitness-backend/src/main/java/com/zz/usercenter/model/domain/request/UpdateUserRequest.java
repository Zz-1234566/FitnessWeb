
package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UpdateUserRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private Integer gender;
    private Double height;
    private Double weight;
    private Integer age;
    private String activityLevel;
    private Double customDailyCalories;
    private Double targetWeight;
    private String fitnessGoal;
    private String experienceLevel;
    private String preferredEquipment;
    private String userProfile;

    /** 每周训练天数(1-7) */
    private Integer weeklyTrainingDays;
    /** 每次训练时长(分钟) */
    private Integer trainingDuration;
    /** 职业 */
    private String occupation;
    /** 性格特征 */
    private String personality;
    /** 伤病/疾病史 */
    private String medicalHistory;
    /** 饮食偏好/忌口 */
    private String dietPreference;
    /** 训练偏好 */
    private String trainingPreference;

    /** 所在城市 */
    private String city;
    /** 所在城市（英文） */
    private String cityEn;
}
