create table zz.chat_history
(
    id              bigint auto_increment
        primary key,
    userId          bigint                             not null comment '用户ID（唯一）',
    summary         text                               null comment 'AI对本次对话的总结',
    emotionalState  varchar(50)                        null comment '用户当前情绪状态',
    createTime      datetime default CURRENT_TIMESTAMP null,
    updateTime      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    messageCount    int      default 0                 null,
    pendingMessages text                               null comment '待总结的对话缓冲（JSON数组，每5轮总结后清空）',
    constraint uk_user_id
        unique (userId)
)
    comment 'AI对话记录表';

