package com.jjdx.xoj.codeSandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.ArrayUtil;
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
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate implements CodeSandbox {
    public static boolean Is_First_Init = false;
    public static String image = "openjdk:8-alpine";
    private DockerClient dockerClient;
    private String containerId;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = super.executeCode(executeCodeRequest);
        // 11. 容器清理
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            System.out.println("容器清理失败: " + e.getMessage());
        }
        return executeCodeResponse;
    }

    @Override
    public List<ExecuteMessage> runFile(List<String> inputList, File userCodeFile) {
        runDockerContainer(userCodeFile);
        waitDockerContainer();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i++) {
            String input = inputList.get(i);
            ExecuteMessage executeMessage = getExecuteMessage(input, dockerClient, containerId);
            executeMessageList.add(executeMessage);
            int percent = (int) ((i + 1) * 100.0 / inputList.size());
            int barLength = 20;
            int completedLength = barLength * percent / 100;
            int notCompletedLength = barLength - completedLength;
            String lineBar = "▌".repeat(completedLength) + " ".repeat(notCompletedLength);
            System.out.print("\r测试用例执行进度: " + lineBar + percent + "%");
        }
        return executeMessageList;
    }

    private void waitDockerContainer() {
        while (true) {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            Boolean running = inspectResponse.getState().getRunning();
            if (Boolean.TRUE.equals(running)) {
                break;
            }
            try {
                Thread.sleep(100); // 短暂等待，避免频繁检查
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void runDockerContainer(File userCodeFile) {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder().dockerHost(config.getDockerHost()).build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        //  拉取jdk镜像
        pullImage(dockerClient, image);
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig(); // 限制程序的权限
        hostConfig.withMemory(300 * 1024 * 1024L);// 最大内存300MB
        hostConfig.withMemorySwap(0L);
        hostConfig.withReadonlyRootfs(true);// 禁止向root目录写文件
        hostConfig.setBinds(new Bind(userCodeFile.getParentFile().getAbsolutePath(), new Volume("/app")));// 将用户的代码挂载到容器中
        // hostConfig.withSecurityOpts(Arrays.asList("seccomp="));
        CreateContainerResponse exec = createContainerCmd.withHostConfig(hostConfig).withNetworkDisabled(true) // 禁用网络
                .withAttachStdout(true).withAttachStderr(true).withAttachStdin(true).withTty(true).exec();
        this.containerId = exec.getId();
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
    }


    /**
     * 执行Java程序
     *
     * @param input        输入用例
     * @param dockerClient
     * @param containerId
     * @return
     */
    private static ExecuteMessage getExecuteMessage(String input, DockerClient dockerClient, String containerId) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        // 创建内存监控
        StatsCmd statsCmd = dockerClient.statsCmd(containerId);
        AtomicLong maxMemoryUsage = new AtomicLong(0);
        ResultCallback<Statistics> statsCallback = new ResultCallback<>() {
            @Override
            public void onNext(Statistics statistics) {
                Long usage = statistics.getMemoryStats().getUsage();
                if (usage != null) {
                    maxMemoryUsage.updateAndGet(prev -> Math.max(prev, usage));
                }
            }

            @Override
            public void onStart(Closeable closeable) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void close() {
            }
        };
        statsCmd.exec(statsCallback);

        String[] cmdArr = new String[]{"java", "-cp", "/app", "Main"};
        String[] cmd = ArrayUtil.append(cmdArr);
        try {
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId).withCmd(cmd).withAttachStdin(true).withAttachStdout(true).withAttachStderr(true).exec();
            String execCreateCmdResponseId = execCreateCmdResponse.getId();

            AtomicBoolean hasError = new AtomicBoolean(false);
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    String payload = new String(frame.getPayload()); // 获取程序输出
                    if (StreamType.STDOUT.equals(streamType)) {
                        if (executeMessage.getOutput() == null) executeMessage.setOutput("");
                        executeMessage.setOutput(executeMessage.getOutput() + payload);
                    } else {
                        hasError.set(true);
                        executeMessage.setErrorMessage(payload);
                    }
                    super.onNext(frame);
                }
            };

            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            boolean isCompleted = dockerClient.execStartCmd(execCreateCmdResponseId).withStdIn(new ByteArrayInputStream(input.getBytes())).exec(execStartResultCallback).awaitCompletion(Time_Out, TimeUnit.MILLISECONDS);// 超时控制
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
            executeMessage.setMemory(maxMemoryUsage.get());

            // 关闭统计回调
            try {
                statsCallback.close();
            } catch (IOException e) {
                System.err.println("关闭统计回调失败: " + e.getMessage());
            }

            if (!isCompleted) {
                executeMessage.setExecuteStatus(ExecuteStatus.TIMEOUT);
                executeMessage.setErrorMessage("运行超时");
                return executeMessage;
            }

            InspectExecResponse inspectResponse = dockerClient.inspectExecCmd(execCreateCmdResponseId).exec();
            if (inspectResponse.isRunning()) { // 理论上不应该发生，因为awaitCompletion已经返回
                executeMessage.setExecuteStatus(ExecuteStatus.DOCKER_ERROR);
                executeMessage.setErrorMessage("进程仍在运行");
            } else {
                int exitCode = inspectResponse.getExitCode();
                executeMessage.setProcessExitCode(exitCode);
                if (exitCode == 0 && !hasError.get()) {
                    executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);
                } else {
                    executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
                    if (executeMessage.getErrorMessage() == null) {
                        executeMessage.setErrorMessage("Process exited with code " + exitCode);
                    }
                }
            }
        } catch (InterruptedException e) {
            executeMessage.setExecuteStatus(ExecuteStatus.INTERRUPTED);
            executeMessage.setErrorMessage("Execution interrupted");
        } catch (Exception e) {
            executeMessage.setExecuteStatus(ExecuteStatus.DOCKER_ERROR);
            executeMessage.setErrorMessage("Docker error: " + e.getMessage());
        } finally {
            try {
                statsCallback.close();
            } catch (IOException e) {
                System.err.println("关闭统计回调失败: " + e.getMessage());
            }
        }
        return executeMessage;
    }

    /**
     * 拉取镜像
     *
     * @param dockerClient
     * @param image
     * @throws InterruptedException
     */
    public static void pullImage(DockerClient dockerClient, String image) {
        if (!Is_First_Init) return;
        Is_First_Init = false;

        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                super.onNext(item);
            }
        };
        try {
            pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("##########   成功下载镜像   ##########");
    }

}
