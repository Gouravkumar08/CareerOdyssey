package com.outreach.dto;

import com.outreach.model.EmailTemplate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmailTemplateResponse {

    private Long id;
    private String name;
    private String subjectTemplate;
    private boolean active;
    private boolean followUp;

    public static EmailTemplateResponse from(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .subjectTemplate(template.getSubjectTemplate())
                .active(template.isActive())
                .followUp(template.isFollowUp())
                .build();
    }
}
