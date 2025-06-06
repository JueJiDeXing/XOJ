package com.jjdx.xoj.utils;

import com.jjdx.xoj.enums.ExecuteStatus;
import com.jjdx.xoj.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

import static com.jjdx.xoj.codeSandbox.JavaCodeSandboxTemplate.Time_Out;

public class JavaProcessUtil {
    private JavaProcessUtil() {}

    /**
     执行Java程序, 并获取执行信息

     @param process
     @param input
     @return
     @throws Exception
     */
    public static ExecuteMessage runInterAndGetMessage(Process process, String input) throws Exception {
        ExecuteMessage executeMessage = new ExecuteMessage();

        StopWatch stopWatch = new StopWatch();
        // 输入测试用例
        OutputStream outputStream = process.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        outputStreamWriter.write(input);
        outputStreamWriter.flush();
        outputStreamWriter.close();
        outputStream.close();

        Future<String> outputFuture = readStreamAsync(process.getInputStream());
        Future<String> errorFuture = readStreamAsync(process.getErrorStream());

        // 内存监控
        MemoryMonitor memoryMonitor = new MemoryMonitor(process);
        Future<Long> memoryFuture = memoryMonitor.startMonitoring();
        stopWatch.start();
        boolean exited = process.waitFor(Time_Out, TimeUnit.MILLISECONDS);
        if (!exited) {// 执行超时
            process.destroyForcibly();
            executeMessage.setProcessExitCode(-1);
            executeMessage.setErrorMessage("超时");
            executeMessage.setExecuteStatus(ExecuteStatus.TIMEOUT);
            memoryMonitor.stopMonitoring();
            return executeMessage;
        }
        stopWatch.stop();
        // 收集执行信息
        String output = outputFuture.get();
        String error = errorFuture.get();
        Long memoryUsage = memoryFuture.get();
        int exitCode = process.exitValue();

        executeMessage.setProcessExitCode(exitCode);
        executeMessage.setOutput(output);
        executeMessage.setErrorMessage(error);
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        executeMessage.setMemory(memoryUsage != null ? memoryUsage : 0L);
        executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);

        memoryMonitor.stopMonitoring();
        return executeMessage;
    }


    /**
     编译Java文件

     @param process
     @return
     @throws Exception
     */
    public static ExecuteMessage runAndGetMessage(Process process) throws Exception {
        ExecuteMessage executeMessage = new ExecuteMessage();
        int exitCode = process.waitFor();
        executeMessage.setProcessExitCode(exitCode);
        if (exitCode == 0) {
            String output = readStream(process.getInputStream());
            executeMessage.setOutput(output);
            executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);
        } else {
            String errorMessage = readStream(process.getErrorStream());
            executeMessage.setErrorMessage(errorMessage);
            executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
        }
        return executeMessage;
    }

    /**
     获取输出

     @param inputStream
     @return
     @throws IOException
     */
    static String readStream(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder ans = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            ans.append(line).append("\n");
        }
        return ans.toString();
    }

    /**
     异步读取输出

     @param inputStream
     @return
     */
    static Future<String> readStreamAsync(InputStream inputStream) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<String> stringCallable = () -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
            )) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString().trim();
            }
        };
        try {
            return executor.submit(stringCallable);
        } finally {
            executor.shutdown();
        }
    }

}
