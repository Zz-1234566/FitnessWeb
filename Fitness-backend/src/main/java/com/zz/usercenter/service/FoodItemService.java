package com.zz.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zz.usercenter.model.domain.FoodItem;

import java.util.List;

public interface FoodItemService extends IService<FoodItem> {

    List<FoodItem> searchVisibleFoods(Long userId, String keyword);
}
