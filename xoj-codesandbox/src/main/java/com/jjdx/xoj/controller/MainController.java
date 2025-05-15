package com.jjdx.xoj.controller;

import com.jjdx.xoj.codeSandbox.JavaNativeCodeSandbox;
import com.jjdx.xoj.model.ExecuteCodeRequest;
import com.jjdx.xoj.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @GetMapping("/test")
    public String testMethod() {
        return "Hello World";
    }

    @PostMapping("/execute")
    public ExecuteCodeResponse execute(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                       HttpServletRequest httpServletRequest,
                                       HttpServletResponse httpServletResponse) {
        String authHeader = "Authorization";
        String key = httpServletRequest.getHeader(authHeader);
        String securityKey = "SecurityKey";
        if (!securityKey.equals(key)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        return javaNativeCodeSandbox.executeCode(executeCodeRequest);
    }
}
