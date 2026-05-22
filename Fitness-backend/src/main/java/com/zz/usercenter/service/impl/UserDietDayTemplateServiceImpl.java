package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.UserDietDayTemplateMapper;
import com.zz.usercenter.model.domain.UserDietDayTemplate;
import com.zz.usercenter.model.domain.request.SaveUserDietDayTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;
import com.zz.usercenter.service.UserDietDayTemplateService;
import com.zz.usercenter.service.UserDietTemplateService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@Service
public class UserDietDayTemplateServiceImpl implements UserDietDayTemplateService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Long>> MEAL_CONFIG_TYPE = new TypeReference<>() {};

    @Resource
    private UserDietDayTemplateMapper userDietDayTemplateMapper;

    @Resource
    private UserDietTemplateService userDietTemplateService;

    @Override
    public List<UserDietDayTemplateVO> listDayTemplates(Long userId) {
        LambdaQueryWrapper<UserDietDayTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietDayTemplate::getUserId, userId)
                .eq(UserDietDayTemplate::getIsDelete, 0)
                .orderByDesc(UserDietDayTemplate::getId);
        List<UserDietDayTemplate> templates = userDietDayTemplateMapper.selectList(wrapper);
        Map<Long, String> templateNameMap = userDietTemplateService.getTemplateNameMap(userId);
        return templates.stream().map(t -> buildVO(t, templateNameMap)).collect(Collectors.toList());
    }

    @Override
    public UserDietDayTemplateVO getDayTemplate(Long userId, Long id) {
        UserDietDayTemplate tpl = getById(userId, id);
        if (tpl == null) {
            return null;
        }
        Map<Long, String> templateNameMap = userDietTemplateService.getTemplateNameMap(userId);
        return buildVO(tpl, templateNameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDayTemplate(Long userId, SaveUserDietDayTemplateRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusincessException(PARAMS_ERROR, "日模板名称不能为空");
        }
        if (request.getMealConfig() == null || request.getMealConfig().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "至少安排一个餐次");
        }
        String mealConfigJson;
        try {
            mealConfigJson = OBJECT_MAPPER.writeValueAsString(request.getMealConfig());
        } catch (JsonProcessingException e) {
            throw new BusincessException(PARAMS_ERROR, "配置序列化失败");
        }
        UserDietDayTemplate entity;
        if (request.getId() != null) {
            entity = getById(userId, request.getId());
            if (entity == null) {
                throw new BusincessException(PARAMS_ERROR, "日模板不存在");
            }
            entity.setName(request.getName().trim());
            entity.setMealConfig(mealConfigJson);
            userDietDayTemplateMapper.updateById(entity);
        } else {
            entity = new UserDietDayTemplate();
            entity.setUserId(userId);
            entity.setName(request.getName().trim());
            entity.setMealConfig(mealConfigJson);
            entity.setIsDelete(0);
            userDietDayTemplateMapper.insert(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDayTemplate(Long userId, Long id) {
        UserDietDayTemplate tpl = getById(userId, id);
        if (tpl == null) {
            return false;
        }
        LambdaUpdateWrapper<UserDietDayTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDietDayTemplate::getId, id)
                .eq(UserDietDayTemplate::getUserId, userId)
                .set(UserDietDayTemplate::getIsDelete, 1);
        userDietDayTemplateMapper.update(null, wrapper);
        return true;
    }

    @Override
    public Map<Long, String> getTemplateNameMap(Long userId) {
        LambdaQueryWrapper<UserDietDayTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietDayTemplate::getUserId, userId)
                .eq(UserDietDayTemplate::getIsDelete, 0);
        return userDietDayTemplateMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(UserDietDayTemplate::getId, UserDietDayTemplate::getName, (a, b) -> a, LinkedHashMap::new));
    }

    private UserDietDayTemplate getById(Long userId, Long id) {
        LambdaQueryWrapper<UserDietDayTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietDayTemplate::getId, id)
                .eq(UserDietDayTemplate::getUserId, userId)
                .eq(UserDietDayTemplate::getIsDelete, 0);
        return userDietDayTemplateMapper.selectOne(wrapper);
    }

    private UserDietDayTemplateVO buildVO(UserDietDayTemplate tpl, Map<Long, String> templateNameMap) {
        UserDietDayTemplateVO vo = new UserDietDayTemplateVO();
        vo.setId(tpl.getId());
        vo.setName(tpl.getName());

        Map<String, Long> config = parseMealConfig(tpl.getMealConfig());
        List<UserDietDayTemplateVO.MealSlotVO> slots = new ArrayList<>();
        for (Map.Entry<String, Long> entry : config.entrySet()) {
            UserDietDayTemplateVO.MealSlotVO slot = new UserDietDayTemplateVO.MealSlotVO();
            slot.setMealType(entry.getKey());
            slot.setTemplateId(entry.getValue());
            slot.setTemplateName(entry.getValue() != null ? templateNameMap.get(entry.getValue()) : null);
            slots.add(slot);
        }
        vo.setMealSlots(slots);
        return vo;
    }

    private Map<String, Long> parseMealConfig(String mealConfig) {
        if (StringUtils.isBlank(mealConfig)) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(mealConfig, MEAL_CONFIG_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
