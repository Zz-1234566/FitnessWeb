create table zz.user_record
(
    userId           bigint                             not null comment '用户ID，与user表1:1'
        primary key,
    dailyRecord      text                               null comment '每日运动饮食记录（JSON数组）',
    recordDate       varchar(10)                        null comment '当前记录日期(yyyy-MM-dd)',
    yesterdaySummary text                               null comment '昨日AI总结',
    weeklyReviews    text                               null comment '每日复盘数组JSON',
    weeklySummary    text                               null comment '周度AI总结',
    createTime       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
);

