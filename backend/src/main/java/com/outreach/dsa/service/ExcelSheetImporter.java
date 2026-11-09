package com.outreach.dsa.service;

import com.outreach.dsa.exception.BadImportException;
import com.outreach.dsa.model.Difficulty;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads an uploaded .xlsx sheet shaped like the reference "Topic | Sub-topic | Question |
 * Difficulty | Link" layout. Columns are matched by header name (case-insensitive), not fixed
 * position, so slightly different column orders/extra columns (a serial "No." column, etc.)
 * still import fine.
 */
@Component
public class ExcelSheetImporter {

    public record ImportedRow(String topicName, String subTopic, String title, Difficulty difficulty, String link) {
    }

    public record ParseResult(List<ImportedRow> rows, List<String> skippedReasons) {
    }

    private final DataFormatter dataFormatter = new DataFormatter();

    public ParseResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadImportException("No file uploaded");
        }
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new BadImportException("Sheet has no header row");
            }
            Map<String, Integer> columns = mapColumns(headerRow);
            requireColumn(columns, "topic");
            requireColumn(columns, "question");
            requireColumn(columns, "difficulty");

            List<ImportedRow> rows = new ArrayList<>();
            List<String> skippedReasons = new ArrayList<>();

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String topic = cell(row, columns.get("topic"));
                String title = cell(row, columns.get("question"));
                String difficultyRaw = cell(row, columns.get("difficulty"));
                String subTopic = columns.containsKey("subtopic") ? cell(row, columns.get("subtopic")) : null;
                String link = columns.containsKey("link") ? cell(row, columns.get("link")) : null;

                if (!hasText(title)) {
                    continue; // blank trailing row — not an error, just skip silently
                }
                if (!hasText(topic)) {
                    skippedReasons.add("Row " + (r + 1) + " (\"" + title + "\"): missing Topic");
                    continue;
                }
                Difficulty difficulty = parseDifficulty(difficultyRaw);
                if (difficulty == null) {
                    skippedReasons.add("Row " + (r + 1) + " (\"" + title + "\"): unrecognized Difficulty \"" + difficultyRaw + "\"");
                    continue;
                }

                rows.add(new ImportedRow(topic.trim(), hasText(subTopic) ? subTopic.trim() : null, title.trim(), difficulty,
                        hasText(link) ? link.trim() : null));
            }

            return new ParseResult(rows, skippedReasons);
        } catch (IOException ex) {
            throw new BadImportException("Could not read the uploaded file: " + ex.getMessage());
        }
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalized = normalizeHeader(dataFormatter.formatCellValue(cell));
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.equals("subtopic")) {
                columns.putIfAbsent("subtopic", cell.getColumnIndex());
            } else if (normalized.contains("link")) {
                columns.putIfAbsent("link", cell.getColumnIndex());
            } else {
                columns.putIfAbsent(normalized, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private String normalizeHeader(String header) {
        return header.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z]", "");
    }

    private void requireColumn(Map<String, Integer> columns, String name) {
        if (!columns.containsKey(name)) {
            throw new BadImportException("Sheet is missing a required \"" + name + "\" column");
        }
    }

    private String cell(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        return cell == null ? null : dataFormatter.formatCellValue(cell);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Difficulty parseDifficulty(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            return Difficulty.valueOf(raw.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
