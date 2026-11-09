package com.outreach.sysdesign;

import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.service.SdExcelSheetExporter;
import com.outreach.sysdesign.service.SdExcelSheetImporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdExcelSheetExporterTest {

    private final SdExcelSheetExporter exporter = new SdExcelSheetExporter();

    private static SdItemResponse item(String topic, String sub, String title, Category category, String link,
                                       String description, boolean done, String note) {
        return SdItemResponse.builder().topicName(topic).subTopic(sub).title(title).category(category)
                .resourceLink(link).description(description).important(true).completed(done)
                .completedAt(done ? Instant.parse("2026-10-01T10:30:00Z") : null).note(note).build();
    }

    @Test
    void writesEveryItemWithItsProgress() throws Exception {
        byte[] file = exporter.export("HLD Roadmap", List.of(
                item("Storage", "Sharding", "Hash-Based Sharding", Category.HLD, "https://example.com/a", "Shard = Hash(User ID) % N.", true, "revisit"),
                item("Caching", null, "Cache-Aside", Category.HLD, null, null, false, null)));

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("HLD Roadmap", sheet.getSheetName());
            assertEquals(2, sheet.getLastRowNum());
            Row first = sheet.getRow(1);
            assertEquals("Storage", first.getCell(0).getStringCellValue());
            assertEquals("Hash-Based Sharding", first.getCell(2).getStringCellValue());
            assertEquals("Yes", first.getCell(7).getStringCellValue());
            assertEquals("revisit", first.getCell(10).getStringCellValue());
            assertTrue(first.getCell(8).getStringCellValue().startsWith("2026-10-01"));
            Row second = sheet.getRow(2);
            assertEquals("No", second.getCell(7).getStringCellValue());
        }
    }

    @Test
    void exportedFileImportsBackWithTheSameCoreColumns() {
        byte[] file = exporter.export("Any", List.of(
                item("Storage", "Sharding", "Hash-Based Sharding", Category.HLD, "https://example.com/a", "desc", true, "n"),
                item("Patterns", null, "Singleton", Category.LLD, null, null, false, null)));

        SdExcelSheetImporter.ParseResult result = new SdExcelSheetImporter().parse(
                new MockMultipartFile("file", "x.xlsx", "application/octet-stream", file));

        assertTrue(result.skippedReasons().isEmpty());
        assertEquals(2, result.rows().size());
        SdExcelSheetImporter.ImportedRow row = result.rows().get(0);
        assertEquals("Storage", row.topicName());
        assertEquals("Sharding", row.subTopic());
        assertEquals("https://example.com/a", row.link());
        assertEquals(Category.LLD, result.rows().get(1).category());
    }

    @Test
    void longOrInvalidSheetNamesStillProduceAValidWorkbook() throws Exception {
        byte[] file = exporter.export("A/very:long*name[with]bad?chars that goes beyond thirty-one characters", List.of());

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            assertTrue(wb.getSheetAt(0).getSheetName().length() <= 31);
            assertEquals(0, wb.getSheetAt(0).getLastRowNum());
        }
    }
}
