package com.outreach.util;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Writes one header row plus data rows to a single-sheet .xlsx: bold header, frozen, filterable. */
public final class ExcelTableWriter {

    private ExcelTableWriter() {
    }

    /** {@code widths} are in characters; a {@code null} cell value is left blank. */
    public static byte[] write(String sheetName, String[] headers, int[] widths, List<String[]> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(sheetName));

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
                headerRow.getCell(c).setCellStyle(headerStyle);
                sheet.setColumnWidth(c, widths[c] * 256);
            }

            int r = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < values.length; c++) {
                    if (values[c] != null) {
                        row.createCell(c).setCellValue(values[c]);
                    }
                }
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rows.size()), 0, headers.length - 1));
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not build the Excel file", e);
        }
    }

    public static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    /** Excel tab names max out at 31 characters and cannot contain \ / ? * [ ] : */
    static String safeSheetName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (cleaned.isEmpty()) {
            return "Sheet";
        }
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
