package com.jjdx.xoj.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 创建请求
 */
@Data
public class QuestionAddRequest implements Serializable {


    /**
     标题
     */
    private String title;

    /**
     内容
     */
    private String content;

    /**
     标签
     */
    private List<String> tags;

    /**
     标准答案
     */
    private String answer;

    /**
     判题用例
     */
    private List<JudgeCase> judgeCaseList;

    /**
     判题配置
     */
    private  JudgeConfig  judgeConfig;

    private static final long serialVersionUID = 1L;
}
