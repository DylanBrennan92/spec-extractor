package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SourceArtifactId;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.SheetProcessor;
import com.originspecs.specextractor.processor.SpecRecordPostProcessor;
import com.originspecs.specextractor.reader.WorkbookReader;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.SpecRecordWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the complete spec extraction pipeline: read → translate → process → post-process → write.
 * All dependencies must be injected via constructor. Contains no business logic.
 */
@Slf4j
public final class Orchestrator {

    private final WorkbookReader reader;
    private final TranslationService translationService;
    private final SheetProcessor processor;
    private final List<SpecRecordPostProcessor> recordPostProcessors;
    private final SpecRecordWriter writer;

    /**
     * Constructor — all dependencies must be provided (injected at composition root).
     *
     * @param recordPostProcessors applied in iteration order after {@link SheetProcessor#process} (may be empty)
     */
    public Orchestrator(
            WorkbookReader reader,
            TranslationService translationService,
            SheetProcessor processor,
            List<SpecRecordPostProcessor> recordPostProcessors,
            SpecRecordWriter writer) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.translationService = Objects.requireNonNull(translationService, "translationService");
        this.processor = Objects.requireNonNull(processor, "processor");
        this.recordPostProcessors = List.copyOf(Objects.requireNonNull(recordPostProcessors, "recordPostProcessors"));
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    /**
     * Executes the complete spec extraction pipeline.
     *
     * @param config Configuration containing input/output paths
     * @throws IOException if reading or writing fails
     */
    public void execute(Config config) throws IOException {
        log.info("Starting spec extraction pipeline");
        log.info("Input: {} | Output: {} | Source artifact id: {}",
                config.inputFile(),
                config.outputFile(),
                config.sourceArtifactLineage().map(id -> id.value()).orElse("(none)"));

        List<SheetData> sheets = read(config.inputFile());
        var translationResult = translationService.translate(sheets);
        List<SpecRecord> records = process(translationResult.sheets(), config.sourceArtifactId());
        records = applyRecordPostProcessors(records);
        write(records, config.outputFile());

        log.info("Pipeline completed successfully");
    }

    private List<SpecRecord> applyRecordPostProcessors(List<SpecRecord> records) {
        List<SpecRecord> current = records;
        for (SpecRecordPostProcessor postProcessor : recordPostProcessors) {
            current = postProcessor.process(current);
        }
        return current;
    }

    // Order of operations

    private List<SheetData> read(Path inputFile) throws IOException {
        log.debug("Reading workbook");
        return reader.read(inputFile);
    }

    private List<SpecRecord> process(List<SheetData> sheets, SourceArtifactId sourceArtifactId) {
        log.debug("Processing sheets");
        return processor.process(sheets, sourceArtifactId);
    }

    private void write(List<SpecRecord> records, Path outputFile) throws IOException {
        log.debug("Writing JSON");
        writer.write(records, outputFile);
    }
}
