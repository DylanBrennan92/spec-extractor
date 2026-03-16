package com.originspecs.specextractor.service;

import com.originspecs.specextractor.model.SheetData;

import java.util.List;

/**
 * Result of a translation run: the translated sheets and the number of
 * characters billed by the DeepL API for this run.
 */
public record TranslationResult(List<SheetData> sheets, int billedCharacters) {}
