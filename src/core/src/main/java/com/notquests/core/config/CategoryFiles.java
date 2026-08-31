package com.notquests.core.config;

import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.structs.Category;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class CategoryFiles {
    public static final String CATEGORY_FILE = "category.yml";
    public static final String QUESTS_FILE = "quests.yml";
    public static final String ACTIONS_FILE = "actions.yml";
    public static final String CONDITIONS_FILE = "conditions.yml";
    public static final String TAGS_FILE = "tags.yml";
    public static final String ITEMS_FILE = "items.yml";
    public static final String CONVERSATIONS_FOLDER = "conversations";
    public static final List<String> DEFAULT_FILES = List.of(
            CATEGORY_FILE,
            QUESTS_FILE,
            ACTIONS_FILE,
            CONDITIONS_FILE,
            TAGS_FILE,
            ITEMS_FILE);

    private CategoryFiles() {}

    public static List<CategoryFolder> discover(final Path dataFolder) throws IOException {
        if (!Files.isDirectory(dataFolder)) {
            return List.of();
        }
        final List<CategoryFolder> folders = new ArrayList<>();
        try (Stream<Path> paths = Files.list(dataFolder)) {
            for (final Path path : paths.filter(Files::isDirectory).sorted().toList()) {
                collect(path, "", folders);
            }
        }
        return List.copyOf(folders);
    }

    public static List<Path> childCategoryFolders(final Path parentFolder) throws IOException {
        if (!Files.isDirectory(parentFolder)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(parentFolder)) {
            return paths.filter(Files::isDirectory)
                    .filter(CategoryFiles::isCategoryFolder)
                    .sorted()
                    .toList();
        }
    }

    public static boolean isCategoryFolder(final Path path) {
        return Files.exists(path.resolve(CATEGORY_FILE));
    }

    public static void copyMissingDefaultCategory(final Path dataFolder) throws IOException {
        copyMissingCategory(dataFolder, Category.DEFAULT_NAME);
    }

    public static void copyMissingCategory(final Path dataFolder, final String categoryName) throws IOException {
        copyMissingCategoryFolder(categoryFolder(dataFolder, categoryName));
    }

    public static void copyMissingCategoryFolder(final Path categoryFolder) throws IOException {
        Files.createDirectories(categoryFolder);
        for (final String fileName : DEFAULT_FILES) {
            final Path target = categoryFolder.resolve(fileName);
            if (!Files.exists(target)) {
                Files.createFile(target);
            }
        }
        Files.createDirectories(categoryFolder.resolve(CONVERSATIONS_FOLDER));
    }

    public static Path categoryFolder(final Path dataFolder, final String categoryName) {
        final String finalCategoryName = categoryName == null || categoryName.isBlank()
                ? Category.DEFAULT_NAME
                : categoryName;
        Path categoryFolder = dataFolder;
        for (final String part : finalCategoryName.split("\\.")) {
            if (!part.isBlank()) {
                categoryFolder = categoryFolder.resolve(sanitizeCategoryName(part));
            }
        }
        return categoryFolder;
    }

    public static PreparedFolders prepare(
            final Path dataFolder,
            final Collection<Category> categories,
            final Hooks hooks) {
        final Hooks callbacks = hooks == null ? Hooks.NO_OP : hooks;
        final List<CategoryFolder> layouts = new ArrayList<>();
        boolean defaultPresent = false;

        if (!ConfigurationManager.ensureDataFolder(dataFolder, callbacks)) {
            return new PreparedFolders(List.of(), false);
        }

        final List<Category> ordered = (categories == null ? List.<Category>of() : categories.stream()
                .sorted(Comparator.comparingInt((Category category) -> category.getIdentifier().split("\\.").length)
                        .thenComparing(Category::getIdentifier, String.CASE_INSENSITIVE_ORDER))
                .toList());
        for (final Category category : ordered) {
            if (category == null || category.getIdentifier() == null || category.getIdentifier().isBlank()) {
                continue;
            }
            if (Category.isDefault(category.getIdentifier())) {
                defaultPresent = true;
            }
            final CategoryFolder layout = prepareCategory(dataFolder, category.getIdentifier(), callbacks);
            if (layout == null) {
                return new PreparedFolders(List.copyOf(layouts), defaultPresent);
            }
            layouts.add(layout);
        }

        if (!defaultPresent) {
            final CategoryFolder layout = prepareDefaultCategory(dataFolder, callbacks);
            if (layout != null) {
                layouts.add(layout);
                defaultPresent = true;
            }
        }
        return new PreparedFolders(List.copyOf(layouts), defaultPresent);
    }

    private static CategoryFolder prepareCategory(
            final Path dataFolder,
            final String categoryName,
            final Hooks hooks) {
        try {
            copyMissingCategory(dataFolder, categoryName);
            return new CategoryFolder(categoryFolder(dataFolder, categoryName), categoryName);
        } catch (final IOException exception) {
            hooks.disableSaving(
                    "There was an error creating category files for <highlight>"
                            + categoryName
                            + "</highlight>.",
                    exception);
            return null;
        }
    }

    private static CategoryFolder prepareDefaultCategory(
            final Path dataFolder,
            final Hooks hooks) {
        try {
            copyMissingDefaultCategory(dataFolder);
            return new CategoryFolder(categoryFolder(dataFolder, Category.DEFAULT_NAME), Category.DEFAULT_NAME);
        } catch (final IOException exception) {
            hooks.disableSaving("There was an error creating the default category files.", exception);
            return null;
        }
    }

    private static String sanitizeCategoryName(final String categoryName) {
        return categoryName.replaceAll("[^0-9a-zA-Z-._]", "_");
    }

    private static void collect(
            final Path path,
            final String parentCategoryName,
            final List<CategoryFolder> folders) throws IOException {
        final Path categoryFile = path.resolve(CATEGORY_FILE);
        if (!Files.exists(categoryFile)) {
            return;
        }
        final String folderName = path.getFileName().toString();
        final String categoryName = parentCategoryName == null || parentCategoryName.isBlank()
                ? folderName
                : parentCategoryName + "." + folderName;
        folders.add(new CategoryFolder(path, categoryName));
        try (Stream<Path> children = Files.list(path)) {
            for (final Path child : children.filter(Files::isDirectory).sorted().toList()) {
                if (!CONVERSATIONS_FOLDER.equalsIgnoreCase(child.getFileName().toString())) {
                    collect(child, categoryName, folders);
                }
            }
        }
    }

    public record CategoryFolder(Path path, String categoryName) {
        public Path categoryFile() {
            return path.resolve(CATEGORY_FILE);
        }

        public Path questsFile() {
            return path.resolve(QUESTS_FILE);
        }

        public Path actionsFile() {
            return path.resolve(ACTIONS_FILE);
        }

        public Path conditionsFile() {
            return path.resolve(CONDITIONS_FILE);
        }

        public Path tagsFile() {
            return path.resolve(TAGS_FILE);
        }

        public Path itemsFile() {
            return path.resolve(ITEMS_FILE);
        }

        public Path conversationsFolder() {
            return path.resolve(CONVERSATIONS_FOLDER);
        }
    }

    public record PreparedFolders(List<CategoryFolder> layouts, boolean defaultPresent) {}

    public interface Hooks extends ConfigurationManager.DataFolderHooks {
        Hooks NO_OP = new Hooks() {
            @Override
            public void info(final String message) {}

            @Override
            public void disableSaving(final String reason, final Exception exception) {}
        };
    }
}
