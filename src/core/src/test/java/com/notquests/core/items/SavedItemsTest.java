package com.notquests.core.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class SavedItemsTest {
    private final SavedItems savedItems = new SavedItems();

    @Test
    void journalWorldAndSlotPolicyLivesInCore() {
        final AtomicInteger replacedSlot = new AtomicInteger(-1);

        savedItems.journalPlayerJoined(List.of("world"), 7, "WORLD", replacedSlot::set);
        assertEquals(7, replacedSlot.get());

        replacedSlot.set(-1);
        savedItems.journalPlayerJoined(List.of("world"), 7, "nether", replacedSlot::set);
        assertEquals(-1, replacedSlot.get());

        savedItems.journalPlayerRespawned(
                List.of("*"),
                4,
                "world",
                "world_nether",
                slot -> false,
                replacedSlot::set);
        assertEquals(4, replacedSlot.get());
    }

    @Test
    void journalProtectionAndCreativeRefreshAreChosenInCore() {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean refreshed = new AtomicBoolean();
        final AtomicInteger replacedSlot = new AtomicInteger(-1);

        savedItems.journalInventoryClicked(
                List.of("world"),
                3,
                "world",
                true,
                slot -> false,
                () -> cancelled.set(true),
                () -> refreshed.set(true),
                replacedSlot::set);

        assertTrue(cancelled.get());
        assertTrue(refreshed.get());
        assertEquals(3, replacedSlot.get());
    }

    @Test
    void journalUseDeathPickupAndDropPolicyLivesInCore() {
        final AtomicBoolean removedFromDrops = new AtomicBoolean();
        final AtomicBoolean cancelled = new AtomicBoolean();

        assertTrue(savedItems.journalItemUsed(true, true));
        savedItems.journalPlayerDied(
                List.of("world"), "world", true, () -> removedFromDrops.set(true));
        savedItems.journalItemPickedUpOrDropped(
                List.of("world"), "world", true, () -> cancelled.set(true));

        assertTrue(removedFromDrops.get());
        assertTrue(cancelled.get());

        cancelled.set(false);
        assertFalse(savedItems.journalItemUsed(false, true));
        savedItems.journalItemPickedUpOrDropped(
                List.of("world"), "nether", true, () -> cancelled.set(true));
        assertFalse(cancelled.get());
    }

    @Test
    void savedItemNamesTakePrecedenceOverNativeMaterials() {
        savedItems.add("Stone", ItemStackSelection.parse("diamond"), "", "");
        final AtomicBoolean materialParserCalled = new AtomicBoolean();

        final ItemSelection selection = savedItems.parse("stone", new SavedItems.NativeItems() {
            @Override
            public ItemSelection itemInHand() {
                return null;
            }

            @Override
            public String materialId(final String input) {
                materialParserCalled.set(true);
                return input;
            }
        });

        assertEquals(List.of("Stone"), selection.savedItemNames());
        assertFalse(materialParserCalled.get());
    }

    @Test
    void parseErrorsAreTypedAndWordedByCore() {
        final SavedItems.NativeItems noNativeItem = new SavedItems.NativeItems() {
            @Override
            public ItemSelection itemInHand() {
                return null;
            }

            @Override
            public String materialId(final String input) {
                return null;
            }
        };

        final SavedItems.ParseException hand = assertThrows(
                SavedItems.ParseException.class,
                () -> savedItems.parse("hand", noNativeItem));
        assertEquals(SavedItems.ParseProblem.HAND_REQUIRES_PLAYER, hand.problem());
        assertEquals("A player holding an item is required for 'hand'.", hand.getMessage());

        final SavedItems.ParseException material = assertThrows(
                SavedItems.ParseException.class,
                () -> savedItems.parse("not_a_material", noNativeItem));
        assertEquals(SavedItems.ParseProblem.UNKNOWN_MATERIAL, material.problem());
        assertEquals("Unknown item material: not_a_material", material.getMessage());
    }

    @Test
    void recursivelyResolvesSavedItemsInOrderWithTheRootAmount() {
        savedItems.add(
                "Base",
                ItemStackSelection.of(List.of("stone"), List.of(), false, 2),
                "",
                "<gold>Base");
        savedItems.add(
                "Alias",
                ItemStackSelection.of(List.of("gold_ingot"), List.of("Base"), false, 4),
                "",
                "<green>Alias");

        final ItemSelection requested = ItemStackSelection.of(
                List.of(), List.of("Alias"), false, 9);
        final List<SavedItems.ItemChoice> choices = savedItems.resolve(requested);

        assertEquals(2, choices.size());
        assertEquals(List.of("stone"), choices.get(0).selection().materialIds());
        assertEquals(List.of("gold_ingot"), choices.get(1).selection().materialIds());
        assertEquals(9, choices.get(0).selection().amount());
        assertEquals(9, choices.get(1).selection().amount());
        assertEquals("<green>Alias", choices.get(0).displayName());
        assertEquals("<green>Alias", choices.get(1).displayName());
    }

    @Test
    void preservesAnyAmountAndReportsBrokenSavedItemReferences() {
        final ItemSelection any = ItemStackSelection.of(List.of(), List.of(), true, 12);
        assertEquals(12, savedItems.resolve(any).getFirst().selection().amount());

        savedItems.add(
                "Broken",
                ItemStackSelection.of(List.of(), List.of("Missing"), false, 1),
                "",
                "");
        final SavedItems.ParseException missing = assertThrows(
                SavedItems.ParseException.class,
                () -> savedItems.resolve(ItemStackSelection.of(
                        List.of(), List.of("Broken"), false, 3)));
        assertEquals(SavedItems.ParseProblem.MISSING_SAVED_ITEM, missing.problem());
        assertEquals("Saved item no longer exists: Missing", missing.getMessage());
    }

    @Test
    void reportsCircularSavedItemReferences() {
        savedItems.add(
                "First",
                ItemStackSelection.of(List.of(), List.of("Second"), false, 1),
                "",
                "");
        savedItems.add(
                "Second",
                ItemStackSelection.of(List.of(), List.of("First"), false, 1),
                "",
                "");

        final SavedItems.ParseException cycle = assertThrows(
                SavedItems.ParseException.class,
                () -> savedItems.resolve(ItemStackSelection.of(
                        List.of(), List.of("First"), false, 5)));
        assertEquals(SavedItems.ParseProblem.CIRCULAR_SAVED_ITEM, cycle.problem());
        assertTrue(cycle.getMessage().startsWith("Saved item references form a cycle:"));
    }
}
