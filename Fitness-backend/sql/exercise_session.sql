create table zz.exercise_session
(
    id              bigint auto_increment comment '主键'
        primary key,
    userId          bigint                             not null comment '用户ID',
    recordDate      date                               not null comment '记录日期',
    recordTime      time                               not null comment '记录时间',
    name            varchar(255)                       not null comment '训练摘要',
    durationSeconds int                                null comment '总时长(秒)',
    caloriesBurned  int                                null comment '本次训练消耗热量(kcal)',
    note            varchar(500)                       null comment '备注',
    source          varchar(32)                        null comment '来源',
    isDelete        tinyint  default 0                 not null comment '逻辑删除',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '训练主记录表';

create index idx_user_date
    on zz.exercise_session (userId, recordDate);

