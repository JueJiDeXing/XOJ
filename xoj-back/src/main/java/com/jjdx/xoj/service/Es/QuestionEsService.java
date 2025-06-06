package com.jjdx.xoj.service.Es;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jjdx.xoj.model.dto.question.QuestionQueryRequest;
import com.jjdx.xoj.model.entity.Question;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionEsService {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;


    public Page<Question> searchFromEs(QuestionQueryRequest questionQueryRequest) {
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 从ES查询
        List<Question> questionList = searchTitleAndContent(title, content);
        // 分页
        int from = (int) ((current - 1) * size);
        int to = (int) Math.min(from + size, questionList.size());
        Page<Question> page = new Page<>(current, size);
        page.setRecords(questionList.subList(from, to));
        page.setTotal(questionList.size());
        return page;
    }

    public void saveQuestionEs(Question question) {
        elasticsearchRestTemplate.save(question);
    }

    public void deleteQuestionEs(Long id) {
        elasticsearchRestTemplate.delete(id.toString(), Question.class);
    }

    public List<Question> searchTitleAndContent(String title, String content) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (title != null) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("title", title));
        }
        if (content != null) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("content", content));
        }
        if (!boolQueryBuilder.hasClauses()) {
            boolQueryBuilder.must(QueryBuilders.matchAllQuery());
        }
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();
        SearchHits<Question> searchHits = elasticsearchRestTemplate.search(searchQuery, Question.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

    public void saveAllQuestionsEs(List<Question> questions) {
        elasticsearchRestTemplate.save(questions);
    }
}
