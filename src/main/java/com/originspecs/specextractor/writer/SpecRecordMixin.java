package com.originspecs.specextractor.writer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.SpecRecord;

import java.util.Map;

/**
 * Jackson serialization rules for {@link SpecRecord} — keeps the domain record free of Jackson annotations.
 */
abstract class SpecRecordMixin {

    @JsonUnwrapped
    abstract Map<String, String> fields();

    @JsonProperty(Constants.SOURCE_ARTIFACT_ID_JSON_KEY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    abstract String sourceArtifactId();
}
