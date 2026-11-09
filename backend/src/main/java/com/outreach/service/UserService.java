package com.outreach.service;

import com.outreach.dto.UserRequest;
import com.outreach.dto.UserResponse;
import com.outreach.exception.ConflictException;
import com.outreach.exception.ResourceNotFoundException;
import com.outreach.model.User;
import com.outreach.repository.OutreachRepository;
import com.outreach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final OutreachRepository outreachRepository;

    public UserResponse create(UserRequest request) {
        User user = new User();
        applyRequest(user, request);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        User user = findEntity(id);
        return UserResponse.from(user, lastUsedAtFor(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(String search) {
        Map<Long, Instant> lastUsedByUserId = new HashMap<>();
        for (Object[] row : outreachRepository.findLastSentAtByUser()) {
            lastUsedByUserId.put((Long) row[0], (Instant) row[1]);
        }

        String needle = search == null ? null : search.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(user -> !StringUtils.hasText(needle) || matchesSearch(user, needle))
                .map(user -> UserResponse.from(user, lastUsedByUserId.get(user.getId())))
                .sorted(Comparator.comparing(UserResponse::getLastUsedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(UserResponse::getName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private boolean matchesSearch(User user, String needle) {
        return containsIgnoreCase(user.getName(), needle)
                || containsIgnoreCase(user.getEmail(), needle)
                || containsIgnoreCase(user.getGmailUsername(), needle)
                || containsIgnoreCase(user.getCurrentCompany(), needle)
                || containsIgnoreCase(user.getPreviousCompany(), needle);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private Instant lastUsedAtFor(Long userId) {
        for (Object[] row : outreachRepository.findLastSentAtByUser()) {
            if (row[0].equals(userId)) {
                return (Instant) row[1];
            }
        }
        return null;
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = findEntity(id);
        applyRequest(user, request);
        return UserResponse.from(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = findEntity(id);
        try {
            outreachRepository.deleteByUser_Id(id);
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Cannot delete this candidate: " + ex.getMostSpecificCause().getMessage());
        }
    }

    @Transactional(readOnly = true)
    public User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void applyRequest(User user, UserRequest request) {
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setLocation(request.getLocation());
        user.setLinkedinUrl(request.getLinkedinUrl());
        user.setGithubUrl(request.getGithubUrl());
        user.setPortfolioUrl(request.getPortfolioUrl());
        user.setSkills(request.getSkills());
        user.setEducation(request.getEducation());
        user.setNoticePeriod(request.getNoticePeriod());
        user.setExpectedSalary(request.getExpectedSalary());
        user.setCurrentCompany(request.getCurrentCompany());
        user.setCurrentDesignation(request.getCurrentDesignation());
        user.setCurrentStartDate(request.getCurrentStartDate());
        user.setCurrentEndDate(request.getCurrentEndDate());
        user.setCurrentSummary(request.getCurrentSummary());
        user.setPreviousCompany(request.getPreviousCompany());
        user.setPreviousDesignation(request.getPreviousDesignation());
        user.setPreviousStartDate(request.getPreviousStartDate());
        user.setPreviousEndDate(request.getPreviousEndDate());
        user.setPreviousSummary(request.getPreviousSummary());
        user.setResumeDriveLink(request.getResumeDriveLink());
        user.setGmailUsername(request.getGmailUsername());
        if (request.getGmailAppPassword() != null && !request.getGmailAppPassword().isBlank()) {
            user.setGmailAppPassword(request.getGmailAppPassword());
        }
    }
}
