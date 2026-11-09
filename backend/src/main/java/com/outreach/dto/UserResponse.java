package com.outreach.dto;

import com.outreach.model.User;
import com.outreach.util.DateTimeDisplay;
import com.outreach.util.ExperienceFormatter;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.YearMonth;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String skills;
    private String education;
    private String noticePeriod;
    private String expectedSalary;
    private String overallExperienceDisplay;

    private String currentCompany;
    private String currentDesignation;
    private YearMonth currentStartDate;
    private YearMonth currentEndDate;
    private String currentDurationDisplay;
    private String currentSummary;

    private String previousCompany;
    private String previousDesignation;
    private YearMonth previousStartDate;
    private YearMonth previousEndDate;
    private String previousDurationDisplay;
    private String previousSummary;

    private String resumeDriveLink;
    private String gmailUsername;
    /** Never exposes the actual app password — just whether one has been saved. */
    private boolean gmailAppPasswordSet;
    private Instant lastUsedAt;
    private String lastUsedDisplay;
    private Instant createdAt;
    private Instant updatedAt;

    public static UserResponse from(User user) {
        return from(user, null);
    }

    public static UserResponse from(User user, Instant lastUsedAt) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .location(user.getLocation())
                .linkedinUrl(user.getLinkedinUrl())
                .githubUrl(user.getGithubUrl())
                .portfolioUrl(user.getPortfolioUrl())
                .skills(user.getSkills())
                .education(user.getEducation())
                .noticePeriod(user.getNoticePeriod())
                .expectedSalary(user.getExpectedSalary())
                .overallExperienceDisplay(ExperienceFormatter.formatOverall(
                        user.getCurrentStartDate(), user.getCurrentEndDate(),
                        user.getPreviousStartDate(), user.getPreviousEndDate()))
                .currentCompany(user.getCurrentCompany())
                .currentDesignation(user.getCurrentDesignation())
                .currentStartDate(user.getCurrentStartDate())
                .currentEndDate(user.getCurrentEndDate())
                .currentDurationDisplay(ExperienceFormatter.formatRange(user.getCurrentStartDate(), user.getCurrentEndDate()))
                .currentSummary(user.getCurrentSummary())
                .previousCompany(user.getPreviousCompany())
                .previousDesignation(user.getPreviousDesignation())
                .previousStartDate(user.getPreviousStartDate())
                .previousEndDate(user.getPreviousEndDate())
                .previousDurationDisplay(ExperienceFormatter.formatRange(user.getPreviousStartDate(), user.getPreviousEndDate()))
                .previousSummary(user.getPreviousSummary())
                .resumeDriveLink(user.getResumeDriveLink())
                .gmailUsername(user.getGmailUsername())
                .gmailAppPasswordSet(user.getGmailAppPassword() != null && !user.getGmailAppPassword().isBlank())
                .lastUsedAt(lastUsedAt)
                .lastUsedDisplay(DateTimeDisplay.format(lastUsedAt))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
