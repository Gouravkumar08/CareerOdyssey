package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SdItemRepository extends JpaRepository<SdItem, Long> {
    Optional<SdItem> findByTopicIdAndTitleIgnoreCase(Long topicId, String title);

    List<SdItem> findByIdIn(List<Long> ids);

    List<SdItem> findByTopicId(Long topicId);

    long countByTopicId(Long topicId);

    List<SdItem> findByTopicIdAndSubTopic(Long topicId, String subTopic);

    @Query("select i.topicId, count(i) from SdItem i group by i.topicId")
    List<Object[]> countTotalByTopic();

    @Query("select p.itemId from UserSdItemProgress p where p.userId = :userId and p.completed = true and p.completedAt <= :asOf")
    List<Long> findCompletedItemIdsAsOf(@Param("userId") Long userId, @Param("asOf") Instant asOf);
}
