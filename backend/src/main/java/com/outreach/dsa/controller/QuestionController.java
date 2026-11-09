package com.outreach.dsa.controller;

import com.outreach.dsa.dto.ProgressUpdateRequest;
import com.outreach.dsa.dto.QuestionRequest;
import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.dsa.dto.SheetResponse;
import com.outreach.dsa.dto.SheetStatsResponse;
import com.outreach.dsa.model.Difficulty;
import com.outreach.dsa.service.CurrentUserResolver;
import com.outreach.dsa.service.ExcelSheetExporter;
import com.outreach.dsa.service.QuestionService;
import com.outreach.dsa.service.SheetService;
import com.outreach.util.ExcelDownload;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dsa")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final SheetService sheetService;
    private final ExcelSheetExporter excelExporter;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/sheets/{sheetId}/questions")
    public List<QuestionResponse> list(@PathVariable Long sheetId,
                                        @RequestParam(required = false) String topic,
                                        @RequestParam(required = false) Difficulty difficulty,
                                        @RequestParam(required = false) Boolean important,
                                        @RequestParam(required = false) Boolean revision,
                                        @RequestParam(required = false) Boolean solved,
                                        @RequestParam(required = false) String search,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        Long userId = currentUserResolver.resolve(userIdHeader);
        return questionService.listForSheet(sheetId, userId, topic, difficulty, important, revision, solved, search);
    }

    /** The whole sheet as .xlsx — every question plus the user's own progress — ignoring any on-screen filters. */
    @GetMapping("/sheets/{sheetId}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long sheetId,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        SheetResponse sheet = sheetService.get(sheetId);
        List<QuestionResponse> questions = questionService.listForSheet(
                sheetId, currentUserResolver.resolve(userIdHeader), null, null, null, null, null, null);
        return ExcelDownload.attachment(sheet.getName(), excelExporter.export(sheet.getName(), questions));
    }

    @GetMapping("/sheets/{sheetId}/stats")
    public SheetStatsResponse stats(@PathVariable Long sheetId,
                                     @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return questionService.stats(sheetId, currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/questions/{id}")
    public QuestionResponse updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return questionService.updateQuestion(id, request, currentUserResolver.resolve(userIdHeader));
    }

    @PutMapping("/questions/{id}/progress")
    public QuestionResponse updateProgress(@PathVariable Long id, @RequestBody ProgressUpdateRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) Long userIdHeader) {
        return questionService.updateProgress(id, currentUserResolver.resolve(userIdHeader), request);
    }
}
