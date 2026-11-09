package com.outreach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyRequest {

    @NotBlank
    private String companyName;

    private String location;
    private String website;
    private String careerSite;

    @Email
    private String careerEmail;

    @Email
    private String hrEmail;

    private String hrName;
    private String companyDescription;
    private String linkedinUrl;
    private String notes;
}
