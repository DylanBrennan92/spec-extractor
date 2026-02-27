package com.originspecs.specextractor.model;

import java.util.List;

/**
 * Immutable value object representing a single worksheet: its name, index,
 * resolved header labels, and data rows. Defensive copies are made on
 * construction so the contents cannot be mutated after the reader produces them.
 */
public record SheetData(
        String name,
        int index,
        List<String> headers,
        List<RowData> rows
) {
    public SheetData(String name, int index, List<String> headers, List<RowData> rows) {
        this.name = name;
        this.index = index;
        this.headers = List.copyOf(headers);
        this.rows = List.copyOf(rows);
    }

    /** Convenience factory for an empty sheet (no headers, no rows). */
    public static SheetData empty(String name, int index) {
        return new SheetData(name, index, List.of(), List.of());
    }
}
