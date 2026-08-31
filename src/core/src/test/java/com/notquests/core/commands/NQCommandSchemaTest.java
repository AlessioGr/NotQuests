package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.commands.framework.*;

import java.time.Duration;
import java.util.List;

class NQCommandSchemaTest {
    @Test
    void ownsPublicCommandRootsAndAliases() {
        assertEquals("nq", NQCommandSchema.USER.name());
        assertTrue(NQCommandSchema.USER.names().contains("notquests"));
        assertTrue(NQCommandSchema.USER.names().contains("q"));

        assertEquals("nqa", NQCommandSchema.ADMIN.name());
        assertTrue(NQCommandSchema.ADMIN.names().contains("notquestsadmin"));
        assertTrue(NQCommandSchema.ADMIN.names().contains("qa"));
    }

    @Test
    void portableArgumentTypesConvertFlagValues() {
        assertEquals(4, NQArgumentType.integer("amount").convert("4"));
        assertEquals(2.5, NQArgumentType.number("amount").convert("2.5"));
        assertEquals(true, NQArgumentType.bool("enabled").convert("yes"));
        assertEquals(false, NQArgumentType.bool("enabled").convert("off"));
        assertEquals(Duration.ofMillis(500), NQArgumentType.duration().convert("500milliseconds"));
        assertEquals(Duration.ofMinutes(5), NQArgumentType.duration().convert("5MINUTES"));
        assertThrows(IllegalArgumentException.class, () -> NQArgumentType.bool("enabled").convert("maybe"));
    }

    @Test
    void portableSuggestionPolicyHandlesPrefixesPlaceholdersAndCommaLists() {
        assertEquals(List.of("true"), NQCommandBuilder.prefixMatches(List.of("true", "false"), "tr"));
        assertEquals(List.of("<quest-name>"), NQCommandBuilder.placeholderSuggestion("quest name"));
        assertEquals(
                List.of("acacia_boat", "acacia_boat,", "hanging_roots", "hanging_roots,"),
                NQCommandBuilder.matchingSuggestions(
                        NQArgumentType.itemSelection(),
                        List.of("acacia_boat", "hanging_roots"),
                        "",
                        "item"));
        assertEquals(
                List.of("acacia_boat,hanging_roots"),
                NQCommandBuilder.matchingSuggestions(
                        NQArgumentType.itemSelection(),
                        List.of("acacia_boat", "hanging_roots"),
                        "acacia_boat,ha",
                        "item"));
        assertFalse(NQCommandBuilder.placeholderSuggestion("").iterator().hasNext());
    }
}
