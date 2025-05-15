package com.jjdx.xoj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.xoj.common.BaseResponse;
import com.jjdx.xoj.common.ErrorCode;
import com.jjdx.xoj.common.ResultUtils;
import com.jjdx.xoj.exception.BusinessException;
import com.jjdx.xoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.jjdx.xoj.model.dto.questionsubmit.QuestionSubmitGetRequest;
import com.jjdx.xoj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.jjdx.xoj.model.entity.QuestionSubmit;
import com.jjdx.xoj.model.entity.User;
import com.jjdx.xoj.model.enums.UserRoleEnum;
import com.jjdx.xoj.model.vo.QuestionSubmitVO;
import com.jjdx.xoj.service.QuestionSubmitService;
import com.jjdx.xoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 題目提交接口
 */
@RestController
@RequestMapping("/deprecated/question_submit")
@Slf4j
@Deprecated
public class QuestionSubmitController {

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private UserService userService;

    /**
     提交代码

     @param questionSubmitAddRequest
     @param request
     @return submitId 提交id
     */
    @PostMapping("/")
    public BaseResponse<Long> doSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest, HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        final User loginUser = userService.getLoginUser(request);
        long submitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(submitId);
    }

    /**
     获取代码评测结果
     */
    @PostMapping("/get")
    public BaseResponse<QuestionSubmitVO> getQuestionSubmit(@RequestBody QuestionSubmitGetRequest questionSubmitGetRequest, HttpServletRequest request) {
        if (questionSubmitGetRequest == null || questionSubmitGetRequest.getQuestionSubmitId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long questionSubmitId = questionSubmitGetRequest.getQuestionSubmitId();
        QuestionSubmitVO questionSubmitVO = questionSubmitService.getQuestionSubmitVO(questionSubmitId);
        return ResultUtils.success(questionSubmitVO);
    }

    /**
     分页获取题目提交列表

     @return submitId 提交id
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest, HttpServletRequest request) {
        if (questionSubmitQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        String userRole = loginUser.getUserRole();
        if (!UserRoleEnum.ADMIN.getValue().equals(userRole)) {// 非管理员, 只能查询自己的提交
            questionSubmitQueryRequest.setUserId(loginUser.getId());
        }
        long current = questionSubmitQueryRequest.getCurrent();
        long pageSize = questionSubmitQueryRequest.getPageSize();
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(
                new Page<>(current, pageSize),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest)
        );

        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage));
    }

}
