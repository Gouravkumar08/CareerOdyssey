package com.outreach.resources.dto;

import com.outreach.resources.model.ResourceItem;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceItemResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String subCategory;
    private String name;
    private String link;
    private boolean important;

    public static ResourceItemResponse from(ResourceItem item, String categoryName) {
        return ResourceItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategoryId())
                .categoryName(categoryName)
                .subCategory(item.getSubCategory())
                .name(item.getName())
                .link(item.getLink())
                .important(item.isImportant())
                .build();
    }
}
