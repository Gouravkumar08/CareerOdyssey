package com.outreach.sysdesign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdTopicRequest {

    @NotBlank
    private String name;
}
