package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.util.List;

@Data
public class SaveUserTrainingTemplateRequest {

    private Long id;
    private String name;
    private List<TrainingItemDTO> items;

    @Data
    public static class TrainingItemDTO {
        private Long id;
        private String sectionType;
        private Integer sortOrder;
        private Long exerciseId;
        private String note;
    }
}
