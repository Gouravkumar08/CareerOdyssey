package com.outreach.dsa.service;

import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.model.Question;
import com.outreach.dsa.model.SheetQuestion;
import com.outreach.dsa.repository.QuestionRepository;
import com.outreach.dsa.repository.SheetQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a topic's matching question id set given an optional difficulty filter and an optional
 * sheet scope. Shared by {@link TopicService} (for the live filtered-count endpoint) and
 * {@link PlannerService} (for goal progress) so this filtering logic lives in exactly one place.
 */
@Component
@RequiredArgsConstructor
public class QuestionScopeResolver {

    private final QuestionRepository questionRepository;
    private final SheetQuestionRepository sheetQuestionRepository;

    public Set<Long> resolve(Long topicId, List<Difficulty> difficulties, Long sheetId) {
        List<Question> questions = questionRepository.findByTopicId(topicId);
        Set<Long> ids = questions.stream()
                .filter(q -> difficulties == null || difficulties.isEmpty() || difficulties.contains(q.getDifficulty()))
                .map(Question::getId)
                .collect(Collectors.toSet());
        if (sheetId != null) {
            Set<Long> sheetIds = sheetQuestionRepository.findBySheetIdOrderByOrderIndexAsc(sheetId).stream()
                    .map(SheetQuestion::getQuestionId)
                    .collect(Collectors.toCollection(HashSet::new));
            ids.retainAll(sheetIds);
        }
        return ids;
    }
}
