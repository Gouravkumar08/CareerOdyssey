package com.outreach.sysdesign.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join table between {@link SdSheet} and {@link SdItem} — this is what makes sheets customizable
 * and mixable: adding/removing/reordering an item in one sheet never affects any other sheet
 * or the shared item catalog.
 */
@Entity
@Table(name = "sd_sheet_items", uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id", "item_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SdSheetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private Long sheetId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private int orderIndex;
}
