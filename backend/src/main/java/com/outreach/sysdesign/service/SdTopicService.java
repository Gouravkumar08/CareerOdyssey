package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdSubTopicResponse;
import com.outreach.sysdesign.dto.SdTopicResponse;
import com.outreach.sysdesign.exception.SdResourceNotFoundException;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.repository.SdItemRepository;
import com.outreach.sysdesign.repository.SdSheetItemRepository;
import com.outreach.sysdesign.repository.SdTopicRepository;
import com.outreach.sysdesign.repository.UserSdItemProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SdTopicService {

    private final SdTopicRepository topicRepository;
    private final SdItemRepository itemRepository;
    private final SdSheetItemRepository sheetItemRepository;
    private final UserSdItemProgressRepository progressRepository;

    @Transactional
    public SdTopic findOrCreate(String name) {
        String trimmed = name.trim();
        return topicRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> topicRepository.save(SdTopic.builder()
                        .name(trimmed)
                        .orderIndex((int) topicRepository.count())
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<SdTopicResponse> list() {
        return topicRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .map(t -> SdTopicResponse.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .orderIndex(t.getOrderIndex())
                        .itemCount(itemRepository.countByTopicId(t.getId()))
                        .build())
                .toList();
    }

    @Transactional
    public SdTopicResponse rename(Long id, String name) {
        SdTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new SdResourceNotFoundException("Topic not found: " + id));
        topic.setName(name.trim());
        SdTopic saved = topicRepository.save(topic);
        return SdTopicResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .orderIndex(saved.getOrderIndex())
                .itemCount(itemRepository.countByTopicId(saved.getId()))
                .build();
    }

    /**
     * Deletes a topic and every item under it, across every sheet — items are shared
     * catalog entries, not sheet-scoped, so this cascades sheet-links and progress too.
     */
    @Transactional
    public void deleteCascade(Long id) {
        if (!topicRepository.existsById(id)) {
            throw new SdResourceNotFoundException("Topic not found: " + id);
        }
        List<SdItem> items = itemRepository.findByTopicId(id);
        List<Long> itemIds = items.stream().map(SdItem::getId).toList();
        if (!itemIds.isEmpty()) {
            progressRepository.deleteByItemIdIn(itemIds);
            sheetItemRepository.deleteByItemIdIn(itemIds);
            itemRepository.deleteAll(items);
        }
        topicRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SdSubTopicResponse> listSubTopics(Long topicId) {
        List<SdItem> items = itemRepository.findByTopicId(topicId);
        return items.stream()
                .filter(i -> StringUtils.hasText(i.getSubTopic()))
                .collect(Collectors.groupingBy(SdItem::getSubTopic, Collectors.counting()))
                .entrySet().stream()
                .map(e -> SdSubTopicResponse.builder().name(e.getKey()).itemCount(e.getValue()).build())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional
    public void renameSubTopic(Long topicId, String oldName, String newName) {
        List<SdItem> items = itemRepository.findByTopicIdAndSubTopic(topicId, oldName.trim());
        String trimmedNew = newName.trim();
        items.forEach(i -> i.setSubTopic(trimmedNew));
        itemRepository.saveAll(items);
    }

    /**
     * Deletes every item under this (topic, sub-topic) pair — cascades sheet-links and
     * progress, same as {@link #deleteCascade}, just scoped to the sub-topic rather than
     * the whole topic.
     */
    @Transactional
    public void deleteSubTopicCascade(Long topicId, String subTopic) {
        List<SdItem> items = itemRepository.findByTopicIdAndSubTopic(topicId, subTopic.trim());
        List<Long> itemIds = items.stream().map(SdItem::getId).toList();
        if (!itemIds.isEmpty()) {
            progressRepository.deleteByItemIdIn(itemIds);
            sheetItemRepository.deleteByItemIdIn(itemIds);
            itemRepository.deleteAll(items);
        }
    }

    @Transactional
    public void reorder(List<Long> orderedTopicIds) {
        List<SdTopic> topics = topicRepository.findAllById(orderedTopicIds);
        java.util.Map<Long, SdTopic> byId = new java.util.HashMap<>();
        topics.forEach(t -> byId.put(t.getId(), t));
        for (int i = 0; i < orderedTopicIds.size(); i++) {
            SdTopic topic = byId.get(orderedTopicIds.get(i));
            if (topic != null) {
                topic.setOrderIndex(i);
            }
        }
        topicRepository.saveAll(topics);
    }
}
