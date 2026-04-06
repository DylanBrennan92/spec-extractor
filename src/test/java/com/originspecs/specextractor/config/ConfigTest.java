package com.originspecs.specextractor.config;

import com.originspecs.specextractor.model.SourceArtifactId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ConfigParser}, {@link Config}, {@link OutputNamer}, and {@link CliParser}.
 */
class ConfigTest {

    private static final String VALID_INPUT = "input.xls";
    private static final UUID SAMPLE_ARTIFACT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void parse_validArgument_createsConfigWithCorrectInputFile() {
        Config config = ConfigParser.parse(new String[]{VALID_INPUT});

        assertThat(config.inputFile().toString()).isEqualTo(VALID_INPUT);
        assertThat(config.sourceArtifactId()).isNull();
        assertThat(config.sourceArtifactLineage()).isEmpty();
    }

    @Test
    void parse_outputFileIsInExpectedDirectory() {
        Config config = ConfigParser.parse(new String[]{VALID_INPUT});

        assertThat(config.outputFile().toString())
                .startsWith("src/main/resources/local-data/output/");
    }

    @Test
    void parse_outputFilenameContainsInputBaseName() {
        Config config = ConfigParser.parse(new String[]{"pre_processed_file.xls"});

        assertThat(config.outputFile().getFileName().toString())
                .contains("pre_processed_file")
                .endsWith(".json");
    }

    @Test
    void parse_outputFileHasJsonExtension() {
        Config config = ConfigParser.parse(new String[]{VALID_INPUT});

        assertThat(config.outputFile().getFileName().toString()).endsWith(".json");
    }

    @Test
    void parse_sourceArtifactFlagBeforeFile_setsId() {
        String id = SAMPLE_ARTIFACT_ID.toString();
        Config config = ConfigParser.parse(new String[]{Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, id, VALID_INPUT});

        assertThat(config.sourceArtifactLineage()).contains(SourceArtifactId.parse(id));
        assertThat(config.inputFile().toString()).isEqualTo(VALID_INPUT);
    }

    @Test
    void parse_sourceArtifactFlagAfterFile_setsId() {
        String id = SAMPLE_ARTIFACT_ID.toString();
        Config config = ConfigParser.parse(new String[]{VALID_INPUT, Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, id});

        assertThat(config.sourceArtifactLineage()).contains(SourceArtifactId.parse(id));
        assertThat(config.inputFile().toString()).isEqualTo(VALID_INPUT);
    }

    @Test
    void parse_duplicateSourceArtifactFlag_throws() {
        String id = SAMPLE_ARTIFACT_ID.toString();
        assertThatThrownBy(() -> ConfigParser.parse(new String[]{
                Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, id,
                Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, id,
                VALID_INPUT
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void parse_invalidUuid_throws() {
        assertThatThrownBy(() -> ConfigParser.parse(new String[]{
                Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, "not-uuid", VALID_INPUT
        }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_flagWithoutValue_throws() {
        assertThatThrownBy(() -> ConfigParser.parse(new String[]{Constants.CLI_SOURCE_ARTIFACT_ID_FLAG}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing value");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void parse_wrongPositionalCount_throwsIllegalArgumentException(int argCount) {
        String[] args = new String[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = "arg" + i + (i == 0 && argCount > 0 ? ".xls" : "");
        }

        assertThatThrownBy(() -> ConfigParser.parse(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one input file");
    }

    @Test
    void parse_onlySourceArtifactFlagAndUuid_throwsBecauseNoInputFile() {
        String id = SAMPLE_ARTIFACT_ID.toString();
        assertThatThrownBy(() -> ConfigParser.parse(new String[]{Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, id}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one input file");
    }

    // --- OutputNamer ---

    @Test
    void outputNamer_usesIsoTimestampAndOriginalBaseName() {
        Path output = OutputNamer.derive(Path.of("myfile.xls"));
        String filename = output.getFileName().toString();

        assertThat(filename).startsWith("20"); // year
        assertThat(filename).contains("_myfile.json");
        assertThat(filename).endsWith(".json");
        assertThat(filename).matches("\\d{8}T\\d{6}_myfile\\.json");
    }

    @Test
    void outputNamer_timestampFormatIsDateHoursMinutesSeconds() {
        Path output = OutputNamer.derive(Path.of("data.xls"));
        String filename = output.getFileName().toString();
        String timestamp = filename.substring(0, filename.indexOf('_'));

        assertThat(timestamp).matches("\\d{8}T\\d{6}");
        assertThat(timestamp).hasSize(15);
    }

    @Test
    void outputNamer_preservesOriginalBaseName() {
        Path output = OutputNamer.derive(Path.of("5.1.Gzyouyou_WLTC_output.xls"));
        String filename = output.getFileName().toString();

        assertThat(filename).contains("5.1.Gzyouyou_WLTC_output");
        assertThat(filename).endsWith(".json");
    }

    @Test
    void outputNamer_placesFileInOutputDirectory() {
        Path output = OutputNamer.derive(Path.of("any.xls"));

        assertThat(output.getParent().toString())
                .isEqualTo(Constants.OUTPUT_DIR);
    }

    @Test
    void outputNamer_handlesInputWithoutExtension() {
        Path output = OutputNamer.derive(Path.of("noextension"));

        assertThat(output.getFileName().toString())
                .contains("noextension")
                .endsWith(".json");
        assertThat(output.getFileName().toString()).matches("\\d{8}T\\d{6}_noextension\\.json");
    }

    // --- CliParser ---

    @Test
    void cliParser_invalidArgs_throwsCliException() {
        assertThatThrownBy(() -> CliParser.parseOrExit(new String[]{}))
                .isInstanceOf(CliException.class);
    }

    @Test
    void cliParser_acceptsSourceArtifactId(@TempDir Path temp) throws Exception {
        Path input = temp.resolve("in.xls");
        Files.createFile(input);
        Config config = CliParser.parseOrExit(new String[]{
                Constants.CLI_SOURCE_ARTIFACT_ID_FLAG, SAMPLE_ARTIFACT_ID.toString(), input.toString()
        });

        assertThat(config.inputFile()).isEqualTo(input);
        assertThat(config.sourceArtifactLineage()).contains(SourceArtifactId.parse(SAMPLE_ARTIFACT_ID.toString()));
    }

    @Test
    void cliParser_loadsLineageFromDataprepSidecar_whenFlagOmitted(@TempDir Path temp) throws Exception {
        Path input = temp.resolve("cleaned.xls");
        Files.createFile(input);
        Files.writeString(input.resolveSibling("cleaned.xls.source-artifact-id"), SAMPLE_ARTIFACT_ID + "\n");

        Config config = CliParser.parseOrExit(new String[]{input.toString()});

        assertThat(config.sourceArtifactLineage()).contains(SourceArtifactId.parse(SAMPLE_ARTIFACT_ID.toString()));
    }

    @Test
    void cliParser_throwsWhenCliSourceArtifactIdDoesNotMatchSidecar(@TempDir Path temp) throws Exception {
        Path input = temp.resolve("cleaned.xls");
        Files.createFile(input);
        Files.writeString(input.resolveSibling("cleaned.xls.source-artifact-id"), SAMPLE_ARTIFACT_ID + "\n");

        assertThatThrownBy(() -> CliParser.parseOrExit(new String[]{
                Constants.CLI_SOURCE_ARTIFACT_ID_FLAG,
                "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                input.toString()
        }))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("does not match");
    }
}
