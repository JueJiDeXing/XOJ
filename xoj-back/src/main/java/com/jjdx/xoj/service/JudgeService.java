package com.jjdx.xoj.service;

import com.jjdx.xoj.model.entity.QuestionSubmit;

public interface JudgeService {
    /**
     判题服务
     */
    QuestionSubmit doJudge(long questionSubmitId);
}
