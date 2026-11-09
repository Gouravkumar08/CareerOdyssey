package com.outreach.resources.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceCategoryRequest {

    @NotBlank
    private String name;
}
