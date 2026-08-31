package com.notquests.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.structs.Category;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class CategoryFilesPreparationTest {
    @TempDir
    Path dataFolder;

    @Test
    void preparesCategoryFilesInParentFirstOrder() {
        final Hooks hooks = new Hooks();

        final CategoryFiles.PreparedFolders result = CategoryFiles.prepare(
                dataFolder,
                List.of(new Category("story.daily"), new Category("story"), new Category("default")),
                hooks);

        assertEquals(
                List.of("default", "story", "story.daily"),
                result.layouts().stream().map(CategoryFiles.CategoryFolder::categoryName).toList());
        assertTrue(Files.exists(dataFolder.resolve("story").resolve("category.yml")));
        assertTrue(Files.exists(dataFolder.resolve("story").resolve("daily").resolve("quests.yml")));
        assertTrue(result.defaultPresent());
        assertTrue(hooks.disabled.isEmpty());
    }

    @Test
    void createsDefaultCategoryWhenCoreStateDoesNotContainOne() {
        final CategoryFiles.PreparedFolders result = CategoryFiles.prepare(
                dataFolder,
                List.of(new Category("story")),
                new Hooks());

        assertEquals(
                List.of("story", "default"),
                result.layouts().stream().map(CategoryFiles.CategoryFolder::categoryName).toList());
        assertTrue(Files.exists(dataFolder.resolve("default").resolve("category.yml")));
        assertTrue(result.defaultPresent());
    }

    private static final class Hooks implements CategoryFiles.Hooks {
        private final List<String> disabled = new ArrayList<>();

        @Override
        public void info(final String message) {}

        @Override
        public void disableSaving(final String reason, final Exception exception) {
            disabled.add(reason);
        }
    }
}
