package com.jjdx.xoj.judge.impl;

import com.jjdx.xoj.judge.CodeSandbox;
import com.jjdx.xoj.judge.model.ExecuteCaseInfo;
import com.jjdx.xoj.judge.model.ExecuteCodeRequest;
import com.jjdx.xoj.judge.model.ExecuteCodeResponse;

import java.util.ArrayList;
import java.util.List;

/**
 示例代码沙箱(为跑通业务流程)
 */
public class ExampleCodeSandbox implements CodeSandbox {


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();

        executeCodeResponse.setErrorMessage(null);
        List<ExecuteCaseInfo> executeCaseInfoList = new ArrayList<>();
        ExecuteCaseInfo case1 = new ExecuteCaseInfo();
        case1.setOutput("输出1");
        case1.setTime(100L);
        case1.setMemory(100L);
        executeCaseInfoList.add(case1);
        executeCodeResponse.setExecuteCaseInfoList(executeCaseInfoList);
        return executeCodeResponse;
    }
}
