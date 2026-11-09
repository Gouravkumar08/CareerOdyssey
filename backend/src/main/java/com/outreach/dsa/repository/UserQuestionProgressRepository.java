package com.outreach.dsa.repository;

import com.outreach.dsa.model.UserQuestionProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserQuestionProgressRepository extends JpaRepository<UserQuestionProgress, Long> {
    Optional<UserQuestionProgress> findByUserIdAndQuestionId(Long userId, Long questionId);

    List<UserQuestionProgress> findByUserIdAndQuestionIdIn(Long userId, List<Long> questionIds);

    List<UserQuestionProgress> findByUserIdAndSolvedTrueAndSolvedAtBetween(Long userId, Instant from, Instant to);

    List<UserQuestionProgress> findByUserIdAndSolvedTrue(Long userId);

    List<UserQuestionProgress> findByUserIdAndSolvedTrueAndSolvedAtAfterOrderBySolvedAtDesc(Long userId, Instant after);
}
