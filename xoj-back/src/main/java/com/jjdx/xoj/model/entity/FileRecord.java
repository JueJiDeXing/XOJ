package com.jjdx.xoj.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件记录
 */
@Data
@TableName("file_record")
public class FileRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalName;
    private String storageName;
    private Long fileSize;
    private String fileType;
    private String fileExtension;
    private String fileCategory;
    private Date createdTime;
    private Date updatedTime;
    private Integer isDelete;
}
