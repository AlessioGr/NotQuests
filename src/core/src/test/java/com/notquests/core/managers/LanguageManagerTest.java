package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.managers.LanguageManager.Placeholders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class LanguageManagerTest {
    @TempDir
    private Path dataFolder;

    @Test
    void loadsBundledLanguageFilesAndAppliesMiniMessageSpecialTags() throws Exception {
        final LanguageManager translations = new LanguageManager();

        translations.load(dataFolder, "en-US", ignored -> {});

        assertTrue(Files.exists(dataFolder.resolve("languages").resolve("en-US.yml")));
        assertTrue(Files.exists(dataFolder.resolve("languages").resolve("default.yml")));
        assertEquals(
                "<error>Quest <highlight>Example</highlight> does not exist!",
                translations.translate(
                        "chat.quest-does-not-exist",
                        Map.of("%QUESTNAME%", "Example"),
                        "fallback"));
    }

    @Test
    void usesFallbackWhenLanguageKeyDoesNotExist() throws Exception {
        final LanguageManager translations = new LanguageManager();

        translations.load(dataFolder, "en-US", ignored -> {});

        assertEquals(
                "<main>Hello Alex",
                translations.translate(
                        "missing.key",
                        Map.of("%PLAYER%", "Alex"),
                        "<main>Hello %PLAYER%"));
    }

    @Test
    void ownsMissingLanguageMessagesForStringsListsAndComponentLists() throws Exception {
        final LanguageManager translations = new LanguageManager();

        translations.load(dataFolder, "en-US", ignored -> {});

        assertEquals(
                "Language string not found: missing.key",
                translations.translateString("missing.key", null, message -> message));
        assertEquals(
                "Language string not found: missing.%EXTERNAL%",
                translations.translateString("missing.%EXTERNAL%", null, message -> "changed"));
        assertEquals(
                List.of("Language string list not found: missing.lines"),
                translations.translateStringList("missing.lines", null, messages -> messages));
        assertEquals(
                List.of("Language string not found: missing.gui-lines"),
                translations.translateComponentStrings("missing.gui-lines", null, messages -> messages));
    }

    @Test
    void appliesInternalPlaceholdersThenExternalPlaceholdersThenSpecialTags() throws Exception {
        Files.createDirectories(dataFolder.resolve("languages"));
        Files.writeString(dataFolder.resolve("languages").resolve("en-US.yml"), """
                messages:
                  text: "%NAME%:%EXTERNAL%"
                  lines:
                    - "Line %NAME%"
                    - "%EXTERNAL%"
                """);
        final LanguageManager translations = new LanguageManager();

        translations.load(dataFolder, "en-US", ignored -> {});

        final Placeholders placeholders = Placeholders.create()
                .putLiteral("%NAME%", "Alex");
        assertEquals(
                "Alex: done",
                translations.translateString(
                        "messages.text",
                        placeholders,
                        message -> message.replace("%EXTERNAL%", message.contains("Alex") ? "<EMPTY>done" : "wrong")));
        assertEquals(
                List.of("Line Alex", " done"),
                translations.translateStringList(
                        "messages.lines",
                        placeholders,
                        messages -> messages.stream()
                                .map(message -> message.replace("%EXTERNAL%", "<EMPTY>done"))
                                .toList()));
    }

    @Test
    void logsOriginPaperUpdateMessagesWhenDefaultLanguageAddsMissingStrings() throws Exception {
        Files.createDirectories(dataFolder.resolve("languages"));
        Files.writeString(dataFolder.resolve("languages").resolve("en-US.yml"), "");
        final LanguageManager translations = new LanguageManager();
        final List<String> messages = new ArrayList<>();

        translations.load(dataFolder, "en-US", messages::add);

        assertTrue(messages.stream().anyMatch(message -> message.startsWith("Updating string: <highlight>")));
        assertTrue(messages.contains(
                "<DARK_PURPLE>Language ConfigurationManager <highlight>en-US.yml "
                        + "<DARK_PURPLE>was updated with new values! Saving it..."));
    }
}
