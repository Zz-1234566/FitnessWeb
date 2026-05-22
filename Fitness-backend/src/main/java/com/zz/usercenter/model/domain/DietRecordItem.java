package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("diet_record_item")
public class DietRecordItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dietRecordId")
    private Long dietRecordId;

    @TableField("foodItemId")
    private Long foodItemId;

    @TableField("foodNameSnapshot")
    private String name;

    private String unit;

    private BigDecimal amount;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private BigDecimal fiber;

    @TableField("sortOrder")
    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
