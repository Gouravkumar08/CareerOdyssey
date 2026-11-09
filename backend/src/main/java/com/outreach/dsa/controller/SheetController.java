package com.outreach.dsa.controller;

import com.outreach.dsa.dto.ImportResultResponse;
import com.outreach.dsa.dto.QuestionRequest;
import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.dsa.dto.SheetRequest;
import com.outreach.dsa.dto.SheetResponse;
import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.dsa.service.SheetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dsa/sheets")
@RequiredArgsConstructor
public class SheetController {

    private final SheetService sheetService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ResponseEntity<SheetResponse> create(@Valid @RequestBody SheetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.create(request));
    }

    @GetMapping
    public List<SheetResponse> list() {
        return sheetService.list();
    }

    @GetMapping("/{id}")
    public SheetResponse get(@PathVariable Long id) {
        return sheetService.get(id);
    }

    @PutMapping("/{id}")
    public SheetResponse update(@PathVariable Long id, @Valid @RequestBody SheetRequest request) {
        return sheetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sheetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/import")
    public ImportResultResponse importSheet(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return sheetService.importExcel(id, file);
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request,
                                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        Long userId = currentUserResolver.resolve(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(sheetService.addQuestion(id, request, userId));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public ResponseEntity<Void> removeQuestion(@PathVariable Long id, @PathVariable Long questionId) {
        sheetService.removeQuestion(id, questionId);
        return ResponseEntity.noContent().build();
    }
}
