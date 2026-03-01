package com.originspecs.specextractor.service;

public class TranslationService {

    private record CellPosition(int sheetIndex, int rowIndex, int colIndex, String originalText) {}

    public List<SheetData> translate(List<SheetData> sheets) {
        

    }

    private List<CellPosition> extractTexts(List<SheetData> sheets) {
        List<CellPosition> cellPositions = new ArrayList<>();

        for (int sheetIndex = 0; sheetIndex < sheets.size(); sheetIndex++) {
            List<RowData> rows = sheets.get(sheetIndex).rows();

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<String> cells = rows.get(rowIndex).cellValues();

                for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                    String cell = cells.get(colIndex);
                    if (!cell.isBlank()) {
                        cellPositions.add(new CellPosition(sheetIndex, rowIndex, colIndex, cell));
                    }
                }
            }
        }

        return cellPositions;
    }
}









