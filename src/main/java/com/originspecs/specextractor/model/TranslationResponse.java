package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TranslationResponse(
        @JsonProperty("translations") List<Translation> translations) {

    public record Translation(
            @JsonProperty("detected_source_language") String detectedSourceLanguage,
            @JsonProperty("text") String text) {}
}
