package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveRecognizedFoodRequest {
    private String imageUrl;
    private String foodName;
    private String unit;
    private BigDecimal actualAmount;
    private BigDecimal perUnitAmount;
    private BigDecimal calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private BigDecimal fiber;
    private String mealType;
    private String source;
}
