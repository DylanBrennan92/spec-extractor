package com.originspecs.specextractor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

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
                .startsWith("pre_processed_file_");
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
    void outputNamer_stripsExtensionAndAppendsSuffix() {
        Path output = OutputNamer.derive(Path.of("myfile.xls"));
        String filename = output.getFileName().toString();

        assertThat(filename).startsWith("myfile_");
        assertThat(filename).endsWith(".json");
        // baseName(6) + "_"(1) + suffix(10) + ".json"(5) = 22 chars
        assertThat(filename).hasSize(22);
    }

    @Test
    void outputNamer_suffixIsAlphanumericAndTenCharsLong() {
        Path output = OutputNamer.derive(Path.of("data.xls"));
        String filename = output.getFileName().toString();
        String suffix = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));

        assertThat(suffix).hasSize(10);
        assertThat(suffix).matches("[a-z0-9]+");
    }

    @Test
    void outputNamer_producesUniqueSuffixesAcrossMultipleCalls() {
        Set<Path> outputs = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            outputs.add(OutputNamer.derive(Path.of("data.xls")));
        }
        assertThat(outputs).hasSizeGreaterThan(1);
    }

    @Test
    void outputNamer_placesFileInOutputDirectory() {
        Path output = OutputNamer.derive(Path.of("any.xls"));

        assertThat(output.getParent().toString())
                .isEqualTo(Constants.OUTPUT_DIR);
    }

    // --- CliParser ---

    @Test
    void cliParser_invalidArgs_throwsCliException() {
        assertThatThrownBy(() -> CliParser.parseOrExit(new String[]{}))
                .isInstanceOf(CliException.class);
    }
}
