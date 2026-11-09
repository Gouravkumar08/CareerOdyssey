package com.outreach.service;

import com.outreach.dto.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort resume text extraction — regex/heuristic based, no AI/NLP service involved.
 * Reliable for contact info and skills/education; company/role/date detection is approximate
 * and expected to need manual correction, which is why the result is only ever a prefilled form,
 * never saved directly.
 */
@Service
@Slf4j
public class ResumeParsingService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3,5}\\)?[-.\\s]?\\d{3,4}[-.\\s]?\\d{3,4}");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("(https?://)?(www\\.)?linkedin\\.com/\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB_PATTERN = Pattern.compile("(https?://)?(www\\.)?github\\.com/\\S+", Pattern.CASE_INSENSITIVE);

    private static final List<String> SECTION_HEADERS = List.of(
            "experience", "work experience", "professional experience", "employment history",
            "education", "skills", "technical skills", "projects", "certifications", "summary", "objective");

    private static final String MONTH = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t)?(?:ember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?";
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(?i)((?:" + MONTH + ")\\s*,?\\s*)?(\\d{4})\\s*(?:-|–|—|to)\\s*((?:" + MONTH + ")\\s*,?\\s*)?(\\d{4}|present|current)");

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    public UserRequest parse(MultipartFile resumeFile) {
        UserRequest request = new UserRequest();
        if (resumeFile == null || resumeFile.isEmpty()) {
            return request;
        }

        String text;
        try (PDDocument document = Loader.loadPDF(resumeFile.getBytes())) {
            text = new PDFTextStripper().getText(document);
        } catch (Exception ex) {
            log.warn("Could not parse resume PDF: {}", ex.getMessage());
            return request;
        }

        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }

        request.setEmail(firstMatch(text, EMAIL_PATTERN));
        request.setPhone(firstMatch(text, PHONE_PATTERN));
        request.setLinkedinUrl(firstMatch(text, LINKEDIN_PATTERN));
        request.setGithubUrl(firstMatch(text, GITHUB_PATTERN));
        request.setName(guessName(lines));
        request.setSkills(extractSection(lines, "skills", "technical skills"));
        request.setEducation(extractSection(lines, "education"));

        applyExperience(lines, request);

        return request;
    }

    private String firstMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private String guessName(List<String> lines) {
        for (String line : lines.subList(0, Math.min(5, lines.size()))) {
            if (line.length() < 3 || line.length() > 60) {
                continue;
            }
            if (line.contains("@") || LINKEDIN_PATTERN.matcher(line).find() || GITHUB_PATTERN.matcher(line).find()) {
                continue;
            }
            if (PHONE_PATTERN.matcher(line).matches()) {
                continue;
            }
            String[] words = line.split("\\s+");
            if (words.length >= 2 && words.length <= 4 && Character.isUpperCase(line.charAt(0))) {
                return line;
            }
        }
        return null;
    }

    private boolean isSectionHeader(String line) {
        String normalized = line.toLowerCase(Locale.ENGLISH).replace(":", "").trim();
        return SECTION_HEADERS.contains(normalized);
    }

    private String extractSection(List<String> lines, String... headerNames) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            String normalized = lines.get(i).toLowerCase(Locale.ENGLISH).replace(":", "").trim();
            for (String header : headerNames) {
                if (normalized.equals(header)) {
                    start = i + 1;
                    break;
                }
            }
            if (start != -1) {
                break;
            }
        }
        if (start == -1) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (int i = start; i < lines.size() && i < start + 15; i++) {
            String line = lines.get(i);
            if (isSectionHeader(line)) {
                break;
            }
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(line);
        }
        return !result.isEmpty() ? result.toString() : null;
    }

    private void applyExperience(List<String> lines, UserRequest request) {
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            String normalized = lines.get(i).toLowerCase(Locale.ENGLISH).replace(":", "").trim();
            if (normalized.equals("experience") || normalized.equals("work experience") || normalized.equals("professional experience")) {
                start = i + 1;
                break;
            }
        }
        if (start == -1) {
            return;
        }

        List<int[]> matchLineIndexes = new ArrayList<>();
        List<Matcher> matches = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isSectionHeader(line) && i > start) {
                break;
            }
            Matcher matcher = DATE_RANGE_PATTERN.matcher(line);
            if (matcher.find()) {
                matches.add(matcher);
                matchLineIndexes.add(new int[]{i});
                if (matches.size() == 2) {
                    break;
                }
            }
        }

        if (!matches.isEmpty()) {
            applyRole(lines, matchLineIndexes.get(0)[0], matches.get(0), true, request);
        }
        if (matches.size() > 1) {
            applyRole(lines, matchLineIndexes.get(1)[0], matches.get(1), false, request);
        }
    }

    private void applyRole(List<String> lines, int lineIndex, Matcher dateMatch, boolean current, UserRequest request) {
        YearMonth startDate = toYearMonth(dateMatch.group(1), dateMatch.group(2));
        String endRaw = dateMatch.group(4);
        boolean present = endRaw != null && endRaw.toLowerCase(Locale.ENGLISH).matches("present|current");
        YearMonth endDate = present ? null : toYearMonth(dateMatch.group(3), endRaw);

        String context = lines.get(lineIndex).replace(dateMatch.group(), "").trim();
        if (context.isEmpty() && lineIndex > 0) {
            context = lines.get(lineIndex - 1).trim();
        }
        context = context.replaceAll("^[|,•\\-–—\\s]+|[|,•\\-–—\\s]+$", "");

        String designation = null;
        String company = context;
        for (String separator : List.of("|", " at ", " - ", "–", "—", ",")) {
            int idx = context.indexOf(separator);
            if (idx > 0) {
                designation = context.substring(0, idx).trim();
                company = context.substring(idx + separator.length()).trim();
                break;
            }
        }

        if (current) {
            request.setCurrentStartDate(startDate);
            request.setCurrentEndDate(endDate);
            request.setCurrentDesignation(designation);
            request.setCurrentCompany(StringUtils.hasText(company) ? company : null);
        } else {
            request.setPreviousStartDate(startDate);
            request.setPreviousEndDate(endDate);
            request.setPreviousDesignation(designation);
            request.setPreviousCompany(StringUtils.hasText(company) ? company : null);
        }
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ENGLISH);
    }

    private YearMonth toYearMonth(String monthText, String yearText) {
        if (yearText == null || !yearText.matches("\\d{4}")) {
            return null;
        }
        int year = Integer.parseInt(yearText);
        if (monthText == null || monthText.isBlank()) {
            return YearMonth.of(year, 1);
        }
        String abbr = capitalize(monthText.trim().substring(0, Math.min(3, monthText.trim().length())));
        try {
            return YearMonth.parse(abbr + " " + year, MONTH_YEAR);
        } catch (Exception ex) {
            return YearMonth.of(year, 1);
        }
    }
}
