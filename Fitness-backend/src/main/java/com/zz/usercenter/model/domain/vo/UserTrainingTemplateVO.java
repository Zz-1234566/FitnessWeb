package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserTrainingTemplateVO {

    private Long id;
    private String name;
    private List<TrainingItemVO> items;

    @Data
    public static class TrainingItemVO {
        private Long id;
        private String sectionType;
        private Integer sortOrder;
        private Long exerciseId;
        private String exerciseName;
        private String muscleGroup;
        private String equipment;
        private String difficulty;
        private Integer recommendedSets;
        private String recommendedReps;
        private Integer restSeconds;
        private String videoUrl;
        private String note;
    }
}
