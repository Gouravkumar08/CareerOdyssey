package com.outreach.sysdesign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * One row per (userId, itemId) — deliberately decoupled from {@link SdSheet}, so progress
 * survives regardless of which sheet(s) an item currently belongs to.
 */
@Entity
@Table(name = "user_sd_item_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSdItemProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private boolean revisionStarred;

    @Column(length = 2000)
    private String note;

    @Column(length = 1000)
    private String githubSolutionLink;

    private Instant completedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        this.updatedAt = Instant.now();
    }
}
