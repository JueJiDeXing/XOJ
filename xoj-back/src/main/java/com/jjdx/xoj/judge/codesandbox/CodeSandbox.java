package com.jjdx.xoj.judge.codesandbox;

import com.jjdx.xoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.jjdx.xoj.judge.codesandbox.model.ExecuteCodeResponse;

import java.util.*;

/**
 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     执行代码
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
