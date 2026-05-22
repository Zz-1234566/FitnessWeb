create table zz.exercise_session_item
(
    id              bigint auto_increment comment '主键'
        primary key,
    sessionId       bigint                             not null comment '训练主记录ID',
    exerciseId      bigint                             null comment '动作ID',
    name            varchar(255)                       not null comment '动作名称',
    muscleGroup     varchar(64)                        null comment '训练部位',
    completedSets   int                                null comment '完成组数',
    totalSets       int                                null comment '总组数',
    durationSeconds int                                null comment '动作时长(秒)',
    note            varchar(500)                       null comment '动作备注',
    sortOrder       int      default 0                 not null comment '排序',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '训练动作明细表';

create index idx_session_id
    on zz.exercise_session_item (sessionId);

