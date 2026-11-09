package com.outreach.quicklinks.service;

import com.outreach.quicklinks.dto.QuickLinkRequest;
import com.outreach.quicklinks.dto.QuickLinkResponse;
import com.outreach.quicklinks.model.QuickLink;
import com.outreach.quicklinks.repository.QuickLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuickLinkService {

    private final QuickLinkRepository repository;

    @Transactional(readOnly = true)
    public List<QuickLinkResponse> list(Long userId) {
        return repository.findByUserIdOrderByOrderIndexAscIdAsc(userId).stream().map(QuickLinkResponse::from).toList();
    }

    @Transactional
    public QuickLinkResponse create(Long userId, QuickLinkRequest request) {
        QuickLink link = QuickLink.builder().userId(userId).orderIndex((int) repository.countByUserId(userId)).build();
        apply(link, request);
        return QuickLinkResponse.from(repository.save(link));
    }

    @Transactional
    public QuickLinkResponse update(Long userId, Long id, QuickLinkRequest request) {
        QuickLink link = find(userId, id);
        apply(link, request);
        return QuickLinkResponse.from(repository.save(link));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(find(userId, id));
    }

    private QuickLink find(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link " + id + " not found"));
    }

    /** Adds https:// when no scheme was typed, and only ever stores http(s) links. */
    private void apply(QuickLink link, QuickLinkRequest request) {
        String raw = request.url().trim();
        String url = raw.matches("(?i)^[a-z][a-z0-9+.-]*:.*") ? raw : "https://" + raw;
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That doesn't look like a valid link");
        }
        boolean web = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        if (!web || uri.getHost() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only http and https links are supported");
        }
        String title = request.title() == null ? "" : request.title().trim();
        link.setTitle(title.isEmpty() ? uri.getHost().replaceFirst("^www\\.", "") : title);
        link.setUrl(url);
    }
}
