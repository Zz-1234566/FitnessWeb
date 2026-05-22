package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.util.Map;

@Data
public class SaveUserDietDayTemplateRequest {

    private Long id;

    private String name;

    /**
     * key=mealType(BREAKFAST/LUNCH/DINNER/SNACK), value=餐次模板ID
     */
    private Map<String, Long> mealConfig;
}
