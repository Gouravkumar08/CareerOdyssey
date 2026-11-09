package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdCategoryStats;
import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.sysdesign.dto.SdProgressUpdateRequest;
import com.outreach.sysdesign.dto.SdSheetStatsResponse;
import com.outreach.sysdesign.exception.SdResourceNotFoundException;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.SdSheetItem;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.model.UserSdItemProgress;
import com.outreach.sysdesign.repository.SdItemRepository;
import com.outreach.sysdesign.repository.SdSheetItemRepository;
import com.outreach.sysdesign.repository.SdTopicRepository;
import com.outreach.sysdesign.repository.UserSdItemProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SdItemService {

    private final SdItemRepository itemRepository;
    private final SdTopicRepository topicRepository;
    private final SdSheetItemRepository sheetItemRepository;
    private final UserSdItemProgressRepository progressRepository;
    private final SdTopicService topicService;

    @Transactional(readOnly = true)
    public List<SdItemResponse> listForSheet(Long sheetId, Long userId, String topicName, Category category,
                                              Boolean important, Boolean revision, Boolean completed, String search) {
        List<SdSheetItem> links = sheetItemRepository.findBySheetIdOrderByOrderIndexAsc(sheetId);
        List<Long> itemIds = links.stream().map(SdSheetItem::getItemId).toList();
        if (itemIds.isEmpty()) {
            return List.of();
        }

        Map<Long, SdItem> itemsById = new HashMap<>();
        itemRepository.findByIdIn(itemIds).forEach(i -> itemsById.put(i.getId(), i));

        Map<Long, SdTopic> topicsById = new HashMap<>();
        topicRepository.findAllById(itemsById.values().stream().map(SdItem::getTopicId).distinct().toList())
                .forEach(t -> topicsById.put(t.getId(), t));

        Map<Long, UserSdItemProgress> progressByItemId = new HashMap<>();
        progressRepository.findByUserIdAndItemIdIn(userId, itemIds)
                .forEach(p -> progressByItemId.put(p.getItemId(), p));

        String needle = StringUtils.hasText(search) ? search.trim().toLowerCase() : null;

        return links.stream()
                .map(link -> itemsById.get(link.getItemId()))
                .filter(i -> i != null)
                .filter(i -> topicName == null || topicsById.get(i.getTopicId()) != null
                        && topicsById.get(i.getTopicId()).getName().equalsIgnoreCase(topicName))
                .filter(i -> category == null || i.getCategory() == category)
                .filter(i -> important == null || i.isImportant() == important)
                .filter(i -> {
                    if (revision == null) return true;
                    UserSdItemProgress p = progressByItemId.get(i.getId());
                    return revision == (p != null && p.isRevisionStarred());
                })
                .filter(i -> {
                    if (completed == null) return true;
                    UserSdItemProgress p = progressByItemId.get(i.getId());
                    return completed == (p != null && p.isCompleted());
                })
                .filter(i -> needle == null
                        || i.getTitle().toLowerCase().contains(needle)
                        || (i.getSubTopic() != null && i.getSubTopic().toLowerCase().contains(needle))
                        || topicsById.get(i.getTopicId()).getName().toLowerCase().contains(needle))
                .map(i -> SdItemResponse.from(i, topicsById.get(i.getTopicId()).getName(), progressByItemId.get(i.getId())))
                .toList();
    }

    @Transactional
    public SdItemResponse updateItem(Long id, SdItemRequest request, Long userId) {
        SdItem item = findEntity(id);
        SdTopic topic = topicService.findOrCreate(request.getTopicName());
        item.setTopicId(topic.getId());
        item.setSubTopic(request.getSubTopic());
        item.setTitle(request.getTitle());
        item.setCategory(request.getCategory());
        item.setImportant(request.isImportant());
        item.setResourceLink(request.getResourceLink());
        item.setDescription(request.getDescription());
        SdItem saved = itemRepository.save(item);
        UserSdItemProgress progress = progressRepository.findByUserIdAndItemId(userId, id).orElse(null);
        return SdItemResponse.from(saved, topic.getName(), progress);
    }

    @Transactional
    public SdItemResponse updateProgress(Long id, Long userId, SdProgressUpdateRequest request) {
        SdItem item = findEntity(id);
        SdTopic topic = topicRepository.findById(item.getTopicId())
                .orElseThrow(() -> new SdResourceNotFoundException("Topic not found: " + item.getTopicId()));

        UserSdItemProgress progress = progressRepository.findByUserIdAndItemId(userId, id)
                .orElseGet(() -> UserSdItemProgress.builder()
                        .userId(userId)
                        .itemId(id)
                        .completed(false)
                        .revisionStarred(false)
                        .build());

        if (request.getCompleted() != null) {
            progress.setCompleted(request.getCompleted());
            progress.setCompletedAt(request.getCompleted() ? Instant.now() : null);
        }
        if (request.getRevisionStarred() != null) {
            progress.setRevisionStarred(request.getRevisionStarred());
        }
        if (request.getNote() != null) {
            progress.setNote(request.getNote());
        }
        if (request.getGithubSolutionLink() != null) {
            progress.setGithubSolutionLink(request.getGithubSolutionLink());
        }

        UserSdItemProgress saved = progressRepository.save(progress);
        return SdItemResponse.from(item, topic.getName(), saved);
    }

    @Transactional(readOnly = true)
    public SdSheetStatsResponse stats(Long sheetId, Long userId) {
        List<Long> itemIds = sheetItemRepository.findBySheetIdOrderByOrderIndexAsc(sheetId).stream()
                .map(SdSheetItem::getItemId)
                .toList();
        List<SdItem> items = itemRepository.findByIdIn(itemIds);
        Map<Long, UserSdItemProgress> progressByItemId = new HashMap<>();
        progressRepository.findByUserIdAndItemIdIn(userId, itemIds)
                .forEach(p -> progressByItemId.put(p.getItemId(), p));

        long topicsCount = items.stream().map(SdItem::getTopicId).distinct().count();
        long completedTotal = items.stream().filter(i -> isCompleted(i, progressByItemId)).count();
        long importantTotal = items.stream().filter(SdItem::isImportant).count();
        long importantSolved = items.stream().filter(SdItem::isImportant).filter(i -> isCompleted(i, progressByItemId)).count();

        SdCategoryStats hld = categoryStats(items, progressByItemId, Category.HLD);
        SdCategoryStats lld = categoryStats(items, progressByItemId, Category.LLD);

        return SdSheetStatsResponse.builder()
                .total(items.size())
                .completed(completedTotal)
                .overallPercent(items.isEmpty() ? 0 : (int) Math.round(completedTotal * 100.0 / items.size()))
                .topicsCount(topicsCount)
                .importantTotal(importantTotal)
                .importantSolved(importantSolved)
                .hldTotal(hld.getTotal())
                .hldCompleted(hld.getCompleted())
                .lldTotal(lld.getTotal())
                .lldCompleted(lld.getCompleted())
                .build();
    }

    private SdCategoryStats categoryStats(List<SdItem> items, Map<Long, UserSdItemProgress> progressByItemId, Category category) {
        long total = items.stream().filter(i -> i.getCategory() == category).count();
        long completed = items.stream().filter(i -> i.getCategory() == category).filter(i -> isCompleted(i, progressByItemId)).count();
        return SdCategoryStats.builder().total(total).completed(completed).build();
    }

    private boolean isCompleted(SdItem item, Map<Long, UserSdItemProgress> progressByItemId) {
        UserSdItemProgress p = progressByItemId.get(item.getId());
        return p != null && p.isCompleted();
    }

    @Transactional(readOnly = true)
    public SdItem findEntity(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new SdResourceNotFoundException("Item not found: " + id));
    }
}
