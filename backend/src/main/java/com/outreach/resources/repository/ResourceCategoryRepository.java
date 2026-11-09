package com.outreach.resources.repository;

import com.outreach.resources.model.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {
    Optional<ResourceCategory> findByNameIgnoreCase(String name);
}
