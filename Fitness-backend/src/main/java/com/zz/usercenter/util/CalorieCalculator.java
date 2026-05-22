package com.zz.usercenter.util;

import com.zz.usercenter.model.domain.User;

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

    public static Double calculateDailyCalorieBurn(User user) {
        if (user == null
                || user.getGender() == null
                || user.getHeight() == null
                || user.getWeight() == null
                || user.getAge() == null) {
            return null;
        }

        Double factor = user.getActivityFactor() != null
                ? user.getActivityFactor()
                : resolveActivityFactor(user.getActivityLevel());
        if (factor == null) {
            return null;
        }

        double bmr = user.getGender() == 1
                ? 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge() + 5
                : 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * user.getAge() - 161;
        return Math.round(bmr * factor * 100.0) / 100.0;
    }
}
