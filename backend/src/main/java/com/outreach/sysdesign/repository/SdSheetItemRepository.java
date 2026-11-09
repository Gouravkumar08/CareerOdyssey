package com.outreach.sysdesign.repository;

import com.outreach.sysdesign.model.SdSheetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SdSheetItemRepository extends JpaRepository<SdSheetItem, Long> {
    List<SdSheetItem> findBySheetIdOrderByOrderIndexAsc(Long sheetId);

    Optional<SdSheetItem> findBySheetIdAndItemId(Long sheetId, Long itemId);

    boolean existsBySheetIdAndItemId(Long sheetId, Long itemId);

    long countBySheetId(Long sheetId);

    void deleteBySheetIdAndItemId(Long sheetId, Long itemId);

    void deleteBySheetId(Long sheetId);

    void deleteByItemIdIn(List<Long> itemIds);
}
