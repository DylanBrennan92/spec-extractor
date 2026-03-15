package com.originspecs.specextractor.writer;

import com.originspecs.specextractor.model.SpecRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Contract for writing spec records to output. */
public interface SpecRecordWriter {

    void write(List<SpecRecord> records, Path outputPath) throws IOException;
}
