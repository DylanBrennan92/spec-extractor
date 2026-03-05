package com.originspecs.specextractor.service;

import com.originspecs.specextractor.client.DeepLClient;
import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import com.originspecs.specextractor.config.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates all cell values in a list of SheetData from Japanese to English
 * using the DeepL API. Headers are left untouched — they are already English
 * from DataPrep. The returned list has identical structure to the input.
 */
@Slf4j
public class TranslationService {

    private static final int DEEPL_BATCH_SIZE = Constants.DEEPL_BATCH_SIZE;

    private final DeepLClient client;

    private record CellPosition(int sheetIndex, int rowIndex, int colIndex, String originalText) {}

    /** Default constructor for production — client is created per translate call. */
    public TranslationService() {
        this.client = null;
    }

    /** Constructor for tests — use the provided client so no real API calls are made. */
    public TranslationService(DeepLClient client) {
        this.client = client;
    }

    /**
     * Translates all non-blank cell values across all sheets.
     *
     * @param sheets List of SheetData with Japanese cell values
     * @param apiKey DeepL API key
     * @return New list of SheetData with identical structure but English cell values
     */
    public List<SheetData> translate(List<SheetData> sheets, String apiKey) {
        log.info("Starting translation for {} sheet(s)", sheets.size());

        List<CellPosition> positions = extractTexts(sheets);
        log.info("Extracted {} non-blank cell(s) to translate", positions.size());

        List<String> textsToTranslate = positions.stream()
                .map(CellPosition::originalText)
                .toList();

        List<List<String>> batches = batch(textsToTranslate);
        log.info("Split into {} batch(es) of up to {} texts", batches.size(), DEEPL_BATCH_SIZE);

        List<String> translatedTexts = sendBatches(batches, apiKey);

        return rebuildSheets(sheets, positions, translatedTexts);
    }

    private List<CellPosition> extractTexts(List<SheetData> sheets) {
        List<CellPosition> cellPositions = new ArrayList<>();

        for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
            List<RowData> rows = sheets.get(sheetIndex).rows();

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<String> cells = rows.get(rowIndex).cellValues();

                for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                    String cell = cells.get(colIndex);
                    if (!cell.isBlank()) {
                        cellPositions.add(new CellPosition(sheetIndex, rowIndex, colIndex, cell));
                    }
                }
            }
        }

        return cellPositions;
    }

    private List<List<String>> batch(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += DEEPL_BATCH_SIZE) {
            batches.add(texts.subList(i, Math.min(i + DEEPL_BATCH_SIZE, texts.size())));
        }

        return batches;
    }

    private List<String> sendBatches(List<List<String>> batches, String apiKey) {
        DeepLClient clientToUse = this.client != null ? this.client : new DeepLClient(apiKey);
        List<String> allTranslated = new ArrayList<>();

        for (int i = 0; i < batches.size(); i++) {
            log.debug("Sending batch {}/{}", i + 1, batches.size());

            TranslationRequest request = new TranslationRequest(batches.get(i),
                    Constants.SOURCE_LANG, Constants.TARGET_LANG);
            TranslationResponse response = clientToUse.translate(request);

            response.translations().stream()
                    .map(TranslationResponse.Translation::text)
                    .forEach(allTranslated::add);
        }

        return allTranslated;
    }

    private List<SheetData> rebuildSheets(List<SheetData> originalSheets,
                                          List<CellPosition> positions,
                                          List<String> translatedTexts) {
        // Build a mutable copy of all cell value lists so we can substitute translations
        List<List<List<String>>> mutableCells = new ArrayList<>();
        for (SheetData sheet : originalSheets) {
            List<List<String>> sheetCells = new ArrayList<>();
            for (RowData row : sheet.rows()) {
                sheetCells.add(new ArrayList<>(row.cellValues()));
            }
            mutableCells.add(sheetCells);
        }

        if (translatedTexts.size() != positions.size()) {
            throw new IllegalStateException(
                    "translatedTexts size (" + translatedTexts.size() +
                    ") != positions size (" + positions.size() +
                    ") in TranslationService — DeepL returned an unexpected number of translations");
        }

        // Substitute each translated text back at the exact position it came from
        for (int i = 0; i < positions.size(); i++) {
            CellPosition pos = positions.get(i);
            mutableCells.get(pos.sheetIndex())
                        .get(pos.rowIndex())
                        .set(pos.colIndex(), translatedTexts.get(i));
        }

        // Reconstruct immutable SheetData records from the updated cell values
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
}
