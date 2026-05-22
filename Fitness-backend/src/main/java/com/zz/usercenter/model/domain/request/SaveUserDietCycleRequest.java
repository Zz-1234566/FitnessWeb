package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SaveUserDietCycleRequest {

    private Long id;

    private String name;

    private Integer dayCount;

    private LocalDate startDate;

    private Boolean activate;

    private List<CycleDayDTO> days;

    @Data
    public static class CycleDayDTO {
        private Integer dayIndex;
        private Long dayTemplateId;
    }
}
