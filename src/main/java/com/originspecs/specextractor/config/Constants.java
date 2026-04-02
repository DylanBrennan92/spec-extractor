package com.originspecs.specextractor.config;

import java.util.List;

public final class Constants {

    public static final String OUTPUT_DIR = "src/main/resources/local-data/output";

    public static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";
    public static final String DEEPL_USAGE_URL = "https://api-free.deepl.com/v2/usage";

    public static final String SOURCE_LANG = "JA";
    public static final String TARGET_LANG = "EN-GB";

    public static final int DEEPL_BATCH_SIZE = 1500;

    /** Custom instructions to avoid parenthetical explanations (e.g. "Daihatsu (Japanese automobile manufacturer)"). */
    public static final List<String> DEEPL_CUSTOM_INSTRUCTIONS = List.of(
            "Provide concise translations only; omit parenthetical explanations.");

    /** Context to improve translation of short vehicle-spec cell values. */
    public static final String DEEPL_CONTEXT =
            "Vehicle specification spreadsheet. Translate each cell value literally without adding explanations.";

    /** Header name for the Common Name column in spec records. */
    public static final String COMMON_NAME_HEADER = "Common Name";

    /**
     * JSON property for the ministry workbook lineage id (same UUID minted for DataPrep
     * {@code --source-artifact-id}; original file is {@code local-data/artifacts/{id}.{ext}}).
     */
    public static final String SOURCE_ARTIFACT_ID_JSON_KEY = "sourceArtifactId";

    /** CLI flag; must match DataPrep for the same run pipeline. */
    public static final String CLI_SOURCE_ARTIFACT_ID_FLAG = "--source-artifact-id";

    private Constants() {}
}
