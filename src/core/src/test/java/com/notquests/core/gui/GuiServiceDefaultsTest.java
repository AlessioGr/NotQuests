package com.notquests.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class GuiServiceDefaultsTest {
    @TempDir
    Path tempDir;

    @Test
    void bundledDefaultsLoadEverySharedGuiLayout() {
        final var layouts = GuiService.loadBundledDefaults();

        assertEquals(GuiService.GUI_NAMES.size(), layouts.size());
        assertTrue(layouts.containsKey("main-base"));
        assertEquals("TAB", layouts.get("main-base").type());
        assertEquals("PAGED_ITEMS", layouts.get("main-active").type());
    }

    @Test
    void copyMissingDefaultsCreatesTheSharedYamlFiles() throws Exception {
        GuiService.copyMissingDefaults(tempDir);

        for (final String guiName : GuiService.GUI_NAMES) {
            assertTrue(
                    Files.exists(tempDir.resolve("guis").resolve(guiName + ".yml")),
                    () -> "Missing copied GUI default: " + guiName);
        }
    }
}
