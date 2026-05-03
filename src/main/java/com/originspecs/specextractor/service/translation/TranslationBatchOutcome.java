package com.originspecs.specextractor.service.translation;

import java.util.List;
import java.util.Objects;

/**
 * Ordered translated strings and total billed characters for one send-all run.
 */
public record TranslationBatchOutcome(List<String> translatedTexts, int billedCharacters) {

    public TranslationBatchOutcome {
        translatedTexts = List.copyOf(Objects.requireNonNull(translatedTexts, "translatedTexts"));
    }
}
