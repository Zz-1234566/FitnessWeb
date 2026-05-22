package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AgentTodayTrainingPlanVO {

    private LocalDate date;
    private String weekday;
    private String cycleName;
    private Integer dayCount;
    private Integer todayIndex;
    private Long templateId;
    private String templateName;
    private List<UserTrainingTemplateVO.TrainingItemVO> items;
}
