package com.zz.usercenter.model.domain.request;

import lombok.Data;

@Data
public class AddExerciseRecordRequest {

    private Long exerciseId;

    private String time;

    private String exerciseName;

    private String muscleGroup;

    private Integer completedSets;

    private Integer durationSeconds;

    private Integer caloriesBurned;

    private String note;

    private String source;
}
