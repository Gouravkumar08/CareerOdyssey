package com.outreach.dsa.repository;

import com.outreach.dsa.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<Question> findByTopicIdAndTitleIgnoreCase(Long topicId, String title);

    List<Question> findByIdIn(List<Long> ids);

    @Query("select q.topicId, count(q) from Question q group by q.topicId")
    List<Object[]> countTotalByTopic();

    @Query("select p.questionId from UserQuestionProgress p where p.userId = :userId and p.solved = true and p.solvedAt <= :asOf")
    List<Long> findSolvedQuestionIdsAsOf(@Param("userId") Long userId, @Param("asOf") Instant asOf);

    @Query("select q.id from Question q where q.topicId = :topicId")
    List<Long> findIdsByTopicId(@Param("topicId") Long topicId);

    List<Question> findByTopicId(Long topicId);
}
