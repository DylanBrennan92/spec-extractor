package com.originspecs.specextractor.processor;

import com.originspecs.specextractor.model.RowData;
import com.originspecs.specextractor.model.SheetData;
import com.originspecs.specextractor.model.SpecRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

        List<SpecRecord> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).fields())
                .containsEntry("Car Name", "Nissan")
                .containsEntry("Common Name", "Note")
                .containsEntry("Model Number", "6AA-E13");
        assertThat(records.get(1).fields())
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

        List<SpecRecord> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).get("Car Name")).isEqualTo("Nissan");
        assertThat(records.get(1).get("Car Name")).isEqualTo("Toyota");
    }

    @Test
    void process_blankHeaderColumnsAreExcluded() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "", "Model Number"),
                List.of(List.of("Nissan", "ignored", "6AA-E13"))
        );

        List<SpecRecord> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).fields()).doesNotContainKey("");
        assertThat(records.get(0).fields())
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

        List<SpecRecord> records = processor.process(List.of(sheet1, sheet2));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).get("Car Name")).isEqualTo("Nissan");
        assertThat(records.get(1).get("Car Name")).isEqualTo("Toyota");
    }

    @Test
    void process_emptyHeaderList_returnsNoRecords() {
        SheetData sheet = new SheetData("Empty", 0, List.of(), List.of(new RowData(List.of("some", "data"))));

        List<SpecRecord> records = processor.process(List.of(sheet));

        assertThat(records).isEmpty();
    }

    @Test
    void process_rowWithFewerCellsThanHeaders_fillsRemainingWithEmpty() {
        SheetData sheet = buildSheet(
                List.of("Car Name", "Common Name", "Model Number"),
                List.of(List.of("Nissan"))
        );

        List<SpecRecord> records = processor.process(List.of(sheet));

        assertThat(records).hasSize(1);
        assertThat(records.get(0).fields())
                .containsEntry("Car Name", "Nissan")
                .containsEntry("Common Name", "")
                .containsEntry("Model Number", "");
    }

    @Test
    void specRecord_get_returnsEmptyStringForAbsentHeader() {
        SheetData sheet = buildSheet(
                List.of("Car Name"),
                List.of(List.of("Nissan"))
        );

        SpecRecord record = processor.process(List.of(sheet)).get(0);

        assertThat(record.get("NonExistent")).isEmpty();
    }

    // --- Helper ---

    private SheetData buildSheet(List<String> headers, List<List<String>> rowValues) {
        List<RowData> rows = rowValues.stream()
                .map(RowData::new)
                .toList();
        return new SheetData("TestSheet", 0, headers, rows);
    }
}
