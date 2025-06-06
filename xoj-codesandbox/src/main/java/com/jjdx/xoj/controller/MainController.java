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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController("/")
public class MainController {
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;

    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;

    private final AtomicInteger dockerThreadCount = new AtomicInteger(0);
    private final AtomicInteger javaThreadCount = new AtomicInteger(0);

    private static final int DOCKER_THREAD_THRESHOLD = 50;
    private static final int JAVA_THREAD_THRESHOLD = 100;


    @GetMapping("/test")
    public String testMethod() {
        return "Hello World";
    }

    /**
     执行代码(java)
     */
    @PostMapping("/execute")
    public ExecuteCodeResponse execute(@RequestBody ExecuteCodeRequest executeCodeRequest,
                                       HttpServletRequest httpServletRequest,
                                       HttpServletResponse httpServletResponse) throws IOException {
        // 环境检查
        JavaCodeSandboxTemplate selectedSandbox = selectSandbox();
        if (selectedSandbox == null) {
            httpServletResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "系统错误,没有可用的环境或服务过载");
            return null;
        }
        try {
            // 计数++
            if (selectedSandbox instanceof JavaDockerCodeSandbox) {
                dockerThreadCount.incrementAndGet();
            } else {
                javaThreadCount.incrementAndGet();
            }
            // 执行
            return selectedSandbox.executeCode(executeCodeRequest);
        } finally {
            // 计数--
            if (selectedSandbox instanceof JavaDockerCodeSandbox) {
                dockerThreadCount.decrementAndGet();
            } else {
                javaThreadCount.decrementAndGet();
            }
        }
    }

    /**
     根据环境选择代码沙箱类型
     优先选择Docker,其次选择JavaNative
     * @return 代码沙箱实例, 若`无可用环境`或`请求数过多`返回null
     */
    private JavaCodeSandboxTemplate selectSandbox() {
        boolean hasDocker = CheckEnvironment.isDockerValidAndLinux();
        boolean hasJava = CheckEnvironment.isJavaValid();

        if (hasDocker && dockerThreadCount.get() < DOCKER_THREAD_THRESHOLD) {// 优先选择docker
            return javaDockerCodeSandbox;
        }
        if (hasJava && javaThreadCount.get() < JAVA_THREAD_THRESHOLD) {// 选择Java本地
            return javaNativeCodeSandbox;
        }
        return null;
    }
}
