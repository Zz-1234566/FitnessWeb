package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserDietTemplateVO {

    private Long id;

    private String name;

    private String mealType;

    private List<DietTemplateItemVO> items;

    @Data
    public static class DietTemplateItemVO {
        private Long id;
        private Integer sortOrder;
        private Long foodItemId;
        private String foodName;
        private String imageUrl;
        private BigDecimal baseAmount;
        private BigDecimal amount;
        private String unit;
        private BigDecimal calories;
        private BigDecimal protein;
        private BigDecimal carbs;
        private BigDecimal fat;
        private BigDecimal fiber;
        private String note;
    }
}
