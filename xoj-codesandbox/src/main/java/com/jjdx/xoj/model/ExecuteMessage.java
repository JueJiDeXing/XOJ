package com.jjdx.xoj.model;

import com.jjdx.xoj.enums.ExecuteStatus;
import lombok.Data;

/**
 * 对于一个测试用例的运行情况
 */
@Data
public class ExecuteMessage {
    /*
     * 运行状态
     */
    private ExecuteStatus executeStatus;
    /**
     * 进程退出码
     */
    private Integer processExitCode;
    /**
     * 运行出现错误时的输出结果
     */
    private String errorMessage;
    /**
     * 运行输出的结果
     */
    private String output;
    /**
     * 消耗时间(ms)
     */
    private Long time;
    /**
     * 使用的内存(B)
     */
    private Long memory;
}
