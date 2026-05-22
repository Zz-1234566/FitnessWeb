package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.UserTrainingCycleMapper;
import com.zz.usercenter.model.domain.UserTrainingCycle;
import com.zz.usercenter.model.domain.request.SaveUserTrainingCycleRequest;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;
import com.zz.usercenter.service.UserTrainingCycleService;
import com.zz.usercenter.service.UserTrainingTemplateService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@Slf4j
@Service
public class UserTrainingCycleServiceImpl implements UserTrainingCycleService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<SaveUserTrainingCycleRequest.CycleDayDTO>> DAY_CONFIG_TYPE =
            new TypeReference<>() {};

    @Resource
    private UserTrainingCycleMapper userTrainingCycleMapper;

    @Resource
    private UserTrainingTemplateService userTrainingTemplateService;

    @Override
    public List<UserTrainingCycleVO> listCycles(Long userId) {
        LambdaQueryWrapper<UserTrainingCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingCycle::getUserId, userId)
                .eq(UserTrainingCycle::getIsDelete, 0)
                .orderByDesc(UserTrainingCycle::getIsActive)
                .orderByDesc(UserTrainingCycle::getId);
        List<UserTrainingCycle> cycles = userTrainingCycleMapper.selectList(wrapper);
        Map<Long, String> nameMap = userTrainingTemplateService.getTemplateNameMap(userId);
        return cycles.stream().map(c -> buildVO(c, nameMap)).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public UserTrainingCycleVO getActiveCycle(Long userId) {
        LambdaQueryWrapper<UserTrainingCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingCycle::getUserId, userId)
                .eq(UserTrainingCycle::getIsActive, 1)
                .eq(UserTrainingCycle::getIsDelete, 0)
                .last("LIMIT 1");
        UserTrainingCycle cycle = userTrainingCycleMapper.selectOne(wrapper);
        if (cycle == null) {
            return null;
        }
        Map<Long, String> nameMap = userTrainingTemplateService.getTemplateNameMap(userId);
        return buildVO(cycle, nameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCycle(Long userId, SaveUserTrainingCycleRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusincessException(PARAMS_ERROR, "循环名称不能为空");
        }
        if (request.getDayCount() == null || request.getDayCount() <= 0) {
            throw new BusincessException(PARAMS_ERROR, "循环天数必须大于0");
        }
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "至少安排一天");
        }
        Map<Long, String> validTemplates = userTrainingTemplateService.getTemplateNameMap(userId);
        for (SaveUserTrainingCycleRequest.CycleDayDTO day : request.getDays()) {
            if (day.getTemplateId() != null && !validTemplates.containsKey(day.getTemplateId())) {
                throw new BusincessException(PARAMS_ERROR, "安排中包含无效的训练日");
            }
        }
        boolean activate = request.getActivate() == null || request.getActivate();
        if (activate) {
            deactivateOthers(userId, request.getId());
        }
        String dayConfigJson;
        try {
            dayConfigJson = OBJECT_MAPPER.writeValueAsString(request.getDays());
        } catch (JsonProcessingException e) {
            throw new BusincessException(PARAMS_ERROR, "配置序列化失败");
        }
        UserTrainingCycle entity;
        if (request.getId() != null) {
            entity = getCycle(userId, request.getId());
            if (entity == null) {
                throw new BusincessException(PARAMS_ERROR, "循环不存在");
            }
            entity.setName(request.getName().trim());
            entity.setDayCount(request.getDayCount());
            entity.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
            entity.setDayConfig(dayConfigJson);
            entity.setIsActive(activate ? 1 : 0);
            userTrainingCycleMapper.updateById(entity);
        } else {
            entity = new UserTrainingCycle();
            entity.setUserId(userId);
            entity.setName(request.getName().trim());
            entity.setDayCount(request.getDayCount());
            entity.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
            entity.setDayConfig(dayConfigJson);
            entity.setIsActive(activate ? 1 : 0);
            entity.setIsDelete(0);
            userTrainingCycleMapper.insert(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateCycle(Long userId, Long cycleId) {
        UserTrainingCycle cycle = getCycle(userId, cycleId);
        if (cycle == null) {
            return false;
        }
        deactivateOthers(userId, cycleId);
        cycle.setIsActive(1);
        userTrainingCycleMapper.updateById(cycle);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCycle(Long userId, Long cycleId) {
        UserTrainingCycle cycle = getCycle(userId, cycleId);
        if (cycle == null) {
            return false;
        }
        LambdaUpdateWrapper<UserTrainingCycle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserTrainingCycle::getId, cycleId)
                .eq(UserTrainingCycle::getUserId, userId)
                .set(UserTrainingCycle::getIsDelete, 1);
        userTrainingCycleMapper.update(null, wrapper);
        if (cycle.getIsActive() == 1) {
            activateLatest(userId);
        }
        return true;
    }

    private void deactivateOthers(Long userId, Long excludeId) {
        LambdaUpdateWrapper<UserTrainingCycle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserTrainingCycle::getUserId, userId)
                .eq(UserTrainingCycle::getIsActive, 1)
                .eq(UserTrainingCycle::getIsDelete, 0);
        if (excludeId != null) {
            wrapper.ne(UserTrainingCycle::getId, excludeId);
        }
        wrapper.set(UserTrainingCycle::getIsActive, 0);
        userTrainingCycleMapper.update(null, wrapper);
    }

    private void activateLatest(Long userId) {
        LambdaQueryWrapper<UserTrainingCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingCycle::getUserId, userId)
                .eq(UserTrainingCycle::getIsDelete, 0)
                .orderByDesc(UserTrainingCycle::getId)
                .last("LIMIT 1");
        UserTrainingCycle latest = userTrainingCycleMapper.selectOne(wrapper);
        if (latest != null) {
            latest.setIsActive(1);
            userTrainingCycleMapper.updateById(latest);
        }
    }

    private UserTrainingCycle getCycle(Long userId, Long id) {
        LambdaQueryWrapper<UserTrainingCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTrainingCycle::getId, id)
                .eq(UserTrainingCycle::getUserId, userId)
                .eq(UserTrainingCycle::getIsDelete, 0);
        return userTrainingCycleMapper.selectOne(wrapper);
    }

    private UserTrainingCycleVO buildVO(UserTrainingCycle cycle, Map<Long, String> templateNameMap) {
        UserTrainingCycleVO vo = new UserTrainingCycleVO();
        vo.setId(cycle.getId());
        vo.setName(cycle.getName());
        vo.setDayCount(cycle.getDayCount());
        vo.setStartDate(cycle.getStartDate());
        vo.setIsActive(cycle.getIsActive());
        vo.setTodayIndex(resolveTodayIndex(cycle.getStartDate(), cycle.getDayCount()));
        List<SaveUserTrainingCycleRequest.CycleDayDTO> days = parseDayConfig(cycle.getDayConfig());
        List<UserTrainingCycleVO.CycleDayVO> dayVOs = new ArrayList<>();
        for (SaveUserTrainingCycleRequest.CycleDayDTO day : days) {
            UserTrainingCycleVO.CycleDayVO dvo = new UserTrainingCycleVO.CycleDayVO();
            dvo.setDayIndex(day.getDayIndex());
            dvo.setTemplateId(day.getTemplateId());
            dvo.setTemplateName(day.getTemplateId() != null ? templateNameMap.get(day.getTemplateId()) : null);
            dayVOs.add(dvo);
        }
        vo.setDays(dayVOs);
        return vo;
    }

    private List<SaveUserTrainingCycleRequest.CycleDayDTO> parseDayConfig(String dayConfig) {
        if (StringUtils.isBlank(dayConfig)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(dayConfig, DAY_CONFIG_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Integer resolveTodayIndex(LocalDate startDate, Integer dayCount) {
        if (startDate == null || dayCount == null || dayCount <= 0) {
            return 1;
        }
        long diff = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        long normalized = ((diff % dayCount) + dayCount) % dayCount;
        return (int) normalized + 1;
    }
}
