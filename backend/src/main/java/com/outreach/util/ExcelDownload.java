package com.outreach.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** The HTTP response for an .xlsx file the browser should save. */
public final class ExcelDownload {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private ExcelDownload() {
    }

    public static ResponseEntity<byte[]> attachment(String baseName, byte[] file) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(baseName + ".xlsx", StandardCharsets.UTF_8).build().toString())
                .contentType(XLSX)
                .body(file);
    }
}
