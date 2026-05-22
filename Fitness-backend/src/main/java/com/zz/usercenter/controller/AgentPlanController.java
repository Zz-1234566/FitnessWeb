package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.model.domain.User;
import com.zz.usercenter.model.domain.vo.AgentTodayDietPlanVO;
import com.zz.usercenter.model.domain.vo.AgentTodayTrainingPlanVO;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.UserDietCycleService;
import com.zz.usercenter.service.UserDietDayTemplateService;
import com.zz.usercenter.service.UserDietTemplateService;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.NOT_LOGIN;

@RestController
@RequestMapping("/agent/plan")
public class AgentPlanController {

    @Resource
    private UserTrainingCycleService userTrainingCycleService;

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    @Resource
    private UserDietCycleService userDietCycleService;

    @Resource
    private UserDietDayTemplateService userDietDayTemplateService;

    @Resource
    private UserDietTemplateService userDietTemplateService;

    @GetMapping("/training/today")
    public BaseResponse<AgentTodayTrainingPlanVO> getTodayTrainingPlan(HttpServletRequest request) {
        User user = getLoginUser(request);
        UserTrainingCycleVO activeCycle = userTrainingCycleService.getActiveCycle(user.getId());
        if (activeCycle == null || activeCycle.getTodayIndex() == null) {
            return ResultUtils.success(null);
        }
        Map<Long, UserTrainingTemplateVO> templateMap = userTrainingTemplateService.listTemplates(user.getId()).stream()
                .collect(Collectors.toMap(UserTrainingTemplateVO::getId, Function.identity(), (a, b) -> a));
        UserTrainingCycleVO.CycleDayVO today = activeCycle.getDays() == null ? null : activeCycle.getDays().stream()
                .filter(day -> day.getDayIndex() != null && day.getDayIndex().equals(activeCycle.getTodayIndex()))
                .findFirst()
                .orElse(null);
        if (today == null || today.getTemplateId() == null) {
            return ResultUtils.success(null);
        }
        UserTrainingTemplateVO template = templateMap.get(today.getTemplateId());
        if (template == null) {
            return ResultUtils.success(null);
        }
        AgentTodayTrainingPlanVO vo = new AgentTodayTrainingPlanVO();
        vo.setDate(LocalDate.now());
        vo.setWeekday(resolveWeekday(LocalDate.now().getDayOfWeek()));
        vo.setCycleName(activeCycle.getName());
        vo.setDayCount(activeCycle.getDayCount());
        vo.setTodayIndex(activeCycle.getTodayIndex());
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getName());
        vo.setItems(template.getItems());
        return ResultUtils.success(vo);
    }

    @GetMapping("/diet/today")
    public BaseResponse<AgentTodayDietPlanVO> getTodayDietPlan(HttpServletRequest request) {
        User user = getLoginUser(request);
        UserDietCycleVO activeCycle = userDietCycleService.getActiveCycle(user.getId());
        if (activeCycle == null || activeCycle.getTodayIndex() == null) {
            return ResultUtils.success(null);
        }
        Map<Long, UserDietDayTemplateVO> dayTemplateMap = userDietDayTemplateService.listDayTemplates(user.getId()).stream()
                .collect(Collectors.toMap(UserDietDayTemplateVO::getId, Function.identity(), (a, b) -> a));
        Map<Long, UserDietTemplateVO> mealTemplateMap = userDietTemplateService.listTemplates(user.getId()).stream()
                .collect(Collectors.toMap(UserDietTemplateVO::getId, Function.identity(), (a, b) -> a));
        UserDietCycleVO.CycleDayVO today = activeCycle.getDays() == null ? null : activeCycle.getDays().stream()
                .filter(day -> day.getDayIndex() != null && day.getDayIndex().equals(activeCycle.getTodayIndex()))
                .findFirst()
                .orElse(null);
        if (today == null || today.getDayTemplateId() == null) {
            return ResultUtils.success(null);
        }
        UserDietDayTemplateVO dayTemplate = dayTemplateMap.get(today.getDayTemplateId());
        if (dayTemplate == null) {
            return ResultUtils.success(null);
        }
        String currentMealType = resolveCurrentMealType();
        AgentTodayDietPlanVO vo = new AgentTodayDietPlanVO();
        vo.setDate(LocalDate.now());
        vo.setWeekday(resolveWeekday(LocalDate.now().getDayOfWeek()));
        vo.setCycleName(activeCycle.getName());
        vo.setDayCount(activeCycle.getDayCount());
        vo.setTodayIndex(activeCycle.getTodayIndex());
        vo.setCurrentMealType(currentMealType);
        vo.setDayTemplateId(dayTemplate.getId());
        vo.setDayTemplateName(dayTemplate.getName());
        List<AgentTodayDietPlanVO.MealPlanVO> meals = dayTemplate.getMealSlots() == null ? List.of() : dayTemplate.getMealSlots().stream()
                .map(slot -> {
                    AgentTodayDietPlanVO.MealPlanVO meal = new AgentTodayDietPlanVO.MealPlanVO();
                    meal.setMealType(slot.getMealType());
                    meal.setCurrentMeal(currentMealType.equals(normalizeMealType(slot.getMealType())));
                    meal.setTemplateId(slot.getTemplateId());
                    meal.setTemplateName(slot.getTemplateName());
                    UserDietTemplateVO template = slot.getTemplateId() == null ? null : mealTemplateMap.get(slot.getTemplateId());
                    meal.setItems(template == null || template.getItems() == null ? List.of() : template.getItems());
                    return meal;
                })
                .toList();
        vo.setMeals(meals);
        return ResultUtils.success(vo);
    }

    private User getLoginUser(HttpServletRequest request) {
        Object obj = request.getSession().getAttribute("userLoginState");
        if (obj == null) {
            throw new BusincessException(NOT_LOGIN, "请先登录");
        }
        return (User) obj;
    }

    private String resolveWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }

    private String resolveCurrentMealType() {
        int hour = LocalTime.now().getHour();
        if (hour < 10) {
            return "早餐";
        }
        if (hour < 14) {
            return "午餐";
        }
        if (hour < 17) {
            return "加餐";
        }
        if (hour < 21) {
            return "晚餐";
        }
        return "练后餐";
    }

    private String normalizeMealType(String mealType) {
        if (mealType == null || mealType.isBlank()) {
            return "";
        }
        if (mealType.contains("练后")) return "练后餐";
        if (mealType.contains("早餐")) return "早餐";
        if (mealType.contains("午餐") || mealType.contains("午饭")) return "午餐";
        if (mealType.contains("晚餐") || mealType.contains("晚饭")) return "晚餐";
        if (mealType.contains("加餐") || mealType.contains("夜宵") || mealType.contains("宵夜")) return "加餐";
        return mealType.trim();
    }
}
