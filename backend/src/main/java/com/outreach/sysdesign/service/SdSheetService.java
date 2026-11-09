package com.outreach.sysdesign.service;

import com.outreach.sysdesign.dto.SdImportResultResponse;
import com.outreach.sysdesign.dto.SdItemRequest;
import com.outreach.sysdesign.dto.SdItemResponse;
import com.outreach.sysdesign.dto.SdSheetRequest;
import com.outreach.sysdesign.dto.SdSheetResponse;
import com.outreach.sysdesign.exception.SdResourceNotFoundException;
import com.outreach.sysdesign.model.Category;
import com.outreach.sysdesign.model.SdItem;
import com.outreach.sysdesign.model.SdSheet;
import com.outreach.sysdesign.model.SdSheetItem;
import com.outreach.sysdesign.model.SdTopic;
import com.outreach.sysdesign.model.UserSdItemProgress;
import com.outreach.sysdesign.repository.SdItemRepository;
import com.outreach.sysdesign.repository.SdSheetItemRepository;
import com.outreach.sysdesign.repository.SdSheetRepository;
import com.outreach.sysdesign.repository.UserSdItemProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SdSheetService {

    private final SdSheetRepository sheetRepository;
    private final SdSheetItemRepository sheetItemRepository;
    private final SdItemRepository itemRepository;
    private final UserSdItemProgressRepository progressRepository;
    private final SdTopicService topicService;
    private final SdExcelSheetImporter excelSheetImporter;

    @Transactional
    public SdSheetResponse create(SdSheetRequest request) {
        SdSheet sheet = SdSheet.builder().name(request.getName()).description(request.getDescription()).build();
        return SdSheetResponse.from(sheetRepository.save(sheet), 0);
    }

    @Transactional(readOnly = true)
    public List<SdSheetResponse> list() {
        return list(null);
    }

    /**
     * Filters to sheets that contain at least one item of the given category — lets the
     * frontend hide, e.g., the HLD Roadmap sheet while the user is viewing the LLD tab.
     */
    @Transactional(readOnly = true)
    public List<SdSheetResponse> list(Category category) {
        return sheetRepository.findAll().stream()
                .filter(sheet -> category == null || sheetHasCategory(sheet.getId(), category))
                .map(sheet -> SdSheetResponse.from(sheet, sheetItemRepository.countBySheetId(sheet.getId())))
                .toList();
    }

    private boolean sheetHasCategory(Long sheetId, Category category) {
        List<Long> itemIds = sheetItemRepository.findBySheetIdOrderByOrderIndexAsc(sheetId).stream()
                .map(SdSheetItem::getItemId)
                .toList();
        if (itemIds.isEmpty()) {
            return true; // an empty sheet isn't excluded from either tab
        }
        return itemRepository.findByIdIn(itemIds).stream().anyMatch(i -> i.getCategory() == category);
    }

    @Transactional(readOnly = true)
    public SdSheetResponse get(Long id) {
        SdSheet sheet = findEntity(id);
        return SdSheetResponse.from(sheet, sheetItemRepository.countBySheetId(id));
    }

    @Transactional
    public SdSheetResponse update(Long id, SdSheetRequest request) {
        SdSheet sheet = findEntity(id);
        sheet.setName(request.getName());
        sheet.setDescription(request.getDescription());
        SdSheet saved = sheetRepository.save(sheet);
        return SdSheetResponse.from(saved, sheetItemRepository.countBySheetId(id));
    }

    @Transactional
    public void delete(Long id) {
        findEntity(id);
        sheetItemRepository.deleteBySheetId(id);
        sheetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SdSheet findEntity(Long id) {
        return sheetRepository.findById(id)
                .orElseThrow(() -> new SdResourceNotFoundException("Sheet not found: " + id));
    }

    /**
     * Manually mixes an item into a sheet — either an existing catalog item (matched by
     * topic + title) or a brand-new one, created on the fly.
     */
    @Transactional
    public SdItemResponse addItem(Long sheetId, SdItemRequest request, Long userId) {
        SdSheet sheet = findEntity(sheetId);
        SdTopic topic = topicService.findOrCreate(request.getTopicName());
        SdItem item = itemRepository.findByTopicIdAndTitleIgnoreCase(topic.getId(), request.getTitle().trim())
                .orElseGet(() -> itemRepository.save(SdItem.builder()
                        .topicId(topic.getId())
                        .subTopic(request.getSubTopic())
                        .title(request.getTitle().trim())
                        .category(request.getCategory())
                        .important(request.isImportant())
                        .resourceLink(request.getResourceLink())
                        .description(request.getDescription())
                        .build()));

        if (!sheetItemRepository.existsBySheetIdAndItemId(sheet.getId(), item.getId())) {
            int nextOrder = (int) sheetItemRepository.countBySheetId(sheet.getId());
            sheetItemRepository.save(SdSheetItem.builder()
                    .sheetId(sheet.getId())
                    .itemId(item.getId())
                    .orderIndex(nextOrder)
                    .build());
        }

        UserSdItemProgress progress = progressRepository.findByUserIdAndItemId(userId, item.getId()).orElse(null);
        return SdItemResponse.from(item, topic.getName(), progress);
    }

    @Transactional
    public void removeItem(Long sheetId, Long itemId) {
        findEntity(sheetId);
        sheetItemRepository.deleteBySheetIdAndItemId(sheetId, itemId);
    }

    /**
     * Bulk-imports items from an uploaded .xlsx sheet — mirrors
     * {@code com.outreach.dsa.service.SheetService#importExcel}: rows matching an existing
     * catalog item (by topic + title) are reused rather than duplicated, and each newly-added
     * row's Category (HLD/LLD) comes from its own column, not the currently active tab, so a
     * single sheet can mix both in one import.
     */
    @Transactional
    public SdImportResultResponse importExcel(Long sheetId, MultipartFile file) {
        SdSheet sheet = findEntity(sheetId);
        SdExcelSheetImporter.ParseResult parsed = excelSheetImporter.parse(file);

        List<String> skippedReasons = new ArrayList<>(parsed.skippedReasons());
        int imported = 0;
        int nextOrder = (int) sheetItemRepository.countBySheetId(sheet.getId());

        for (SdExcelSheetImporter.ImportedRow row : parsed.rows()) {
            SdTopic topic = topicService.findOrCreate(row.topicName());
            SdItem item = itemRepository.findByTopicIdAndTitleIgnoreCase(topic.getId(), row.title())
                    .orElseGet(() -> itemRepository.save(SdItem.builder()
                            .topicId(topic.getId())
                            .subTopic(row.subTopic())
                            .title(row.title())
                            .category(row.category())
                            .important(false)
                            .resourceLink(row.link())
                            .description(row.description())
                            .build()));

            if (sheetItemRepository.existsBySheetIdAndItemId(sheet.getId(), item.getId())) {
                skippedReasons.add("\"" + row.title() + "\": already in this sheet");
                continue;
            }

            sheetItemRepository.save(SdSheetItem.builder()
                    .sheetId(sheet.getId())
                    .itemId(item.getId())
                    .orderIndex(nextOrder++)
                    .build());
            imported++;
        }

        return SdImportResultResponse.builder()
                .importedCount(imported)
                .skippedCount(skippedReasons.size())
                .skippedReasons(skippedReasons)
                .build();
    }
}
