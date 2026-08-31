package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.gui.GuiService;
import com.notquests.core.structs.Category;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

class ConfigurationManagerDefaultFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultGeneratedFilesAreIdenticalForPaperAndNeoForgeFolders() throws Exception {
        final Path paperFolder = tempDir.resolve("paper");
        final Path neoForgeFolder = tempDir.resolve("neoforge");

        ConfigurationManager.copyMissingDefaults(paperFolder);
        ConfigurationManager.copyMissingDefaults(neoForgeFolder);

        final Map<String, String> paperFiles = readFiles(paperFolder);
        final Map<String, String> neoForgeFiles = readFiles(neoForgeFolder);

        assertEquals(paperFiles, neoForgeFiles);
        assertFalse(paperFiles.get("general.yml").isBlank(), "general.yml must not be generated empty");
        assertTrue(paperFiles.containsKey("default/category.yml"));
        assertTrue(paperFiles.containsKey("default/quests.yml"));
        assertTrue(paperFiles.containsKey("default/actions.yml"));
        assertTrue(paperFiles.containsKey("default/conditions.yml"));
        assertTrue(paperFiles.containsKey("default/tags.yml"));
        assertTrue(paperFiles.containsKey("default/items.yml"));
        for (final String guiName : GuiService.GUI_NAMES) {
            assertTrue(paperFiles.containsKey("guis/" + guiName + ".yml"), "Missing GUI default: " + guiName);
        }
    }

    @Test
    void demoConversationGeneratedForBothPlatformsIsIdentical() throws Exception {
        final Path paperFolder = tempDir.resolve("paper");
        final Path neoForgeFolder = tempDir.resolve("neoforge");

        final Path paperConversation = ConversationManager.create(
                paperFolder,
                "DemoConversation",
                true,
                Category.DEFAULT_NAME);
        final Path neoForgeConversation = ConversationManager.create(
                neoForgeFolder,
                "DemoConversation",
                true,
                Category.DEFAULT_NAME);

        assertEquals(
                Files.readString(paperConversation),
                Files.readString(neoForgeConversation));
        assertEquals(
                ConfigurationManager.bundledResourceText("conversations/demo.yml"),
                Files.readString(paperConversation));
    }

    private static Map<String, String> readFiles(final Path root) throws Exception {
        final Map<String, String> files = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (final Path path : paths.filter(Files::isRegularFile).toList()) {
                files.put(
                        root.relativize(path).toString().replace('\\', '/'),
                        Files.readString(path, StandardCharsets.UTF_8));
            }
        }
        return files;
    }
}
