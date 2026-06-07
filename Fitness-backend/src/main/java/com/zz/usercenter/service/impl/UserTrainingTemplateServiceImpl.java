package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.ExerciseMapper;
import com.zz.usercenter.mapper.UserTrainingCycleMapper;
import com.zz.usercenter.mapper.UserTrainingTemplateItemMapper;
import com.zz.usercenter.mapper.UserTrainingTemplateMapper;
import com.zz.usercenter.model.domain.Exercise;
import com.zz.usercenter.model.domain.UserTrainingCycle;
import com.zz.usercenter.model.domain.UserTrainingTemplate;
import com.zz.usercenter.model.domain.UserTrainingTemplateItem;
import com.zz.usercenter.model.domain.request.SaveUserTrainingCycleRequest;
import com.zz.usercenter.model.domain.request.SaveUserTrainingTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@Service
public class UserTrainingTemplateServiceImpl implements UserTrainingTemplateService {

    @Resource
    private UserTrainingTemplateMapper userTrainingTemplateMapper;

    @Resource
    private UserTrainingTemplateItemMapper userTrainingTemplateItemMapper;

    @Resource
    private ExerciseMapper exerciseMapper;

    @Override
    public List<UserTrainingTemplateVO> listTemplates(Long userId) {
        LambdaQueryWrapper<UserTrainingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingTemplate::getUserId, userId)
                .eq(UserTrainingTemplate::getIsDelete, 0)
                .orderByDesc(UserTrainingTemplate::getId);
        List<UserTrainingTemplate> templates = userTrainingTemplateMapper.selectList(wrapper);
        return templates.stream().map(this::buildVO).collect(Collectors.toList());
    }

    @Override
    public UserTrainingTemplateVO getTemplate(Long userId, Long id) {
        UserTrainingTemplate tpl = getById(userId, id);
        return tpl != null ? buildVO(tpl) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTemplate(Long userId, SaveUserTrainingTemplateRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusincessException(PARAMS_ERROR, "训练日名称不能为空");
        }
        if (request.getItems() == null) {
            request.setItems(new ArrayList<>());
        }
        UserTrainingTemplate entity;
        if (request.getId() != null) {
            entity = getById(userId, request.getId());
            if (entity == null) {
                throw new BusincessException(PARAMS_ERROR, "训练日不存在");
            }
            entity.setName(request.getName().trim());
            userTrainingTemplateMapper.updateById(entity);
            deleteItems(entity.getId());
        } else {
            entity = new UserTrainingTemplate();
            entity.setUserId(userId);
            entity.setName(request.getName().trim());
            entity.setIsDelete(0);
            userTrainingTemplateMapper.insert(entity);
        }
        saveItems(entity.getId(), request.getItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long userId, Long templateId) {
        UserTrainingTemplate tpl = getById(userId, templateId);
        if (tpl == null) {
            return false;
        }
        LambdaUpdateWrapper<UserTrainingTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserTrainingTemplate::getId, templateId)
                .eq(UserTrainingTemplate::getUserId, userId)
                .set(UserTrainingTemplate::getIsDelete, 1);
        userTrainingTemplateMapper.update(null, wrapper);
        deleteItems(templateId);
        return true;
    }

    UserTrainingTemplate getById(Long userId, Long id) {
        LambdaQueryWrapper<UserTrainingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingTemplate::getId, id)
                .eq(UserTrainingTemplate::getUserId, userId)
                .eq(UserTrainingTemplate::getIsDelete, 0);
        return userTrainingTemplateMapper.selectOne(wrapper);
    }

    public Map<Long, String> getTemplateNameMap(Long userId) {
        LambdaQueryWrapper<UserTrainingTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingTemplate::getUserId, userId)
                .eq(UserTrainingTemplate::getIsDelete, 0);
        return userTrainingTemplateMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(UserTrainingTemplate::getId, UserTrainingTemplate::getName, (a, b) -> a, LinkedHashMap::new));
    }

    private void deleteItems(Long templateId) {
        LambdaQueryWrapper<UserTrainingTemplateItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingTemplateItem::getTemplateId, templateId);
        userTrainingTemplateItemMapper.delete(wrapper);
    }

    private void saveItems(Long templateId, List<SaveUserTrainingTemplateRequest.TrainingItemDTO> items) {
        for (int i = 0; i < items.size(); i++) {
            SaveUserTrainingTemplateRequest.TrainingItemDTO dto = items.get(i);
            UserTrainingTemplateItem item = new UserTrainingTemplateItem();
            item.setTemplateId(templateId);
            item.setSectionType(dto.getSectionType());
            item.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i);
            item.setExerciseId(dto.getExerciseId());
            item.setNote(dto.getNote());
            userTrainingTemplateItemMapper.insert(item);
        }
    }

    private UserTrainingTemplateVO buildVO(UserTrainingTemplate tpl) {
        UserTrainingTemplateVO vo = new UserTrainingTemplateVO();
        vo.setId(tpl.getId());
        vo.setName(tpl.getName());
        LambdaQueryWrapper<UserTrainingTemplateItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingTemplateItem::getTemplateId, tpl.getId())
                .orderByAsc(UserTrainingTemplateItem::getSortOrder)
                .orderByAsc(UserTrainingTemplateItem::getId);
        List<UserTrainingTemplateItem> items = userTrainingTemplateItemMapper.selectList(wrapper);
        Map<Long, Exercise> exerciseMap = getExerciseMap(items);
        List<UserTrainingTemplateVO.TrainingItemVO> itemVOs = new ArrayList<>();
        for (UserTrainingTemplateItem item : items) {
            UserTrainingTemplateVO.TrainingItemVO itemVO = new UserTrainingTemplateVO.TrainingItemVO();
            itemVO.setId(item.getId());
            itemVO.setSectionType(item.getSectionType());
            itemVO.setSortOrder(item.getSortOrder());
            itemVO.setExerciseId(item.getExerciseId());
            itemVO.setNote(item.getNote());
            Exercise ex = exerciseMap.get(item.getExerciseId());
            if (ex != null) {
                itemVO.setExerciseName(ex.getName());
                itemVO.setMuscleGroup(ex.getMuscleGroup());
                itemVO.setEquipment(ex.getEquipment());
                itemVO.setDifficulty(ex.getDifficulty());
                itemVO.setRecommendedSets(ex.getRecommendedSets());
                itemVO.setRecommendedReps(ex.getRecommendedReps());
                itemVO.setRestSeconds(ex.getRestSeconds());
                itemVO.setVideoUrl(ex.getVideoUrl());
            }
            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        return vo;
    }

    private Map<Long, Exercise> getExerciseMap(List<UserTrainingTemplateItem> items) {
        List<Long> ids = items.stream()
                .map(UserTrainingTemplateItem::getExerciseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<Exercise> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Exercise::getId, ids);
        return exerciseMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Exercise::getId, e -> e, (a, b) -> a));
    }
}
