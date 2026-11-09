package com.outreach.resources.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceItemRequest {

    @NotBlank
    private String categoryName;

    private String subCategory;

    @NotBlank
    private String name;

    @NotBlank
    private String link;

    private boolean important;
}
