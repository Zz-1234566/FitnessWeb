package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_training_template_item")
public class UserTrainingTemplateItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String sectionType;

    private Integer sortOrder;

    private Long exerciseId;

    private String note;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
