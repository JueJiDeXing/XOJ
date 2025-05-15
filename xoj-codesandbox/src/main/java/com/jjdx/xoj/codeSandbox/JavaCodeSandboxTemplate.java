package com.jjdx.xoj.codeSandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.jjdx.xoj.enums.ExecuteErrorMessageEnum;
import com.jjdx.xoj.enums.ExecuteStatus;
import com.jjdx.xoj.model.ExecuteCaseInfo;
import com.jjdx.xoj.model.ExecuteCodeRequest;
import com.jjdx.xoj.model.ExecuteCodeResponse;
import com.jjdx.xoj.model.ExecuteMessage;
import com.jjdx.xoj.utils.JavaCodeValidator;
import com.jjdx.xoj.utils.ProcessUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    static final String Temp_Code_Dir_Name = "tempCode";
    static final String Java_File_Name = "Main.java";
    public static final Long Time_Out = 5 * 1000L;

    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String tempCodeDir = userDir + File.separator + Temp_Code_Dir_Name;
        if (!FileUtil.exist(tempCodeDir)) {
            FileUtil.mkdir(tempCodeDir);
        }
        String userCodeDir = tempCodeDir + File.separator + UUID.randomUUID();
        String userCodePath = userCodeDir + File.separator + Java_File_Name;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsoluteFile());
        try {
            Process complieProcess = Runtime.getRuntime().exec(compileCmd);
            return ProcessUtils.runAndGetMessage(complieProcess);
        } catch (Exception e) {
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
            executeMessage.setProcessExitCode(-1);
            executeMessage.setErrorMessage(e.getMessage());
            return executeMessage;
        }
    }

    public List<ExecuteMessage> runFile(List<String> inputList, File userCodeFile) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            String memoryCmd = "-Xmx256m";
            String encodingCmd = "-Dfile.encoding=UTF-8";
            String runCmd = "java" + " " + memoryCmd + " " + encodingCmd
                    + " " + "-cp" + " " + userCodeFile.getParentFile().getAbsolutePath() + " " + "Main";
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                ExecuteMessage executeMessage = ProcessUtils.runInterAndGetMessage(runProcess, inputArgs);
                executeMessageList.add(executeMessage);// 运行成功(但是可能会出现超时超内存等问题)
            } catch (Exception e) {// 运行失败
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setExecuteStatus(ExecuteStatus.EXITED);
                executeMessage.setProcessExitCode(-1);
                executeMessage.setErrorMessage(e.getMessage());
                executeMessageList.add(executeMessage);
            }
        }
        return executeMessageList;
    }

    public List<ExecuteCaseInfo> organizeInformation(List<ExecuteMessage> executeMessageList) {
        List<ExecuteCaseInfo> executeCaseInfoList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            ExecuteCaseInfo executeCaseInfo = new ExecuteCaseInfo();
            executeCaseInfo.setOutput(executeMessage.getOutput());
            executeCaseInfo.setTime(executeMessage.getTime());
            executeCaseInfo.setMemory(executeMessage.getMemory());
            executeCaseInfo.setErrorMessage(executeMessage.getErrorMessage());
            executeCaseInfoList.add(executeCaseInfo);
        }
        return executeCaseInfoList;
    }

    public boolean clean(File userCodeFile) {
        if (userCodeFile.getParentFile().exists()) {
            return FileUtil.del(userCodeFile.getParentFile());
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        String code = executeCodeRequest.getCode();
        // 1. 校验
        boolean isValid = JavaCodeValidator.validateCode(code);
        if (!isValid) {
            executeCodeResponse.setExecuteErrorMessageEnum(ExecuteErrorMessageEnum.DANGEROUS_CODE.getValue());
            executeCodeResponse.setErrorMessage("代码校验不通过");
            return executeCodeResponse;
        }
        // 2. 保存用户代码
        File userCodeFile = saveCodeToFile(code);
        // 3. 编译
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        if (!ExecuteStatus.SUCCESS.equals(compileFileExecuteMessage.getExecuteStatus())) {
            executeCodeResponse.setExecuteErrorMessageEnum(ExecuteErrorMessageEnum.COMPILE_ERROR.getValue());
            executeCodeResponse.setErrorMessage("编译失败");
            return executeCodeResponse;
        }
        // 4. 运行
        List<String> inputList = executeCodeRequest.getInputList();
        List<ExecuteMessage> executeMessageList = runFile(inputList, userCodeFile);
        // 5. 收集结果
        List<ExecuteCaseInfo> executeCaseInfoList = organizeInformation(executeMessageList);
        executeCodeResponse.setExecuteCaseInfoList(executeCaseInfoList);
        // 6. 文件清理
        boolean del = clean(userCodeFile);
        if (!del) System.out.println("文件清理失败");
        return executeCodeResponse;
    }


}
