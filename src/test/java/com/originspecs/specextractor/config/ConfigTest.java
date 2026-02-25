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
 * Unit tests for {@link Config} argument parsing and output path derivation.
 *
 * Note: {@code Config.validate()} checks the input file exists on disk, so
 * we test only {@code Config.fromArgs()} and {@code Config.deriveOutputPath()} here.
 */
class ConfigTest {

    private static final String VALID_INPUT = "input.xls";

    // --- Happy path ---

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

    // --- Output path derivation ---

    @Test
    void deriveOutputPath_stripsExtensionAndAppendsSuffix() {
        Path output = Config.deriveOutputPath(Path.of("myfile.xls"));
        String filename = output.getFileName().toString();

        assertThat(filename).startsWith("myfile_");
        assertThat(filename).endsWith(".json");
        // baseName(6) + "_"(1) + suffix(10) + ".json"(5) = 22 chars
        assertThat(filename).hasSize(22);
    }

    @Test
    void deriveOutputPath_suffixIsAlphanumericAndTenCharsLong() {
        Path output = Config.deriveOutputPath(Path.of("data.xls"));
        String filename = output.getFileName().toString();
        // extract the 10-char suffix between "_" and ".json"
        String suffix = filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf('.'));

        assertThat(suffix).hasSize(10);
        assertThat(suffix).matches("[a-z0-9]+");
    }

    @Test
    void deriveOutputPath_producesUniqueSuffixesAcrossMultipleCalls() {
        Set<Path> outputs = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            outputs.add(Config.deriveOutputPath(Path.of("data.xls")));
        }
        assertThat(outputs).hasSizeGreaterThan(1);
    }

    // --- Wrong argument count ---

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
}
