package com.outreach.resources.service;

import com.outreach.resources.dto.ResourceItemResponse;
import com.outreach.util.ExcelTableWriter;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.outreach.util.ExcelTableWriter.yesNo;

/** Writes the whole Free Resources library to .xlsx, one row per link. */
@Component
public class ResourceExcelExporter {

    private static final String[] HEADERS = {"Category", "Sub-category", "Name", "Link", "Important"};

    private static final int[] WIDTHS = {32, 24, 40, 70, 11};

    public byte[] export(List<ResourceItemResponse> items) {
        List<String[]> rows = items.stream().map(i -> new String[]{
                i.getCategoryName(), i.getSubCategory(), i.getName(), i.getLink(), yesNo(i.isImportant())
        }).toList();
        return ExcelTableWriter.write("Free Resources", HEADERS, WIDTHS, rows);
    }
}
