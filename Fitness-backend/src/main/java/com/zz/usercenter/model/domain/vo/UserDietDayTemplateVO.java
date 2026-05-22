package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UserDietDayTemplateVO {

    private Long id;

    private String name;

    private List<MealSlotVO> mealSlots;

    @Data
    public static class MealSlotVO {
        private String mealType;
        private Long templateId;
        private String templateName;
    }
}
