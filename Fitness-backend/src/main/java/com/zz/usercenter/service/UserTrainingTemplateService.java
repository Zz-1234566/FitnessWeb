package com.zz.usercenter.service;

import com.zz.usercenter.model.domain.request.SaveUserTrainingTemplateRequest;
import com.zz.usercenter.model.domain.vo.UserTrainingTemplateVO;

import java.util.List;
import java.util.Map;

public interface UserTrainingTemplateService {

    List<UserTrainingTemplateVO> listTemplates(Long userId);

    UserTrainingTemplateVO getTemplate(Long userId, Long id);

    Long saveTemplate(Long userId, SaveUserTrainingTemplateRequest request);

    boolean deleteTemplate(Long userId, Long templateId);

    Map<Long, String> getTemplateNameMap(Long userId);
}
