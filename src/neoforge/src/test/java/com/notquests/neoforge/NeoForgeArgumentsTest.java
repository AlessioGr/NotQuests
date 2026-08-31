package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class NeoForgeArgumentsTest {
    @Test
    void commaListsUseRegisteredNativeTokenArgument() {
        assertInstanceOf(
                NeoForgeArguments.WhitespaceTerminatedStringArgument.class,
                NeoForgeArguments.commaToken());
    }

    @Test
    void itemSelectionReadsCommasUntilWhitespaceWithoutSwallowingAmount() throws Exception {
        final StringReader reader = new StringReader("hanging_roots,acacia_boat 4");

        assertEquals("hanging_roots,acacia_boat", NeoForgeArguments.commaToken().parse(reader));
        assertEquals(' ', reader.peek());
    }

    @Test
    void itemSelectionStillSupportsQuotedCustomNames() throws Exception {
        final StringReader reader = new StringReader("\"custom item\" 2");

        assertEquals("custom item", NeoForgeArguments.commaToken().parse(reader));
        assertEquals(' ', reader.peek());
    }

    @Test
    void adapterArgumentDoesNotOwnPortableSuggestionPolicy() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeArguments.java"));
        assertFalse(source.contains("BuiltInRegistries"));
        assertFalse(source.contains("builder.suggest("));
    }

    @Test
    void compilerDelegatesPortableSuggestionsToCore() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/com/notquests/neoforge/NeoForgeCoreCommandCompiler.java"));

        assertTrue(source.contains("commands.suggestions("));
        assertTrue(source.contains("commands.flagSuggestions("));
        assertFalse(source.contains("placeholderSuggestion("));
        assertFalse(source.contains("suggestCommaSeparated("));
    }

    @Test
    void nestedAliasesAreAcceptedWithoutBeingSuggested() throws Exception {
        final var alias = LiteralArgumentBuilder.<Object>literal("view")
                .executes(context -> 1)
                .build();
        final var hiddenAlias = NeoForgeArguments.hiddenLiteralAlias(alias);
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(hiddenAlias);

        final List<String> suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("", new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();

        assertEquals(List.of(), suggestions);
        assertEquals("", dispatcher.parse("view", new Object()).getReader().getRemaining());
        assertNotNull(hiddenAlias.getCommand());
    }
}
