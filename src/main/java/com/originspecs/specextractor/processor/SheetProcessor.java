package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;

import java.util.List;

/** Contract for converting sheet data into spec records. */
public interface SheetProcessor {

    List<SpecRecord> process(List<SheetData> sheets);
}
