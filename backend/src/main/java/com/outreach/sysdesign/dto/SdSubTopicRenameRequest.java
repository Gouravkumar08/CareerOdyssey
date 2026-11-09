package com.outreach.sysdesign.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdSubTopicRenameRequest {

    @NotBlank
    private String oldName;

    @NotBlank
    private String newName;
}
