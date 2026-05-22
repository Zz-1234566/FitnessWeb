package com.zz.usercenter.model.domain.request;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class KnowledgeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String category;

    private String title;

    private String content;

    private String keywords;

    private Integer isActive;
}
