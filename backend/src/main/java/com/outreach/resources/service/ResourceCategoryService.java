package com.outreach.resources.service;

import com.outreach.resources.dto.ResourceCategoryResponse;
import com.outreach.resources.dto.ResourceSubCategoryResponse;
import com.outreach.resources.exception.ResourceNotFoundException;
import com.outreach.resources.model.ResourceCategory;
import com.outreach.resources.model.ResourceItem;
import com.outreach.resources.repository.ResourceCategoryRepository;
import com.outreach.resources.repository.ResourceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceCategoryService {

    private final ResourceCategoryRepository categoryRepository;
    private final ResourceItemRepository itemRepository;

    @Transactional
    public ResourceCategory findOrCreate(String name) {
        String trimmed = name.trim();
        return categoryRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> categoryRepository.save(ResourceCategory.builder()
                        .name(trimmed)
                        .orderIndex((int) categoryRepository.count())
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<ResourceCategoryResponse> list() {
        return categoryRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(c -> ResourceCategoryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .orderIndex(c.getOrderIndex())
                        .itemCount(itemRepository.countByCategoryId(c.getId()))
                        .build())
                .toList();
    }

    @Transactional
    public ResourceCategoryResponse rename(Long id, String name) {
        ResourceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setName(name.trim());
        ResourceCategory saved = categoryRepository.save(category);
        return ResourceCategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .orderIndex(saved.getOrderIndex())
                .itemCount(itemRepository.countByCategoryId(saved.getId()))
                .build();
    }

    @Transactional
    public void deleteCascade(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found: " + id);
        }
        List<ResourceItem> items = itemRepository.findByCategoryId(id);
        if (!items.isEmpty()) {
            itemRepository.deleteAll(items);
        }
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ResourceSubCategoryResponse> listSubCategories(Long categoryId) {
        List<ResourceItem> items = itemRepository.findByCategoryId(categoryId);
        return items.stream()
                .filter(i -> StringUtils.hasText(i.getSubCategory()))
                .collect(Collectors.groupingBy(ResourceItem::getSubCategory, Collectors.counting()))
                .entrySet().stream()
                .map(e -> ResourceSubCategoryResponse.builder().name(e.getKey()).itemCount(e.getValue()).build())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional
    public void renameSubCategory(Long categoryId, String oldName, String newName) {
        List<ResourceItem> items = itemRepository.findByCategoryIdAndSubCategory(categoryId, oldName.trim());
        String trimmedNew = newName.trim();
        items.forEach(i -> i.setSubCategory(trimmedNew));
        itemRepository.saveAll(items);
    }

    @Transactional
    public void deleteSubCategoryCascade(Long categoryId, String subCategory) {
        List<ResourceItem> items = itemRepository.findByCategoryIdAndSubCategory(categoryId, subCategory.trim());
        if (!items.isEmpty()) {
            itemRepository.deleteAll(items);
        }
    }

    @Transactional
    public void reorder(List<Long> orderedCategoryIds) {
        List<ResourceCategory> categories = categoryRepository.findAllById(orderedCategoryIds);
        java.util.Map<Long, ResourceCategory> byId = new java.util.HashMap<>();
        categories.forEach(c -> byId.put(c.getId(), c));
        for (int i = 0; i < orderedCategoryIds.size(); i++) {
            ResourceCategory category = byId.get(orderedCategoryIds.get(i));
            if (category != null) {
                category.setOrderIndex(i);
            }
        }
        categoryRepository.saveAll(categories);
    }

    @Transactional(readOnly = true)
    public ResourceCategory findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
