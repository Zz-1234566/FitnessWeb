package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("food_item")
public class FoodItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String imageUrl;

    private String category;

    private String unit;

    private BigDecimal baseAmount;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private BigDecimal fiber;

    private Long createdBy;

    private Integer isSystem;

    @TableLogic
    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
