package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.CommonNameCorrector;
import com.originspecs.specextractor.processor.SheetProcessor;
import com.originspecs.specextractor.reader.WorkbookReader;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.SpecRecordWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the complete spec extraction pipeline: read → translate → process → write.
 * All dependencies must be injected via constructor. Contains no business logic.
 */
@Slf4j
public class Orchestrator {

    private final WorkbookReader reader;
    private final TranslationService translationService;
    private final SheetProcessor processor;
    private final CommonNameCorrector commonNameCorrector;
    private final SpecRecordWriter writer;

    /**
     * Constructor — all dependencies must be provided (injected at composition root).
     */
    public Orchestrator(WorkbookReader reader, TranslationService translationService,
            SheetProcessor processor, CommonNameCorrector commonNameCorrector,
            SpecRecordWriter writer) {
        this.reader = reader;
        this.translationService = translationService;
        this.processor = processor;
        this.commonNameCorrector = commonNameCorrector;
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
        var translationResult = translationService.translate(sheets);
        List<SpecRecord> records = process(translationResult.sheets());
        records = commonNameCorrector.correct(records);
        write(records, config.outputFile());

        log.info("Pipeline completed successfully");
    }

    // Order of operations

    // 1.0 Read
    private List<SheetData> read(Path inputFile) throws IOException {
        log.debug("Reading workbook");
        return reader.read(inputFile);
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
