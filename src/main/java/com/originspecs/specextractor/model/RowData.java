package com.originspecs.specextractor.model;

import java.util.List;

/**
 * Immutable value object representing a single row of cell values.
 * The cell list is defensively copied on construction so no caller
 * can mutate the row's contents after it is created.
 */
public record RowData(List<String> cellValues) {

    public RowData(List<String> cellValues) {
        this.cellValues = List.copyOf(cellValues);
    }

    public String getCell(int index) {
        return index < cellValues.size() ? cellValues.get(index) : "";
    }

    public int size() {
        return cellValues.size();
    }

    public boolean isEmpty() {
        return cellValues.stream().allMatch(String::isBlank);
    }
}
