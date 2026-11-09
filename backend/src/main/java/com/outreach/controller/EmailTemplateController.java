package com.outreach.controller;

import com.outreach.dto.EmailTemplateResponse;
import com.outreach.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateRepository emailTemplateRepository;

    @GetMapping
    public List<EmailTemplateResponse> list() {
        return emailTemplateRepository.findAll().stream()
                .map(EmailTemplateResponse::from)
                .toList();
    }
}
