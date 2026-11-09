package com.outreach.resources.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceSubCategoryRenameRequest {

    @NotBlank
    private String oldName;

    @NotBlank
    private String newName;
}
