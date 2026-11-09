package com.outreach.dsa.service;

import com.outreach.dsa.dto.QuestionResponse;
import com.outreach.util.ExcelTableWriter;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.outreach.util.ExcelTableWriter.yesNo;

/**
 * Writes a DSA sheet to .xlsx. The first five columns use the headers {@link ExcelSheetImporter}
 * reads (the practice link comes first, so it is the one picked up as "Link"), so an exported file
 * can be imported back as a new sheet; the remaining columns carry the user's progress.
 */
@Component
public class ExcelSheetExporter {

    private static final String[] HEADERS = {
            "Topic", "Sub-topic", "Question", "Difficulty", "Link", "YouTube Link", "Article Link",
            "Important", "Solved", "Solved At", "Revision", "Note", "GitHub Solution Link"
    };

    private static final int[] WIDTHS = {22, 24, 48, 11, 50, 40, 40, 11, 8, 18, 10, 40, 40};

    private static final DateTimeFormatter SOLVED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] export(String sheetName, List<QuestionResponse> questions) {
        List<String[]> rows = questions.stream().map(q -> new String[]{
                q.getTopicName(), q.getSubTopic(), q.getTitle(), q.getDifficulty().name(),
                q.getPracticeLink(), q.getYoutubeLink(), q.getArticleLink(),
                yesNo(q.isImportant()), yesNo(q.isSolved()),
                q.getSolvedAt() == null ? null : SOLVED_AT.format(q.getSolvedAt()),
                yesNo(q.isRevisionStarred()), q.getNote(), q.getGithubSolutionLink()
        }).toList();
        return ExcelTableWriter.write(sheetName, HEADERS, WIDTHS, rows);
    }
}
