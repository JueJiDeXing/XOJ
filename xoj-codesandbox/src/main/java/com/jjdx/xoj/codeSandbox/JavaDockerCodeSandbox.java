package com.jjdx.xoj.codeSandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.jjdx.xoj.enums.ExecuteStatus;
import com.jjdx.xoj.model.ExecuteCodeRequest;
import com.jjdx.xoj.model.ExecuteCodeResponse;
import com.jjdx.xoj.model.ExecuteMessage;
import com.jjdx.xoj.utils.CheckEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate implements CodeSandbox {

    public static String imageName; // java镜像名
    public static boolean firstInitImage;// 镜像是否为第一次拉取
    private static int PreContainers; // 预创建容器数量


    @Value("${docker.imageName:openjdk:8-jre-alpine}")
    public void setImage(String imageName) {
        JavaDockerCodeSandbox.imageName = imageName;
    }

    @Value("${docker.firstInitImage:true}")
    public void setIsFirstInit(boolean firstInitImage) {
        JavaDockerCodeSandbox.firstInitImage = firstInitImage;
    }

    @Value("${docker.PreContainers:50}")
    public void setPreContainers(int PreContainers) {
        JavaDockerCodeSandbox.PreContainers = PreContainers;
    }

    private static DockerClient dockerClient;

    private static final LinkedBlockingQueue<String> containerPool = new LinkedBlockingQueue<>();


    @PostConstruct
    public static void initContainerPool() {
        // 检查Docker环境是否可用
        boolean dockerAvailable = CheckEnvironment.isDockerValidAndLinux();
        if (!dockerAvailable) {
            log.error("Docker环境不可用, 跳过容器池初始化");
            return;
        }

        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
        // 拉取镜像
        pullImage(dockerClient, imageName);
        log.info("开始预创建容器,个数={}", PreContainers);
        // 预创建容器
        for (int i = 0; i < PreContainers; i++) {
            String containerId = createAndStartContainer();
            if (containerId != null) {
                containerPool.offer(containerId);
            }
        }
        log.info("预创建容器完成");
    }

    public static void main(String[] args) throws Exception {
        initContainerPool();
        System.out.println("containerPool.size() = " + containerPool.size());
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        JavaDockerCodeSandbox javaCodeSandbox = new JavaDockerCodeSandbox();
        List<String> inputList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
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

    /**
     创建容器并启动

     @return 容器id
     */
    private static String createAndStartContainer() {
        try {
            CreateContainerResponse response = dockerClient.createContainerCmd(imageName)
                    .withHostConfig(getHostConfig())
                    .withCmd("sh", "-c", "mkdir -p /app && while true; do sleep 1; done") // 启动时创建/app目录, 并保持容器运行
                    .exec();
            String id = response.getId();
            dockerClient.startContainerCmd(id).exec();
            return id;
        } catch (Exception e) {
            log.error("容器创建失败", e);
            return null;
        }
    }

    private static HostConfig getHostConfig() {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(300 * 1024 * 1024L); // 最大内存300MB
        hostConfig.withMemorySwap(0L);
        hostConfig.withReadonlyRootfs(false); // 根目录可写
        hostConfig.withNetworkMode("none"); // 禁用网络

        // 安全配置
        String seccomp = ResourceUtil.readStr("seccomp.json", StandardCharsets.UTF_8);
        hostConfig.withSecurityOpts(List.of("seccomp=" + seccomp));
        return hostConfig;
    }

    private void copyFilesToContainer(String containerId, String filePath) {
        try {
            dockerClient.copyArchiveToContainerCmd(containerId)
                    .withHostResource(filePath)
                    .withRemotePath("/app")
                    .exec();
            log.info("复制文件到容器成功");
        } catch (Exception e) {
            log.error("复制文件到容器失败", e);
            throw new RuntimeException("复制文件到容器失败", e);
        }
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    @Override
    public List<ExecuteMessage> runFile(List<String> inputList, File userCodeFile) {

        String containerId = null;
        try {
            // 从池中获取容器
            containerId = containerPool.poll(100, TimeUnit.MILLISECONDS);
            if (containerId == null) containerId = createAndStartContainer(); // 紧急创建

            // 复制文件到容器
            copyFilesToContainer(containerId, userCodeFile.getAbsolutePath().replace(".java", ".class"));

            // 并行执行测试用例
            return parallelExecute(inputList, containerId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取容器被中断", e);
        } finally {
            if (containerId != null) {
                containerPool.offer(containerId); // 归还容器
            }
        }
    }

    private List<ExecuteMessage> parallelExecute(List<String> inputList, String containerId) {
        log.info("并行执行测试用例");
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(inputList.size(), 8)); // 限制线程数

        try {
            List<Future<ExecuteMessage>> futures = new ArrayList<>();
            for (String input : inputList) {
                futures.add(executor.submit(() -> getExecuteMessage(input + "\n", dockerClient, containerId)));
            }

            List<ExecuteMessage> results = new ArrayList<>();
            for (Future<ExecuteMessage> future : futures) {
                try {
                    results.add(future.get(Time_Out * 2, TimeUnit.MILLISECONDS)); // 适当延长超时
                } catch (Exception e) {
                    log.error("测试用例执行失败", e);
                    ExecuteMessage errorMessage = new ExecuteMessage();
                    errorMessage.setExecuteStatus(ExecuteStatus.DOCKER_ERROR);
                    errorMessage.setErrorMessage("测试用例执行异常: " + e.getMessage());
                    results.add(errorMessage);
                }
            }
            log.info("测试用例执行完成");
            return results;
        } finally {
            executor.shutdown();
        }
    }

    private static ExecuteMessage getExecuteMessage(String input, DockerClient dockerClient, String containerId) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 创建内存监控
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        AtomicLong maxMemory = new AtomicLong(0);
        ResultCallback<Statistics> statsCallback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Statistics stats) {
                Long usage = stats.getMemoryStats().getUsage();
                if (usage != null) {
                    maxMemory.set(Math.max(maxMemory.get(), usage));
                }
            }
        };
        statsCmd.exec(statsCallback);

        try {
            String[] cmdArr = new String[]{"java", "-cp", "/app", "Main"};
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArr)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();
            String execId = execCreateCmdResponse.getId();

            AtomicBoolean hasError = new AtomicBoolean(false);
            StringBuilder outputBuilder = new StringBuilder();
            StringBuilder errorBuilder = new StringBuilder();

            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    String payload = new String(frame.getPayload());
                    if (StreamType.STDOUT.equals(streamType)) {
                        outputBuilder.append(payload);
                    } else {
                        hasError.set(true);
                        errorBuilder.append(payload);
                    }
                    super.onNext(frame);
                }
            };

            // 执行并等待完成
            boolean isCompleted = dockerClient.execStartCmd(execId)
                    .withStdIn(new ByteArrayInputStream(input.getBytes()))
                    .exec(execStartResultCallback)
                    .awaitCompletion(Time_Out, TimeUnit.MILLISECONDS);

            // 设置执行结果
            executeMessage.setOutput(outputBuilder.toString());
            if (hasError.get()) {
                executeMessage.setErrorMessage(errorBuilder.toString());
            }

            // 检查执行状态
            if (!isCompleted) {
                executeMessage.setExecuteStatus(ExecuteStatus.TIMEOUT);
                executeMessage.setErrorMessage("运行超时");
                return executeMessage;
            }

            InspectExecResponse inspectResponse = dockerClient.inspectExecCmd(execId).exec();
            if (inspectResponse.isRunning()) {
                executeMessage.setExecuteStatus(ExecuteStatus.DOCKER_ERROR);
                executeMessage.setErrorMessage("进程仍在运行");
            } else {
                int exitCode = inspectResponse.getExitCode();
                executeMessage.setProcessExitCode(exitCode);
                if (exitCode != 0 || hasError.get()) {
                    executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
                    if (executeMessage.getErrorMessage() == null) {
                        executeMessage.setErrorMessage("程序退出码:" + exitCode);
                    }
                } else {
                    executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executeMessage.setExecuteStatus(ExecuteStatus.INTERRUPTED);
            executeMessage.setErrorMessage("执行被中断");
        } catch (Exception e) {
            executeMessage.setExecuteStatus(ExecuteStatus.DOCKER_ERROR);
            executeMessage.setErrorMessage("Docker发生错误: " + e.getMessage());
        } finally {
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(maxMemory.get());
            statsCmd.close();
        }
        return executeMessage;
    }

    private static void pullImage(DockerClient dockerClient, String image) {
        if (!firstInitImage) return;
        firstInitImage = false;
        log.info("拉取镜像: {}", image);
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                log.info("拉取镜像进度: {}", item.getStatus());
                super.onNext(item);
            }
        };
        try {
            pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            log.info("镜像拉取完成: {}", image);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("镜像拉取被中断", e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("开始清理Docker容器...");
        try {
            // 清理容器池中的容器
            while (!containerPool.isEmpty()) {
                String containerId = containerPool.poll();
                if (containerId != null) {
                    stopAndRemoveContainer(containerId);
                }
            }

            // 清理其他可能遗留的容器
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)  // 包括已停止的容器
                    .withStatusFilter(Arrays.asList("created", "running", "exited", "paused"))
                    .exec();

            for (Container container : containers) {
                String imageId = container.getImageId();

                if (imageId != null && imageId.contains(imageName.split(":")[0])) {
                    stopAndRemoveContainer(container.getId());
                }
            }

            log.info("Docker容器清理完成");
        } catch (Exception e) {
            log.error("容器清理过程中发生异常", e);
        }
    }

    private void stopAndRemoveContainer(String containerId) {
        try {
            // 停止容器
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(2)  // 2秒超时
                    .exec();
            log.debug("已停止容器: {}", containerId);
        } catch (Exception e) {
            log.warn("停止容器失败: {} - {}", containerId, e.getMessage());
        }

        try {
            // 删除容器
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)  // 强制删除
                    .exec();
            log.debug("已删除容器: {}", containerId);
        } catch (Exception e) {
            log.warn("删除容器失败: {} - {}", containerId, e.getMessage());
        }
    }
}
