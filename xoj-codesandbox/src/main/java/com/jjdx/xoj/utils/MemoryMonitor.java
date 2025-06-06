package com.jjdx.xoj.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 内存监控
 */
public class MemoryMonitor {
    private final Process process;
    private volatile boolean running;
    private ExecutorService executor;
    private long maxMemoryUsed;

    public MemoryMonitor(Process process) {
        this.process = process;
        this.running = true;
        this.maxMemoryUsed = 0;
    }

    public Future<Long> startMonitoring() {
        executor = Executors.newSingleThreadExecutor();
        return executor.submit(this::monitorMemory);
    }

    /**
     停止监控
     */
    public void stopMonitoring() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     启动内存监控任务
     */
    private Long monitorMemory() {
        try {
            String pid = Long.toString(process.pid());
            while (running && process.isAlive()) {
                long memory = getProcessMemory(pid);
                if (memory > maxMemoryUsed) {
                    maxMemoryUsed = memory;
                }
                Thread.sleep(50);
            }
            return maxMemoryUsed;
        } catch (Exception e) {
            return maxMemoryUsed;
        }
    }

    /**
     获取进程占用的内存
     * @param pid 进程id
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private long getProcessMemory(String pid) throws IOException, InterruptedException {
        if (CheckEnvironment.isWindowsEnvironment()) { // Windows系统使用wmic
            ProcessBuilder builder = new ProcessBuilder(
                    "wmic", "process", "where", "processid=" + pid, "get", "WorkingSetSize"
            );
            Process p = builder.start();
            p.waitFor();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) {
                        return Long.parseLong(line);
                    }
                }
            }
        } else {// Linux/Mac系统使用ps
            ProcessBuilder builder = new ProcessBuilder(
                    "ps", "-o", "rss=", "-p", pid
            );
            Process p = builder.start();
            p.waitFor();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        return Long.parseLong(line) * 1024;// 转换为字节
                    }
                }
            }
        }
        return 0;
    }
}
