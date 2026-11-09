package com.outreach.dto;

import com.outreach.model.Outreach;
import com.outreach.model.OutreachStatus;
import com.outreach.model.RecipientType;
import com.outreach.model.ReplyStatus;
import com.outreach.util.DateTimeDisplay;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class OutreachResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long companyId;
    private String companyName;
    private String recipientEmail;
    private String recipientName;
    private RecipientType recipientType;
    private Long templateId;
    private String templateName;
    private String subject;
    private String jobTitle;
    private String jobUrl;
    private OutreachStatus status;
    private Instant sentAt;
    private String sentAtDisplay;
    private boolean replyReceived;
    private Instant repliedAt;
    private ReplyStatus replyStatus;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static OutreachResponse from(Outreach outreach) {
        return OutreachResponse.builder()
                .id(outreach.getId())
                .userId(outreach.getUser().getId())
                .userName(outreach.getUser().getName())
                .companyId(outreach.getCompany().getId())
                .companyName(outreach.getCompany().getCompanyName())
                .recipientEmail(outreach.getRecipientEmail())
                .recipientName(outreach.getRecipientName())
                .recipientType(outreach.getRecipientType())
                .templateId(outreach.getTemplateId())
                .templateName(outreach.getTemplateName())
                .subject(outreach.getSubject())
                .jobTitle(outreach.getJobTitle())
                .jobUrl(outreach.getJobUrl())
                .status(outreach.getStatus())
                .sentAt(outreach.getSentAt())
                .sentAtDisplay(DateTimeDisplay.format(outreach.getSentAt()))
                .replyReceived(outreach.isReplyReceived())
                .repliedAt(outreach.getRepliedAt())
                .replyStatus(outreach.getReplyStatus())
                .notes(outreach.getNotes())
                .createdAt(outreach.getCreatedAt())
                .updatedAt(outreach.getUpdatedAt())
                .build();
    }
}
