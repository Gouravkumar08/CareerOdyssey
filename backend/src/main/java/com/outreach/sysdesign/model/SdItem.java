package com.outreach.sysdesign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Shared, sheet-independent item metadata — the same item can be added to many sheets
 * (see {@link SdSheetItem}) without duplicating its definition.
 */
@Entity
@Table(name = "sd_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SdItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long topicId;

    /** Pattern/sub-topic, e.g. "Consistent Hashing", "Rate Limiting". Optional. */
    private String subTopic;

    @Column(nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private boolean important;

    @Column(length = 1000)
    private String resourceLink;

    @Column(length = 4000)
    private String description;

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
