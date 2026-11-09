package com.outreach.sysdesign.controller;

import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.sysdesign.dto.SdProgressUpdateRequest;
import com.outreach.sysdesign.dto.SdSheetResponse;
import com.outreach.sysdesign.dto.SdSheetStatsResponse;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.service.SdExcelSheetExporter;
import com.outreach.sysdesign.service.SdItemService;
import com.outreach.sysdesign.service.SdSheetService;
import com.outreach.util.ExcelDownload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sysdesign")
@RequiredArgsConstructor
public class SdItemController {

    private final SdItemService itemService;
    private final SdSheetService sheetService;
    private final SdExcelSheetExporter excelExporter;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/sheets/{sheetId}/items")
    public List<SdItemResponse> list(@PathVariable Long sheetId,
                                      @RequestParam(required = false) String topic,
                                      @RequestParam(required = false) Category category,
                                      @RequestParam(required = false) Boolean important,
                                      @RequestParam(required = false) Boolean revision,
                                      @RequestParam(required = false) Boolean completed,
                                      @RequestParam(required = false) String search,
                                      @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        Long userId = currentUserResolver.resolve(userIdHeader);
        return itemService.listForSheet(sheetId, userId, topic, category, important, revision, completed, search);
    }

    /** The whole sheet as .xlsx — every item plus the user's own progress — ignoring any on-screen filters. */
    @GetMapping("/sheets/{sheetId}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long sheetId,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        SdSheetResponse sheet = sheetService.get(sheetId);
        List<SdItemResponse> items = itemService.listForSheet(
                sheetId, currentUserResolver.resolve(userIdHeader), null, null, null, null, null, null);
        return ExcelDownload.attachment(sheet.getName(), excelExporter.export(sheet.getName(), items));
    }

    @GetMapping("/sheets/{sheetId}/stats")
    public SdSheetStatsResponse stats(@PathVariable Long sheetId,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return itemService.stats(sheetId, currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/items/{id}")
    public SdItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody SdItemRequest request,
                                      @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return itemService.updateItem(id, request, currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/items/{id}/progress")
    public SdItemResponse updateProgress(@PathVariable Long id, @RequestBody SdProgressUpdateRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return itemService.updateProgress(id, currentUserResolver.resolve(userIdHeader), request);
    }
}
