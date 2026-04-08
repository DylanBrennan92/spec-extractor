package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.SpecRecord;

import java.util.List;

/**
 * Transforms a list of {@link SpecRecord}s after sheet processing and before writing JSON.
 * Implementations are applied in order by {@link com.originspecs.specextractor.orchestration.Orchestrator}.
 */
@FunctionalInterface
public interface SpecRecordPostProcessor {

    /**
     * Returns the post-processed records (may be the same instances or new ones; must not mutate the input list).
     */
    List<SpecRecord> process(List<SpecRecord> records);
}
