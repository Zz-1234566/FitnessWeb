create table zz.user
(
    id                  bigint auto_increment comment 'id'
        primary key,
    username            varchar(256)                          null comment '用户昵称',
    userAccount         varchar(256)                          null comment '账号',
    avatarUrl           varchar(1024)                         null comment '用户头像',
    gender              tinyint                               null comment '性别',
    height              decimal(5, 2)                         null comment '身高(cm)',
    weight              decimal(5, 2)                         null comment '体重(kg)',
    age                 int                                   null comment '年龄',
    activityLevel       varchar(32)                           null comment '活动等级',
    activityFactor      decimal(4, 3)                         null comment '活动系数',
    dailyCalorieBurn    decimal(7, 2)                         null comment '日消耗热量(kcal)',
    customDailyCalories decimal(10, 2)                        null comment '用户自定义每日目标热量(kcal)',
    targetWeight        decimal(5, 2)                         null comment '用户自定义目标体重(kg)',
    fitnessGoal         varchar(50)                           null comment '健身目标',
    userProfile         text                                  null comment 'AI用户画像',
    userPassword        varchar(512)                          not null comment '密码',
    userStatus          int         default 0                 not null comment '状态: 0-正常',
    createTime          datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime          datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete            tinyint     default 0                 not null comment '是否删除',
    userRole            int         default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员 ',
    favoritesExercises  text                                  null comment '收藏动作ID（JSON数组，如[1,3,7]）',
    modelPreference     varchar(50) default 'qwen-plus'       null comment '用户选择的AI模型',
    experienceLevel     varchar(20)                           null comment '训练水平：beginner/intermediate/advanced',
    preferredEquipment  varchar(200)                          null comment '可用器械，逗号分隔，如"哑铃,杠铃"',
    notificationStates  text                                  null
);

