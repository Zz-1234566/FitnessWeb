package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AgentTodayDietPlanVO {

    private LocalDate date;
    private String weekday;
    private String cycleName;
    private Integer dayCount;
    private Integer todayIndex;
    private String currentMealType;
    private Long dayTemplateId;
    private String dayTemplateName;
    private List<MealPlanVO> meals;

    @Data
    public static class MealPlanVO {
        private String mealType;
        private boolean currentMeal;
        private Long templateId;
        private String templateName;
        private List<UserDietTemplateVO.DietTemplateItemVO> items;
    }
}
