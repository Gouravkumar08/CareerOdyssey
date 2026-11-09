package com.outreach.resources.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A single curated link — video, article, or playlist — under a category and an optional
 * sub-category. No per-user progress tracking; {@code important} is a plain flag on the
 * shared catalog row, not a per-user preference.
 */
@Entity
@Table(name = "resource_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long categoryId;

    /** Sub-category, e.g. "React", "Java", "Docker". Optional — many items sit flat under the category. */
    private String subCategory;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 1000)
    private String link;

    @Column(nullable = false)
    private boolean important;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
