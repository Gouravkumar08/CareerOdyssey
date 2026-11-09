package com.outreach.quicklinks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuickLinkRequest(
        @Size(max = 100) String title,
        @NotBlank @Size(max = 2000) String url) {
}
