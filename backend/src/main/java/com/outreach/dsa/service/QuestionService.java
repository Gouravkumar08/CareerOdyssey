package com.outreach.dsa.service;

import com.outreach.dsa.dto.DifficultyStats;
import com.outreach.dsa.dto.ProgressUpdateRequest;
import com.outreach.dsa.dto.QuestionRequest;
import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.dsa.dto.SheetStatsResponse;
import com.outreach.dsa.exception.ResourceNotFoundException;
import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.model.Question;
import com.outreach.dsa.model.SheetQuestion;
import com.outreach.dsa.model.Topic;
import com.outreach.dsa.model.UserQuestionProgress;
import com.outreach.dsa.repository.QuestionRepository;
import com.outreach.dsa.repository.SheetQuestionRepository;
import com.outreach.dsa.repository.TopicRepository;
import com.outreach.dsa.repository.UserQuestionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final SheetQuestionRepository sheetQuestionRepository;
    private final UserQuestionProgressRepository progressRepository;
    private final TopicService topicService;

    @Transactional(readOnly = true)
    public List<QuestionResponse> listForSheet(Long sheetId, Long userId, String topicName, Difficulty difficulty,
                                                Boolean important, Boolean revision, Boolean solved, String search) {
        List<SheetQuestion> links = sheetQuestionRepository.findBySheetIdOrderByOrderIndexAsc(sheetId);
        List<Long> questionIds = links.stream().map(SheetQuestion::getQuestionId).toList();
        if (questionIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Question> questionsById = new HashMap<>();
        questionRepository.findByIdIn(questionIds).forEach(q -> questionsById.put(q.getId(), q));

        Map<Long, Topic> topicsById = new HashMap<>();
        topicRepository.findAllById(questionsById.values().stream().map(Question::getTopicId).distinct().toList())
                .forEach(t -> topicsById.put(t.getId(), t));

        Map<Long, UserQuestionProgress> progressByQuestionId = new HashMap<>();
        progressRepository.findByUserIdAndQuestionIdIn(userId, questionIds)
                .forEach(p -> progressByQuestionId.put(p.getQuestionId(), p));

        String needle = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;

        return links.stream()
                .map(link -> questionsById.get(link.getQuestionId()))
                .filter(q -> q != null)
                .filter(q -> topicName == null || topicsById.get(q.getTopicId()) != null
                        && topicsById.get(q.getTopicId()).getName().equalsIgnoreCase(topicName))
                .filter(q -> difficulty == null || q.getDifficulty() == difficulty)
                .filter(q -> important == null || q.isImportant() == important)
                .filter(q -> {
                    if (revision == null) return true;
                    UserQuestionProgress p = progressByQuestionId.get(q.getId());
                    return revision == (p != null && p.isRevisionStarred());
                })
                .filter(q -> {
                    if (solved == null) return true;
                    UserQuestionProgress p = progressByQuestionId.get(q.getId());
                    return solved == (p != null && p.isSolved());
                })
                .filter(q -> needle == null
                        || q.getTitle().toLowerCase().contains(needle)
                        || (q.getSubTopic() != null && q.getSubTopic().toLowerCase().contains(needle))
                        || topicsById.get(q.getTopicId()).getName().toLowerCase().contains(needle))
                .map(q -> QuestionResponse.from(q, topicsById.get(q.getTopicId()).getName(), progressByQuestionId.get(q.getId())))
                .toList();
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionRequest request, Long userId) {
        Question question = findEntity(id);
        Topic topic = topicService.findOrCreate(request.getTopicName());
        question.setTopicId(topic.getId());
        question.setSubTopic(request.getSubTopic());
        question.setTitle(request.getTitle());
        question.setDifficulty(request.getDifficulty());
        question.setImportant(request.isImportant());
        question.setPracticeLink(request.getPracticeLink());
        question.setYoutubeLink(request.getYoutubeLink());
        question.setArticleLink(request.getArticleLink());
        Question saved = questionRepository.save(question);
        UserQuestionProgress progress = progressRepository.findByUserIdAndQuestionId(userId, id).orElse(null);
        return QuestionResponse.from(saved, topic.getName(), progress);
    }

    @Transactional
    public QuestionResponse updateProgress(Long id, Long userId, ProgressUpdateRequest request) {
        Question question = findEntity(id);
        Topic topic = topicRepository.findById(question.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + question.getTopicId()));

        UserQuestionProgress progress = progressRepository.findByUserIdAndQuestionId(userId, id)
                .orElseGet(() -> UserQuestionProgress.builder()
                        .userId(userId)
                        .questionId(id)
                        .solved(false)
                        .revisionStarred(false)
                        .build());

        if (request.getSolved() != null) {
            progress.setSolved(request.getSolved());
            progress.setSolvedAt(request.getSolved() ? Instant.now() : null);
        }
        if (request.getRevisionStarred() != null) {
            progress.setRevisionStarred(request.getRevisionStarred());
        }
        if (request.getNote() != null) {
            progress.setNote(request.getNote());
        }
        if (request.getGithubSolutionLink() != null) {
            progress.setGithubSolutionLink(request.getGithubSolutionLink());
        }

        UserQuestionProgress saved = progressRepository.save(progress);
        return QuestionResponse.from(question, topic.getName(), saved);
    }

    @Transactional(readOnly = true)
    public SheetStatsResponse stats(Long sheetId, Long userId) {
        List<Long> questionIds = sheetQuestionRepository.findBySheetIdOrderByOrderIndexAsc(sheetId).stream()
                .map(SheetQuestion::getQuestionId)
                .toList();
        List<Question> questions = questionRepository.findByIdIn(questionIds);
        Map<Long, UserQuestionProgress> progressByQuestionId = new HashMap<>();
        progressRepository.findByUserIdAndQuestionIdIn(userId, questionIds)
                .forEach(p -> progressByQuestionId.put(p.getQuestionId(), p));

        long topicsCount = questions.stream().map(Question::getTopicId).distinct().count();
        long solvedTotal = questions.stream().filter(q -> isSolved(q, progressByQuestionId)).count();
        long importantTotal = questions.stream().filter(Question::isImportant).count();
        long importantSolved = questions.stream().filter(Question::isImportant).filter(q -> isSolved(q, progressByQuestionId)).count();

        return SheetStatsResponse.builder()
                .totalQuestions(questions.size())
                .solvedQuestions(solvedTotal)
                .overallPercent(questions.isEmpty() ? 0 : (int) Math.round(solvedTotal * 100.0 / questions.size()))
                .topicsCount(topicsCount)
                .importantTotal(importantTotal)
                .importantSolved(importantSolved)
                .easy(difficultyStats(questions, progressByQuestionId, Difficulty.EASY))
                .medium(difficultyStats(questions, progressByQuestionId, Difficulty.MEDIUM))
                .hard(difficultyStats(questions, progressByQuestionId, Difficulty.HARD))
                .build();
    }

    private DifficultyStats difficultyStats(List<Question> questions, Map<Long, UserQuestionProgress> progressByQuestionId, Difficulty difficulty) {
        long total = questions.stream().filter(q -> q.getDifficulty() == difficulty).count();
        long solved = questions.stream().filter(q -> q.getDifficulty() == difficulty).filter(q -> isSolved(q, progressByQuestionId)).count();
        return DifficultyStats.builder().total(total).solved(solved).build();
    }

    private boolean isSolved(Question question, Map<Long, UserQuestionProgress> progressByQuestionId) {
        UserQuestionProgress p = progressByQuestionId.get(question.getId());
        return p != null && p.isSolved();
    }

    @Transactional(readOnly = true)
    public Question findEntity(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + id));
    }
}
