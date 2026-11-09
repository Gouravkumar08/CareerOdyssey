package com.outreach.resources.controller;

import com.outreach.resources.dto.ResourceCategoryRequest;
import com.outreach.resources.dto.ResourceCategoryResponse;
import com.outreach.resources.dto.ResourceSubCategoryRenameRequest;
import com.outreach.resources.dto.ResourceSubCategoryResponse;
import com.outreach.resources.model.ResourceCategory;
import com.outreach.resources.service.ResourceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources/categories")
@RequiredArgsConstructor
public class ResourceCategoryController {

    private final ResourceCategoryService categoryService;

    @GetMapping
    public List<ResourceCategoryResponse> list() {
        return categoryService.list();
    }

    @PostMapping
    public ResourceCategoryResponse create(@Valid @RequestBody ResourceCategoryRequest request) {
        ResourceCategory category = categoryService.findOrCreate(request.getName());
        return categoryService.list().stream()
                .filter(c -> c.getId().equals(category.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PutMapping("/{id}")
    public ResourceCategoryResponse rename(@PathVariable Long id, @Valid @RequestBody ResourceCategoryRequest request) {
        return categoryService.rename(id, request.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.deleteCascade(id);
    }

    @PutMapping("/reorder")
    public void reorder(@RequestBody List<Long> orderedCategoryIds) {
        categoryService.reorder(orderedCategoryIds);
    }

    @GetMapping("/{id}/subcategories")
    public List<ResourceSubCategoryResponse> listSubCategories(@PathVariable Long id) {
        return categoryService.listSubCategories(id);
    }

    @PutMapping("/{id}/subcategories/rename")
    public List<ResourceSubCategoryResponse> renameSubCategory(@PathVariable Long id,
                                                                 @Valid @RequestBody ResourceSubCategoryRenameRequest request) {
        categoryService.renameSubCategory(id, request.getOldName(), request.getNewName());
        return categoryService.listSubCategories(id);
    }

    @DeleteMapping("/{id}/subcategories")
    public List<ResourceSubCategoryResponse> deleteSubCategory(@PathVariable Long id, @RequestParam String name) {
        categoryService.deleteSubCategoryCascade(id, name);
        return categoryService.listSubCategories(id);
    }
}
