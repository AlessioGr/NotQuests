package com.notquests.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class YamlConfigTest {
    @Test
    @DisplayName("YAML config reads nested values")
    void readsNestedConfig() {
        final YamlConfig config = YamlConfig.parse("""
                quests:
                  TheVirus:
                    displayName: "A Deadly Virus"
                    objectiveType: BreakBlocks
                    progressNeededExpression: "64"
                    materials:
                      - dirt
                      - grass_block
                    enabled: true
                """);

        assertEquals("A Deadly Virus", config.getString("quests.TheVirus.displayName"));
        assertEquals("BreakBlocks", config.getString("quests.TheVirus.objectiveType"));
        assertEquals("64", config.getString("quests.TheVirus.progressNeededExpression"));
        assertEquals(List.of("dirt", "grass_block"), config.getStringList("quests.TheVirus.materials"));
        assertTrue(config.getBoolean("quests.TheVirus.enabled"));
    }

    @Test
    @DisplayName("YAML config renders parseable block YAML")
    void rendersParseableYaml() {
        final YamlConfig config = YamlConfig.empty();

        config.set("quests.TheVirus.displayName", "A Deadly Virus");
        config.set("quests.TheVirus.objectiveType", "BreakBlocks");
        config.set("quests.TheVirus.progressNeededExpression", "64");
        config.set("quests.TheVirus.materials", List.of("dirt", "grass_block"));
        config.set("quests.TheVirus.location", Map.of("world", "world", "x", 10, "y", 64, "z", -3));

        final String rendered = config.render();

        assertTrue(rendered.contains("quests:"));
        assertTrue(rendered.contains("displayName: A Deadly Virus"));
        assertTrue(rendered.contains("objectiveType: BreakBlocks"));
        assertTrue(rendered.contains("progressNeededExpression: '64'"));
        assertFalse(rendered.contains("!!"));

        final YamlConfig reparsed = YamlConfig.parse(rendered);
        assertEquals("A Deadly Virus", reparsed.getString("quests.TheVirus.displayName"));
        assertEquals("BreakBlocks", reparsed.getString("quests.TheVirus.objectiveType"));
        assertEquals(10, reparsed.getInt("quests.TheVirus.location.x"));
    }

    @Test
    @DisplayName("YAML config can save to a current-directory path")
    void savesToCurrentDirectoryPath() throws IOException {
        final Path file = Path.of("notquests-yaml-current-dir-test.yml");
        final YamlConfig config = YamlConfig.empty();

        try {
            config.set("name", "NotQuests");
            config.save(file);

            final YamlConfig loaded = YamlConfig.load(file);
            assertEquals("NotQuests", loaded.getString("name"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("section keys support shallow and deep reads")
    void supportsSectionViews() {
        final YamlConfig config = YamlConfig.empty();
        config.set("quests.TheVirus.objectives.1.objectiveType", "BreakBlocks");
        config.set("quests.TheVirus.objectives.1.specifics.materials", List.of("dirt"));

        final YamlConfig.Section section = config.section("quests.TheVirus.objectives.1");

        assertNotNull(section);
        assertTrue(section.keys(false).contains("objectiveType"));
        assertTrue(section.keys(false).contains("specifics"));
        assertTrue(section.keys(true).contains("specifics.materials"));
        assertEquals("BreakBlocks", section.getString("objectiveType"));
    }

    @Test
    @DisplayName("numeric YAML keys behave like Bukkit string paths")
    void normalizesNumericYamlKeysToStringPaths() {
        final YamlConfig config = YamlConfig.parse("""
                quests:
                  TheVirus:
                    objectives:
                      1:
                        objectiveType: BreakBlocks
                """);

        assertEquals("BreakBlocks", config.getString("quests.TheVirus.objectives.1.objectiveType"));
        assertNotNull(config.getConfigurationSection("quests.TheVirus.objectives.1"));
    }

    @Test
    @DisplayName("YAML parse and render preserve block and inline comments")
    void preservesCommentsAcrossParseAndRender() {
        final YamlConfig config = YamlConfig.parse("""
                # NotQuests configuration
                # Keep this header
                general: # General settings
                  # Controls saving
                  save-data: true # User preference

                  # Controls loading
                  load-data: true
                """);

        config.set("general.save-data", false);
        final String rendered = config.render();

        assertTrue(rendered.startsWith("# NotQuests configuration\n# Keep this header\n"));
        assertTrue(rendered.contains("general: # General settings"));
        assertTrue(rendered.contains("  # Controls saving\n  save-data: false # User preference"));
        assertTrue(rendered.contains("  # Controls loading\n  load-data: true"));
        assertEquals(
                rendered.indexOf("# NotQuests configuration"),
                rendered.lastIndexOf("# NotQuests configuration"));
    }

    @Test
    @DisplayName("setComments comments values inserted later")
    void commentsDefaultsInsertedLater() {
        final YamlConfig config = YamlConfig.empty();

        config.setComments("general.new-setting", List.of("Inserted default", "Default: true"));
        config.set("general.new-setting", true);

        assertTrue(config.render().contains("  # Inserted default\n  # Default: true\n  new-setting: true"));
    }

    @Test
    @DisplayName("replaceContents retains loaded comment metadata")
    void replacesContentsWithoutLosingComments() throws IOException {
        final Path file = Files.createTempFile("notquests-replaced-comments", ".yml");
        Files.writeString(file, """
                # Migration header
                quest:
                  # Renamed value explanation
                  value: old # Keep inline detail
                """);

        try {
            final YamlConfig config = YamlConfig.load(file);
            final Map<String, Object> replacement = new LinkedHashMap<>();
            replacement.put("quest", new LinkedHashMap<>(Map.of("value", "new", "added", true)));
            config.replaceContents(replacement);
            config.save(file);

            final String rendered = Files.readString(file);

            assertTrue(rendered.startsWith("# Migration header\n"));
            assertTrue(rendered.contains("  # Renamed value explanation\n  value: new # Keep inline detail"));
            assertEquals("new", YamlConfig.parse(rendered).getString("quest.value"));
            assertTrue(YamlConfig.parse(rendered).getBoolean("quest.added"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @DisplayName("YAML load and save preserve comments")
    void preservesCommentsAcrossLoadAndSave() throws IOException {
        final Path file = Files.createTempFile("notquests-comments", ".yml");
        Files.writeString(file, """
                # File header
                root:
                  # Stored value
                  value: before # Inline value
                """);

        try {
            final YamlConfig config = YamlConfig.load(file, YamlConfig.ValueCodec.PASSTHROUGH);
            config.set("root.value", "after");
            YamlConfig.save(config, file, YamlConfig.ValueCodec.PASSTHROUGH);

            final String saved = Files.readString(file);
            assertTrue(saved.startsWith("# File header\n"));
            assertTrue(saved.contains("  # Stored value\n  value: after # Inline value"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
