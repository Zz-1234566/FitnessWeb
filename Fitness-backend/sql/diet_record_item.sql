create table zz.diet_record_item
(
    id               bigint auto_increment comment '主键'
        primary key,
    dietRecordId     bigint                                   not null comment '饮食主记录ID',
    foodItemId       bigint                                   null comment '关联食物ID',
    foodNameSnapshot varchar(128)                             not null comment '食物名称快照',
    amount           decimal(10, 2)                           not null comment '摄入量',
    unit             varchar(32)                              not null comment '单位，如g/ml/个/份',
    calories         decimal(10, 2) default 0.00              not null comment '该项热量(kcal)',
    protein          decimal(10, 2) default 0.00              not null comment '该项蛋白质(g)',
    carbs            decimal(10, 2) default 0.00              not null comment '该项碳水(g)',
    fat              decimal(10, 2) default 0.00              not null comment '该项脂肪(g)',
    fiber            decimal(10, 2) default 0.00              not null comment '该项膳食纤维(g)',
    sortOrder        int            default 0                 not null comment '排序',
    createTime       datetime       default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '饮食记录明细表';

create index idx_record_id
    on zz.diet_record_item (dietRecordId);

