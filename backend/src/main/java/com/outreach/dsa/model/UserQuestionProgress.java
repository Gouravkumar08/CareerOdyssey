package com.outreach.dsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per (userId, questionId) — deliberately decoupled from {@link Sheet}, so progress
 * survives regardless of which sheet(s) a question currently belongs to.
 */
@Entity
@Table(name = "user_question_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private boolean solved;

    @Column(nullable = false)
    private boolean revisionStarred;

    @Column(length = 2000)
    private String note;

    /**
     * Manual, optional — only meaningful once solved. Never auto-populated.
     */
    @Column(length = 500)
    private String githubSolutionLink;

    private Instant solvedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        this.updatedAt = Instant.now();
    }
}
