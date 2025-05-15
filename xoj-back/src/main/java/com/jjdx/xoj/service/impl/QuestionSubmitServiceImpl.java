package com.jjdx.xoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.xoj.common.ErrorCode;
import com.jjdx.xoj.constant.CommonConstant;
import com.jjdx.xoj.exception.BusinessException;
import com.jjdx.xoj.service.JudgeService;
import com.jjdx.xoj.mapper.QuestionSubmitMapper;
import com.jjdx.xoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.jjdx.xoj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.jjdx.xoj.model.entity.Question;
import com.jjdx.xoj.model.entity.QuestionSubmit;
import com.jjdx.xoj.model.entity.User;
import com.jjdx.xoj.model.enums.JudgeInfoMessageEnum;
import com.jjdx.xoj.model.enums.QuestionSubmitLanguageEnum;
import com.jjdx.xoj.model.enums.QuestionSubmitStatusEnum;
import com.jjdx.xoj.model.vo.QuestionSubmitVO;
import com.jjdx.xoj.service.QuestionService;
import com.jjdx.xoj.service.QuestionSubmitService;
import com.jjdx.xoj.service.UserService;
import com.jjdx.xoj.utils.SqlUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 @author 34084
 @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 @createDate 2025-03-29 15:18:50 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;
    @Resource
    private UserService userService;
    @Resource
    @Lazy
    private JudgeService judgeService;

    /**
     提交题目

     @param questionSubmitAddRequest
     @param loginUser
     @return 提交记录id
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        String code = questionSubmitAddRequest.getCode();

        Long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setUserId(userId);
        questionSubmit.setCode(code);
        questionSubmit.setLanguage(language);
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING );
        questionSubmit.setJudgeInfoList("[]"); // 初始化空判题结果
        questionSubmit.setJudgeResult(JudgeInfoMessageEnum.WAITING );
        boolean result = save(questionSubmit);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目提交失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        CompletableFuture.runAsync(() -> judgeService.doJudge(questionSubmitId));// 调用判题服务
        return questionSubmitId;
    }

    /**
     获取查询包装类

     @param questionSubmitQueryRequest
     @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        String judgeResult = questionSubmitQueryRequest.getJudgeResult();

        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(QuestionSubmitStatusEnum.isValid(status), "status", status);
        queryWrapper.eq(StringUtils.isNotBlank(judgeResult), "judgeResult", judgeResult);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField),
                sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(Long questionSubmitId) {
        QuestionSubmit byId = getById(questionSubmitId);
        return QuestionSubmitVO.objToVo(byId);
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(
                questionSubmitPage.getCurrent(),
                questionSubmitPage.getSize(),
                questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(QuestionSubmitVO::objToVo)
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
}




