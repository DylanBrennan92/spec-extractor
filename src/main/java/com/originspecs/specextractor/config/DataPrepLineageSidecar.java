package com.originspecs.specextractor.config;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Path to the UTF-8 sidecar DataPrep writes next to each cleaned workbook: {@code name.xls.source-artifact-id}.
 */
public final class DataPrepLineageSidecar {

    private DataPrepLineageSidecar() {}

    /**
     * Sidecar path for a cleaned workbook (same directory, filename + {@link Constants#DATAPREP_LINEAGE_SIDECAR_SUFFIX}).
     */
    public static Path pathForInputWorkbook(Path inputWorkbook) {
        Objects.requireNonNull(inputWorkbook, "inputWorkbook");
        Path name = inputWorkbook.getFileName();
        if (name == null) {
            throw new IllegalArgumentException("Input path has no file name: " + inputWorkbook);
        }
        return inputWorkbook.resolveSibling(name + Constants.DATAPREP_LINEAGE_SIDECAR_SUFFIX);
    }
}
