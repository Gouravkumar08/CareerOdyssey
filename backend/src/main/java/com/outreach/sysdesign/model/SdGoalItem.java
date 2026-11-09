package com.outreach.sysdesign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A single System Design goal: complete every item under one or more topics
 * (optionally restricted to a single sheet) by a deadline. A user may have
 * several of these running at once.
 */
@Entity
@Table(name = "sd_goal_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SdGoalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ElementCollection
    @CollectionTable(name = "sd_goal_item_topics", joinColumns = @JoinColumn(name = "goal_item_id"))
    @Column(name = "topic_id", nullable = false)
    @Builder.Default
    private List<Long> topicIds = new ArrayList<>();

    /** Optional scope: null = across all sheets, else restrict to this sheet's items. */
    private Long sheetId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
