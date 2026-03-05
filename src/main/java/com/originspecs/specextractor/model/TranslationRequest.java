package com.originspecs.specextractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TranslationRequest(
        List<String> text,
        @JsonProperty("source_lang") String sourceLang,
        @JsonProperty("target_lang") String targetLang) {}
