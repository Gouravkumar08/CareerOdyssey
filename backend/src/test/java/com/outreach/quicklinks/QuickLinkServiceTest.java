package com.outreach.quicklinks;

import com.outreach.quicklinks.dto.QuickLinkRequest;
import com.outreach.quicklinks.dto.QuickLinkResponse;
import com.outreach.quicklinks.model.QuickLink;
import com.outreach.quicklinks.repository.QuickLinkRepository;
import com.outreach.quicklinks.service.QuickLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class QuickLinkServiceTest {

    @Mock QuickLinkRepository repository;

    private QuickLinkResponse create(String title, String url) {
        lenient().when(repository.save(any(QuickLink.class))).thenAnswer(inv -> inv.getArgument(0));
        return new QuickLinkService(repository).create(1L, new QuickLinkRequest(title, url));
    }

    @Test
    void addsHttpsWhenNoSchemeAndUsesHostAsDefaultTitle() {
        QuickLinkResponse r = create(null, "www.github.com/someone");

        assertEquals("https://www.github.com/someone", r.url());
        assertEquals("github.com", r.title());
    }

    @Test
    void keepsExplicitSchemeAndTitle() {
        QuickLinkResponse r = create("  Docs  ", "http://localhost:3000/x");

        assertEquals("http://localhost:3000/x", r.url());
        assertEquals("Docs", r.title());
    }

    @Test
    void rejectsNonWebSchemes() {
        assertThrows(ResponseStatusException.class, () -> create("x", "javascript:alert(1)"));
        assertThrows(ResponseStatusException.class, () -> create("x", "file:///etc/passwd"));
    }

    @Test
    void rejectsUnparseableLinks() {
        assertThrows(ResponseStatusException.class, () -> create("x", "https://exa mple.com"));
    }
}
