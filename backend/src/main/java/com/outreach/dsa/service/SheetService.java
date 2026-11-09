package com.outreach.dsa.service;

import com.outreach.dsa.dto.ImportResultResponse;
import com.outreach.dsa.dto.QuestionRequest;
import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.dsa.dto.SheetRequest;
import com.outreach.dsa.dto.SheetResponse;
import com.outreach.dsa.exception.ResourceNotFoundException;
import com.outreach.dsa.model.Question;
import com.outreach.dsa.model.Sheet;
import com.outreach.dsa.model.SheetQuestion;
import com.outreach.dsa.model.Topic;
import com.outreach.dsa.model.UserQuestionProgress;
import com.outreach.dsa.repository.QuestionRepository;
import com.outreach.dsa.repository.SheetQuestionRepository;
import com.outreach.dsa.repository.SheetRepository;
import com.outreach.dsa.repository.UserQuestionProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SheetService {

    private final SheetRepository sheetRepository;
    private final SheetQuestionRepository sheetQuestionRepository;
    private final QuestionRepository questionRepository;
    private final UserQuestionProgressRepository progressRepository;
    private final TopicService topicService;
    private final ExcelSheetImporter excelSheetImporter;

    @Transactional
    public SheetResponse create(SheetRequest request) {
        Sheet sheet = Sheet.builder().name(request.getName()).description(request.getDescription()).build();
        return SheetResponse.from(sheetRepository.save(sheet), 0);
    }

    @Transactional(readOnly = true)
    public List<SheetResponse> list() {
        return sheetRepository.findAll().stream()
                .map(sheet -> SheetResponse.from(sheet, sheetQuestionRepository.countBySheetId(sheet.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SheetResponse get(Long id) {
        Sheet sheet = findEntity(id);
        return SheetResponse.from(sheet, sheetQuestionRepository.countBySheetId(id));
    }

    @Transactional
    public SheetResponse update(Long id, SheetRequest request) {
        Sheet sheet = findEntity(id);
        sheet.setName(request.getName());
        sheet.setDescription(request.getDescription());
        Sheet saved = sheetRepository.save(sheet);
        return SheetResponse.from(saved, sheetQuestionRepository.countBySheetId(id));
    }

    @Transactional
    public void delete(Long id) {
        findEntity(id);
        sheetQuestionRepository.deleteBySheetId(id);
        sheetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Sheet findEntity(Long id) {
        return sheetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sheet not found: " + id));
    }

    /**
     * Manually mixes a question into a sheet — either an existing catalog question (matched by
     * topic + title) or a brand-new one, created on the fly.
     */
    @Transactional
    public QuestionResponse addQuestion(Long sheetId, QuestionRequest request, Long userId) {
        Sheet sheet = findEntity(sheetId);
        Topic topic = topicService.findOrCreate(request.getTopicName());
        Question question = questionRepository.findByTopicIdAndTitleIgnoreCase(topic.getId(), request.getTitle().trim())
                .orElseGet(() -> questionRepository.save(Question.builder()
                        .topicId(topic.getId())
                        .subTopic(request.getSubTopic())
                        .title(request.getTitle().trim())
                        .difficulty(request.getDifficulty())
                        .important(request.isImportant())
                        .practiceLink(request.getPracticeLink())
                        .youtubeLink(request.getYoutubeLink())
                        .articleLink(request.getArticleLink())
                        .build()));

        if (!sheetQuestionRepository.existsBySheetIdAndQuestionId(sheet.getId(), question.getId())) {
            int nextOrder = (int) sheetQuestionRepository.countBySheetId(sheet.getId());
            sheetQuestionRepository.save(SheetQuestion.builder()
                    .sheetId(sheet.getId())
                    .questionId(question.getId())
                    .orderIndex(nextOrder)
                    .build());
        }

        UserQuestionProgress progress = progressRepository.findByUserIdAndQuestionId(userId, question.getId()).orElse(null);
        return QuestionResponse.from(question, topic.getName(), progress);
    }

    @Transactional
    public void removeQuestion(Long sheetId, Long questionId) {
        findEntity(sheetId);
        sheetQuestionRepository.deleteBySheetIdAndQuestionId(sheetId, questionId);
    }

    @Transactional
    public ImportResultResponse importExcel(Long sheetId, MultipartFile file) {
        Sheet sheet = findEntity(sheetId);
        ExcelSheetImporter.ParseResult parsed = excelSheetImporter.parse(file);

        List<String> skippedReasons = new ArrayList<>(parsed.skippedReasons());
        int imported = 0;
        int nextOrder = (int) sheetQuestionRepository.countBySheetId(sheet.getId());

        for (ExcelSheetImporter.ImportedRow row : parsed.rows()) {
            Topic topic = topicService.findOrCreate(row.topicName());
            Question question = questionRepository.findByTopicIdAndTitleIgnoreCase(topic.getId(), row.title())
                    .orElseGet(() -> questionRepository.save(Question.builder()
                            .topicId(topic.getId())
                            .subTopic(row.subTopic())
                            .title(row.title())
                            .difficulty(row.difficulty())
                            .important(false)
                            .practiceLink(row.link())
                            .build()));

            if (sheetQuestionRepository.existsBySheetIdAndQuestionId(sheet.getId(), question.getId())) {
                skippedReasons.add("\"" + row.title() + "\": already in this sheet");
                continue;
            }

            sheetQuestionRepository.save(SheetQuestion.builder()
                    .sheetId(sheet.getId())
                    .questionId(question.getId())
                    .orderIndex(nextOrder++)
                    .build());
            imported++;
        }

        return ImportResultResponse.builder()
                .importedCount(imported)
                .skippedCount(skippedReasons.size())
                .skippedReasons(skippedReasons)
                .build();
    }
}
