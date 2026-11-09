package com.outreach.dsa.dto;

import com.outreach.dsa.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

    @NotBlank
    private String topicName;

    private String subTopic;

    @NotBlank
    private String title;

    @NotNull
    private Difficulty difficulty;

    private boolean important;

    private String practiceLink;
    private String youtubeLink;
    private String articleLink;

    /**
     * Only used by "add question to sheet": when set, the new/matched question is also attached
     * to this sheet. Ignored by the plain question-catalog create/edit endpoints.
     */
    private Long sheetId;
}
