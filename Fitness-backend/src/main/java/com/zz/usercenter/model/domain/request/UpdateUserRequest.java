
package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 接收前端传回数据的请求体，更新用户信息
 *
 * @author zhouzhou
 */
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
}
