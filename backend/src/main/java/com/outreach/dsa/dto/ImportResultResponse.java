package com.outreach.dsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ImportResultResponse {
    private int importedCount;
    private int skippedCount;
    private List<String> skippedReasons;
}
