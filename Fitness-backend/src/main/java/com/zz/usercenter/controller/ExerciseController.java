package com.zz.usercenter.controller;

import com.zz.usercenter.common.BaseResponse;
import com.zz.usercenter.common.ResultUtils;
import com.zz.usercenter.model.domain.Exercise;
import com.zz.usercenter.service.ExerciseService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练动作接口
 */
@RestController
@RequestMapping("/exercise")
public class ExerciseController {

    @Resource
    private ExerciseService exerciseService;

    /**
     * 获取所有启用的动作
     */
    @GetMapping("/list")
    public BaseResponse<List<Exercise>> getAllActive() {
        return ResultUtils.success(exerciseService.getAllActive());
    }

    /**
     * 根据肌群查询动作
     */
    @GetMapping("/listByGroup")
    public BaseResponse<List<Exercise>> getByMuscleGroup(@RequestParam String muscleGroup) {
        return ResultUtils.success(exerciseService.getByMuscleGroup(muscleGroup));
    }
}