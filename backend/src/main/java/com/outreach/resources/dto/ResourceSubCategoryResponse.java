package com.outreach.resources.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceSubCategoryResponse {
    private String name;
    private long itemCount;
}
