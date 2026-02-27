package com.originspecs.specextractor.config;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public record Config(
        Path inputFile,
        Path outputFile
) {
    public static Config fromArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Exactly 1 argument required: <inputFile.xls>");
        }

        Path inputFile = Path.of(args[0]);
        return new Config(inputFile, OutputNamer.derive(inputFile));
    }

    public void validate() {
        if (!inputFile.toFile().exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.toAbsolutePath());
        }
    }
}
