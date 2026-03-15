package com.originspecs.specextractor.service;

import com.originspecs.specextractor.client.TranslationClient;
import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import com.originspecs.specextractor.model.UsageResponse;
import com.originspecs.specextractor.config.Constants;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Translates all cell values in a list of SheetData from Japanese to English
 * using the DeepL API. Headers are left untouched — they are already English
 * from DataPrep. The returned list has identical structure to the input.
 */
@Slf4j
public class TranslationService {

    private static final int DEEPL_BATCH_SIZE = Constants.DEEPL_BATCH_SIZE;

    /** Values matching this pattern (ints, decimals, ranges, percentages) are kept as-is — no DeepL call. */
    private static final Pattern PURELY_NUMERIC = Pattern.compile("^[\\d\\s.,~\\-%]+$");

    private final TranslationClient client;

    private int lastRunBilledCharacters;

    private record CellPosition(int sheetIndex, int rowIndex, int colIndex, String originalText) {}

    /** Returns the total billed characters from the last translate run. */
    public int getLastRunBilledCharacters() {
        return lastRunBilledCharacters;
    }

    /** Constructor — client must be provided (injected at composition root). */
    public TranslationService(TranslationClient client) {
        this.client = client;
    }

    /**
     * Translates all non-blank cell values across all sheets.
     *
     * @param sheets List of SheetData with Japanese cell values
     * @return New list of SheetData with identical structure but English cell values
     */
    public List<SheetData> translate(List<SheetData> sheets) {
        log.info("Starting translation for {} sheet(s)", sheets.size());

        List<CellPosition> positions = extractTexts(sheets);
        int totalNonBlank = countNonBlankCells(sheets);
        int skipped = totalNonBlank - positions.size();
        log.info("Extracted {} non-blank cell(s); {} to translate, {} skipped (purely numeric)",
                totalNonBlank, positions.size(), skipped);

        List<String> textsToTranslate = positions.stream()
                .map(CellPosition::originalText)
                .toList();

        List<String> translatedTexts;
        if (positions.isEmpty()) {
            log.info("No cells require translation — skipping DeepL API");
            translatedTexts = List.of();
        } else {
            List<List<String>> batches = batch(textsToTranslate);
            log.info("Split into {} batch(es) of up to {} texts", batches.size(), DEEPL_BATCH_SIZE);
            translatedTexts = sendBatchesWithUsageLogging(batches);
        }

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
                    if (!cell.isBlank() && !isPurelyNumeric(cell)) {
                        cellPositions.add(new CellPosition(sheetIndex, rowIndex, colIndex, cell));
                    }
                }
            }
        }

        return cellPositions;
    }

    private int countNonBlankCells(List<SheetData> sheets) {
        int count = 0;
        for (SheetData sheet : sheets) {
            for (RowData row : sheet.rows()) {
                for (String cell : row.cellValues()) {
                    if (!cell.isBlank()) count++;
                }
            }
        }
        return count;
    }

    /** Returns true if the value is purely numeric (ints, decimals, ranges, percentages) — no translation needed. */
    static boolean isPurelyNumeric(String value) {
        return value != null && PURELY_NUMERIC.matcher(value.trim()).matches();
    }

    private List<List<String>> batch(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += DEEPL_BATCH_SIZE) {
            batches.add(texts.subList(i, Math.min(i + DEEPL_BATCH_SIZE, texts.size())));
        }

        return batches;
    }

    private List<String> sendBatchesWithUsageLogging(List<List<String>> batches) {
        logUsageBefore(client);

        List<String> allTranslated = new ArrayList<>();
        int totalBilledThisRun = 0;

        for (int i = 0; i < batches.size(); i++) {
            log.debug("Sending batch {}/{}", i + 1, batches.size());

            TranslationRequest request = new TranslationRequest(
                    batches.get(i),
                    Constants.SOURCE_LANG,
                    Constants.TARGET_LANG,
                    Constants.DEEPL_CUSTOM_INSTRUCTIONS,
                    Constants.DEEPL_CONTEXT,
                    true);
            TranslationResponse response = client.translate(request);

            if (response.billedCharacters() != null) {
                totalBilledThisRun += response.billedCharacters();
            } else {
                for (TranslationResponse.Translation t : response.translations()) {
                    if (t.billedCharacters() != null) {
                        totalBilledThisRun += t.billedCharacters();
                    }
                }
            }
            response.translations().stream()
                    .map(TranslationResponse.Translation::text)
                    .forEach(allTranslated::add);
        }

        lastRunBilledCharacters = totalBilledThisRun;
        logUsageAfter(client, totalBilledThisRun);
        return allTranslated;
    }

    private void logUsageBefore(TranslationClient client) {
        UsageResponse usage = client.getUsage();
        if (usage != null) {
            log.info("DeepL quota before translation: {} chars remaining (used: {}/{})",
                    usage.charsRemaining(), usage.characterCount(), usage.characterLimit());
        }
    }

    private void logUsageAfter(TranslationClient client, int totalBilledThisRun) {
        UsageResponse usage = client.getUsage();
        if (usage != null) {
            log.info("DeepL quota after translation: {} chars remaining (used: {}/{})",
                    usage.charsRemaining(), usage.characterCount(), usage.characterLimit());
        }
        log.info("Chars converted this run: {}", totalBilledThisRun);
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
