package com.outreach.dsa.repository;

import com.outreach.dsa.model.SheetQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SheetQuestionRepository extends JpaRepository<SheetQuestion, Long> {
    List<SheetQuestion> findBySheetIdOrderByOrderIndexAsc(Long sheetId);

    Optional<SheetQuestion> findBySheetIdAndQuestionId(Long sheetId, Long questionId);

    boolean existsBySheetIdAndQuestionId(Long sheetId, Long questionId);

    long countBySheetId(Long sheetId);

    void deleteBySheetIdAndQuestionId(Long sheetId, Long questionId);

    void deleteBySheetId(Long sheetId);
}
