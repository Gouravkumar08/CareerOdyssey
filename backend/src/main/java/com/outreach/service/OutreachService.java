package com.outreach.service;

import com.outreach.dto.OutreachResponse;
import com.outreach.dto.OutreachSendRequest;
import com.outreach.exception.ResourceNotFoundException;
import com.outreach.model.*;
import com.outreach.repository.EmailTemplateRepository;
import com.outreach.repository.OutreachRepository;
import com.outreach.repository.OutreachSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutreachService {

    private final OutreachRepository outreachRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final OutreachMailService outreachMailService;

    @Transactional
    public List<OutreachResponse> send(OutreachSendRequest request, MultipartFile resumeFile) {
        User user = userService.findEntity(request.getUserId());
        EmailTemplate template = emailTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found: " + request.getTemplateId()));

        return request.getCompanyIds().stream()
                .map(companyId -> sendToOneCompany(user, companyId, template, request.getJobTitle(), request.getJobUrl(), resumeFile))
                .toList();
    }

    private OutreachResponse sendToOneCompany(User user, Long companyId, EmailTemplate template, String jobTitle,
                                               String jobUrl, MultipartFile resumeFile) {
        Company company = companyService.findEntity(companyId);

        RecipientType recipientType = StringUtils.hasText(company.getHrEmail()) ? RecipientType.HR : RecipientType.CAREER;
        String recipientEmail = resolveRecipientEmail(recipientType, company);
        String recipientName = recipientType == RecipientType.HR ? company.getHrName() : company.getCompanyName();

        Outreach outreach = new Outreach();
        outreach.setUser(user);
        outreach.setCompany(company);
        outreach.setRecipientType(recipientType);
        outreach.setTemplateId(template.getId());
        outreach.setTemplateName(template.getName());
        outreach.setJobTitle(jobTitle);
        outreach.setJobUrl(jobUrl);
        outreach.setRecipientName(recipientName);

        if (!StringUtils.hasText(recipientEmail)) {
            outreach.setRecipientEmail("");
            outreach.setStatus(OutreachStatus.FAILED);
            outreach.setNotes("No HR or career email configured for company " + company.getId());
            return OutreachResponse.from(outreachRepository.save(outreach));
        }
        outreach.setRecipientEmail(recipientEmail);

        if (!StringUtils.hasText(user.getGmailUsername()) || !StringUtils.hasText(user.getGmailAppPassword())) {
            outreach.setStatus(OutreachStatus.FAILED);
            outreach.setNotes("Candidate has no Gmail sending credentials configured (set them on the candidate profile)");
            return OutreachResponse.from(outreachRepository.save(outreach));
        }

        String subject = outreachMailService.renderSubject(template, user, company, jobTitle);
        outreach.setSubject(subject);

        boolean resumeAttached = resumeFile != null && !resumeFile.isEmpty();
        try {
            String htmlBody = outreachMailService.renderBody(template, user, company, jobTitle, jobUrl, resumeAttached);
            outreachMailService.send(user, recipientEmail, subject, htmlBody, resumeFile);
            outreach.setStatus(OutreachStatus.SENT);
            outreach.setSentAt(Instant.now());
        } catch (Exception ex) {
            log.error("Failed to send outreach email to {}", recipientEmail, ex);
            outreach.setStatus(OutreachStatus.FAILED);
            outreach.setNotes("Send failed: " + ex.getMessage());
        }

        return OutreachResponse.from(outreachRepository.save(outreach));
    }

    @Transactional(readOnly = true)
    public OutreachResponse get(Long id) {
        return OutreachResponse.from(findEntity(id));
    }

    /**
     * Builds a prefilled send request from a past outreach record, for the "resend" and
     * "follow up" actions — the caller shows this to the user for editing before it's actually
     * submitted to {@link #send}, rather than sending immediately.
     */
    @Transactional(readOnly = true)
    public OutreachSendRequest buildResendRequest(Long id, boolean followUp) {
        Outreach original = findEntity(id);
        Long templateId = followUp
                ? emailTemplateRepository.findFirstByFollowUpTrue()
                        .map(EmailTemplate::getId)
                        .orElse(original.getTemplateId())
                : original.getTemplateId();

        OutreachSendRequest request = new OutreachSendRequest();
        request.setUserId(original.getUser().getId());
        request.setCompanyIds(new ArrayList<>(List.of(original.getCompany().getId())));
        request.setTemplateId(templateId);
        request.setJobTitle(original.getJobTitle());
        request.setJobUrl(original.getJobUrl());
        return request;
    }

    @Transactional(readOnly = true)
    public Outreach findEntity(Long id) {
        return outreachRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Outreach not found: " + id));
    }

    @Transactional
    public OutreachResponse updateReplyStatus(Long id, ReplyStatus replyStatus) {
        Outreach outreach = findEntity(id);
        outreach.setReplyStatus(replyStatus);
        boolean gotResponse = replyStatus == ReplyStatus.GOT_REPLY;
        outreach.setReplyReceived(gotResponse);
        outreach.setRepliedAt(gotResponse ? Instant.now() : null);
        return OutreachResponse.from(outreachRepository.save(outreach));
    }

    @Transactional
    public void delete(Long id) {
        if (!outreachRepository.existsById(id)) {
            throw new ResourceNotFoundException("Outreach not found: " + id);
        }
        outreachRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        outreachRepository.deleteAllInBatch();
    }

    @Transactional(readOnly = true)
    public List<OutreachResponse> search(Long userId, Long companyId, OutreachStatus status,
                                          RecipientType recipientType, Boolean replyReceived,
                                          Instant sentFrom, Instant sentTo) {
        Specification<Outreach> spec = Specification.allOf(
                OutreachSpecifications.userIdEquals(userId),
                OutreachSpecifications.companyIdEquals(companyId),
                OutreachSpecifications.statusEquals(status),
                OutreachSpecifications.recipientTypeEquals(recipientType),
                OutreachSpecifications.replyReceivedEquals(replyReceived),
                OutreachSpecifications.sentAtFrom(sentFrom),
                OutreachSpecifications.sentAtTo(sentTo)
        );
        return outreachRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(OutreachResponse::from)
                .toList();
    }

    private String resolveRecipientEmail(RecipientType recipientType, Company company) {
        return recipientType == RecipientType.HR ? company.getHrEmail() : company.getCareerEmail();
    }
}
