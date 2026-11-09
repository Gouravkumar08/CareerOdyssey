package com.outreach.sysdesign.dto;

import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.UserSdItemProgress;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SdItemResponse {

    private Long id;
    private Long topicId;
    private String topicName;
    private String subTopic;
    private String title;
    private Category category;
    private boolean important;
    private String resourceLink;
    private String description;

    private boolean completed;
    private boolean revisionStarred;
    private String note;
    private String githubSolutionLink;
    private Instant completedAt;

    public static SdItemResponse from(SdItem item, String topicName, UserSdItemProgress progress) {
        return SdItemResponse.builder()
                .id(item.getId())
                .topicId(item.getTopicId())
                .topicName(topicName)
                .subTopic(item.getSubTopic())
                .title(item.getTitle())
                .category(item.getCategory())
                .important(item.isImportant())
                .resourceLink(item.getResourceLink())
                .description(item.getDescription())
                .completed(progress != null && progress.isCompleted())
                .revisionStarred(progress != null && progress.isRevisionStarred())
                .note(progress == null ? null : progress.getNote())
                .githubSolutionLink(progress == null ? null : progress.getGithubSolutionLink())
                .completedAt(progress == null ? null : progress.getCompletedAt())
                .build();
    }
}
