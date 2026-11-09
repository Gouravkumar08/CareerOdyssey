package com.outreach.dsa;

import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.service.ExcelSheetExporter;
import com.outreach.dsa.service.ExcelSheetImporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelSheetExporterTest {

    private final ExcelSheetExporter exporter = new ExcelSheetExporter();

    private static QuestionResponse question(String topic, String title, Difficulty difficulty, String practice,
                                             String youtube, boolean solved) {
        return QuestionResponse.builder().topicName(topic).subTopic("Two Pointers").title(title).difficulty(difficulty)
                .practiceLink(practice).youtubeLink(youtube).important(true).solved(solved)
                .solvedAt(solved ? Instant.parse("2026-10-02T08:00:00Z") : null).note("n").build();
    }

    @Test
    void writesQuestionsWithAllLinksAndProgress() throws Exception {
        byte[] file = exporter.export("DSA Imp 170", List.of(
                question("Array", "Two Sum", Difficulty.EASY, "https://leetcode.com/two-sum", "https://youtu.be/x", true)));

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            Row row = wb.getSheetAt(0).getRow(1);
            assertEquals("Two Sum", row.getCell(2).getStringCellValue());
            assertEquals("EASY", row.getCell(3).getStringCellValue());
            assertEquals("https://leetcode.com/two-sum", row.getCell(4).getStringCellValue());
            assertEquals("https://youtu.be/x", row.getCell(5).getStringCellValue());
            assertEquals("Yes", row.getCell(8).getStringCellValue());
            assertTrue(row.getCell(9).getStringCellValue().startsWith("2026-10-02"));
        }
    }

    @Test
    void exportedFileImportsBackUsingThePracticeLinkAsLink() {
        byte[] file = exporter.export("Any", List.of(
                question("Array", "Two Sum", Difficulty.EASY, "https://leetcode.com/two-sum", "https://youtu.be/x", true),
                question("Graph", "Clone Graph", Difficulty.MEDIUM, null, null, false)));

        ExcelSheetImporter.ParseResult result = new ExcelSheetImporter().parse(
                new MockMultipartFile("file", "x.xlsx", "application/octet-stream", file));

        assertTrue(result.skippedReasons().isEmpty());
        assertEquals(2, result.rows().size());
        assertEquals("https://leetcode.com/two-sum", result.rows().get(0).link());
        assertEquals(Difficulty.MEDIUM, result.rows().get(1).difficulty());
    }
}
