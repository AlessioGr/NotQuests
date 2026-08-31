package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void npcSelectorsAcceptCitizensIdsAndFancyNpcsUuids() throws Exception {
        assertEquals("citizens:0", NeoForgeArguments.commaToken().parse(new StringReader("citizens:0")));
        assertEquals(
                "fancynpcs:b7ffc743-0e12-4415-bf43-feb69a73f649",
                NeoForgeArguments.commaToken().parse(
                        new StringReader("fancynpcs:b7ffc743-0e12-4415-bf43-feb69a73f649")));
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
        assertTrue(source.contains("ITEM_SELECTION, ACTION_LIST, NPC_SELECTOR, NPC_SELECTOR_OR_NONE"));
        assertFalse(source.contains("placeholderSuggestion("));
        assertFalse(source.contains("suggestCommaSeparated("));
    }
}
