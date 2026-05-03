package com.originspecs.specextractor.service.translation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultTranslatableCellPredicateTest {

    private final DefaultTranslatableCellPredicate predicate = new DefaultTranslatableCellPredicate();

    @Test
    void shouldTranslate_falseWhenBlank() {
        assertThat(predicate.shouldTranslate("")).isFalse();
        assertThat(predicate.shouldTranslate("   ")).isFalse();
    }

    @Test
    void shouldTranslate_falseWhenPurelyNumeric() {
        assertThat(predicate.shouldTranslate("1190")).isFalse();
        assertThat(predicate.shouldTranslate("1.198")).isFalse();
        assertThat(predicate.shouldTranslate("650~670")).isFalse();
        assertThat(predicate.shouldTranslate("0.658")).isFalse();
        assertThat(predicate.shouldTranslate("25.0")).isFalse();
    }

    @Test
    void shouldTranslate_trueWhenJapaneseOrMixed() {
        assertThat(predicate.shouldTranslate("ニッサン")).isTrue();
        assertThat(predicate.shouldTranslate("hello")).isTrue();
    }

    @Test
    void isPurelyNumeric_matchesExpectedPattern() {
        assertThat(DefaultTranslatableCellPredicate.isPurelyNumeric("100 %")).isTrue();
        assertThat(DefaultTranslatableCellPredicate.isPurelyNumeric(null)).isFalse();
        assertThat(DefaultTranslatableCellPredicate.isPurelyNumeric("abc")).isFalse();
    }
}
