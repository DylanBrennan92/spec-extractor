package com.originspecs.specextractor.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SheetData {

    private String name;
    private int index;

    // Resolved single-row headers from row 0 of the pre-processed XLS
    private List<String> headers = new ArrayList<>();

    // Data rows — one per vehicle specification entry
    private List<RowData> rows = new ArrayList<>();
}
