package com.originspecs.specextractor.config;

import com.originspecs.specextractor.model.SourceArtifactId;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CLI arguments into a {@link Config}. Does not touch the filesystem or {@code DEEPL_API_KEY}.
 */
public final class ConfigParser {

    private ConfigParser() {}

    /**
     * Exactly one positional argument: input workbook path. Optional {@code --source-artifact-id &lt;uuid&gt;} at most once
     * (any order relative to the path).
     */
    public static Config parse(String[] args) {
        List<String> positionals = new ArrayList<>();
        SourceArtifactId sourceArtifactId = null;
        for (int i = 0; i < args.length; i++) {
            if (Constants.CLI_SOURCE_ARTIFACT_ID_FLAG.equals(args[i])) {
                if (sourceArtifactId != null) {
                    throw new IllegalArgumentException(
                            "Duplicate " + Constants.CLI_SOURCE_ARTIFACT_ID_FLAG + " — at most one allowed");
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value after " + Constants.CLI_SOURCE_ARTIFACT_ID_FLAG);
                }
                sourceArtifactId = SourceArtifactId.parse(args[++i]);
                continue;
            }
            positionals.add(args[i]);
        }
        if (positionals.size() != 1) {
            throw new IllegalArgumentException(
                    "Exactly one input file path is required (positional). Use: <inputFile.xls> or "
                            + Constants.CLI_SOURCE_ARTIFACT_ID_FLAG + " <uuid> <inputFile.xls>");
        }
        Path inputFile = Path.of(positionals.get(0));
        return new Config(
                inputFile,
                OutputNamer.derive(inputFile),
                resolveDeeplApiKey(),
                sourceArtifactId);
    }

    private static String resolveDeeplApiKey() {
        String key = System.getenv("DEEPL_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "DEEPL_API_KEY environment variable is not set. " +
                    "Export it before running: export DEEPL_API_KEY=your-key");
        }
        return key;
    }
}
