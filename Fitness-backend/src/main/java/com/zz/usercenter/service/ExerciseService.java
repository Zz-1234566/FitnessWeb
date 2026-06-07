package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.Exercise;

import java.util.List;

/**
 * 动作服务接口
 *
 * @author zhouzhou
 */
public interface ExerciseService extends IService<Exercise> {

    /** 获取所有启用的动作 */
    List<Exercise> getAllActive();

    /** 根据肌群查询动作 */
    List<Exercise> getByMuscleGroup(String muscleGroup);

    /** 根据动作 ID 查询动作*/
    List<Exercise> getByIds(List<Long> ids);

    /** 按用户画像过滤动作（器械 + 难度） */
    List<Exercise> getByFilters(String preferredEquipment, String experienceLevel);

    /** 按用户画像过滤后，按肌群均匀采样最多 limit 个训练动作 */
    List<Exercise> sampleByFilters(String preferredEquipment, String experienceLevel, int limit);

    /** 根据器械名称模糊匹配动作（用于视觉识别器械后推荐动作） */
    List<Exercise> searchByEquipment(String equipmentName);
}