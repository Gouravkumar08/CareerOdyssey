package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.UserSdItemProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSdItemProgressRepository extends JpaRepository<UserSdItemProgress, Long> {
    Optional<UserSdItemProgress> findByUserIdAndItemId(Long userId, Long itemId);

    List<UserSdItemProgress> findByUserIdAndItemIdIn(Long userId, List<Long> itemIds);

    List<UserSdItemProgress> findByUserIdAndCompletedTrueAndCompletedAtBetween(Long userId, Instant from, Instant to);

    List<UserSdItemProgress> findByUserIdAndCompletedTrue(Long userId);

    List<UserSdItemProgress> findByUserIdAndCompletedTrueAndCompletedAtAfterOrderByCompletedAtDesc(Long userId, Instant after);

    void deleteByItemIdIn(List<Long> itemIds);
}
