package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SourceArtifactId;
import com.originspecs.specextractor.model.SpecRecord;

import java.util.List;

/** Contract for converting sheet data into spec records. */
public interface SheetProcessor {

    /**
     * @param sourceArtifactId {@code null} when {@code --source-artifact-id} was not set for this run.
     */
    List<SpecRecord> process(List<SheetData> sheets, SourceArtifactId sourceArtifactId);
}
