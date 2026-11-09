package com.outreach.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.YearMonth;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String location;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    @Column(length = 2000)
    private String skills;

    @Column(length = 2000)
    private String education;

    private String noticePeriod;

    private String expectedSalary;

    private String currentCompany;

    private String currentDesignation;

    private YearMonth currentStartDate;

    /**
     * Null means still working there ("Present") — no end date needed.
     */
    private YearMonth currentEndDate;

    @Column(length = 2000)
    private String currentSummary;

    private String previousCompany;

    private String previousDesignation;

    private YearMonth previousStartDate;

    private YearMonth previousEndDate;

    @Column(length = 2000)
    private String previousSummary;

    private String resumeDriveLink;

    /**
     * Gmail address outreach emails for this candidate are sent from.
     */
    private String gmailUsername;

    /**
     * Gmail app password (not the account password) for gmailUsername.
     */
    private String gmailAppPassword;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
