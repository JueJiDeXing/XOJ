package com.jjdx.xoj.judge.codesandbox.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.jjdx.xoj.common.ErrorCode;
import com.jjdx.xoj.exception.BusinessException;
import com.jjdx.xoj.judge.codesandbox.CodeSandbox;
import com.jjdx.xoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.jjdx.xoj.judge.codesandbox.model.ExecuteCodeResponse;

/**
 远程代码沙箱
 */
public class RemoteCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String url = "http://localhost:8082/execute";
        String json = JSONUtil.toJsonStr(executeCodeRequest);

        String authHeader = "Authorization";
        String securityKey = "SecurityKey";
        HttpResponse execute = HttpUtil.createPost(url)
                .header(authHeader, securityKey).body(json).execute();
        String response = execute.body();
        if (response == null || response.isEmpty()) {
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "代码沙箱服务执行错误:" + response);
        }
        ExecuteCodeResponse executeCodeResponse = JSONUtil.toBean(response, ExecuteCodeResponse.class);
        System.out.println("[远程代码沙箱]响应:" + executeCodeResponse);
        return executeCodeResponse;
    }
}
