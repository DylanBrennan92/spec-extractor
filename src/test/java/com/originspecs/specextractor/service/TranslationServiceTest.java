package com.originspecs.specextractor.service;

import com.originspecs.specextractor.client.DeepLClient;
import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TranslationService}. Uses a mocked {@link DeepLClient}
 * so no real DeepL API requests are made.
 */
@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private DeepLClient mockClient;

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService(mockClient);
    }

    @Test
    void translate_singleSheet_replacesNonBlankCellsWithMockedTranslations() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Common Name"),
                List.of(
                        List.of("ニッサン", "ノート"),
                        List.of("トヨタ", "アクア")
                )
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Nissan", "Note", "Toyota", "Aqua"));

        List<SheetData> result = translationService.translate(List.of(sheet), "fake-api-key");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("TestSheet");
        assertThat(result.get(0).headers()).containsExactly("Car Name", "Common Name");
        assertThat(result.get(0).rows()).hasSize(2);
        assertThat(result.get(0).rows().get(0).cellValues()).containsExactly("Nissan", "Note");
        assertThat(result.get(0).rows().get(1).cellValues()).containsExactly("Toyota", "Aqua");
    }

    @Test
    void translate_sendsRequestWithCorrectLangAndTexts() {
        SheetData sheet = buildSheet(
                List.of("A", "B"),
                List.of(List.of("hello", "world"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Hello", "World"));

        translationService.translate(List.of(sheet), "fake-api-key");

        ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
        verify(mockClient).translate(captor.capture());

        TranslationRequest request = captor.getValue();
        assertThat(request.text()).containsExactly("hello", "world");
        assertThat(request.sourceLang()).isEqualTo("JA");
        assertThat(request.targetLang()).isEqualTo("EN-GB");
    }

    @Test
    void translate_blankCellsAreSkipped_andNotSentToClient() {
        SheetData sheet = buildSheet(
                List.of("A", "B", "C"),
                List.of(List.of("only", "", "three"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Only", "Three"));

        List<SheetData> result = translationService.translate(List.of(sheet), "fake-api-key");

        ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
        verify(mockClient).translate(captor.capture());
        assertThat(captor.getValue().text()).containsExactly("only", "three");

        assertThat(result.get(0).rows().get(0).cellValues()).containsExactly("Only", "", "Three");
    }

    @Test
    void translate_multipleBatches_callsClientPerBatch() {
        int batchSize = com.originspecs.specextractor.config.Constants.DEEPL_BATCH_SIZE;
        int totalCells = batchSize + 1;

        List<String> manyCells = new java.util.ArrayList<>();
        for (int i = 0; i < totalCells; i++) {
            manyCells.add("text" + i);
        }
        SheetData sheet = buildSheet(List.of("Col"), List.of(manyCells));

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenAnswer(inv -> {
                    TranslationRequest req = inv.getArgument(0);
                    List<String> translated = req.text().stream()
                            .map(s -> "translated-" + s)
                            .toList();
                    return responseWithTranslations(translated.toArray(new String[0]));
                });

        List<SheetData> result = translationService.translate(List.of(sheet), "fake-api-key");

        verify(mockClient, org.mockito.Mockito.times(2)).translate(any(TranslationRequest.class));
        assertThat(result.get(0).rows().get(0).cellValues().get(0)).isEqualTo("translated-text0");
        assertThat(result.get(0).rows().get(0).cellValues().get(batchSize)).isEqualTo("translated-text" + batchSize);
    }

    @Test
    void translate_allBlankSheet_doesNotCallClient() {
        SheetData sheet = buildSheet(
                List.of("A", "B"),
                List.of(List.of("", ""))
        );

        List<SheetData> result = translationService.translate(List.of(sheet), "fake-api-key");

        verify(mockClient, never()).translate(any(TranslationRequest.class));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).rows().get(0).cellValues()).containsExactly("", "");
    }

    @Test
    void translate_preservesSheetStructureAndHeaders() {
        SheetData sheet = buildSheet(
                List.of("Header1", "Header2"),
                List.of(List.of("a", "b"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("A", "B"));

        List<SheetData> result = translationService.translate(List.of(sheet), "key");

        assertThat(result.get(0).name()).isEqualTo(sheet.name());
        assertThat(result.get(0).index()).isEqualTo(sheet.index());
        assertThat(result.get(0).headers()).isEqualTo(sheet.headers());
    }

    private static TranslationResponse responseWithTranslations(String... texts) {
        List<TranslationResponse.Translation> list = java.util.Arrays.stream(texts)
                .map(t -> new TranslationResponse.Translation("JA", t))
                .toList();
        return new TranslationResponse(list);
    }

    private static SheetData buildSheet(List<String> headers, List<List<String>> rowValues) {
        List<RowData> rows = rowValues.stream()
                .map(RowData::new)
                .toList();
        return new SheetData("TestSheet", 0, headers, rows);
    }
}
