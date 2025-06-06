package com.jjdx.xoj.judge;

import com.jjdx.xoj.judge.model.ExecuteCodeRequest;
import com.jjdx.xoj.judge.model.ExecuteCodeResponse;

/**
 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     执行代码
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
