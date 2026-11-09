package com.outreach.sysdesign.dto;

import com.outreach.sysdesign.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SdItemRequest {

    @NotBlank
    private String topicName;

    private String subTopic;

    @NotBlank
    private String title;

    @NotNull
    private Category category;

    private boolean important;

    private String resourceLink;

    private String description;

    /**
     * Only used by "add item to sheet": when set, the new/matched item is also attached
     * to this sheet. Ignored by the plain item-catalog create/edit endpoints.
     */
    private Long sheetId;
}
