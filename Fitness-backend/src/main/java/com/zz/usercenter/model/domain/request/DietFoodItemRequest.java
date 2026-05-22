package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DietFoodItemRequest {

    private Long foodItemId;

    private BigDecimal amount;
}
