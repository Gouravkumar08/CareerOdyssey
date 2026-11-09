package com.outreach.dto;

import com.outreach.model.Company;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CompanyResponse {

    private Long id;
    private String companyName;
    private String location;
    private String website;
    private String careerSite;
    private String careerEmail;
    private String hrEmail;
    private String hrName;
    private String companyDescription;
    private String linkedinUrl;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .location(company.getLocation())
                .website(company.getWebsite())
                .careerSite(company.getCareerSite())
                .careerEmail(company.getCareerEmail())
                .hrEmail(company.getHrEmail())
                .hrName(company.getHrName())
                .companyDescription(company.getCompanyDescription())
                .linkedinUrl(company.getLinkedinUrl())
                .notes(company.getNotes())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
