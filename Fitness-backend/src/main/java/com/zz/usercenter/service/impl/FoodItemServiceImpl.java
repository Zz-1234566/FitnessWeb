package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zz.usercenter.mapper.FoodItemMapper;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.service.FoodItemService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodItemServiceImpl extends ServiceImpl<FoodItemMapper, FoodItem> implements FoodItemService {

    @Override
    public List<FoodItem> searchVisibleFoods(Long userId, String keyword) {
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodItem::getIsDelete, 0)
                .and(q -> q.eq(FoodItem::getIsSystem, 1)
                        .or()
                        .eq(userId != null, FoodItem::getCreatedBy, userId));
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(FoodItem::getName, keyword.trim());
        }
        wrapper.orderByDesc(FoodItem::getIsSystem)
                .orderByAsc(FoodItem::getName)
                .last("limit 20");
        return list(wrapper);
    }
}
