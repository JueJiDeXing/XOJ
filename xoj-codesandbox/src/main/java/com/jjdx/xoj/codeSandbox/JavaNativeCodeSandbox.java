package com.jjdx.xoj.codeSandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import com.jjdx.xoj.model.ExecuteCodeRequest;
import com.jjdx.xoj.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate implements CodeSandbox {
    // 见模版类
    public static void main(String[] args) throws Exception {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        JavaNativeCodeSandbox javaCodeSandbox = new JavaNativeCodeSandbox();
        List<String> inputList = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String input = "0 " + i + "\n";
            inputList.add(input);
        }
        executeCodeRequest.setInputList(inputList);
        String code = ResourceUtil.readStr("Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        StopWatch stopWatch = new StopWatch();
        System.out.println("开始执行");
        stopWatch.start();
        ExecuteCodeResponse executeCodeResponse = javaCodeSandbox.executeCode(executeCodeRequest);
        stopWatch.stop();
        System.out.println(stopWatch.prettyPrint());
        System.out.println("response: " + executeCodeResponse);
    }
}
