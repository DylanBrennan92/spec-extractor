package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.SpecProcessor;
import com.originspecs.specextractor.reader.WorkBookReader;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.JsonWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Orchestrator}. All dependencies are mocked so no
 * file I/O or DeepL API calls are made.
 */
@ExtendWith(MockitoExtension.class)
class OrchestratorTest {

    private static final Path INPUT_PATH = Path.of("input.xls");
    private static final Path OUTPUT_PATH = Path.of("output.json");
    private static final String API_KEY = "test-api-key";

    @Mock
    private WorkBookReader reader;

    @Mock
    private TranslationService translationService;

    @Mock
    private SpecProcessor processor;

    @Mock
    private JsonWriter writer;

    private Orchestrator orchestrator;
    private Config config;

    @BeforeEach
    void setUp() {
        orchestrator = new Orchestrator(reader, processor, writer, translationService);
        config = new Config(INPUT_PATH, OUTPUT_PATH, API_KEY);
    }

    @Test
    void execute_callsReadThenTranslateThenProcessThenWrite() throws IOException {
        List<SheetData> sheets = List.of(
                SheetData.empty("Sheet1", 0)
        );
        List<SheetData> translatedSheets = List.of(
                SheetData.empty("Sheet1", 0)
        );
        List<SpecRecord> records = List.of(
                new SpecRecord(java.util.Map.of("Car", "Nissan"))
        );

        when(reader.read(INPUT_PATH)).thenReturn(sheets);
        when(translationService.translate(sheets, API_KEY)).thenReturn(translatedSheets);
        when(processor.process(translatedSheets)).thenReturn(records);

        orchestrator.execute(config);

        verify(reader).read(INPUT_PATH);
        verify(translationService).translate(sheets, API_KEY);
        verify(processor).process(translatedSheets);
        verify(writer).write(records, OUTPUT_PATH);
    }

    @Test
    void execute_processReceivesTranslatedSheetsNotOriginal() throws IOException {
        List<SheetData> originalSheets = List.of(
                SheetData.empty("Original", 0)
        );
        List<SheetData> translatedSheets = List.of(
                SheetData.empty("Translated", 0)
        );
        when(reader.read(INPUT_PATH)).thenReturn(originalSheets);
        when(translationService.translate(originalSheets, API_KEY)).thenReturn(translatedSheets);
        when(processor.process(translatedSheets)).thenReturn(List.of());

        orchestrator.execute(config);

        ArgumentCaptor<List<SheetData>> processArg = ArgumentCaptor.forClass(List.class);
        verify(processor).process(processArg.capture());
        assertThat(processArg.getValue()).isSameAs(translatedSheets);
    }
}
