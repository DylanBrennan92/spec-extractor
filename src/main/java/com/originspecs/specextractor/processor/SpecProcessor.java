package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a list of SheetData objects into a flat list of spec records.
 * Each record is a {@link LinkedHashMap} of header name → cell value, preserving column order.
 * Columns with blank headers and completely empty rows are skipped.
 */
@Slf4j
public class SpecProcessor {

    /**
     * Processes all sheets and returns a combined flat list of records from all of them.
     *
     * @param sheets List of SheetData read from the pre-processed workbook
     * @return Flat list of records, one map per data row across all sheets
     */
    public List<Map<String, String>> process(List<SheetData> sheets) {
        List<Map<String, String>> records = new ArrayList<>();

        for (SheetData sheet : sheets) {
            List<Map<String, String>> sheetRecords = processSheet(sheet);
            records.addAll(sheetRecords);
            log.info("Sheet '{}': extracted {} record(s)", sheet.getName(), sheetRecords.size());
        }

        log.info("Total records extracted: {}", records.size());
        return records;
    }

    private List<Map<String, String>> processSheet(SheetData sheet) {
        List<String> headers = sheet.getHeaders();

        if (headers.isEmpty()) {
            log.warn("Sheet '{}' has no headers — skipping", sheet.getName());
            return List.of();
        }

        List<Map<String, String>> records = new ArrayList<>();

        for (RowData row : sheet.getRows()) {
            if (row.isEmpty()) {
                log.trace("Skipping empty row in sheet '{}'", sheet.getName());
                continue;
            }
            records.add(toMap(headers, row));
        }

        return records;
    }

    private Map<String, String> toMap(List<String> headers, RowData row) {
        Map<String, String> record = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.isBlank()) continue;
            record.put(header, row.getCell(i));
        }
        return record;
    }
}
