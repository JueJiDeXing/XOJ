package com.jjdx.xoj.schedule;

import com.jjdx.xoj.model.entity.Question;
import com.jjdx.xoj.service.Es.QuestionEsService;
import com.jjdx.xoj.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 同步ElasticSearch
 */
@Service
@Slf4j
public class QuestionEsSyncSchedule {

    @Resource
    private QuestionService questionService;
    @Resource
    private QuestionEsService questionEsService;

    /**
     增量同步 - 每10分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void incrementalSync() {
        Date tenMinutesAgo = new Date(System.currentTimeMillis() - 11 * 60 * 1000);
        List<Question> questions = questionService.listQuestionsAfterUpdateTime(tenMinutesAgo);
        if (!questions.isEmpty()) {
            questionEsService.saveAllQuestionsEs(questions);
            log.info("Incremental sync completed, {} questions synchronized", questions.size());
        }
    }

    /**
     全量同步 - 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void fullSync() {
        List<Question> questions = questionService.list();
        if (!questions.isEmpty()) {
            questionEsService.saveAllQuestionsEs(questions);
            log.info("Full sync completed, {} questions synchronized", questions.size());
        }
    }
}
