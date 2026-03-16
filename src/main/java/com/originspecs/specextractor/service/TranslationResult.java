package com.originspecs.specextractor.service;

import com.originspecs.specextractor.model.SheetData;

import java.util.List;

/**
 * Result of a translation run: the translated sheets and the number of
 * characters billed by the DeepL API for this run.
 * Defensive copies are made on construction and on access so the internal
 * list cannot be mutated by callers.
 */
public record TranslationResult(List<SheetData> sheets, int billedCharacters) {

    public TranslationResult(List<SheetData> sheets, int billedCharacters) {
        this.sheets = List.copyOf(sheets);
        this.billedCharacters = billedCharacters;
    }

    @Override
    public List<SheetData> sheets() {
        return List.copyOf(sheets);
    }
}
