package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SdCategoryStats {
    private long total;
    private long completed;
}
