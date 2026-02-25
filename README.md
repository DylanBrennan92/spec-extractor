# Spec Extractor

A lightweight Java CLI tool that transforms pre-processed Excel (.xls) files into structured JSON.
Designed to operate as the second stage of a two-step pipeline — it consumes the output of
[DataPrep](../DataPrep) and produces a JSON array of spec records, one object per data row.

## Technology Stack

- Java 21 — Modern Java with records and pattern matching
- Apache POI 5.5.1 — Excel file parsing
- Jackson 2.21.0 — JSON serialisation with pretty printing
- Lombok — Reduced boilerplate
- SLF4J & Logback — Configurable logging
- Maven — Build automation and dependency management

## Pipeline overview

```
DataPrep (.xls → cleaned .xls)  →  spec-extractor (cleaned .xls → .json)
```

DataPrep resolves multi-row headers, filters sparse columns, expands merged cells, and fills down
group identifiers. Spec Extractor reads that cleaned output and maps each data row to a JSON object
whose keys are the column header names.

## Building from source

```bash
git clone https://github.com/Signal-Shift/spec-extractor.git
cd spec-extractor
mvn clean package
```

## Usage

```bash
java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar <inputFile.xls>
```

### CLI Arguments

| Argument        | Required | Description                                           | Example                                                        |
|-----------------|----------|-------------------------------------------------------|----------------------------------------------------------------|
| `inputFile.xls` | Yes      | Path to the pre-processed .xls file (DataPrep output) | `src/main/resources/local-data/input/pre_processed_file.xls`  |

The output file is generated automatically — the input filename has its extension replaced with
`.json`, a 10-character random alphanumeric suffix appended, and is written to
`src/main/resources/local-data/output/`.



## Example usage

```bash
java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```
# With DEBUG logging
```bash
java -DLOG_LEVEL=DEBUG \
  -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

## Output format

Each row in the input spreadsheet becomes a JSON object. Keys are taken directly from the header
row of the pre-processed file, so the output structure reflects whatever columns DataPrep retained.

```json
[
  {
    "Car Name": "Nissan",
    "Common Name": "Note",
    "Model Type": "6AA-E13",
    "Engine Displacement (L)": "1.198",
    "Vehicle Weight (kg)": "1190",
    ...
  },
  ...
]
```

Multi-sheet workbooks are supported — all sheets are flattened into a single JSON array.

## Logging options

Set the `LOG_LEVEL` system property to control verbosity:

```bash
# INFO (default — progress and record counts)
java -jar target/spec-extractor-*.jar input.xls

# DEBUG (per-sheet detail)
java -DLOG_LEVEL=DEBUG -jar target/spec-extractor-*.jar input.xls

# TRACE (maximum verbosity for troubleshooting)
java -DLOG_LEVEL=TRACE -jar target/spec-extractor-*.jar input.xls
```
