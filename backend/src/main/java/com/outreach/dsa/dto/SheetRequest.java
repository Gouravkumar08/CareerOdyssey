package com.outreach.dsa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SheetRequest {

    @NotBlank
    private String name;

    private String description;
}
