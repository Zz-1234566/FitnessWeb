create table zz.knowledge
(
    id         bigint auto_increment comment '主键'
        primary key,
    category   varchar(50)                          not null comment '分类',
    title      varchar(200)                         not null comment '标题',
    content    text                                 not null comment '内容',
    keywords   varchar(500)                         null comment '关键词，逗号分隔',
    isActive   tinyint(1) default 1                 null comment '是否启用',
    createTime datetime   default CURRENT_TIMESTAMP null,
    updateTime datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    syncedAt   datetime                             null comment '向量同步时间'
)
    comment '知识库';

