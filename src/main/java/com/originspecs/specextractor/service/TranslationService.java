package com.originspecs.specextractor.service;

import com.originspecs.specextractor.client.TranslationClient;
import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.service.translation.DefaultTranslatableCellPredicate;
import com.originspecs.specextractor.service.translation.TranslatableCellCollector;
import com.originspecs.specextractor.service.translation.TranslatableCellPosition;
import com.originspecs.specextractor.service.translation.TranslatedSheetAssembler;
import com.originspecs.specextractor.service.translation.TranslationBatchOutcome;
import com.originspecs.specextractor.service.translation.TranslationBatchSender;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * Facade for the translation pipeline: collect translatable cells, batch-translate via DeepL,
 * then rebuild sheets. Delegates to focused collaborators under {@code service.translation}.
 */
@Slf4j
public final class TranslationService {

    private final TranslatableCellCollector collector;
    private final TranslationBatchSender batchSender;
    private final TranslatedSheetAssembler assembler;

    /** Wires default collaborators for production use. */
    public TranslationService(TranslationClient client) {
        this(
                new TranslatableCellCollector(new DefaultTranslatableCellPredicate()),
                new TranslationBatchSender(client, Constants.DEEPL_BATCH_SIZE),
                new TranslatedSheetAssembler());
    }

    TranslationService(
            TranslatableCellCollector collector,
            TranslationBatchSender batchSender,
            TranslatedSheetAssembler assembler) {
        this.collector = Objects.requireNonNull(collector, "collector");
        this.batchSender = Objects.requireNonNull(batchSender, "batchSender");
        this.assembler = Objects.requireNonNull(assembler, "assembler");
    }

    /**
     * Translates all eligible cell values across all sheets.
     *
     * @param sheets workbook sheets (typically Japanese cell values)
     * @return translated sheets and billed character count for this run
     */
    public TranslationResult translate(List<SheetData> sheets) {
        log.info("Starting translation for {} sheet(s)", sheets.size());

        List<TranslatableCellPosition> positions = collector.collect(sheets);
        int totalNonBlank = TranslatableCellCollector.countNonBlankCells(sheets);
        int skipped = totalNonBlank - positions.size();
        log.info("Extracted {} non-blank cell(s); {} to translate, {} skipped (purely numeric)",
                totalNonBlank, positions.size(), skipped);

        List<String> translatedTexts;
        int billedCharacters;
        if (positions.isEmpty()) {
            log.info("No cells require translation — skipping DeepL API");
            translatedTexts = List.of();
            billedCharacters = 0;
        } else {
            List<String> textsInOrder = positions.stream()
                    .map(TranslatableCellPosition::originalText)
                    .toList();
            TranslationBatchOutcome outcome = batchSender.sendAll(textsInOrder);
            translatedTexts = outcome.translatedTexts();
            billedCharacters = outcome.billedCharacters();
        }

        List<SheetData> translatedSheets = assembler.assemble(sheets, positions, translatedTexts);
        return new TranslationResult(translatedSheets, billedCharacters);
    }
}
