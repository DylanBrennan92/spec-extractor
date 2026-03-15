package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a list of SheetData objects into a flat list of SpecRecords.
 * Column order matches the header order in the source sheet.
 * Columns with blank headers and completely empty rows are skipped.
 */
@Slf4j
public class SpecProcessor implements SheetProcessor {

    /**
     * Processes all sheets and returns a combined flat list of records.
     *
     * @param sheets List of SheetData read from the pre-processed workbook
     * @return Flat list of SpecRecords, one per data row across all sheets
     */
    public List<SpecRecord> process(List<SheetData> sheets) {
        List<SpecRecord> records = new ArrayList<>();

        for (SheetData sheet : sheets) {
            List<SpecRecord> sheetRecords = processSheet(sheet);
            records.addAll(sheetRecords);
            log.info("Sheet '{}': extracted {} record(s)", sheet.name(), sheetRecords.size());
        }

        log.info("Total records extracted: {}", records.size());
        return records;
    }

    private List<SpecRecord> processSheet(SheetData sheet) {
        if (sheet.headers().isEmpty()) {
            log.warn("Sheet '{}' has no headers — skipping", sheet.name());
            return List.of();
        }

        List<SpecRecord> records = new ArrayList<>();

        for (RowData row : sheet.rows()) {
            if (row.isEmpty()) {
                log.trace("Skipping empty row in sheet '{}'", sheet.name());
                continue;
            }
            records.add(toRecord(sheet.headers(), row));
        }

        return records;
    }

    private SpecRecord toRecord(List<String> headers, RowData row) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.isBlank()) continue;
            fields.put(header, row.getCell(i));
        }
        return new SpecRecord(fields);
    }
}
