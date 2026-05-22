package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.request.SaveUserDietDayTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietDayTemplateVO;

import java.util.List;
import java.util.Map;

public interface UserDietDayTemplateService {

    List<UserDietDayTemplateVO> listDayTemplates(Long userId);

    UserDietDayTemplateVO getDayTemplate(Long userId, Long id);

    Long saveDayTemplate(Long userId, SaveUserDietDayTemplateRequest request);

    boolean deleteDayTemplate(Long userId, Long id);

    Map<Long, String> getTemplateNameMap(Long userId);
}
