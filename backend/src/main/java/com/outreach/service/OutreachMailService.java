package com.outreach.service;

import com.outreach.model.Company;
import com.outreach.model.User;
import com.outreach.model.EmailTemplate;
import com.outreach.util.ExperienceFormatter;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OutreachMailService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final TemplateEngine templateEngine;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    public String renderSubject(EmailTemplate template, User user, Company company, String jobTitle) {
        Map<String, String> values = subjectPlaceholders(user, company, jobTitle);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template.getSubjectTemplate());
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public String renderBody(EmailTemplate template, User user, Company company, String jobTitle, String jobUrl, boolean resumeAttached) {
        Context context = new Context();
        context.setVariable("candidateName", user.getName());
        context.setVariable("resumeAttached", resumeAttached);
        context.setVariable("skills", user.getSkills());
        context.setVariable("education", user.getEducation());
        context.setVariable("linkedinUrl", user.getLinkedinUrl());
        context.setVariable("githubUrl", user.getGithubUrl());
        context.setVariable("portfolioUrl", user.getPortfolioUrl());
        context.setVariable("noticePeriod", user.getNoticePeriod());
        context.setVariable("expectedSalary", user.getExpectedSalary());
        context.setVariable("phone", user.getPhone());
        context.setVariable("candidateEmail", user.getEmail());
        context.setVariable("location", user.getLocation());
        context.setVariable("currentCompany", user.getCurrentCompany());
        context.setVariable("currentDesignation", user.getCurrentDesignation());
        context.setVariable("currentDuration", ExperienceFormatter.formatRange(user.getCurrentStartDate(), user.getCurrentEndDate()));
        context.setVariable("currentSummary", user.getCurrentSummary());
        context.setVariable("previousCompany", user.getPreviousCompany());
        context.setVariable("previousDesignation", user.getPreviousDesignation());
        context.setVariable("previousDuration", ExperienceFormatter.formatRange(user.getPreviousStartDate(), user.getPreviousEndDate()));
        context.setVariable("previousSummary", user.getPreviousSummary());
        context.setVariable("overallExperience", ExperienceFormatter.formatOverall(
                user.getCurrentStartDate(), user.getCurrentEndDate(),
                user.getPreviousStartDate(), user.getPreviousEndDate()));
        context.setVariable("resumeDriveLink", user.getResumeDriveLink());
        context.setVariable("companyName", company.getCompanyName());
        context.setVariable("hrName", company.getHrName());
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("jobUrl", jobUrl);
        return templateEngine.process(template.getViewName(), context);
    }

    /**
     * No resume is attached unless resumeFile is provided — there is no fallback default file.
     */
    public void send(User sender, String recipientEmail, String subject, String htmlBody, MultipartFile resumeFile) throws Exception {
        JavaMailSender mailSender = buildMailSender(sender);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(recipientEmail);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (resumeFile != null && !resumeFile.isEmpty()) {
            String filename = StringUtils.hasText(resumeFile.getOriginalFilename())
                    ? resumeFile.getOriginalFilename()
                    : "resume";
            helper.addAttachment(filename, new ByteArrayResource(resumeFile.getBytes()));
        }

        mailSender.send(message);
    }

    /**
     * Each candidate sends from their own Gmail account (email + app password stored on their
     * profile), so the sender has to be built per-send rather than reused as a shared bean.
     */
    private JavaMailSender buildMailSender(User sender) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(sender.getGmailUsername());
        mailSender.setPassword(sender.getGmailAppPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return mailSender;
    }

    private Map<String, String> subjectPlaceholders(User user, Company company, String jobTitle) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("candidateName", nullToEmpty(user.getName()));
        values.put("companyName", nullToEmpty(company.getCompanyName()));
        values.put("hrName", nullToEmpty(company.getHrName()));
        values.put("jobTitle", nullToEmpty(jobTitle));
        return values;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
