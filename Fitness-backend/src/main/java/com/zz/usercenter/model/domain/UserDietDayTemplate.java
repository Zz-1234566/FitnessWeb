package com.zz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("user_diet_day_template")
public class UserDietDayTemplate implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String mealConfig;

    @TableLogic
    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
