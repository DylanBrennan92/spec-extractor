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
Read (WorkbookReaderImpl)
  → Translate (TranslationService + DeepLClient)
  → Process (SpecProcessor)
  → Write (JsonWriter)
```

| Stage | Class | What it does |
|---|---|---|
| Read | `WorkbookReaderImpl` | Parses the .xls workbook into `SheetData` records |
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

The runnable fat JAR is `target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## How to run (step by step)

1. **Use Java 21+** and ensure the JAR exists (see **Building from source**).

2. **Set your DeepL API key** in the shell where you invoke Java. The tool reads `DEEPL_API_KEY` at startup and fails fast if it is missing or blank.

   ```bash
   export DEEPL_API_KEY=your-deepl-api-key
   ```

3. **Point at a cleaned workbook.** The input must be an `.xls` file produced by DataPrep (single English header row, sparse columns removed). Paths can be relative to the project root or absolute.

4. **Run the JAR** from the **spec-extractor project root** (or use absolute paths).

   **Lineage (`sourceArtifactId` in JSON):** If DataPrep’s sidecar is next to your input — same directory, named `<inputBaseName>.source-artifact-id` (for example `cleaned.xls.source-artifact-id`) — spec-extractor uses the **first line in that file that parses as a UUID** (blank lines and non-UUID lines such as comments are skipped) and stamps every JSON row. You do **not** need `--source-artifact-id` in that case.

   Optionally pass `--source-artifact-id <uuid>` to **override** when there is no sidecar, or to **assert** the same id when a sidecar exists (a mismatch fails fast).

   **Typical run** (sidecar present — same folder as the cleaned `.xls` after DataPrep):

   ```bash
   java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
     /path/to/cleaned.xls
   ```

   **Explicit UUID** (optional; must match sidecar if both are present):

   ```bash
   java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
     --source-artifact-id 550e8400-e29b-41d4-a716-446655440000 \
     /path/to/cleaned.xls
   ```

   The flag and the file path can appear in either order, as long as there is **exactly one** positional path and **at most one** `--source-artifact-id` (with its value immediately after the flag).

5. **Collect output.** JSON is written under `src/main/resources/local-data/output/` with an automatic name: `{iso_timestamp}_{original_base_name}.json` (for example `20260225T214530_pre_processed_file.json`).

### End-to-end with DataPrep and `sourceArtifactId`

Use this when you want traceability back to the **byte-identical** ministry file DataPrep archived.

1. In **DataPrep**, run the pipeline on your ministry `.xls` (see the DataPrep README). For each workbook it writes:
   - `local-data/artifacts/<generated-uuid>.<ext>` — copy of the original file  
   - `local-data/output/<name>.xls` — cleaned workbook  
   - `local-data/output/<name>.xls.source-artifact-id` — one-line UUID for that run  
2. Point spec-extractor at the **cleaned** `.xls`, and keep the `.source-artifact-id` file **beside it** (same directory). Then a normal run picks up lineage automatically:

   ```bash
   export DEEPL_API_KEY=your-deepl-api-key
   java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
     /path/to/cleaned_output.xls
   ```

If you copy only the `.xls` and not the sidecar, JSON will omit `sourceArtifactId` unless you pass `--source-artifact-id` with the correct UUID from the sidecar or logs.

Every object in the JSON array then includes `"sourceArtifactId": "<uuid>"`, identical on every row for that run.

## Usage (quick reference)

```bash
java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  [--source-artifact-id <uuid>] \
  <inputFile.xls>
```

### CLI arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `<inputFile.xls>` | Yes | Path to the pre-processed `.xls` (DataPrep output). Exactly one positional argument. |
| `--source-artifact-id <uuid>` | No | At most once. Optional override or cross-check: each JSON row gets `sourceArtifactId` when this flag is set **or** when `<inputFile>.source-artifact-id` exists (DataPrep). If both are present, they must match. |

The output file is generated automatically — the filename is `{iso_timestamp}_{original_base_name}.json`, written to `src/main/resources/local-data/output/`.

## Examples

**Basic run**

```bash
export DEEPL_API_KEY=your-deepl-api-key

java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

**Lineage via DataPrep sidecar** (keep `5.1.Gzyouyou_WLTC_output.xls.source-artifact-id` next to the `.xls`)

```bash
export DEEPL_API_KEY=your-deepl-api-key

java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

**Same with explicit UUID** (optional; must agree with sidecar if both exist)

```bash
export DEEPL_API_KEY=your-deepl-api-key

java -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  --source-artifact-id 550e8400-e29b-41d4-a716-446655440000 \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

**DEBUG logging**

```bash
java -DLOG_LEVEL=DEBUG \
  -jar target/spec-extractor-1.0-SNAPSHOT-jar-with-dependencies.jar \
  src/main/resources/local-data/input/5.1.Gzyouyou_WLTC_output.xls
```

## Output format

Each row in the input spreadsheet becomes a JSON object. Keys are taken from the English header
row produced by DataPrep; values are the DeepL-translated cell contents.

If lineage was supplied (DataPrep sidecar and/or `--source-artifact-id`), every object also contains `sourceArtifactId` (same string on all rows). Do not use the literal column header `sourceArtifactId` in your spreadsheet — that name is reserved for this JSON field.

**Without** lineage (no sidecar and no `--source-artifact-id`):

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

**With** lineage (sidecar and/or `--source-artifact-id 550e8400-e29b-41d4-a716-446655440000`):

```json
[
  {
    "Car Name": "Nissan",
    "Common Name": "Note",
    "Model Type": "6AA-E13",
    "Engine Displacement (L)": "1.198",
    "Vehicle Weight (kg)": "1190",
    "sourceArtifactId": "550e8400-e29b-41d4-a716-446655440000"
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
