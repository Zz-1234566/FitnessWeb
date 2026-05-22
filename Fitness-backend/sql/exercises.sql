create table zz.exercise
(
    id              bigint auto_increment comment '动作ID'
        primary key,
    name            varchar(100)                          not null comment '动作名称（中文）',
    muscleGroup     varchar(50)                           not null comment '所属肌群',
    equipment       varchar(50)                           not null comment '器械类型',
    difficulty      varchar(20) default 'beginner'        not null comment '难度',
    steps           text                                  null comment '训练步骤（JSON数组）',
    tips            text                                  null comment '注意事项（JSON数组）',
    recommendedSets int                                   null comment '推荐组数',
    recommendedReps varchar(20)                           null comment '推荐次数',
    restSeconds     int                                   null comment '组间休息时间（秒）',
    videoUrl        varchar(255)                          null comment '视频URL',
    sortOrder       int         default 0                 null comment '排序顺序',
    isActive        tinyint     default 1                 not null comment '是否启用',
    createTime      datetime    default CURRENT_TIMESTAMP null,
    updateTime      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '训练动作表';

create index idx_difficulty
    on zz.exercise (difficulty);

create index idx_equipment
    on zz.exercise (equipment);

create index idx_muscle_group
    on zz.exercise (muscleGroup);

