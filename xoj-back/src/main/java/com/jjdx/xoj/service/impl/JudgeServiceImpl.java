package com.jjdx.xoj.service.impl;

import cn.hutool.json.JSONUtil;
import com.jjdx.xoj.common.ErrorCode;
import com.jjdx.xoj.exception.BusinessException;
import com.jjdx.xoj.judge.codesandbox.CodeSandbox;
import com.jjdx.xoj.judge.codesandbox.CodeSandboxFactory;
import com.jjdx.xoj.judge.codesandbox.CodeSandboxProxy;
import com.jjdx.xoj.judge.codesandbox.model.*;
import com.jjdx.xoj.model.dto.question.JudgeCase;
import com.jjdx.xoj.model.dto.question.JudgeConfig;
import com.jjdx.xoj.model.entity.Question;
import com.jjdx.xoj.model.entity.QuestionSubmit;
import com.jjdx.xoj.model.enums.JudgeInfoMessageEnum;
import com.jjdx.xoj.model.enums.QuestionSubmitStatusEnum;
import com.jjdx.xoj.service.JudgeService;
import com.jjdx.xoj.service.QuestionService;
import com.jjdx.xoj.service.QuestionSubmitService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {
    @Resource
    private QuestionService questionService;
    @Resource
    private QuestionSubmitService questionSubmitService;
    @Value("${codesandbox.defaultType}")
    String codeSandBoxType;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 获取并验证提交信息和题目信息
        QuestionSubmit questionSubmit = validateAndGetQuestionSubmit(questionSubmitId);
        Question question = validateAndGetQuestion(questionSubmit.getQuestionId());

        // 更新判题状态为运行中
        updateQuestionSubmitStatusToRunning(questionSubmitId);
        try {
            // 执行代码沙箱
            ExecuteCodeResponse executeCodeResponse = executeCodeSandbox(questionSubmit, question);
            // 处理判题结果
            return processJudgeResult(questionSubmitId, question, executeCodeResponse);
        } catch (Exception e) {
            // 处理系统异常
            handleJudgeException(questionSubmitId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题系统异常");
        }
    }

    private QuestionSubmit validateAndGetQuestionSubmit(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }

        Integer status = questionSubmit.getStatus();
        if (Objects.equals(status, QuestionSubmitStatusEnum.RUNNING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交正在判题中");
        }
        if (Objects.equals(status, QuestionSubmitStatusEnum.SUCCEED.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交已判题完毕");
        }

        return questionSubmit;
    }

    private Question validateAndGetQuestion(long questionId) {
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        return question;
    }

    private void updateQuestionSubmitStatusToRunning(long questionSubmitId) {
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING);
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
    }

    private ExecuteCodeResponse executeCodeSandbox(QuestionSubmit questionSubmit, Question question) {
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(question.getJudgeCaseList(), JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());

        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(codeSandBoxType);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder()
                .code(code).language(language).inputList(inputList).build();

        return codeSandbox.executeCode(executeCodeRequest);
    }

    private QuestionSubmit processJudgeResult(long questionSubmitId, Question question, ExecuteCodeResponse executeCodeResponse) {
        List<JudgeCase> judgeCaseList = JSONUtil.toList(question.getJudgeCaseList(), JudgeCase.class);
        List<String> outputList = judgeCaseList.stream().map(JudgeCase::getOutput).collect(Collectors.toList());
        JudgeConfig judgeConfig = JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class);

        List<JudgeInfo> judgeInfoList = handleExecuteResponse(executeCodeResponse, outputList, judgeConfig, judgeCaseList.size());

        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setJudgeInfoList(JSONUtil.toJsonStr(judgeInfoList));

        // 判断总体结果
        JudgeInfoMessageEnum judgeResult;
        String executeErrorMessageEnum = executeCodeResponse.getExecuteErrorMessageEnum();
        if (executeErrorMessageEnum != null) { // 如果有全局错误, 直接使用该错误作为最终结果
            ExecuteErrorMessageEnum errorEnum = ExecuteErrorMessageEnum.getEnumByValue(executeErrorMessageEnum);
            judgeResult = convertExecuteErrorToJudgeResult(errorEnum);
        } else {  // 否则按测试用例结果判断
            judgeResult = determineFinalResult(judgeInfoList);
        }
        questionSubmitUpdate.setJudgeResult(judgeResult);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED);

        // 更新判题结果
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        return questionSubmitService.getById(questionSubmitId);
    }

    private JudgeInfoMessageEnum convertExecuteErrorToJudgeResult(ExecuteErrorMessageEnum errorEnum) {
        if (errorEnum == null) {
            return JudgeInfoMessageEnum.SYSTEM_ERROR;
        }
        switch (errorEnum) {
            case COMPILE_ERROR:
                return JudgeInfoMessageEnum.COMPILE_ERROR;
            case DANGEROUS_CODE:
                return JudgeInfoMessageEnum.DANGEROUS_CODE;
            case SYSTEM_ERROR:
            default:
                return JudgeInfoMessageEnum.SYSTEM_ERROR;
        }
    }

    private List<JudgeInfo> handleExecuteResponse(ExecuteCodeResponse executeCodeResponse, List<String> outputList,
                                                  JudgeConfig judgeConfig, int caseCount) {
        List<JudgeInfo> judgeInfoList = new ArrayList<>();
        List<ExecuteCaseInfo> executeCaseInfoList = executeCodeResponse.getExecuteCaseInfoList();
        String executeError = executeCodeResponse.getExecuteErrorMessageEnum();
        String errorMessage = executeCodeResponse.getErrorMessage();

        if (executeError != null || StringUtils.isNotBlank(errorMessage)) {
            ExecuteErrorMessageEnum errorEnum = ExecuteErrorMessageEnum.getEnumByValue(executeError);
            handleGlobalErrorCases(judgeInfoList, errorEnum, errorMessage, executeCaseInfoList,
                    outputList, judgeConfig, caseCount);
        } else {
            handleNormalCases(judgeInfoList, executeCaseInfoList, outputList, judgeConfig, caseCount);
        }

        return judgeInfoList;
    }

    private void handleGlobalErrorCases(List<JudgeInfo> judgeInfoList, ExecuteErrorMessageEnum errorEnum,
                                        String errorMessage, List<ExecuteCaseInfo> executeCaseInfoList,
                                        List<String> outputList, JudgeConfig judgeConfig, int caseCount) {
        if (errorEnum == ExecuteErrorMessageEnum.DANGEROUS_CODE || errorEnum == ExecuteErrorMessageEnum.COMPILE_ERROR) {
            // 对于危险代码和编译错误,所有用例都未执行
            for (int i = 0; i < caseCount; i++) {
                JudgeInfo info = createJudgeInfoWithError(errorEnum.getValue(), errorMessage);
                judgeInfoList.add(info);
            }
        } else if (errorEnum == ExecuteErrorMessageEnum.SYSTEM_ERROR) {
            // 对于系统错误,部分用例可能已执行
            for (int i = 0; i < caseCount; i++) {
                JudgeInfo info = new JudgeInfo();
                if (i < executeCaseInfoList.size() && executeCaseInfoList.get(i) != null) {
                    ExecuteCaseInfo caseInfo = executeCaseInfoList.get(i);
                    info.setOutput(caseInfo.getOutput());
                    info.setTime(caseInfo.getTime());
                    info.setMemory(caseInfo.getMemory());
                    info.setErrorMessage(caseInfo.getErrorMessage());

                    JudgeInfoMessageEnum judgeCaseResult = determineCaseResult(
                            outputList, i, caseInfo.getErrorMessage(),
                            caseInfo.getOutput(), caseInfo.getTime(), judgeConfig, caseInfo.getMemory()
                    );
                    info.setJudgeCaseResult(judgeCaseResult);
                } else {
                    info.setJudgeCaseResult(JudgeInfoMessageEnum.SYSTEM_ERROR);
                    info.setErrorMessage("该用例未执行");
                }
                judgeInfoList.add(info);
            }
        }
    }

    private void handleNormalCases(List<JudgeInfo> judgeInfoList, List<ExecuteCaseInfo> executeCaseInfoList,
                                   List<String> outputList, JudgeConfig judgeConfig, int caseCount) {
        for (int i = 0; i < caseCount; i++) {
            JudgeInfo info = new JudgeInfo();

            if (i >= executeCaseInfoList.size()) {
                info.setJudgeCaseResult(JudgeInfoMessageEnum.SYSTEM_ERROR);
                info.setErrorMessage("该用例未执行");
                judgeInfoList.add(info);
                continue;
            }

            ExecuteCaseInfo caseInfo = executeCaseInfoList.get(i);
            info.setOutput(caseInfo.getOutput());
            info.setTime(caseInfo.getTime());
            info.setMemory(caseInfo.getMemory());
            info.setErrorMessage(caseInfo.getErrorMessage());

            JudgeInfoMessageEnum judgeCaseResult = determineCaseResult(
                    outputList, i, caseInfo.getErrorMessage(),
                    caseInfo.getOutput(), caseInfo.getTime(), judgeConfig, caseInfo.getMemory()
            );
            info.setJudgeCaseResult(judgeCaseResult.getValue());
            judgeInfoList.add(info);
        }
    }

    private JudgeInfo createJudgeInfoWithError(String errorType, String errorMessage) {
        JudgeInfo info = new JudgeInfo();
        info.setJudgeCaseResult(errorType);
        info.setErrorMessage(errorMessage);
        return info;
    }

    private void handleJudgeException(long questionSubmitId) {
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED);
        questionSubmitUpdate.setJudgeResult(JudgeInfoMessageEnum.SYSTEM_ERROR);
        questionSubmitService.updateById(questionSubmitUpdate);
    }

    private static JudgeInfoMessageEnum determineCaseResult(List<String> outputList, int i, String errorMessage,
                                                            String output, Long time, JudgeConfig judgeConfig, Long memory) {
        String stdOutput = outputList.get(i);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return JudgeInfoMessageEnum.RUNTIME_ERROR;
        } else if (!stdOutput.equals(output)) {
            return JudgeInfoMessageEnum.WRONG_ANSWER;
        } else if (time > judgeConfig.getTimeLimit()) {
            return JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
        } else if (memory > judgeConfig.getMemoryLimit()) {
            return JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
        } else {
            return JudgeInfoMessageEnum.ACCEPTED;
        }
    }

    private JudgeInfoMessageEnum determineFinalResult(List<JudgeInfo> judgeInfoList) {
        if (judgeInfoList == null || judgeInfoList.isEmpty()) {
            return JudgeInfoMessageEnum.SYSTEM_ERROR;
        }

        // 优先级从高到低排序
        for (JudgeInfo info : judgeInfoList) {
            String result = info.getJudgeCaseResult();
            if (JudgeInfoMessageEnum.RUNTIME_ERROR.getValue().equals(result)) {
                return JudgeInfoMessageEnum.RUNTIME_ERROR;
            }
            if (JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue().equals(result)) {
                return JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            }
            if (JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue().equals(result)) {
                return JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            }
            if (JudgeInfoMessageEnum.WRONG_ANSWER.getValue().equals(result)) {
                return JudgeInfoMessageEnum.WRONG_ANSWER;
            }
            if (JudgeInfoMessageEnum.SYSTEM_ERROR.getValue().equals(result)) {
                return JudgeInfoMessageEnum.SYSTEM_ERROR;
            }
        }
        // 所有用例都通过
        return JudgeInfoMessageEnum.ACCEPTED;
    }
}
