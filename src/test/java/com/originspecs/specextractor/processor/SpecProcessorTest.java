package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpecProcessorTest {

    private SpecProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SpecProcessor();
    }

    @Test
    void process_singleSheetWithRows_mapsHeadersToValues() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Common Name", "Model Number"),
                List.of(
                        List.of("Nissan", "Note", "6AA-E13"),
                        List.of("Toyota", "Aqua", "5BA-MXPK11")
                )
        );

        List<Map<String, String>> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(2);
        assertThat(records.get(0))
                .containsEntry("Car Name", "Nissan")
                .containsEntry("Common Name", "Note")
                .containsEntry("Model Number", "6AA-E13");
        assertThat(records.get(1))
                .containsEntry("Car Name", "Toyota")
                .containsEntry("Common Name", "Aqua")
                .containsEntry("Model Number", "5BA-MXPK11");
    }

    @Test
    void process_emptyRowsAreSkipped() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Model Number"),
                List.of(
                        List.of("Nissan", "6AA-E13"),
                        List.of("", ""),
                        List.of("Toyota", "5BA-MXPK11")
                )
        );

        List<Map<String, String>> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(2);
        assertThat(records.get(0)).containsEntry("Car Name", "Nissan");
        assertThat(records.get(1)).containsEntry("Car Name", "Toyota");
    }

    @Test
    void process_blankHeaderColumnsAreExcluded() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "", "Model Number"),
                List.of(List.of("Nissan", "ignored", "6AA-E13"))
        );

        List<Map<String, String>> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(1);
        assertThat(records.get(0)).doesNotContainKey("");
        assertThat(records.get(0))
                .containsEntry("Car Name", "Nissan")
                .containsEntry("Model Number", "6AA-E13");
    }

    @Test
    void process_multipleSheets_flattensAllRecords() {
        SheetData sheet1 = buildSheet(
                List.of("Car Name", "Model Number"),
                List.of(List.of("Nissan", "6AA-E13"))
        );
        SheetData sheet2 = buildSheet(
                List.of("Car Name", "Model Number"),
                List.of(List.of("Toyota", "5BA-MXPK11"))
        );

        List<Map<String, String>> records = processor.process(List.of(sheet1, sheet2));

        assertThat(records).hasSize(2);
        assertThat(records.get(0)).containsEntry("Car Name", "Nissan");
        assertThat(records.get(1)).containsEntry("Car Name", "Toyota");
    }

    @Test
    void process_emptyHeaderList_returnsNoRecords() {
        SheetData sheet = new SheetData();
        sheet.setName("Empty");
        sheet.getRows().add(new RowData(List.of("some", "data")));

        List<Map<String, String>> records = processor.process(List.of(sheet));

        assertThat(records).isEmpty();
    }

    @Test
    void process_rowWithFewerCellsThanHeaders_fillsRemainingWithEmpty() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Common Name", "Model Number"),
                List.of(List.of("Nissan"))
        );

        List<Map<String, String>> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(1);
        assertThat(records.get(0))
                .containsEntry("Car Name", "Nissan")
                .containsEntry("Common Name", "")
                .containsEntry("Model Number", "");
    }

    // --- Helper ---

    private SheetData buildSheet(List<String> headers, List<List<String>> rowValues) {
        SheetData sheet = new SheetData();
        sheet.setName("TestSheet");
        sheet.setHeaders(headers);
        for (List<String> values : rowValues) {
            sheet.getRows().add(new RowData(values));
        }
        return sheet;
    }
}
