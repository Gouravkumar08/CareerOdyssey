package com.outreach.sysdesign.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SdImportResultResponse {
    private int importedCount;
    private int skippedCount;
    private List<String> skippedReasons;
}
