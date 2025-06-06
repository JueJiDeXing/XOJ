package com.jjdx.xoj.judge.impl;

import com.jjdx.xoj.judge.CodeSandbox;
import com.jjdx.xoj.judge.model.ExecuteCodeRequest;
import com.jjdx.xoj.judge.model.ExecuteCodeResponse;

/**
 第三方代码沙箱(非自己编写的代码沙箱)
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return null;
    }
}
