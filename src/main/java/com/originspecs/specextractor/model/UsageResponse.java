package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from DeepL GET /v2/usage endpoint.
 */
public record UsageResponse(
        @JsonProperty("character_count") int characterCount,
        @JsonProperty("character_limit") int characterLimit) {

    public int charsRemaining() {
        return Math.max(0, characterLimit - characterCount);
    }
}
