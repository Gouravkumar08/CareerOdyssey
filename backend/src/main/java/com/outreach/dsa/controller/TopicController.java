package com.outreach.dsa.controller;

import com.outreach.dsa.dto.TopicQuestionCountResponse;
import com.outreach.dsa.dto.TopicResponse;
import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.repository.QuestionRepository;
import com.outreach.dsa.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dsa/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final QuestionRepository questionRepository;

    @GetMapping
    public List<TopicResponse> list() {
        return topicService.findAllOrdered().stream()
                .map(t -> new TopicResponse(t.getId(), t.getName(), questionRepository.findIdsByTopicId(t.getId()).size()))
                .toList();
    }

    @GetMapping("/{id}/question-count")
    public TopicQuestionCountResponse questionCount(@PathVariable Long id,
                                                      @RequestParam(required = false) List<Difficulty> difficulties,
                                                      @RequestParam(required = false) Long sheetId) {
        return new TopicQuestionCountResponse(topicService.countQuestions(id, difficulties, sheetId));
    }
}
