use xoj;
create table question
(
    id            bigint auto_increment comment 'id'
        primary key,
    title         varchar(512)                       null comment '标题',
    content       text                               null comment '内容',
    tagList       varchar(1024)                      null comment '标签(json数组)',
    answer        text                               null comment '标准答案',
    submitNum     int      default 0                 not null comment '提交数',
    acceptNum     int      default 0                 not null comment '通过数',
    judgeCaseList text                               null comment '判题用例(json数组)',
    judgeConfig   text                               null comment '判题配置(json对象)',
    thumbNum      int      default 0                 not null comment '点赞数',
    favourNum     int      default 0                 not null comment '收藏数',
    userId        bigint                             not null comment '创建用户id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '题目' collate = utf8mb4_unicode_ci;

create index idx_userId
    on question (userId);

