package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.SpecProcessor;
import com.originspecs.specextractor.reader.WorkBookReader;
import com.originspecs.specextractor.writer.JsonWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the complete spec extraction pipeline: read → process → write.
 * Wires all components together. Contains no business logic.
 */
@Slf4j
public class SpecExtractorOrchestrator {

    private final WorkBookReader reader;
    private final SpecProcessor processor;
    private final JsonWriter writer;

    /**
     * Default constructor: wires all components with their default implementations.
     */
    public SpecExtractorOrchestrator() {
        this.reader = new WorkBookReader();
        this.processor = new SpecProcessor();
        this.writer = new JsonWriter();
    }

    /**
     * Full constructor for testing — inject any implementation of each component.
     */
    public SpecExtractorOrchestrator(WorkBookReader reader, SpecProcessor processor, JsonWriter writer) {
        this.reader = reader;
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
        List<SpecRecord> records = process(sheets);
        write(records, config.outputFile());

        log.info("Pipeline completed successfully");
    }

    private List<SheetData> read(Path inputFile) throws IOException {
        log.debug("Reading workbook");
        return reader.read(inputFile);
    }

    private List<SpecRecord> process(List<SheetData> sheets) {
        log.debug("Processing sheets");
        return processor.process(sheets);
    }

    private void write(List<SpecRecord> records, Path outputFile) throws IOException {
        log.debug("Writing JSON");
        writer.write(records, outputFile);
    }
}
