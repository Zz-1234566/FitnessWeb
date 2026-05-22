package com.zz.usercenter.model.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class UserProgressTrendVO {

    private List<TrendPoint> points;
    private Double latestWeight;
    private Double weeklyWeightChange;
    private Double weeklyCalorieBalance;
    private Double dailyCalorieBurn;
    private Double customDailyCalories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private String label;
        private Double weight;
        private Integer intakeCalories;
        private Double targetCalories;
        private Double calorieBalance;
        private Integer exerciseCalories;
    }
}
