package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranslationRequest(
        List<String> text,
        @JsonProperty("source_lang") String sourceLang,
        @JsonProperty("target_lang") String targetLang,
        @JsonProperty("custom_instructions") List<String> customInstructions,
        @JsonProperty("context") String context,
        @JsonProperty("show_billed_characters") Boolean showBilledCharacters) {}
