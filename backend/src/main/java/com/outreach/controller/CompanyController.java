package com.outreach.controller;

import com.outreach.dto.CompanyEmailView;
import com.outreach.dto.CompanyRequest;
import com.outreach.dto.CompanyResponse;
import com.outreach.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request));
    }

    @GetMapping("/{id}")
    public CompanyResponse get(@PathVariable Long id) {
        return companyService.get(id);
    }

    @GetMapping
    public List<CompanyResponse> list(@RequestParam(required = false) String search) {
        return companyService.list(search);
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @Valid @RequestBody CompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/emails")
    public List<CompanyEmailView> emails(@RequestParam(required = false) String location,
                                          @RequestParam(required = false) Boolean hasHrEmail,
                                          @RequestParam(required = false) Boolean hasCareerEmail) {
        return companyService.findEmails(location, hasHrEmail, hasCareerEmail);
    }
}
