package com.originspecs.specextractor.service;

import com.originspecs.specextractor.client.TranslationClient;
import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.TranslationRequest;
import com.originspecs.specextractor.model.TranslationResponse;
import com.originspecs.specextractor.config.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    private TranslationClient mockClient;

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService(mockClient);
        lenient().when(mockClient.getUsage()).thenReturn(Optional.empty());
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

        TranslationResult result = translationService.translate(List.of(sheet));

        assertThat(result.sheets()).hasSize(1);
        assertThat(result.sheets().get(0).name()).isEqualTo("TestSheet");
        assertThat(result.sheets().get(0).headers()).containsExactly("Car Name", "Common Name");
        assertThat(result.sheets().get(0).rows()).hasSize(2);
        assertThat(result.sheets().get(0).rows().get(0).cellValues()).containsExactly("Nissan", "Note");
        assertThat(result.sheets().get(0).rows().get(1).cellValues()).containsExactly("Toyota", "Aqua");
    }

    @Test
    void translate_sendsRequestWithCorrectLangAndTexts() {
        SheetData sheet = buildSheet(
                List.of("A", "B"),
                List.of(List.of("hello", "world"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Hello", "World"));

        translationService.translate(List.of(sheet));

        ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
        verify(mockClient).translate(captor.capture());

        TranslationRequest request = captor.getValue();
        assertThat(request.text()).containsExactly("hello", "world");
        assertThat(request.sourceLang()).isEqualTo("JA");
        assertThat(request.targetLang()).isEqualTo("EN-GB");
        assertThat(request.customInstructions()).isEqualTo(Constants.DEEPL_CUSTOM_INSTRUCTIONS);
        assertThat(request.context()).isEqualTo(Constants.DEEPL_CONTEXT);
        assertThat(request.showBilledCharacters()).isTrue();
    }

    @Test
    void translate_blankCellsAreSkipped_andNotSentToClient() {
        SheetData sheet = buildSheet(
                List.of("A", "B", "C"),
                List.of(List.of("only", "", "three"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Only", "Three"));

        TranslationResult result = translationService.translate(List.of(sheet));

        ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
        verify(mockClient).translate(captor.capture());
        assertThat(captor.getValue().text()).containsExactly("only", "three");

        assertThat(result.sheets().get(0).rows().get(0).cellValues()).containsExactly("Only", "", "Three");
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

        TranslationResult result = translationService.translate(List.of(sheet));

        verify(mockClient, org.mockito.Mockito.times(2)).translate(any(TranslationRequest.class));
        assertThat(result.sheets().get(0).rows().get(0).cellValues().get(0)).isEqualTo("translated-text0");
        assertThat(result.sheets().get(0).rows().get(0).cellValues().get(batchSize)).isEqualTo("translated-text" + batchSize);
    }

    @Test
    void translate_allBlankSheet_doesNotCallClient() {
        SheetData sheet = buildSheet(
                List.of("A", "B"),
                List.of(List.of("", ""))
        );

        TranslationResult result = translationService.translate(List.of(sheet));

        verify(mockClient, never()).translate(any(TranslationRequest.class));
        assertThat(result.sheets()).hasSize(1);
        assertThat(result.sheets().get(0).rows().get(0).cellValues()).containsExactly("", "");
    }

    @Test
    void translate_accumulatesBilledCharactersFromResponseLevel() {
        SheetData sheet = buildSheet(List.of("A"), List.of(List.of("hello")));

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(new TranslationResponse(
                        List.of(new TranslationResponse.Translation("JA", "Hello", null)),
                        42));

        TranslationResult result = translationService.translate(List.of(sheet));

        assertThat(result.billedCharacters()).isEqualTo(42);
    }

    @Test
    void translate_accumulatesBilledCharactersFromTranslations() {
        SheetData sheet = buildSheet(List.of("A", "B"), List.of(List.of("a", "b")));

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(new TranslationResponse(
                        List.of(
                                new TranslationResponse.Translation("JA", "A", 10),
                                new TranslationResponse.Translation("JA", "B", 12)
                        ),
                        null));

        TranslationResult result = translationService.translate(List.of(sheet));

        assertThat(result.billedCharacters()).isEqualTo(22);
    }

    @Test
    void translate_callsGetUsageBeforeAndAfterTranslation() {
        SheetData sheet = buildSheet(List.of("A"), List.of(List.of("x")));
        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("X"));

        translationService.translate(List.of(sheet));

        verify(mockClient, org.mockito.Mockito.times(2)).getUsage();
    }

    @Test
    void translate_throwsWhenTranslatedCountMismatchesPositions() {
        SheetData sheet = buildSheet(List.of("A", "B"), List.of(List.of("a", "b")));

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("A")); // only 1 translation for 2 positions

        assertThatThrownBy(() -> translationService.translate(List.of(sheet)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("translatedTexts size (1) != positions size (2)");
    }

    @Test
    void translate_purelyNumericCells_skippedAndKeptAsIs() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Weight", "Displacement"),
                List.of(
                        List.of("ニッサン", "1190", "1.198"),
                        List.of("トヨタ", "650~670", "0.658")
                )
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("Nissan", "Toyota"));

        TranslationResult result = translationService.translate(List.of(sheet));

        ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
        verify(mockClient).translate(captor.capture());
        assertThat(captor.getValue().text()).containsExactly("ニッサン", "トヨタ");

        assertThat(result.sheets().get(0).rows().get(0).cellValues()).containsExactly("Nissan", "1190", "1.198");
        assertThat(result.sheets().get(0).rows().get(1).cellValues()).containsExactly("Toyota", "650~670", "0.658");
    }

    @Test
    void translate_allNumericCells_doesNotCallClient() {
        SheetData sheet = buildSheet(
                List.of("A", "B"),
                List.of(List.of("1190", "0.658"), List.of("25.0", "650~670"))
        );

        TranslationResult result = translationService.translate(List.of(sheet));

        verify(mockClient, never()).translate(any(TranslationRequest.class));
        assertThat(result.sheets().get(0).rows().get(0).cellValues()).containsExactly("1190", "0.658");
        assertThat(result.sheets().get(0).rows().get(1).cellValues()).containsExactly("25.0", "650~670");
    }

    @Test
    void translate_preservesSheetStructureAndHeaders() {
        SheetData sheet = buildSheet(
                List.of("Header1", "Header2"),
                List.of(List.of("a", "b"))
        );

        when(mockClient.translate(any(TranslationRequest.class)))
                .thenReturn(responseWithTranslations("A", "B"));

        TranslationResult result = translationService.translate(List.of(sheet));

        assertThat(result.sheets().get(0).name()).isEqualTo(sheet.name());
        assertThat(result.sheets().get(0).index()).isEqualTo(sheet.index());
        assertThat(result.sheets().get(0).headers()).isEqualTo(sheet.headers());
    }

    private static TranslationResponse responseWithTranslations(String... texts) {
        List<TranslationResponse.Translation> list = java.util.Arrays.stream(texts)
                .map(t -> new TranslationResponse.Translation("JA", t, null))
                .toList();
        return new TranslationResponse(list, null);
    }

    private static SheetData buildSheet(List<String> headers, List<List<String>> rowValues) {
        List<RowData> rows = rowValues.stream()
                .map(RowData::new)
                .toList();
        return new SheetData("TestSheet", 0, headers, rows);
    }
}
