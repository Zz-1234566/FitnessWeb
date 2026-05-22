package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("user_diet_template_item")
public class UserDietTemplateItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Integer sortOrder;

    private Long foodItemId;

    private BigDecimal amount;

    private String unit;

    private String note;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
