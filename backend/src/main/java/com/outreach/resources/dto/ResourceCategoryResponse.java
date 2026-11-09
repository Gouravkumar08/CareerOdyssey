package com.outreach.resources.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceCategoryResponse {
    private Long id;
    private String name;
    private int orderIndex;
    private long itemCount;
}
