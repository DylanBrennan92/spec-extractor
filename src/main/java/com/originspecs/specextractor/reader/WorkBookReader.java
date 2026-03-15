package com.originspecs.specextractor.reader;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a pre-processed .xls workbook (output from DataPrep) into a list of SheetData models.
 * Since the input has already been cleaned by DataPrep, each sheet is expected to have:
 * <ul>
 *   <li>Row 0: a single resolved English header row</li>
 *   <li>Rows 1+: data rows with fill-down already applied</li>
 * </ul>
 * No header detection, merged cell expansion, or column filtering is performed here.
 */
@Slf4j
public class WorkBookReader implements WorkbookReader {

    private final DataFormatter formatter = new DataFormatter();

    /**
     * Reads all sheets from the given .xls file.
     *
     * @param inputPath Path to the pre-processed .xls file
     * @return List of SheetData, one per worksheet
     * @throws IOException if the file cannot be read
     */
    public List<SheetData> read(Path inputPath) throws IOException {
        log.info("Reading pre-processed XLS from {}", inputPath.toAbsolutePath());

        try (InputStream is = Files.newInputStream(inputPath);
             Workbook workbook = new HSSFWorkbook(is)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<SheetData> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheets.add(readSheet(sheet, i, evaluator));
            }

            log.info("Read {} sheet(s) from '{}'", sheets.size(), inputPath.getFileName());
            return sheets;
        }
    }

    private SheetData readSheet(Sheet sheet, int index, FormulaEvaluator evaluator) {
        int lastRow = sheet.getLastRowNum();

        if (lastRow < 0) {
            log.warn("Sheet '{}' is empty", sheet.getSheetName());
            return SheetData.empty(sheet.getSheetName(), index);
        }

        Row headerRow = sheet.getRow(0);
        List<String> headers = headerRow != null ? readRow(headerRow, evaluator) : List.of();

        List<RowData> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            rows.add(new RowData(readRow(row, evaluator)));
        }

        log.debug("Sheet '{}': {} column(s), {} data row(s)",
                sheet.getSheetName(), headers.size(), rows.size());

        return new SheetData(sheet.getSheetName(), index, headers, rows);
    }

    private List<String> readRow(Row row, FormulaEvaluator evaluator) {
        int lastCell = row.getLastCellNum();
        List<String> values = new ArrayList<>(Math.max(lastCell, 0));
        for (int i = 0; i < lastCell; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : evaluateCell(cell, evaluator));
        }
        return values;
    }

    /**
     * Evaluates a cell using the formula evaluator, falling back to the cached value on error.
     */
    private String evaluateCell(Cell cell, FormulaEvaluator evaluator) {
        try {
            return formatter.formatCellValue(cell, evaluator).strip();
        } catch (Exception e) {
            log.debug("Formula evaluation failed for cell [{},{}]: {} — using cached value",
                    cell.getRowIndex(), cell.getColumnIndex(), e.getMessage());
            return formatter.formatCellValue(cell).strip();
        }
    }
}
