package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SdRecentCompletedResponse {
    private Long itemId;
    private String title;
    private String topicName;
    private Instant completedAt;
}
