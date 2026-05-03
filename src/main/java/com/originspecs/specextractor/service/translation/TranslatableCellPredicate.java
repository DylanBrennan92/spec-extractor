package com.originspecs.specextractor.service.translation;

/**
 * Decides whether a non-blank cell value should be sent for translation.
 */
@FunctionalInterface
public interface TranslatableCellPredicate {

    /**
     * @param cellValue cell text (may be blank; callers typically skip blanks first)
     * @return {@code true} if the value should be translated via the API
     */
    boolean shouldTranslate(String cellValue);
}
