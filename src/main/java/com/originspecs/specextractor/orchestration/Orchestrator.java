package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.SpecProcessor;
import com.originspecs.specextractor.reader.WorkBookReader;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the complete spec extraction pipeline: read → translate → process → write.
 * Wires all components together. Contains no business logic.
 */
@Slf4j
public class Orchestrator {

    private final WorkBookReader reader;
    private final TranslationService translationService;
    private final SpecProcessor processor;
    private final JsonWriter writer;

    /**
     * Default constructor: wires all components with their default implementations.
     */
    public Orchestrator() {
        this.reader = new WorkBookReader();
        this.translationService = new TranslationService();
        this.processor = new SpecProcessor();
        this.writer = new JsonWriter();
    }

    /**
     * Full constructor for testing — inject any implementation of each component.
     */
    public Orchestrator(WorkBookReader reader, SpecProcessor processor,
     JsonWriter writer, TranslationService translationService) {
        this.reader = reader;
        this.translationService = translationService;
        this.processor = processor;
        this.writer = writer;
    }

    /**
     * Executes the complete spec extraction pipeline.
     *
     * @param config Configuration containing input/output paths
     * @throws IOException if reading or writing fails
     */
    public void execute(Config config) throws IOException {
        log.info("Starting spec extraction pipeline");
        log.info("Input: {} | Output: {}", config.inputFile(), config.outputFile());

        List<SheetData> sheets = read(config.inputFile());
        List<SheetData> translatedSheets = translate(sheets, config.deeplApiKey());
        List<SpecRecord> records = process(translatedSheets);
        write(records, config.outputFile());

        log.info("Pipeline completed successfully");
    }

    // Order of operations

    // 1.0 Read
    private List<SheetData> read(Path inputFile) throws IOException {
        log.debug("Reading workbook");
        return reader.read(inputFile);
    }

    // 2.0 Translate
    private List<SheetData> translate(List<SheetData> sheets, String deeplApiKey) {
        log.debug("Translating sheets");
        return translationService.translate(sheets, deeplApiKey);
    }

    // 3.0 Process
    private List<SpecRecord> process(List<SheetData> sheets) {
        log.debug("Processing sheets");
        return processor.process(sheets);
    }

    // 4.0 Write
    private void write(List<SpecRecord> records, Path outputFile) throws IOException {
        log.debug("Writing JSON");
        writer.write(records, outputFile);
    }
}
