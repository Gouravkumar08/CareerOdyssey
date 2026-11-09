package com.outreach.sysdesign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdSheetRequest {

    @NotBlank
    private String name;

    private String description;
}
