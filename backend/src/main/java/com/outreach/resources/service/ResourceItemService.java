package com.outreach.resources.service;

import com.outreach.resources.dto.ResourceItemRequest;
import com.outreach.resources.dto.ResourceItemResponse;
import com.outreach.resources.exception.ResourceNotFoundException;
import com.outreach.resources.model.ResourceCategory;
import com.outreach.resources.model.ResourceItem;
import com.outreach.resources.repository.ResourceCategoryRepository;
import com.outreach.resources.repository.ResourceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResourceItemService {

    private final ResourceItemRepository itemRepository;
    private final ResourceCategoryRepository categoryRepository;
    private final ResourceCategoryService categoryService;

    @Transactional(readOnly = true)
    public List<ResourceItemResponse> list(String category, String subCategory, Boolean important, String search) {
        List<ResourceItem> items = itemRepository.findAll();

        Map<Long, ResourceCategory> categoriesById = new HashMap<>();
        categoryRepository.findAll().forEach(c -> categoriesById.put(c.getId(), c));

        String needle = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;

        return items.stream()
                .filter(i -> category == null || (categoriesById.get(i.getCategoryId()) != null
                        && categoriesById.get(i.getCategoryId()).getName().equalsIgnoreCase(category)))
                .filter(i -> subCategory == null || subCategory.equalsIgnoreCase(i.getSubCategory()))
                .filter(i -> important == null || i.isImportant() == important)
                .filter(i -> needle == null
                        || i.getName().toLowerCase().contains(needle)
                        || (i.getSubCategory() != null && i.getSubCategory().toLowerCase().contains(needle))
                        || (categoriesById.get(i.getCategoryId()) != null
                        && categoriesById.get(i.getCategoryId()).getName().toLowerCase().contains(needle)))
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(i -> ResourceItemResponse.from(i, categoryNameOf(i, categoriesById)))
                .toList();
    }

    private String categoryNameOf(ResourceItem item, Map<Long, ResourceCategory> categoriesById) {
        ResourceCategory category = categoriesById.get(item.getCategoryId());
        return category == null ? null : category.getName();
    }

    @Transactional
    public ResourceItemResponse create(ResourceItemRequest request) {
        ResourceCategory category = categoryService.findOrCreate(request.getCategoryName());
        ResourceItem item = ResourceItem.builder()
                .categoryId(category.getId())
                .subCategory(StringUtils.hasText(request.getSubCategory()) ? request.getSubCategory().trim() : null)
                .name(request.getName().trim())
                .link(request.getLink().trim())
                .important(request.isImportant())
                .orderIndex((int) itemRepository.countByCategoryId(category.getId()))
                .build();
        ResourceItem saved = itemRepository.save(item);
        return ResourceItemResponse.from(saved, category.getName());
    }

    @Transactional
    public ResourceItemResponse update(Long id, ResourceItemRequest request) {
        ResourceItem item = findEntity(id);
        ResourceCategory category = categoryService.findOrCreate(request.getCategoryName());
        item.setCategoryId(category.getId());
        item.setSubCategory(StringUtils.hasText(request.getSubCategory()) ? request.getSubCategory().trim() : null);
        item.setName(request.getName().trim());
        item.setLink(request.getLink().trim());
        item.setImportant(request.isImportant());
        ResourceItem saved = itemRepository.save(item);
        return ResourceItemResponse.from(saved, category.getName());
    }

    @Transactional
    public ResourceItemResponse toggleImportant(Long id, boolean important) {
        ResourceItem item = findEntity(id);
        item.setImportant(important);
        ResourceItem saved = itemRepository.save(item);
        ResourceCategory category = categoryRepository.findById(saved.getCategoryId()).orElse(null);
        return ResourceItemResponse.from(saved, category == null ? null : category.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource item not found: " + id);
        }
        itemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ResourceItem findEntity(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource item not found: " + id));
    }
}
