package com.outreach.quicklinks.controller;

import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.quicklinks.dto.QuickLinkRequest;
import com.outreach.quicklinks.dto.QuickLinkResponse;
import com.outreach.quicklinks.service.QuickLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quick-links")
@RequiredArgsConstructor
public class QuickLinkController {

    private final QuickLinkService service;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public List<QuickLinkResponse> list(@RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return service.list(currentUserResolver.resolve(userIdHeader));
    }

    @PostMapping
    public QuickLinkResponse create(@Valid @RequestBody QuickLinkRequest request,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return service.create(currentUserResolver.resolve(userIdHeader), request);
    }

    @PutMapping("/{id}")
    public QuickLinkResponse update(@PathVariable Long id, @Valid @RequestBody QuickLinkRequest request,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return service.update(currentUserResolver.resolve(userIdHeader), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        service.delete(currentUserResolver.resolve(userIdHeader), id);
    }
}
