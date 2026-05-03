package com.originspecs.specextractor.service.translation;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies ordered translated strings back onto a copy of the original sheets.
 */
@Slf4j
public final class TranslatedSheetAssembler {

    public List<SheetData> assemble(
            List<SheetData> originalSheets,
            List<TranslatableCellPosition> positions,
            List<String> translatedTexts) {

        if (translatedTexts.size() != positions.size()) {
            throw new IllegalStateException(
                    "translatedTexts size (" + translatedTexts.size()
                            + ") != positions size (" + positions.size()
                            + ") — DeepL returned an unexpected number of translations");
        }

        List<List<List<String>>> mutableCells = copyCellValues(originalSheets);

        for (int i = 0; i < positions.size(); i++) {
            TranslatableCellPosition pos = positions.get(i);
            mutableCells.get(pos.sheetIndex())
                    .get(pos.rowIndex())
                    .set(pos.colIndex(), translatedTexts.get(i));
        }

        List<SheetData> translatedSheets = new ArrayList<>();
        for (int sheetIndex = 0; sheetIndex < originalSheets.size(); sheetIndex++) {
            SheetData original = originalSheets.get(sheetIndex);
            List<RowData> translatedRows = mutableCells.get(sheetIndex).stream()
                    .map(RowData::new)
                    .toList();
            translatedSheets.add(new SheetData(
                    original.name(), original.index(), original.headers(), translatedRows));
        }

        log.info("Rebuilt {} sheet(s) with translated values", translatedSheets.size());
        return translatedSheets;
    }

    private static List<List<List<String>>> copyCellValues(List<SheetData> originalSheets) {
        List<List<List<String>>> mutableCells = new ArrayList<>();
        for (SheetData sheet : originalSheets) {
            List<List<String>> sheetCells = new ArrayList<>();
            for (RowData row : sheet.rows()) {
                sheetCells.add(new ArrayList<>(row.cellValues()));
            }
            mutableCells.add(sheetCells);
        }
        return mutableCells;
    }
}
