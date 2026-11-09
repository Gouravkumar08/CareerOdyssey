package com.outreach.resources;

import com.outreach.resources.dto.ResourceItemResponse;
import com.outreach.resources.service.ResourceExcelExporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceExcelExporterTest {

    @Test
    void writesOneRowPerResourceWithCategoryAndImportance() throws Exception {
        byte[] file = new ResourceExcelExporter().export(List.of(
                ResourceItemResponse.builder().categoryName("System Design").subCategory("HLD").name("Playlist")
                        .link("https://example.com/p").important(true).build(),
                ResourceItemResponse.builder().categoryName("Aptitude").name("indiabix.com")
                        .link("https://www.indiabix.com/").important(false).build()));

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(file))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Free Resources", sheet.getSheetName());
            assertEquals(2, sheet.getLastRowNum());
            Row first = sheet.getRow(1);
            assertEquals("System Design", first.getCell(0).getStringCellValue());
            assertEquals("HLD", first.getCell(1).getStringCellValue());
            assertEquals("https://example.com/p", first.getCell(3).getStringCellValue());
            assertEquals("Yes", first.getCell(4).getStringCellValue());
            assertEquals("No", sheet.getRow(2).getCell(4).getStringCellValue());
        }
    }
}
