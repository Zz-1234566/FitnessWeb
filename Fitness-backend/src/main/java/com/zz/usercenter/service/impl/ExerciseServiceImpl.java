
package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.ExerciseMapper;
import com.zz.usercenter.model.domain.Exercise;
import com.zz.usercenter.service.ExerciseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * 动作服务实现类
 *
 * @author zhouzhou
 *
 */
@Service
public class ExerciseServiceImpl extends ServiceImpl<ExerciseMapper, Exercise> implements ExerciseService {


    /**
     * 获取所有启用的动作
     * QueryWrapper 是 MyBatis-Plus 的条件构造器，用来写 WHERE 条件
     * .eq("isActive", 1) → WHERE isActive = 1
     * .orderByAsc("sortOrder") → ORDER BY sortOrder ASC
     * list() → 执行查询，返回结果列表（继承自 ServiceImpl）
     */
    @Override
    public List<Exercise> getAllActive() {
        QueryWrapper<Exercise> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isActive", 1);
        queryWrapper.orderByAsc("sortOrder");
        return list(queryWrapper);
    }

    /**
     * 根据肌群查询动作
     * .eq("muscleGroup", muscleGroup) → WHERE muscleGroup = '传入的值'
     */
    @Override
    public List<Exercise> getByMuscleGroup(String muscleGroup) {
        QueryWrapper<Exercise> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isActive", 1);
        queryWrapper.eq("muscleGroup", muscleGroup);
        /**
         * sortOrder 是你自己设的排序权重，比如你想让俯卧撑排在第一个，
         * 就给它 sortOrder = 1，深蹲排第二就 sortOrder = 2
         *      这样查出来就是按你指定的顺序显示。
         */
        queryWrapper.orderByAsc("sortOrder");
        return list(queryWrapper);
    }

    /**
     * 根据动作 ID 查询动作
     * @param ids 动作 ID
     * @return 动作数组
     * @author zhouzhou
     */
    @Override
    public List<Exercise> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<Exercise> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", ids);
        queryWrapper.eq("isActive", 1);
        queryWrapper.orderByAsc("sortOrder");
        return list(queryWrapper);
    }

    /**
     * 按用户画像预过滤动作
     * @param preferredEquipment 用户可用器械（逗号分隔），为空则不按器械过滤
     * @param experienceLevel 训练水平（beginner/intermediate/advanced）
     */
    @Override
    public List<Exercise> getByFilters(String preferredEquipment, String experienceLevel) {
        QueryWrapper<Exercise> wrapper = new QueryWrapper<>();
        wrapper.eq("isActive", 1);

        // 按器械过滤：用户的可用器械列表与动作器械匹配（任一匹配即可）
        if (preferredEquipment != null && !preferredEquipment.isBlank()) {
            String[] equipList = preferredEquipment.split(",");
            // 用 OR 连接多个 LIKE 条件
            StringBuilder likeSql = new StringBuilder("(");
            for (int i = 0; i < equipList.length; i++) {
                if (i > 0) likeSql.append(" OR ");
                likeSql.append("equipment LIKE {").append(i).append("}");
            }
            likeSql.append(")");
            wrapper.apply(likeSql.toString(), equipList);
        }

        // 按难度过滤（数据库存的是中文：初级/中级/高级）
        if (experienceLevel != null) {
            switch (experienceLevel) {
                case "beginner":
                    wrapper.in("difficulty", "初级", "中级");
                    break;
                case "intermediate":
                    // 中级用户可以看到全部
                    break;
                // advanced 不过滤，给全部
            }
        }

        wrapper.orderByAsc("sortOrder");
        return list(wrapper);
    }

    /**
     * 按用户画像过滤后，按肌群均匀采样最多 limit 个训练动作
     * 第一轮每组 shuffle 后取配额，若某肌群不足配额则缺额进入第二轮；
     * 第二轮将剩余可用动作合并 shuffle，补足到 limit
     */
    @Override
    public List<Exercise> sampleByFilters(String preferredEquipment, String experienceLevel, int limit) {
        List<Exercise> all = getByFilters(preferredEquipment, experienceLevel);
        if (all.size() <= limit) {
            return all;
        }

        // 按 muscleGroup 分组，保持插入顺序
        Map<String, List<Exercise>> grouped = all.stream()
                .collect(Collectors.groupingBy(Exercise::getMuscleGroup, LinkedHashMap::new, Collectors.toList()));

        int groupCount = grouped.size();
        int perGroup = limit / groupCount;
        int remainder = limit % groupCount;

        // 第一轮：每组按配额取，记录每个组的已用索引
        List<Exercise> result = new ArrayList<>();
        Map<String, Integer> usedIndex = new LinkedHashMap<>();
        for (Map.Entry<String, List<Exercise>> entry : grouped.entrySet()) {
            List<Exercise> group = entry.getValue();
            Collections.shuffle(group);
            int quota = perGroup + (remainder-- > 0 ? 1 : 0);
            int take = Math.min(quota, group.size());
            result.addAll(group.subList(0, take));
            usedIndex.put(entry.getKey(), take);
        }

        // 第二轮：补足差额（某肌群动作少，导致总数不足 limit）
        int deficit = limit - result.size();
        if (deficit > 0) {
            // 收集各组剩余未取的动作
            List<Exercise> remaining = new ArrayList<>();
            for (Map.Entry<String, List<Exercise>> entry : grouped.entrySet()) {
                int idx = usedIndex.getOrDefault(entry.getKey(), 0);
                if (idx < entry.getValue().size()) {
                    remaining.addAll(entry.getValue().subList(idx, entry.getValue().size()));
                }
            }
            Collections.shuffle(remaining);
            result.addAll(remaining.subList(0, Math.min(deficit, remaining.size())));
        }

        return result;
    }

    @Override
    public List<Exercise> searchByEquipment(String equipmentName) {
        if (equipmentName == null || equipmentName.isBlank()) {
            return List.of();
        }
        QueryWrapper<Exercise> wrapper = new QueryWrapper<>();
        wrapper.eq("isActive", 1);
        wrapper.like("equipment", equipmentName);
        wrapper.last("LIMIT 5");
        return list(wrapper);
    }
}