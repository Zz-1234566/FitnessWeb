package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.util.List;

@Data
public class AddDietRecordRequest {

    private String time;

    private String name;

    private String mealType;

    private Integer calories;

    private String note;

    private String source;

    private List<DietFoodItemRequest> items;
}
