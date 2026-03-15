package com.originspecs.specextractor.reader;

import com.originspecs.specextractor.model.SheetData;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Contract for reading workbooks into sheet data. */
public interface WorkbookReader {

    List<SheetData> read(Path inputPath) throws IOException;
}
