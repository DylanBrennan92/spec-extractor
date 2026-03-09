# Spec Extractor

A lightweight Java CLI tool that reads a pre-processed Excel (.xls) file, translates Japanese cell
values to English via the DeepL API, and writes the result as a structured JSON array.
Designed to operate as the second stage of a two-step pipeline — it consumes the output of
[DataPrep](../DataPrep) and produces a JSON array of spec records, one object per data row.

## Technology Stack

- Java 21 — Modern Java with records and sealed types
- Apache POI 5.5.1 — Excel file parsing
- Jackson 2.21.0 — JSON serialisation with pretty printing
- DeepL Java SDK / HTTP API — Japanese → English translation
- Lombok — Reduced boilerplate
- SLF4J & Logback — Configurable logging
- Maven — Build automation and dependency management

## Pipeline overview

```
DataPrep (.xls → cleaned .xls)  →  spec-extractor (cleaned .xls → translated .json)
```

DataPrep resolves multi-row headers, filters sparse columns, expands merged cells, and fills down
group identifiers. Spec Extractor reads that cleaned output, translates every non-blank cell value
from Japanese to English using DeepL, then maps each data row to a JSON object whose keys are the
English column header names.

### Internal pipeline stages

```
Read (WorkBookReader)
  → Translate (TranslationService + DeepLClient)
  → Process (SpecProcessor)
  → Write (JsonWriter)
```

| Stage | Class | What it does |
|---|---|---|
| Read | `WorkBookReader` | Parses the .xls workbook into `SheetData` records |
| Translate | `TranslationService` | Batches non-blank cells (≤ 50 per request) and sends them to DeepL |
| Process | `SpecProcessor` | Maps each translated row to a `SpecRecord` (header → value) |
| Write | `JsonWriter` | Serialises the record list to a pretty-printed JSON file |

## Prerequisites

- Java 21+
- A [DeepL API](https://www.deepl.com/pro-api) key (free tier works)

Export your key before running:

```bash
export DEEPL_API_KEY=your-deepl-api-key
```

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

The output file is generated automatically — the filename is `{iso_timestamp}_{original_base_name}.json`
(e.g. `20260225T214530_pre_processed_file.json`), written to `src/main/resources/local-data/output/`.

## Example

```bash
export DEEPL_API_KEY=your-deepl-api-key

java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

With DEBUG logging:

```bash
java -DLOG_LEVEL=DEBUG \
  -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

## Output format

Each row in the input spreadsheet becomes a JSON object. Keys are taken from the English header
row produced by DataPrep; values are the DeepL-translated cell contents.

```json
[
  {
    "Car Name": "Nissan",
    "Common Name": "Note",
    "Model Type": "6AA-E13",
    "Engine Displacement (L)": "1.198",
    "Vehicle Weight (kg)": "1190"
  }
]
```

Multi-sheet workbooks are supported — all sheets are flattened into a single JSON array.

## Translation details

- Source language: `JA` (Japanese)
- Target language: `EN-GB` (English UK)
- Blank cells are skipped and preserved as-is — no API cost for empty values
- Texts are batched in groups of up to 50 per DeepL request to stay within API limits
- The API key is read from the `DEEPL_API_KEY` environment variable at startup; the tool exits
  with a clear error message if the variable is not set

## Logging options

Set the `LOG_LEVEL` system property to control verbosity:

```bash
# INFO (default — progress and record counts)
java -jar target/spec-extractor-*.jar input.xls

# DEBUG (per-batch detail)
java -DLOG_LEVEL=DEBUG -jar target/spec-extractor-*.jar input.xls

# TRACE (maximum verbosity for troubleshooting)
java -DLOG_LEVEL=TRACE -jar target/spec-extractor-*.jar input.xls
```
