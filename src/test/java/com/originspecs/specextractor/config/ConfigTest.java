package com.originspecs.specextractor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Config} argument parsing, {@link OutputNamer} path derivation,
 * and {@link CliParser} exception behaviour.
 *
 * Note: {@code Config.validate()} checks the input file exists on disk, so
 * we test only {@code Config.fromArgs()} and {@code OutputNamer} here.
 */
class ConfigTest {

    private static final String VALID_INPUT = "input.xls";

    // --- Config.fromArgs happy path ---

    @Test
    void fromArgs_validArgument_createsConfigWithCorrectInputFile() {
        Config config = Config.fromArgs(new String[]{VALID_INPUT});

        assertThat(config.inputFile().toString()).isEqualTo(VALID_INPUT);
    }

    @Test
    void fromArgs_outputFileIsInExpectedDirectory() {
        Config config = Config.fromArgs(new String[]{VALID_INPUT});

        assertThat(config.outputFile().toString())
                .startsWith("src/main/resources/local-data/output/");
    }

    @Test
    void fromArgs_outputFilenameContainsInputBaseName() {
        Config config = Config.fromArgs(new String[]{"pre_processed_file.xls"});

        assertThat(config.outputFile().getFileName().toString())
                .contains("pre_processed_file")
                .endsWith(".json");
    }

    @Test
    void fromArgs_outputFileHasJsonExtension() {
        Config config = Config.fromArgs(new String[]{VALID_INPUT});

        assertThat(config.outputFile().getFileName().toString()).endsWith(".json");
    }

    // --- Config.fromArgs wrong argument count ---

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void fromArgs_wrongArgumentCount_throwsIllegalArgumentException(int argCount) {
        String[] args = new String[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = "arg" + i;
        }

        assertThatThrownBy(() -> Config.fromArgs(args))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 argument required");
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
}
