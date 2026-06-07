package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DietFoodItemRequest {

    private Long foodItemId;

    private BigDecimal amount;

    /** 单位（g/ml/个/片/根 等） */
    private String unit;
}
