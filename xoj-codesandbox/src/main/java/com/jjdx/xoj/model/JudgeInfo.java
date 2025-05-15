package com.jjdx.xoj.model;

import lombok.Data;

/**
 判题信息
 */
@Data
public class JudgeInfo {
    /**
     执行信息(JudgeInfoMessageEnum枚举:AC、WA等)
     */
    private String message;
    /**
     输出
     */
    private String output;
    /**
     消耗时间(ms)
     */
    private Long time;
    /**
     消耗内存(B)
     */
    private Long memory;

}
