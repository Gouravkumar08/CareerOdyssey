package com.outreach.planner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One completed occurrence of a {@link PlannerTask}; absence of a row means not completed. */
@Entity
@Table(name = "planner_task_completions",
        uniqueConstraints = @UniqueConstraint(name = "uk_planner_completion_task_date",
                columnNames = {"task_id", "occurrence_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlannerTaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;
}
