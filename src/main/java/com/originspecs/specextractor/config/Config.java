package com.originspecs.specextractor.config;

import java.nio.file.Path;

public record Config(
        Path inputFile,
        Path outputFile,
        String deeplApiKey
) {
    public static Config fromArgs(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Exactly 1 argument required: <inputFile.xls>");
        }

        Path inputFile = Path.of(args[0]);
        return new Config(inputFile, OutputNamer.derive(inputFile), resolveDeeplApiKey());
    }

    public void validate() {
        if (!inputFile.toFile().exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + inputFile.toAbsolutePath());
        }
    }

    private static String resolveDeeplApiKey() {
        String key = System.getenv("DEEPL_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "DEEPL_API_KEY environment variable is not set. " +
                    "Export it before running: export DEEPL_API_KEY=your-key"
            );
        }
        return key;
    }
}
