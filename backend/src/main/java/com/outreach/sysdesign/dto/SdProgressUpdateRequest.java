package com.outreach.sysdesign.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * PATCH-style: only non-null fields are applied. Lets the frontend toggle just the checkbox, or
 * just the star, or just save a note, without resending the whole progress row.
 */
@Getter
@Setter
public class SdProgressUpdateRequest {
    private Boolean completed;
    private Boolean revisionStarred;
    private String note;
    private String githubSolutionLink;
}
