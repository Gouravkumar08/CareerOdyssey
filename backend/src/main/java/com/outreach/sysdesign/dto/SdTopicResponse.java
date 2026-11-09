package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SdTopicResponse {
    private Long id;
    private String name;
    private int orderIndex;
    private long itemCount;
}
