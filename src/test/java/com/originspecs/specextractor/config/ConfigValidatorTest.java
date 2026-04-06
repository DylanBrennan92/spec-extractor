package com.originspecs.specextractor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigValidatorTest {

    @Test
    void shouldThrow_whenInputDoesNotExist(@TempDir Path temp) {
        Path missing = temp.resolve("missing.xls");
        Config config = new Config(missing, Path.of("out.json"), "key", null);

        assertThatThrownBy(() -> ConfigValidator.validate(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void shouldThrow_whenInputIsNotRegularFile(@TempDir Path temp) throws Exception {
        Path dir = temp.resolve("folder");
        Files.createDirectory(dir);
        Config config = new Config(dir, Path.of("out.json"), "key", null);

        assertThatThrownBy(() -> ConfigValidator.validate(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a regular file");
    }

    @Test
    void shouldComplete_whenInputIsRegularFile(@TempDir Path temp) throws Exception {
        Path file = temp.resolve("book.xls");
        Files.createFile(file);
        Config config = new Config(file, Path.of("out.json"), "key", null);

        assertThatCode(() -> ConfigValidator.validate(config)).doesNotThrowAnyException();
    }
}
