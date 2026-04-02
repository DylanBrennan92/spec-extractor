package com.originspecs.specextractor.writer;

import com.originspecs.specextractor.config.Constants;
import com.originspecs.specextractor.model.SpecRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonWriterTest {

    @Test
    void write_includesSourceArtifactId_whenSet(@TempDir Path temp) throws Exception {
        Path out = temp.resolve("out.json");
        String id = "550e8400-e29b-41d4-a716-446655440000";
        List<SpecRecord> records = List.of(new SpecRecord(Map.of("Car", "X"), id));

        new JsonWriter().write(records, out);

        String json = Files.readString(out);
        assertThat(json).contains("\"Car\"").contains("\"X\"").contains(Constants.SOURCE_ARTIFACT_ID_JSON_KEY).contains(id);
    }

    @Test
    void write_omitsSourceArtifactId_whenNull(@TempDir Path temp) throws Exception {
        Path out = temp.resolve("out.json");
        List<SpecRecord> records = List.of(new SpecRecord(Map.of("Car", "Y")));

        new JsonWriter().write(records, out);

        assertThat(Files.readString(out)).doesNotContain(Constants.SOURCE_ARTIFACT_ID_JSON_KEY);
    }
}
