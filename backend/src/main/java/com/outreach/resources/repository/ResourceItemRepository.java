package com.outreach.resources.repository;

import com.outreach.resources.model.ResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceItemRepository extends JpaRepository<ResourceItem, Long> {
    Optional<ResourceItem> findByCategoryIdAndSubCategoryAndNameIgnoreCase(Long categoryId, String subCategory, String name);

    List<ResourceItem> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    List<ResourceItem> findByCategoryIdAndSubCategory(Long categoryId, String subCategory);
}
