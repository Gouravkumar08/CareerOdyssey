package com.outreach.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OutreachSendRequest {

    @NotNull
    private Long userId;

    /**
     * One id sends a single outreach; more than one sends the same email to every company.
     */
    @NotEmpty
    private List<Long> companyIds;

    @NotNull
    private Long templateId;

    private String jobTitle;

    private String jobUrl;
}
