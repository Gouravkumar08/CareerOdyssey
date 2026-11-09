package com.outreach.controller;

import com.outreach.dto.UserRequest;
import com.outreach.dto.UserResponse;
import com.outreach.service.ResumeParsingService;
import com.outreach.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ResumeParsingService resumeParsingService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) String search) {
        return userService.list(search);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/parse-resume", consumes = "multipart/form-data")
    public UserRequest parseResume(@RequestParam("resumeFile") MultipartFile resumeFile) {
        return resumeParsingService.parse(resumeFile);
    }
}
