package com.outreach.controller;

import com.outreach.dto.OutreachResponse;
import com.outreach.dto.OutreachSendRequest;
import com.outreach.dto.ReplyStatusUpdateRequest;
import com.outreach.model.OutreachStatus;
import com.outreach.model.RecipientType;
import com.outreach.service.OutreachService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/outreach")
@RequiredArgsConstructor
public class OutreachController {

    private final OutreachService outreachService;

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<OutreachResponse> send(@Valid @ModelAttribute OutreachSendRequest request,
                                        @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile) {
        return outreachService.send(request, resumeFile);
    }

    @GetMapping("/{id}")
    public OutreachResponse get(@PathVariable Long id) {
        return outreachService.get(id);
    }

    @GetMapping("/{id}/resend-request")
    public OutreachSendRequest resendRequest(@PathVariable Long id,
                                              @RequestParam(defaultValue = "false") boolean followUp) {
        return outreachService.buildResendRequest(id, followUp);
    }

    @GetMapping
    public List<OutreachResponse> search(@RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) Long companyId,
                                          @RequestParam(required = false) OutreachStatus status,
                                          @RequestParam(required = false) RecipientType recipientType,
                                          @RequestParam(required = false) Boolean replyReceived,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant sentFrom,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant sentTo) {
        return outreachService.search(userId, companyId, status, recipientType, replyReceived, sentFrom, sentTo);
    }

    @PatchMapping("/{id}/reply-status")
    public OutreachResponse updateReplyStatus(@PathVariable Long id, @Valid @RequestBody ReplyStatusUpdateRequest request) {
        return outreachService.updateReplyStatus(id, request.getReplyStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        outreachService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        outreachService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
