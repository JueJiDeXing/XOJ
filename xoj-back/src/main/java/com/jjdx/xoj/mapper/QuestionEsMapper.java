package com.jjdx.xoj.mapper;

import com.jjdx.xoj.model.entity.Question;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionEsMapper extends ElasticsearchRepository<Question, Long> {
    @Query("{\"match\": {\"title\": {\"query\": \"?0\"}}}")
    List<Question> findByTitle(String title);
}
