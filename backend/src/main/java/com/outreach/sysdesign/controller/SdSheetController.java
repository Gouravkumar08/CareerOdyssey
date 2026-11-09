package com.outreach.sysdesign.controller;

import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.sysdesign.dto.SdImportResultResponse;
import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.sysdesign.dto.SdSheetRequest;
import com.outreach.sysdesign.dto.SdSheetResponse;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.service.SdSheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/sysdesign/sheets")
@RequiredArgsConstructor
public class SdSheetController {

    private final SdSheetService sheetService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<SdSheetResponse> create(@Valid @RequestBody SdSheetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.create(request));
    }

    @GetMapping
    public List<SdSheetResponse> list(@RequestParam(required = false) Category category) {
        return sheetService.list(category);
    }

    @GetMapping("/{id}")
    public SdSheetResponse get(@PathVariable Long id) {
        return sheetService.get(id);
    }

    @PutMapping("/{id}")
    public SdSheetResponse update(@PathVariable Long id, @Valid @RequestBody SdSheetRequest request) {
        return sheetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sheetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/import")
    public SdImportResultResponse importSheet(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return sheetService.importExcel(id, file);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<SdItemResponse> addItem(@PathVariable Long id, @Valid @RequestBody SdItemRequest request,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        Long userId = currentUserResolver.resolve(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.addItem(id, request, userId));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        sheetService.removeItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
