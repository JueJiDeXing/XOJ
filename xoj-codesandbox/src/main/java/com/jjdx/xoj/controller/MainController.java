package com.jjdx.xoj.controller;

import com.jjdx.xoj.codeSandbox.JavaCodeSandboxTemplate;
import com.jjdx.xoj.codeSandbox.JavaDockerCodeSandbox;
import com.jjdx.xoj.codeSandbox.JavaNativeCodeSandbox;
import com.jjdx.xoj.model.ExecuteCodeRequest;
import com.jjdx.xoj.model.ExecuteCodeResponse;
import com.jjdx.xoj.utils.CheckEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RestController("/")
public class MainController {
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    private JavaCodeSandboxTemplate javaCodeSandbox;

    @PostConstruct
    public void init() {
        // 检查环境, 优先docker,其次java
        boolean hasDocker = CheckEnvironment.isDockerValidAndLinux();
        log.info("hasDocker: {}", hasDocker);
        if (hasDocker) {
            javaCodeSandbox = javaDockerCodeSandbox;
            return;
        }
        boolean hasJava = CheckEnvironment.isJavaValid();
        log.info("hasJava: {}", hasJava);
        if (hasJava) {
            javaCodeSandbox = javaNativeCodeSandbox;
            return;
        }
        javaCodeSandbox = null;
    }

    @GetMapping("/test")
    public String testMethod() {
        return "Hello World";
    }

    @PostMapping("/execute")
    public ExecuteCodeResponse execute(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                       HttpServletRequest httpServletRequest,
                                       HttpServletResponse httpServletResponse) throws IOException {
        // 认证检查
        String authHeader = "Authorization", securityKey = "SecurityKey";
        String key = httpServletRequest.getHeader(authHeader);
        if (!securityKey.equals(key)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
        // 环境检查
        if (javaCodeSandbox == null) {
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "系统错误,没有可用的环境");
            return null;
        }
        // 执行
        return javaCodeSandbox.executeCode(executeCodeRequest);
    }
}
