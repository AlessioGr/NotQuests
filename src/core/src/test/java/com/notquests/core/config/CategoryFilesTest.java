package com.notquests.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class CategoryFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void discoversNestedCategoriesUsingFolderNames() throws Exception {
        final Path defaultCategory = tempDir.resolve("default");
        final Path storyFolder = defaultCategory.resolve("story-folder");
        final Path ignoredConversations = defaultCategory.resolve("conversations").resolve("nested");
        final Path ignoredRandomFolder = tempDir.resolve("random-folder");

        Files.createDirectories(storyFolder);
        Files.createDirectories(ignoredConversations);
        Files.createDirectories(ignoredRandomFolder);
        Files.writeString(defaultCategory.resolve("category.yml"), "id: main\n");
        Files.writeString(storyFolder.resolve("category.yml"), "id: story\n");
        Files.writeString(ignoredConversations.resolve("category.yml"), "id: should-not-load\n");
        Files.writeString(ignoredRandomFolder.resolve("not-category.yml"), "id: ignored\n");

        final List<CategoryFiles.CategoryFolder> categories = CategoryFiles.discover(tempDir);

        assertEquals(List.of("default", "default.story-folder"), categories.stream()
                .map(CategoryFiles.CategoryFolder::categoryName)
                .toList());
    }

    @Test
    void exposesCanonicalCategoryLayout() throws Exception {
        final Path categoryFolder = tempDir.resolve("default");
        Files.createDirectories(categoryFolder);
        Files.writeString(categoryFolder.resolve("category.yml"), "id: default\n");

        final CategoryFiles.CategoryFolder layout =
                new CategoryFiles.CategoryFolder(categoryFolder, "default");

        assertEquals(categoryFolder.resolve("category.yml"), layout.categoryFile());
        assertEquals(categoryFolder.resolve("quests.yml"), layout.questsFile());
        assertEquals(categoryFolder.resolve("actions.yml"), layout.actionsFile());
        assertEquals(categoryFolder.resolve("conditions.yml"), layout.conditionsFile());
        assertEquals(categoryFolder.resolve("tags.yml"), layout.tagsFile());
        assertEquals(categoryFolder.resolve("items.yml"), layout.itemsFile());
        assertEquals(categoryFolder.resolve("conversations"), layout.conversationsFolder());
        assertTrue(CategoryFiles.isCategoryFolder(categoryFolder));
        assertFalse(CategoryFiles.isCategoryFolder(tempDir.resolve("not-a-category")));
    }

    @Test
    void findsImmediateChildCategoriesOnly() throws Exception {
        final Path defaultCategory = tempDir.resolve("default");
        final Path nestedCategory = defaultCategory.resolve("nested");
        final Path ignoredFolder = tempDir.resolve("languages");

        Files.createDirectories(nestedCategory);
        Files.createDirectories(ignoredFolder);
        Files.writeString(defaultCategory.resolve("category.yml"), "id: default\n");
        Files.writeString(nestedCategory.resolve("category.yml"), "id: nested\n");

        assertEquals(
                List.of(defaultCategory),
                CategoryFiles.childCategoryFolders(tempDir));
    }
}
