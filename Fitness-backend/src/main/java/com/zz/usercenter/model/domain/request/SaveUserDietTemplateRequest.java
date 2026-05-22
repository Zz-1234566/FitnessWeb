package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SaveUserDietTemplateRequest {

    private Long id;

    private String name;

    private String mealType;

    private List<DietTemplateItemDTO> items;

    @Data
    public static class DietTemplateItemDTO {
        private Long id;
        private Integer sortOrder;
        private Long foodItemId;
        private BigDecimal amount;
        private String unit;
        private String note;
    }
}
