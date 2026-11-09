package com.outreach.resources.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceItemImportantRequest {

    @NotNull
    private Boolean important;
}
