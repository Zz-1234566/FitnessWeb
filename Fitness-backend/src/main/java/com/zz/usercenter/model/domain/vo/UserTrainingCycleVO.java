package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserTrainingCycleVO {

    private Long id;
    private String name;
    private Integer dayCount;
    private LocalDate startDate;
    private Integer isActive;
    private Integer todayIndex;
    private List<CycleDayVO> days;

    @Data
    public static class CycleDayVO {
        private Integer dayIndex;
        private Long templateId;
        private String templateName;
    }
}
