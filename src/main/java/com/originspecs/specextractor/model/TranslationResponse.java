package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TranslationResponse(
        @JsonProperty("translations") List<Translation> translations,
        @JsonProperty("billed_characters") Integer billedCharacters) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Translation(
            @JsonProperty("detected_source_language") String detectedSourceLanguage,
            @JsonProperty("text") String text,
            @JsonProperty("billed_characters") Integer billedCharacters) {}
}
