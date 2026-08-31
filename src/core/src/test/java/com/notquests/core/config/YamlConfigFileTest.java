package com.notquests.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class YamlConfigFileTest {
    @TempDir
    Path tempDir;

    @Test
    void loadAndSaveApplyCodecRecursively() throws Exception {
        final YamlConfig.ValueCodec codec = new YamlConfig.ValueCodec() {
            @Override
            public Object toYamlValue(final Object value) {
                return value instanceof EncodedValue encoded
                        ? Map.of("__testType", "encoded", "value", encoded.value())
                        : value;
            }

            @Override
            public Object fromYamlValue(final Object value) {
                if (value instanceof Map<?, ?> map && "encoded".equals(map.get("__testType"))) {
                    return new EncodedValue(String.valueOf(map.get("value")));
                }
                return value;
            }
        };

        final Path file = tempDir.resolve("config.yml");
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "root",
                Map.of(
                        "direct",
                        new EncodedValue("one"),
                        "list",
                        List.of(new EncodedValue("two")))));

        YamlConfig.save(configuration, file, codec);
        final YamlConfig loaded = YamlConfig.load(file, codec);

        assertEquals(new EncodedValue("one"), loaded.get("root.direct"));
        assertEquals(List.of(new EncodedValue("two")), loaded.get("root.list"));
    }

    private record EncodedValue(String value) {}
}
