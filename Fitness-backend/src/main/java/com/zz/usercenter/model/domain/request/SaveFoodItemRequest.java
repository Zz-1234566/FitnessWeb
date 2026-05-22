package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SaveFoodItemRequest {

    private Long id;

    private String name;

    private String imageUrl;

    private String category;

    private String unit;

    private BigDecimal baseAmount;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private BigDecimal fiber;
}
