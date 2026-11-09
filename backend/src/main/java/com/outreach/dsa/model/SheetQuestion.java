package com.outreach.dsa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join table between {@link Sheet} and {@link Question} — this is what makes sheets customizable
 * and mixable: adding/removing/reordering a question in one sheet never affects any other sheet
 * or the shared question catalog.
 */
@Entity
@Table(name = "sheet_questions", uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SheetQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private Long sheetId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private int orderIndex;
}
