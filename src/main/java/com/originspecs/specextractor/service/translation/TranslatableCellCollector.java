package com.originspecs.specextractor.service.translation;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Walks sheets and collects cell positions that the predicate marks for translation.
 */
public final class TranslatableCellCollector {

    private final TranslatableCellPredicate predicate;

    public TranslatableCellCollector(TranslatableCellPredicate predicate) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
    }

    public List<TranslatableCellPosition> collect(List<SheetData> sheets) {
        List<TranslatableCellPosition> positions = new ArrayList<>();

        for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
            List<RowData> rows = sheets.get(sheetIndex).rows();

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<String> cells = rows.get(rowIndex).cellValues();

                for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                    String cell = cells.get(colIndex);
                    if (predicate.shouldTranslate(cell)) {
                        positions.add(new TranslatableCellPosition(sheetIndex, rowIndex, colIndex, cell));
                    }
                }
            }
        }

        return positions;
    }

    public static int countNonBlankCells(List<SheetData> sheets) {
        int count = 0;
        for (SheetData sheet : sheets) {
            for (RowData row : sheet.rows()) {
                for (String cell : row.cellValues()) {
                    if (!cell.isBlank()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
