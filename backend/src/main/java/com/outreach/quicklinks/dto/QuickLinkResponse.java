package com.outreach.quicklinks.dto;

import com.outreach.quicklinks.model.QuickLink;

public record QuickLinkResponse(Long id, String title, String url) {

    public static QuickLinkResponse from(QuickLink link) {
        return new QuickLinkResponse(link.getId(), link.getTitle(), link.getUrl());
    }
}
