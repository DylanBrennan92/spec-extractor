package com.originspecs.specextractor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsageResponseTest {

    @Test
    void charsRemaining_returnsLimitMinusCount() {
        UsageResponse usage = new UsageResponse(100, 500);

        assertThat(usage.charsRemaining()).isEqualTo(400);
    }

    @Test
    void charsRemaining_returnsZeroWhenCountExceedsLimit() {
        UsageResponse usage = new UsageResponse(600, 500);

        assertThat(usage.charsRemaining()).isEqualTo(0);
    }

    @Test
    void charsRemaining_returnsZeroWhenCountEqualsLimit() {
        UsageResponse usage = new UsageResponse(500, 500);

        assertThat(usage.charsRemaining()).isEqualTo(0);
    }

    // Edge cases: negative values (e.g. malformed API response).
    // charsRemaining() computes limit - count and clamps to 0 via Math.max.

    @Test
    void charsRemaining_negativeCount_computesLimitMinusCount() {
        UsageResponse usage = new UsageResponse(-50, 100);

        assertThat(usage.charsRemaining()).isEqualTo(150);
    }

    @Test
    void charsRemaining_negativeLimit_returnsZero() {
        UsageResponse usage = new UsageResponse(100, -50);

        assertThat(usage.charsRemaining()).isEqualTo(0);
    }

    @Test
    void charsRemaining_bothNegative_computesLimitMinusCountThenClamps() {
        UsageResponse usage = new UsageResponse(-100, -50);

        assertThat(usage.charsRemaining()).isEqualTo(50);
    }
}
