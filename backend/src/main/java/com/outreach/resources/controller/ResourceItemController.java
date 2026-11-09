package com.outreach.resources.controller;

import com.outreach.resources.dto.ResourceItemImportantRequest;
import com.outreach.resources.dto.ResourceItemRequest;
import com.outreach.resources.dto.ResourceItemResponse;
import com.outreach.resources.service.ResourceExcelExporter;
import com.outreach.resources.service.ResourceItemService;
import com.outreach.util.ExcelDownload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources/items")
@RequiredArgsConstructor
public class ResourceItemController {

    private final ResourceItemService itemService;
    private final ResourceExcelExporter excelExporter;

    @GetMapping
    public List<ResourceItemResponse> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) String subCategory,
                                            @RequestParam(required = false) Boolean important,
                                            @RequestParam(required = false) String search) {
        return itemService.list(category, subCategory, important, search);
    }

    /** Every resource in the library as .xlsx, ignoring any on-screen filters. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return ExcelDownload.attachment("Free Resources", excelExporter.export(itemService.list(null, null, null, null)));
    }

    @PostMapping
    public ResourceItemResponse create(@Valid @RequestBody ResourceItemRequest request) {
        return itemService.create(request);
    }

    @PutMapping("/{id}")
    public ResourceItemResponse update(@PathVariable Long id, @Valid @RequestBody ResourceItemRequest request) {
        return itemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        itemService.delete(id);
    }

    @PutMapping("/{id}/important")
    public ResourceItemResponse toggleImportant(@PathVariable Long id, @Valid @RequestBody ResourceItemImportantRequest request) {
        return itemService.toggleImportant(id, request.getImportant());
    }
}
