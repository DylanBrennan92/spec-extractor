package com.originspecs.specextractor.service.translation;

/**
 * Coordinates and original text of one cell selected for translation.
 */
public record TranslatableCellPosition(int sheetIndex, int rowIndex, int colIndex, String originalText) {

    public TranslatableCellPosition {
        if (originalText == null) {
            throw new IllegalArgumentException("originalText must not be null");
        }
    }
}
