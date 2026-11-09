package com.outreach.sysdesign.controller;

import com.outreach.sysdesign.dto.SdSubTopicRenameRequest;
import com.outreach.sysdesign.dto.SdSubTopicResponse;
import com.outreach.sysdesign.dto.SdTopicRequest;
import com.outreach.sysdesign.dto.SdTopicResponse;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.service.SdTopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sysdesign/topics")
@RequiredArgsConstructor
public class SdTopicController {

    private final SdTopicService topicService;

    @GetMapping
    public List<SdTopicResponse> list() {
        return topicService.list();
    }

    @PostMapping
    public SdTopicResponse create(@Valid @RequestBody SdTopicRequest request) {
        SdTopic topic = topicService.findOrCreate(request.getName());
        return topicService.list().stream()
                .filter(t -> t.getId().equals(topic.getId()))
                .findFirst()
                .orElseThrow();
    }

    @PutMapping("/{id}")
    public SdTopicResponse rename(@PathVariable Long id, @Valid @RequestBody SdTopicRequest request) {
        return topicService.rename(id, request.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        topicService.deleteCascade(id);
    }

    @PutMapping("/reorder")
    public void reorder(@RequestBody List<Long> orderedTopicIds) {
        topicService.reorder(orderedTopicIds);
    }

    @GetMapping("/{id}/subtopics")
    public List<SdSubTopicResponse> listSubTopics(@PathVariable Long id) {
        return topicService.listSubTopics(id);
    }

    @PutMapping("/{id}/subtopics/rename")
    public List<SdSubTopicResponse> renameSubTopic(@PathVariable Long id, @Valid @RequestBody SdSubTopicRenameRequest request) {
        topicService.renameSubTopic(id, request.getOldName(), request.getNewName());
        return topicService.listSubTopics(id);
    }

    @DeleteMapping("/{id}/subtopics")
    public List<SdSubTopicResponse> deleteSubTopic(@PathVariable Long id, @RequestParam String name) {
        topicService.deleteSubTopicCascade(id, name);
        return topicService.listSubTopics(id);
    }
}
