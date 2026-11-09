package com.outreach.dsa.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One topic's independent settings and (computed at read time) progress within a {@link DsaGoalItem}.
 * A goal may have several of these, one per topic, each fully independent: its own count-vs-complete-all
 * mode and its own difficulty filter.
 */
@Entity
@Table(name = "dsa_goal_topic_targets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DsaGoalTopicTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_item_id", nullable = false)
    private Long goalItemId;

    @Column(nullable = false)
    private Long topicId;

    /** null = "complete every matching question in this topic"; set = "solve this many". */
    private Integer targetQuestionCount;

    /** Empty = no restriction (all difficulties count). */
    @ElementCollection
    @CollectionTable(name = "dsa_goal_topic_target_difficulties", joinColumns = @JoinColumn(name = "topic_target_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    @Builder.Default
    private List<Difficulty> difficulties = new ArrayList<>();
}
