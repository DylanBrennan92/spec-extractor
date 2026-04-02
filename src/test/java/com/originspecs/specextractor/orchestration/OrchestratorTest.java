package com.originspecs.specextractor.orchestration;

import com.originspecs.specextractor.config.Config;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SourceArtifactId;
import com.originspecs.specextractor.model.SpecRecord;
import com.originspecs.specextractor.processor.CommonNameCorrector;
import com.originspecs.specextractor.processor.SheetProcessor;
import com.originspecs.specextractor.reader.WorkbookReader;
import com.originspecs.specextractor.service.TranslationResult;
import com.originspecs.specextractor.service.TranslationService;
import com.originspecs.specextractor.writer.SpecRecordWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    private WorkbookReader reader;

    @Mock
    private TranslationService translationService;

    @Mock
    private SheetProcessor processor;

    @Mock
    private CommonNameCorrector commonNameCorrector;

    @Mock
    private SpecRecordWriter writer;

    private Orchestrator orchestrator;
    private Config config;

    @BeforeEach
    void setUp() {
        orchestrator = new Orchestrator(reader, translationService, processor, commonNameCorrector, writer);
        config = new Config(INPUT_PATH, OUTPUT_PATH, API_KEY, null);
        lenient().when(commonNameCorrector.correct(anyList())).thenAnswer(inv -> inv.getArgument(0));
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
        when(translationService.translate(sheets)).thenReturn(new TranslationResult(translatedSheets, 0));
        when(processor.process(translatedSheets, null)).thenReturn(records);

        orchestrator.execute(config);

        verify(reader).read(INPUT_PATH);
        verify(translationService).translate(sheets);
        verify(processor).process(eq(translatedSheets), isNull());
        verify(commonNameCorrector).correct(records);
        verify(writer).write(anyList(), eq(OUTPUT_PATH));
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
        when(translationService.translate(originalSheets)).thenReturn(new TranslationResult(translatedSheets, 0));
        when(processor.process(translatedSheets, null)).thenReturn(List.of());

        orchestrator.execute(config);

        ArgumentCaptor<List<SheetData>> processArg = ArgumentCaptor.forClass(List.class);
        verify(processor).process(processArg.capture(), isNull());
        assertThat(processArg.getValue()).isSameAs(translatedSheets);
    }

    @Test
    void execute_passesSourceArtifactIdToProcessor_whenConfigured() throws IOException {
        SourceArtifactId artifactId = SourceArtifactId.parse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000").toString());
        Config cfg = new Config(INPUT_PATH, OUTPUT_PATH, API_KEY, artifactId);
        List<SheetData> sheets = List.of(SheetData.empty("S", 0));
        when(reader.read(INPUT_PATH)).thenReturn(sheets);
        when(translationService.translate(sheets)).thenReturn(new TranslationResult(sheets, 0));
        when(processor.process(sheets, artifactId)).thenReturn(List.of());

        orchestrator.execute(cfg);

        verify(processor).process(sheets, artifactId);
    }

    @Test
    void execute_propagatesIOExceptionFromReader() throws IOException {
        when(reader.read(INPUT_PATH)).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> orchestrator.execute(config))
                .isInstanceOf(IOException.class)
                .hasMessage("read failed");
    }

    @Test
    void execute_propagatesIOExceptionFromWriter() throws IOException {
        List<SheetData> sheets = List.of(SheetData.empty("S", 0));
        when(reader.read(INPUT_PATH)).thenReturn(sheets);
        when(translationService.translate(sheets)).thenReturn(new TranslationResult(sheets, 0));
        when(processor.process(sheets, null)).thenReturn(List.of());
        doThrow(new IOException("write failed")).when(writer).write(anyList(), eq(OUTPUT_PATH));

        assertThatThrownBy(() -> orchestrator.execute(config))
                .isInstanceOf(IOException.class)
                .hasMessage("write failed");
    }
}
