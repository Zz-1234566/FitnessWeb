package com.zz.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zz.usercenter.exception.BusincessException;
import com.zz.usercenter.mapper.UserDietCycleMapper;
import com.zz.usercenter.model.domain.UserDietCycle;
import com.zz.usercenter.model.domain.request.SaveUserDietCycleRequest;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;
import com.zz.usercenter.service.UserDietCycleService;
import com.zz.usercenter.service.UserDietDayTemplateService;
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
import java.util.stream.Collectors;

import static com.zz.usercenter.common.StateCode.PARAMS_ERROR;

@Slf4j
@Service
public class UserDietCycleServiceImpl implements UserDietCycleService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<SaveUserDietCycleRequest.CycleDayDTO>> DAY_CONFIG_TYPE = new TypeReference<>() {};

    @Resource
    private UserDietCycleMapper userDietCycleMapper;

    @Resource
    private UserDietDayTemplateService userDietDayTemplateService;

    @Override
    public List<UserDietCycleVO> listCycles(Long userId) {
        LambdaQueryWrapper<UserDietCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietCycle::getUserId, userId)
                .eq(UserDietCycle::getIsDelete, 0)
                .orderByDesc(UserDietCycle::getIsActive)
                .orderByDesc(UserDietCycle::getId);
        List<UserDietCycle> cycles = userDietCycleMapper.selectList(wrapper);
        Map<Long, String> nameMap = userDietDayTemplateService.getTemplateNameMap(userId);
        return cycles.stream().map(c -> buildVO(c, nameMap)).collect(Collectors.toList());
    }

    @Override
    public UserDietCycleVO getActiveCycle(Long userId) {
        LambdaQueryWrapper<UserDietCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietCycle::getUserId, userId)
                .eq(UserDietCycle::getIsActive, 1)
                .eq(UserDietCycle::getIsDelete, 0)
                .last("LIMIT 1");
        UserDietCycle cycle = userDietCycleMapper.selectOne(wrapper);
        if (cycle == null) {
            return null;
        }
        Map<Long, String> nameMap = userDietDayTemplateService.getTemplateNameMap(userId);
        return buildVO(cycle, nameMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCycle(Long userId, SaveUserDietCycleRequest request) {
        if (request == null || StringUtils.isBlank(request.getName())) {
            throw new BusincessException(PARAMS_ERROR, "循环名称不能为空");
        }
        if (request.getDayCount() == null || request.getDayCount() <= 0) {
            throw new BusincessException(PARAMS_ERROR, "循环天数必须大于0");
        }
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BusincessException(PARAMS_ERROR, "至少安排一天");
        }
        Map<Long, String> validTemplates = userDietDayTemplateService.getTemplateNameMap(userId);
        for (SaveUserDietCycleRequest.CycleDayDTO day : request.getDays()) {
            if (day.getDayTemplateId() != null && !validTemplates.containsKey(day.getDayTemplateId())) {
                throw new BusincessException(PARAMS_ERROR, "安排中包含无效的日模板");
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
        UserDietCycle entity;
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
            userDietCycleMapper.updateById(entity);
        } else {
            entity = new UserDietCycle();
            entity.setUserId(userId);
            entity.setName(request.getName().trim());
            entity.setDayCount(request.getDayCount());
            entity.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
            entity.setDayConfig(dayConfigJson);
            entity.setIsActive(activate ? 1 : 0);
            entity.setIsDelete(0);
            userDietCycleMapper.insert(entity);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateCycle(Long userId, Long cycleId) {
        UserDietCycle cycle = getCycle(userId, cycleId);
        if (cycle == null) {
            return false;
        }
        deactivateOthers(userId, cycleId);
        cycle.setIsActive(1);
        userDietCycleMapper.updateById(cycle);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCycle(Long userId, Long cycleId) {
        UserDietCycle cycle = getCycle(userId, cycleId);
        if (cycle == null) {
            return false;
        }
        LambdaUpdateWrapper<UserDietCycle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDietCycle::getId, cycleId)
                .eq(UserDietCycle::getUserId, userId)
                .set(UserDietCycle::getIsDelete, 1);
        userDietCycleMapper.update(null, wrapper);
        if (cycle.getIsActive() == 1) {
            activateLatest(userId);
        }
        return true;
    }

    private void deactivateOthers(Long userId, Long excludeId) {
        LambdaUpdateWrapper<UserDietCycle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserDietCycle::getUserId, userId)
                .eq(UserDietCycle::getIsActive, 1)
                .eq(UserDietCycle::getIsDelete, 0);
        if (excludeId != null) {
            wrapper.ne(UserDietCycle::getId, excludeId);
        }
        wrapper.set(UserDietCycle::getIsActive, 0);
        userDietCycleMapper.update(null, wrapper);
    }

    private void activateLatest(Long userId) {
        LambdaQueryWrapper<UserDietCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietCycle::getUserId, userId)
                .eq(UserDietCycle::getIsDelete, 0)
                .orderByDesc(UserDietCycle::getId)
                .last("LIMIT 1");
        UserDietCycle latest = userDietCycleMapper.selectOne(wrapper);
        if (latest != null) {
            latest.setIsActive(1);
            userDietCycleMapper.updateById(latest);
        }
    }

    private UserDietCycle getCycle(Long userId, Long id) {
        LambdaQueryWrapper<UserDietCycle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDietCycle::getId, id)
                .eq(UserDietCycle::getUserId, userId)
                .eq(UserDietCycle::getIsDelete, 0);
        return userDietCycleMapper.selectOne(wrapper);
    }

    private UserDietCycleVO buildVO(UserDietCycle cycle, Map<Long, String> templateNameMap) {
        UserDietCycleVO vo = new UserDietCycleVO();
        vo.setId(cycle.getId());
        vo.setName(cycle.getName());
        vo.setDayCount(cycle.getDayCount());
        vo.setStartDate(cycle.getStartDate());
        vo.setIsActive(cycle.getIsActive());
        vo.setTodayIndex(resolveTodayIndex(cycle.getStartDate(), cycle.getDayCount()));

        List<SaveUserDietCycleRequest.CycleDayDTO> days = parseDayConfig(cycle.getDayConfig());
        List<UserDietCycleVO.CycleDayVO> dayVOs = new ArrayList<>();
        for (SaveUserDietCycleRequest.CycleDayDTO day : days) {
            UserDietCycleVO.CycleDayVO dvo = new UserDietCycleVO.CycleDayVO();
            dvo.setDayIndex(day.getDayIndex());
            dvo.setDayTemplateId(day.getDayTemplateId());
            dvo.setDayTemplateName(day.getDayTemplateId() != null ? templateNameMap.get(day.getDayTemplateId()) : null);
            dayVOs.add(dvo);
        }
        vo.setDays(dayVOs);
        return vo;
    }

    private List<SaveUserDietCycleRequest.CycleDayDTO> parseDayConfig(String dayConfig) {
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
