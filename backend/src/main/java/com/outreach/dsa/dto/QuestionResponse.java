package com.outreach.dsa.dto;

import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.model.Question;
import com.outreach.dsa.model.UserQuestionProgress;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class QuestionResponse {

    private Long id;
    private Long topicId;
    private String topicName;
    private String subTopic;
    private String title;
    private Difficulty difficulty;
    private boolean important;
    private String practiceLink;
    private String youtubeLink;
    private String articleLink;

    private boolean solved;
    private boolean revisionStarred;
    private String note;
    private String githubSolutionLink;
    private Instant solvedAt;

    public static QuestionResponse from(Question question, String topicName, UserQuestionProgress progress) {
        return QuestionResponse.builder()
                .id(question.getId())
                .topicId(question.getTopicId())
                .topicName(topicName)
                .subTopic(question.getSubTopic())
                .title(question.getTitle())
                .difficulty(question.getDifficulty())
                .important(question.isImportant())
                .practiceLink(question.getPracticeLink())
                .youtubeLink(question.getYoutubeLink())
                .articleLink(question.getArticleLink())
                .solved(progress != null && progress.isSolved())
                .revisionStarred(progress != null && progress.isRevisionStarred())
                .note(progress == null ? null : progress.getNote())
                .githubSolutionLink(progress == null ? null : progress.getGithubSolutionLink())
                .solvedAt(progress == null ? null : progress.getSolvedAt())
                .build();
    }
}
