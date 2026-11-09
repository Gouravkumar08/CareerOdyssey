package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SdTopicRepository extends JpaRepository<SdTopic, Long> {
    Optional<SdTopic> findByNameIgnoreCase(String name);
}
