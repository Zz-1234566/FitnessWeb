create table zz.user_weight_record
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                             not null comment '用户ID',
    weight      decimal(5, 2)                      not null comment '体重(kg)',
    record_date date                               not null comment '记录日期',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_date
        unique (user_id, record_date)
)
    comment '用户体重记录表';

create index idx_user_id
    on zz.user_weight_record (user_id);

