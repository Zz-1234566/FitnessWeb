package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.request.SaveUserDietCycleRequest;
import com.zz.usercenter.model.domain.vo.UserDietCycleVO;

import java.util.List;

public interface UserDietCycleService {

    List<UserDietCycleVO> listCycles(Long userId);

    UserDietCycleVO getActiveCycle(Long userId);

    Long saveCycle(Long userId, SaveUserDietCycleRequest request);

    boolean activateCycle(Long userId, Long cycleId);

    boolean deleteCycle(Long userId, Long cycleId);
}
