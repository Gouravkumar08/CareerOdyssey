package com.outreach.dto;

import com.outreach.model.Company;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CompanyEmailView {

    private Long id;
    private String companyName;
    private String location;
    private String careerEmail;
    private String hrEmail;

    public static CompanyEmailView from(Company company) {
        return CompanyEmailView.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .location(company.getLocation())
                .careerEmail(company.getCareerEmail())
                .hrEmail(company.getHrEmail())
                .build();
    }
}
