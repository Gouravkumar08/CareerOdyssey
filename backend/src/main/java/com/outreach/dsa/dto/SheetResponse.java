package com.outreach.dsa.dto;

import com.outreach.dsa.model.Sheet;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SheetResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private long questionCount;

    public static SheetResponse from(Sheet sheet, long questionCount) {
        return SheetResponse.builder()
                .id(sheet.getId())
                .name(sheet.getName())
                .description(sheet.getDescription())
                .createdAt(sheet.getCreatedAt())
                .questionCount(questionCount)
                .build();
    }
}
