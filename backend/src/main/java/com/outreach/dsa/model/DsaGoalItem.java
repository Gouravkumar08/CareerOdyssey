package com.outreach.dsa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A single independent goal owned by a user: a shared deadline and (optional) shared sheet scope.
 * The actual per-topic targets (count vs. complete-all, difficulty filters, progress) live in the
 * child {@link DsaGoalTopicTarget} rows — a user may have several topics in one goal, each with
 * fully independent settings.
 */
@Entity
@Table(name = "dsa_goal_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DsaGoalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Optional scope: null = across all sheets, else restrict to this sheet's questions. Shared across all this goal's topics. */
    private Long sheetId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;
}
