package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.util.ExcelTableWriter;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.outreach.util.ExcelTableWriter.yesNo;

/**
 * Writes a System Design sheet to .xlsx. The first six columns use the same headers
 * {@link SdExcelSheetImporter} reads, so an exported file can be imported back as a new sheet;
 * the remaining columns carry the user's progress and are ignored by the importer.
 */
@Component
public class SdExcelSheetExporter {

    private static final String[] HEADERS = {
            "Topic", "Sub-topic", "Title", "Category", "Link", "Description",
            "Important", "Done", "Completed At", "Revision", "Note", "GitHub Solution Link"
    };

    private static final int[] WIDTHS = {22, 24, 48, 10, 50, 60, 11, 8, 18, 10, 40, 40};

    private static final DateTimeFormatter COMPLETED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] export(String sheetName, List<SdItemResponse> items) {
        List<String[]> rows = items.stream().map(item -> new String[]{
                item.getTopicName(), item.getSubTopic(), item.getTitle(), item.getCategory().name(),
                item.getResourceLink(), item.getDescription(),
                yesNo(item.isImportant()), yesNo(item.isCompleted()),
                item.getCompletedAt() == null ? null : COMPLETED_AT.format(item.getCompletedAt()),
                yesNo(item.isRevisionStarred()), item.getNote(), item.getGithubSolutionLink()
        }).toList();
        return ExcelTableWriter.write(sheetName, HEADERS, WIDTHS, rows);
    }
}
