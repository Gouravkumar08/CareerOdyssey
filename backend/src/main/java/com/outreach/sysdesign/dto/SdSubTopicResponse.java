package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SdSubTopicResponse {
    private String name;
    private long itemCount;
}
