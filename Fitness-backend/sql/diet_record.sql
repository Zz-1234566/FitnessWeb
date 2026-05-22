create table zz.diet_record
(
    id            bigint auto_increment comment '主键'
        primary key,
    userId        bigint                                   not null comment '用户ID',
    recordDate    date                                     not null comment '记录日期',
    recordTime    time                                     not null comment '记录时间',
    mealType      varchar(32)                              not null comment '餐次：breakfast/lunch/dinner/snack',
    name          varchar(255)                             not null comment '本次饮食摘要',
    note          varchar(500)                             null comment '备注',
    totalCalories decimal(10, 2) default 0.00              not null comment '总热量(kcal)',
    totalProtein  decimal(10, 2) default 0.00              not null comment '总蛋白质(g)',
    totalCarbs    decimal(10, 2) default 0.00              not null comment '总碳水(g)',
    totalFat      decimal(10, 2) default 0.00              not null comment '总脂肪(g)',
    totalFiber    decimal(10, 2) default 0.00              not null comment '总膳食纤维(g)',
    source        varchar(32)                              null comment '来源',
    isDelete      tinyint        default 0                 not null comment '逻辑删除',
    createTime    datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '饮食记录主表';

create index idx_user_date
    on zz.diet_record (userId, recordDate);

