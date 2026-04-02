package com.originspecs.specextractor.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceArtifactIdTest {

    @Test
    void parse_acceptsCanonicalUuidString() {
        String raw = "550e8400-e29b-41d4-a716-446655440000";
        assertThat(SourceArtifactId.parse(raw).value()).isEqualTo(raw);
    }

    @Test
    void parse_trimsWhitespace() {
        assertThat(SourceArtifactId.parse("  550e8400-e29b-41d4-a716-446655440000  ").value())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void parse_rejectsInvalidUuid() {
        assertThatThrownBy(() -> SourceArtifactId.parse("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_rejectsBlank() {
        assertThatThrownBy(() -> SourceArtifactId.parse("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equals_byValue() {
        UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        assertThat(SourceArtifactId.parse(u.toString())).isEqualTo(SourceArtifactId.parse(u.toString()));
    }
}
