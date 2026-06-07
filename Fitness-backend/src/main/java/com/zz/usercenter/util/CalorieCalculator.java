package com.zz.usercenter.util;

import java.util.Map;

public final class CalorieCalculator {

    public static final Map<String, Double> ACTIVITY_FACTORS = Map.of(
            "sedentary", 1.20,
            "light", 1.375,
            "moderate", 1.55,
            "active", 1.725,
            "very_active", 1.90
    );

    private CalorieCalculator() {
    }

    public static Double resolveActivityFactor(String activityLevel) {
        if (activityLevel == null || activityLevel.isBlank()) {
            return null;
        }
        return ACTIVITY_FACTORS.get(activityLevel);
    }

    public static Double calculateDailyCalorieBurn(Integer gender, Double height, Double weight, Integer age, Double activityFactor, String activityLevel) {
        if (gender == null || height == null || weight == null || age == null) {
            return null;
        }

        Double factor = activityFactor != null
                ? activityFactor
                : resolveActivityFactor(activityLevel);
        if (factor == null) {
            return null;
        }

        double bmr = gender == 1
                ? 10 * weight + 6.25 * height - 5 * age + 5
                : 10 * weight + 6.25 * height - 5 * age - 161;
        return Math.round(bmr * factor * 100.0) / 100.0;
    }
}
