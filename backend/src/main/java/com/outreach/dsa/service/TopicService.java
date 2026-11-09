package com.outreach.dsa.service;

import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.model.Topic;
import com.outreach.dsa.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuestionScopeResolver questionScopeResolver;

    @Transactional(readOnly = true)
    public List<Topic> findAllOrdered() {
        return topicRepository.findAllByOrderByOrderIndexAsc();
    }

    @Transactional
    public Topic findOrCreate(String name) {
        String trimmed = name.trim();
        return topicRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> topicRepository.save(Topic.builder()
                        .name(trimmed)
                        .orderIndex((int) topicRepository.count())
                        .build()));
    }

    @Transactional(readOnly = true)
    public int countQuestions(Long topicId, List<Difficulty> difficulties, Long sheetId) {
        return questionScopeResolver.resolve(topicId, difficulties, sheetId).size();
    }
}
