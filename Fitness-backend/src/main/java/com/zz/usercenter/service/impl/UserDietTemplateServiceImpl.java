package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.FoodItemMapper;
import com.zz.usercenter.mapper.UserDietTemplateItemMapper;
import com.zz.usercenter.mapper.UserDietTemplateMapper;
import com.zz.usercenter.model.domain.FoodItem;
import com.zz.usercenter.model.domain.UserDietTemplate;
import com.zz.usercenter.model.domain.UserDietTemplateItem;
import com.zz.usercenter.model.domain.request.SaveUserDietTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;
import com.zz.usercenter.service.UserDietTemplateService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@Service
public class UserDietTemplateServiceImpl implements UserDietTemplateService {

    @Resource
    private UserDietTemplateMapper userDietTemplateMapper;

    @Resource
    private UserDietTemplateItemMapper userDietTemplateItemMapper;

    @Resource
    private FoodItemMapper foodItemMapper;

    @Override
    public List<UserDietTemplateVO> listTemplates(Long userId) {
        LambdaQueryWrapper<UserDietTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietTemplate::getUserId, userId)
                .eq(UserDietTemplate::getIsDelete, 0)
                .orderByDesc(UserDietTemplate::getId);
        List<UserDietTemplate> templates = userDietTemplateMapper.selectList(wrapper);
        return templates.stream().map(this::buildVO).collect(Collectors.toList());
    }

    @Override
    public UserDietTemplateVO getTemplate(Long userId, Long id) {
        UserDietTemplate tpl = getById(userId, id);
        return tpl != null ? buildVO(tpl) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTemplate(Long userId, SaveUserDietTemplateRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusincessException(PARAMS_ERROR, "模板名称不能为空");
        }
        if (StringUtils.isBlank(request.getMealType())) {
            throw new BusincessException(PARAMS_ERROR, "餐次类型不能为空");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "至少添加一个食物");
        }
        UserDietTemplate entity;
        if (request.getId() != null) {
            entity = getById(userId, request.getId());
            if (entity == null) {
                throw new BusincessException(PARAMS_ERROR, "餐次模板不存在");
            }
            entity.setName(request.getName().trim());
            entity.setMealType(request.getMealType());
            userDietTemplateMapper.updateById(entity);
            deleteItems(entity.getId());
        } else {
            entity = new UserDietTemplate();
            entity.setUserId(userId);
            entity.setName(request.getName().trim());
            entity.setMealType(request.getMealType());
            entity.setIsDelete(0);
            userDietTemplateMapper.insert(entity);
        }
        saveItems(entity.getId(), request.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long userId, Long templateId) {
        UserDietTemplate tpl = getById(userId, templateId);
        if (tpl == null) {
            return false;
        }
        LambdaUpdateWrapper<UserDietTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDietTemplate::getId, templateId)
                .eq(UserDietTemplate::getUserId, userId)
                .set(UserDietTemplate::getIsDelete, 1);
        userDietTemplateMapper.update(null, wrapper);
        deleteItems(templateId);
        return true;
    }

    @Override
    public Map<Long, String> getTemplateNameMap(Long userId) {
        LambdaQueryWrapper<UserDietTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietTemplate::getUserId, userId)
                .eq(UserDietTemplate::getIsDelete, 0);
        return userDietTemplateMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(UserDietTemplate::getId, UserDietTemplate::getName, (a, b) -> a, LinkedHashMap::new));
    }

    private UserDietTemplate getById(Long userId, Long id) {
        LambdaQueryWrapper<UserDietTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietTemplate::getId, id)
                .eq(UserDietTemplate::getUserId, userId)
                .eq(UserDietTemplate::getIsDelete, 0);
        return userDietTemplateMapper.selectOne(wrapper);
    }

    private void deleteItems(Long templateId) {
        LambdaQueryWrapper<UserDietTemplateItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietTemplateItem::getTemplateId, templateId);
        userDietTemplateItemMapper.delete(wrapper);
    }

    private void saveItems(Long templateId, List<SaveUserDietTemplateRequest.DietTemplateItemDTO> items) {
        for (int i = 0; i < items.size(); i++) {
            SaveUserDietTemplateRequest.DietTemplateItemDTO dto = items.get(i);
            UserDietTemplateItem item = new UserDietTemplateItem();
            item.setTemplateId(templateId);
            item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i);
            item.setFoodItemId(dto.getFoodItemId());
            item.setAmount(dto.getAmount());
            item.setUnit(dto.getUnit());
            item.setNote(dto.getNote());
            userDietTemplateItemMapper.insert(item);
        }
    }

    private UserDietTemplateVO buildVO(UserDietTemplate tpl) {
        UserDietTemplateVO vo = new UserDietTemplateVO();
        vo.setId(tpl.getId());
        vo.setName(tpl.getName());
        vo.setMealType(tpl.getMealType());

        LambdaQueryWrapper<UserDietTemplateItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietTemplateItem::getTemplateId, tpl.getId())
                .orderByAsc(UserDietTemplateItem::getSortOrder)
                .orderByAsc(UserDietTemplateItem::getId);
        List<UserDietTemplateItem> items = userDietTemplateItemMapper.selectList(wrapper);
        Map<Long, FoodItem> foodMap = getFoodMap(items);

        List<UserDietTemplateVO.DietTemplateItemVO> itemVOs = new ArrayList<>();
        for (UserDietTemplateItem item : items) {
            UserDietTemplateVO.DietTemplateItemVO itemVO = new UserDietTemplateVO.DietTemplateItemVO();
            itemVO.setId(item.getId());
            itemVO.setSortOrder(item.getSortOrder());
            itemVO.setFoodItemId(item.getFoodItemId());
            itemVO.setAmount(item.getAmount());
            itemVO.setUnit(item.getUnit());
            itemVO.setNote(item.getNote());

            FoodItem food = foodMap.get(item.getFoodItemId());
            if (food != null) {
                itemVO.setFoodName(food.getName());
                itemVO.setImageUrl(food.getImageUrl());
                itemVO.setBaseAmount(food.getBaseAmount());
                BigDecimal ratio = computeRatio(item.getAmount(), food.getBaseAmount());
                itemVO.setCalories(multiply(food.getCalories(), ratio));
                itemVO.setProtein(multiply(food.getProtein(), ratio));
                itemVO.setCarbs(multiply(food.getCarbs(), ratio));
                itemVO.setFat(multiply(food.getFat(), ratio));
                itemVO.setFiber(multiply(food.getFiber(), ratio));
            }
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private BigDecimal computeRatio(BigDecimal amount, BigDecimal baseAmount) {
        if (amount == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(baseAmount, 6, RoundingMode.HALF_UP);
    }

    private BigDecimal multiply(BigDecimal value, BigDecimal ratio) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, FoodItem> getFoodMap(List<UserDietTemplateItem> items) {
        List<Long> ids = items.stream()
                .map(UserDietTemplateItem::getFoodItemId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FoodItem::getId, ids);
        return foodItemMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(FoodItem::getId, f -> f, (a, b) -> a));
    }
}
