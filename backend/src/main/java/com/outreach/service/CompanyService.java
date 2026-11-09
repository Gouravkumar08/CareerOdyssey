package com.outreach.service;

import com.outreach.dto.CompanyEmailView;
import com.outreach.dto.CompanyRequest;
import com.outreach.dto.CompanyResponse;
import com.outreach.exception.ConflictException;
import com.outreach.exception.ResourceNotFoundException;
import com.outreach.model.Company;
import com.outreach.repository.CompanyRepository;
import com.outreach.repository.OutreachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final OutreachRepository outreachRepository;

    public CompanyResponse create(CompanyRequest request) {
        Company company = new Company();
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(Long id) {
        return CompanyResponse.from(findEntity(id));
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list() {
        return list(null);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list(String search) {
        String needle = search == null ? null : search.trim().toLowerCase();
        return companyRepository.findAll().stream()
                .filter(company -> !StringUtils.hasText(needle) || matchesSearch(company, needle))
                .map(CompanyResponse::from)
                .toList();
    }

    private boolean matchesSearch(Company company, String needle) {
        return containsIgnoreCase(company.getCompanyName(), needle)
                || containsIgnoreCase(company.getLocation(), needle)
                || containsIgnoreCase(company.getHrEmail(), needle)
                || containsIgnoreCase(company.getCareerEmail(), needle)
                || containsIgnoreCase(company.getHrName(), needle);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findEntity(id);
        applyRequest(company, request);
        return CompanyResponse.from(companyRepository.save(company));
    }

    public void delete(Long id) {
        Company company = findEntity(id);
        try {
            outreachRepository.deleteByCompany_Id(id);
            companyRepository.delete(company);
            companyRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Cannot delete this company: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Company findEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CompanyEmailView> findEmails(String location, Boolean hasHrEmail, Boolean hasCareerEmail) {
        Specification<Company> spec = Specification.allOf(
                locationEquals(location),
                hasHrEmailSpec(hasHrEmail),
                hasCareerEmailSpec(hasCareerEmail)
        );
        return companyRepository.findAll(spec).stream().map(CompanyEmailView::from).toList();
    }

    private Specification<Company> locationEquals(String location) {
        return (root, query, cb) -> StringUtils.hasText(location)
                ? cb.equal(cb.lower(root.get("location")), location.toLowerCase())
                : null;
    }

    private Specification<Company> hasHrEmailSpec(Boolean hasHrEmail) {
        return (root, query, cb) -> hasHrEmail == null ? null
                : hasHrEmail ? cb.isNotNull(root.get("hrEmail")) : cb.isNull(root.get("hrEmail"));
    }

    private Specification<Company> hasCareerEmailSpec(Boolean hasCareerEmail) {
        return (root, query, cb) -> hasCareerEmail == null ? null
                : hasCareerEmail ? cb.isNotNull(root.get("careerEmail")) : cb.isNull(root.get("careerEmail"));
    }

    private void applyRequest(Company company, CompanyRequest request) {
        company.setCompanyName(request.getCompanyName());
        company.setLocation(request.getLocation());
        company.setWebsite(request.getWebsite());
        company.setCareerSite(request.getCareerSite());
        company.setCareerEmail(request.getCareerEmail());
        company.setHrEmail(request.getHrEmail());
        company.setHrName(request.getHrName());
        company.setCompanyDescription(request.getCompanyDescription());
        company.setLinkedinUrl(request.getLinkedinUrl());
        company.setNotes(request.getNotes());
    }
}
