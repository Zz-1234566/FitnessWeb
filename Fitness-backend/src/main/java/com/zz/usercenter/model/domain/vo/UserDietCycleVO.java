package com.zz.usercenter.model.domain.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserDietCycleVO {

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
        private Long dayTemplateId;
        private String dayTemplateName;
    }
}
