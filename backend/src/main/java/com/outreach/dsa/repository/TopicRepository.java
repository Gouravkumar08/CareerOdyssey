package com.outreach.dsa.repository;

import com.outreach.dsa.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByNameIgnoreCase(String name);

    List<Topic> findAllByOrderByOrderIndexAsc();
}
