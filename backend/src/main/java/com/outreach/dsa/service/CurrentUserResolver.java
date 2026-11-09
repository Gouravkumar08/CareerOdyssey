package com.outreach.dsa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * No auth exists anywhere in this system yet — every request is attributed to a single default
 * user unless the caller passes X-User-Id explicitly. Progress/planner rows are already keyed by
 * userId, so wiring in real auth later needs no schema change, just a real value here.
 */
@Component
public class CurrentUserResolver {

    @Value("${dsa.default-user-id:1}")
    private Long defaultUserId;

    public Long resolve(Long headerUserId) {
        return headerUserId != null ? headerUserId : defaultUserId;
    }
}
