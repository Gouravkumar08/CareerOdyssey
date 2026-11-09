package com.outreach.planner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/**
 * A generic (non-DSA / non-System-Design) task definition. Occurrences are never stored: they are
 * derived from the recurrence rule by {@link #occursOn}, and only completions are persisted (see
 * {@link PlannerTaskCompletion}).
 * <p>
 * {@code startDate} is the due date for a one-time task, and the first day of the schedule
 * otherwise; {@code endDate} optionally closes a recurring schedule. A monthly task repeats on the
 * day-of-month of {@code startDate} (clamped to the month's length).
 */
@Entity
@Table(name = "planner_tasks", indexes = @Index(name = "idx_planner_tasks_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskRecurrence recurrence;

    /** Only meaningful for WEEKLY; empty otherwise. */
    @Convert(converter = WeekdaySetConverter.class)
    @Column(length = 20)
    @Builder.Default
    private Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    /** Optional progress tracking (e.g. pages read). Present only when progressTarget is set. */
    @Column(length = 30)
    private String progressUnit;

    private Integer progressCurrent;

    private Integer progressTarget;

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

    public boolean occursOn(LocalDate date) {
        if (date.isBefore(startDate) || (endDate != null && date.isAfter(endDate))) {
            return false;
        }
        return switch (recurrence) {
            case NONE -> date.equals(startDate);
            case DAILY -> true;
            case WEEKLY -> weekdays.contains(date.getDayOfWeek());
            case MONTHLY -> date.getDayOfMonth() == Math.min(startDate.getDayOfMonth(), date.lengthOfMonth());
        };
    }
}
