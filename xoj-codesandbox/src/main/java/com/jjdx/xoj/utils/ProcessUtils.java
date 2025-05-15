package com.jjdx.xoj.utils;

import com.jjdx.xoj.enums.ExecuteStatus;
import com.jjdx.xoj.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

import static com.jjdx.xoj.codeSandbox.JavaCodeSandboxTemplate.Time_Out;


public class ProcessUtils {

    public static ExecuteMessage runInterAndGetMessage(Process process, String input) throws Exception {
        ExecuteMessage executeMessage = new ExecuteMessage();

        StopWatch stopWatch = new StopWatch();// 计时器
        OutputStream outputStream = process.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        outputStreamWriter.write(input);
        outputStreamWriter.flush();
        outputStreamWriter.close();
        outputStream.close();

        Future<String> outputFuture = readStreamAsync(process.getInputStream());
        Future<String> errorFuture = readStreamAsync(process.getErrorStream());

        // 等待进程结束或超时
        stopWatch.start();
        boolean exited = process.waitFor(Time_Out, TimeUnit.MILLISECONDS);
        if (!exited) {
            process.destroyForcibly();
            executeMessage.setProcessExitCode(-1);
            executeMessage.setErrorMessage("超时");
            executeMessage.setExecuteStatus(ExecuteStatus.TIMEOUT);
            return executeMessage;
        }
        stopWatch.stop();
        // 获取结果
        String output = outputFuture.get();
        String error = errorFuture.get();
        int exitCode = process.exitValue();
        executeMessage.setProcessExitCode(exitCode);
        executeMessage.setOutput(output);
        executeMessage.setErrorMessage(error);
        executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);
        executeMessage.setMemory(0L);
        return executeMessage;
    }


    private static Future<String> readStreamAsync(InputStream inputStream) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
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
            return executor.submit(stringCallable);
        } finally {
            executor.shutdown();  // 关闭线程池
        }
    }

    public static ExecuteMessage runAndGetMessage(Process process) throws Exception {
        ExecuteMessage executeMessage = new ExecuteMessage();
        int exitCode = process.waitFor();// 等待
        executeMessage.setProcessExitCode(exitCode);
        if (exitCode == 0) {// 正常退出
            String output = getOutput(process.getInputStream());
            executeMessage.setOutput(output);
            executeMessage.setExecuteStatus(ExecuteStatus.SUCCESS);
        } else {
            String errorMessage = getOutput(process.getErrorStream());
            executeMessage.setErrorMessage(errorMessage);
            executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
        }
        return executeMessage;
    }

    static String getOutput(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder ans = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            ans.append(line).append("\n");
        }
        return ans.toString();
    }
}
