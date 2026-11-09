package com.outreach.sysdesign.dto;

import com.outreach.sysdesign.model.SdSheet;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SdSheetResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private long itemCount;

    public static SdSheetResponse from(SdSheet sheet, long itemCount) {
        return SdSheetResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .description(sheet.getDescription())
                .createdAt(sheet.getCreatedAt())
                .itemCount(itemCount)
                .build();
    }
}
