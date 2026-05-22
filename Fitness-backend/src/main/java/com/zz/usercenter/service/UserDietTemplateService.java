package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.request.SaveUserDietTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserDietTemplateVO;

import java.util.List;
import java.util.Map;

public interface UserDietTemplateService {

    List<UserDietTemplateVO> listTemplates(Long userId);

    UserDietTemplateVO getTemplate(Long userId, Long id);

    Long saveTemplate(Long userId, SaveUserDietTemplateRequest request);

    boolean deleteTemplate(Long userId, Long templateId);

    Map<Long, String> getTemplateNameMap(Long userId);
}
