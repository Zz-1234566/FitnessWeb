package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.request.SaveUserTrainingCycleRequest;
import com.zz.usercenter.model.domain.vo.UserTrainingCycleVO;

import java.util.List;

public interface UserTrainingCycleService {

    List<UserTrainingCycleVO> listCycles(Long userId);

    UserTrainingCycleVO getActiveCycle(Long userId);

    Long saveCycle(Long userId, SaveUserTrainingCycleRequest request);

    boolean activateCycle(Long userId, Long cycleId);

    boolean deleteCycle(Long userId, Long cycleId);
}
