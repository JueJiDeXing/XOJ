use xoj;
create table file_record
(
    id            bigint auto_increment comment '文件ID'
        primary key,
    userId        bigint                             not null comment '关联用户ID',
    originalName  varchar(255)                       not null comment '原始文件名',
    storageName   varchar(255)                       not null comment '存储文件名(包含路径)',
    fileSize      bigint                             not null comment '文件大小(字节)',
    fileType      varchar(50)                        not null comment '文件类型(MIME类型)',
    fileExtension varchar(20)                        not null comment '文件扩展名',
    fileCategory  varchar(50)                        not null comment '文件分类(avatar, document等)',
    createdTime   datetime DEFAULT CURRENT_TIMESTAMP not null comment '创建时间',
    updatedTime   datetime DEFAULT CURRENT_TIMESTAMP not null ON UPDATE CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
) comment ='文件存储表' collate = utf8mb4_unicode_ci;

create index index_userId
    on file_record (userId);

