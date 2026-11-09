package com.outreach.dto;

import com.outreach.model.ReplyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplyStatusUpdateRequest {

    @NotNull
    private ReplyStatus replyStatus;
}
