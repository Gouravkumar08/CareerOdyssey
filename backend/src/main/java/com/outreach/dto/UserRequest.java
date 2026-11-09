package com.outreach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

@Getter
@Setter
public class UserRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
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

    private String currentCompany;
    private String currentDesignation;
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth currentStartDate;
    /** Null/absent means still working there ("Present"). */
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth currentEndDate;
    private String currentSummary;

    private String previousCompany;
    private String previousDesignation;
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth previousStartDate;
    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth previousEndDate;
    private String previousSummary;

    private String resumeDriveLink;
    private String gmailUsername;

    /**
     * Blank on update means "keep the existing app password" — never redisplayed once saved.
     */
    private String gmailAppPassword;
}
