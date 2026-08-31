package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.config.CategoryFiles;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.gui.GuiService;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.structs.Category;

import java.nio.file.Files;
import java.nio.file.Path;

class NeoForgeDefaultFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void neoForgeGeneratesSharedDefaultConfigFiles() throws Exception {
        ConfigurationManager.copyMissingDefaults(tempDir);

        final Path general = tempDir.resolve("general.yml");
        assertTrue(Files.exists(general));
        assertFalse(Files.readString(general).isBlank());
        assertEquals(ConfigurationManager.bundledResourceText("general.yml"), Files.readString(general));

        for (final String fileName : CategoryFiles.DEFAULT_FILES) {
            assertTrue(Files.exists(tempDir.resolve("default").resolve(fileName)), "Missing default/" + fileName);
        }
        assertTrue(Files.isDirectory(tempDir.resolve("default").resolve("conversations")));

        for (final String guiName : GuiService.GUI_NAMES) {
            assertTrue(Files.exists(tempDir.resolve("guis").resolve(guiName + ".yml")), "Missing GUI " + guiName);
        }
    }

    @Test
    void neoForgeDemoConversationUsesSharedTemplate() throws Exception {
        final Path conversation = ConversationManager.create(
                tempDir,
                "DemoConversation",
                true,
                Category.DEFAULT_NAME);

        assertEquals(ConfigurationManager.bundledResourceText("conversations/demo.yml"), Files.readString(conversation));
    }

    @Test
    void neoForgeDataFolderIsScopedToTheLoadedWorldRoot() throws Exception {
        final String entrypoint = Files.readString(
                Path.of("src/main/java/com/notquests/neoforge/NotQuestsNeoForge.java"));

        assertTrue(entrypoint.contains("currentServer.getWorldPath(LevelResource.ROOT)"));
        assertTrue(entrypoint.contains("Objects.requireNonNull(worldRoot, \"worldRoot\").resolve(\"notquests\")"));
    }
}
