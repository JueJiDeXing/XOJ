use xoj;
create table question_submit
(
    id            bigint auto_increment comment 'id'
        primary key,
    questionId    bigint                             not null comment '题目 id',
    userId        bigint                             not null comment '创建用户 id',
    language      varchar(128)                       not null comment '编程语言',
    code          text                               not null comment '用户代码',
    status        int      default 0                 not null comment '判题状态(0待判题,1判题中,2成功,3失败)',
    judgeInfoList text                               null comment '每个测试用例的输出,时间,内存信息(json数组)',
    judgeResult   varchar(128)                       null comment '总的判题结果',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '题目提交';

create index idx_postId
    on question_submit (questionId);

create index idx_userId
    on question_submit (userId);

