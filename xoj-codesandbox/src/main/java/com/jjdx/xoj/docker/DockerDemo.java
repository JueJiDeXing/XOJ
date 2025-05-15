package com.jjdx.xoj.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.util.Arrays;
import java.util.List;

public class DockerDemo {
    public static void main(String[] args) throws Exception {
        // 创建客户端
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        String image = "nginx:latest";
        // 拉取镜像
//        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
//        PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
//            @Override
//            public void onNext(PullResponseItem item) {
//                System.out.println("下载镜像: " + item.getStatus());
//                super.onNext(item);
//            }
//        };
//        pullImageCmd.exec(pullImageResultCallback)
//                .awaitCompletion();
//        System.out.println("下载完成");
        // 创建容器
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image);
        CreateContainerResponse exec = createContainerCmd
                .withCmd("echo", "hello")
                .exec();
        System.out.println(exec);
        String id = exec.getId();
//        // 查看容器状态
        ListContainersCmd listContainersCmd = dockerClient.listContainersCmd();
        List<Container> list = listContainersCmd.withShowAll(true).exec();
        for (Container container : list) {
            System.out.println(container);
        }
        // 启动容器
        dockerClient.startContainerCmd(id).exec();//afcb5980ee95b6b4c387294d7b378b08f2bede0888eca3591f353240014cd126

        Thread.sleep(1000);
        // 查看日志
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                System.out.println("日志:" + Arrays.toString(item.getPayload()));
                super.onNext(item);
            }
        };
        dockerClient.logContainerCmd(id)
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();

        dockerClient.removeContainerCmd(id).withForce(true).exec();

        dockerClient.removeImageCmd(image) .exec();
    }

}
