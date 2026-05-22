create table zz.user_daily_metric
(
    id             bigint auto_increment comment '主键'
        primary key,
    userId         bigint                             not null comment '用户ID',
    recordDate     date                               not null comment '记录日期',
    intakeCalories decimal(10, 2)                     null comment '当日摄入热量(kcal)',
    targetCalories decimal(7, 2)                      null comment '当日目标消耗热量(kcal)',
    calorieBalance decimal(7, 2)                      null comment '当日热量盈亏(kcal)，摄入-目标',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_date
        unique (userId, recordDate)
)
    comment '用户每日热量指标表';

create index idx_user_id
    on zz.user_daily_metric (userId);

