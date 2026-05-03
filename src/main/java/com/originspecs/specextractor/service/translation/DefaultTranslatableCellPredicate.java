package com.originspecs.specextractor.service.translation;

import java.util.regex.Pattern;

/**
 * Skips blank values and values that look purely numeric (ints, decimals, ranges, percentages).
 */
public final class DefaultTranslatableCellPredicate implements TranslatableCellPredicate {

    private static final Pattern PURELY_NUMERIC = Pattern.compile("^[\\d\\s.,~\\-%]+$");

    @Override
    public boolean shouldTranslate(String cellValue) {
        if (cellValue == null || cellValue.isBlank()) {
            return false;
        }
        return !isPurelyNumeric(cellValue);
    }

    /** True when the trimmed value matches the numeric-only pattern (no DeepL call needed). */
    public static boolean isPurelyNumeric(String value) {
        return value != null && PURELY_NUMERIC.matcher(value.trim()).matches();
    }
}
