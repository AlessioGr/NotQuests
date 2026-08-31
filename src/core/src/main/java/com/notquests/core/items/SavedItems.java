package com.notquests.core.items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** Owns all configured saved items. */
public final class SavedItems {
    private final Map<String, SavedItem> items = new ConcurrentHashMap<>();

    /** The two native facts needed while parsing an item-selection token. */
    public interface NativeItems {
        ItemSelection itemInHand();

        String materialId(String input);
    }

    public enum ParseProblem {
        HAND_REQUIRES_PLAYER,
        UNKNOWN_MATERIAL,
        MISSING_SAVED_ITEM,
        CIRCULAR_SAVED_ITEM
    }

    /** A typed parse failure whose user-facing text is owned by core. */
    public static final class ParseException extends IllegalArgumentException {
        private final ParseProblem problem;
        private final String input;

        private ParseException(final ParseProblem problem, final String input) {
            super(message(problem, input));
            this.problem = problem;
            this.input = input == null ? "" : input;
        }

        public ParseProblem problem() {
            return problem;
        }

        public String input() {
            return input;
        }

        private static String message(final ParseProblem problem, final String input) {
            final String value = input == null ? "" : input;
            return switch (problem) {
                case HAND_REQUIRES_PLAYER -> "A player holding an item is required for 'hand'.";
                case UNKNOWN_MATERIAL -> "Unknown item material: " + value;
                case MISSING_SAVED_ITEM -> "Saved item no longer exists: " + value;
                case CIRCULAR_SAVED_ITEM -> "Saved item references form a cycle: " + value;
            };
        }
    }

    /** One ordered, saved-item-free choice ready for a platform to materialize. */
    public record ItemChoice(ItemSelection selection, String displayName) {
        public ItemChoice {
            displayName = displayName == null ? "" : displayName;
        }
    }

    public List<SavedItem> all() {
        return items.values().stream()
                .sorted(Comparator.comparing(SavedItem::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> names() {
        return all().stream().map(SavedItem::getName).toList();
    }

    public boolean add(
            final String name,
            final ItemSelection selection,
            final String category,
            final String displayName) {
        if (name == null || name.isBlank() || selection == null) {
            return false;
        }
        return items.putIfAbsent(
                key(name),
                new SavedItem(name, selection, category, displayName)) == null;
    }

    public SavedItem get(final String name) {
        return name == null || name.isBlank() ? null : items.get(key(name));
    }

    public boolean remove(final String name) {
        return name != null && !name.isBlank() && items.remove(key(name)) != null;
    }

    public boolean setDisplayName(final String name, final String displayName) {
        final SavedItem item = get(name);
        if (item == null) {
            return false;
        }
        item.setDisplayName(displayName);
        return true;
    }

    public void clear() {
        items.clear();
    }

    public ItemSelection parse(final Object input, final NativeItems nativeItems) {
        return parse(input, this::get, nativeItems);
    }

    /**
     * Parses keywords in one consistent order: {@code any}, {@code hand}, saved item, then native
     * material. The platform only answers what the hand contains and whether a material exists.
     */
    public static ItemSelection parse(
            final Object input,
            final Function<String, SavedItem> savedItemLookup,
            final NativeItems nativeItems) {
        if (input instanceof ItemSelection selection) {
            return selection;
        }
        final String inputText = input == null ? "" : String.valueOf(input);
        if (inputText.isBlank()) {
            return null;
        }

        final LinkedHashSet<String> materialIds = new LinkedHashSet<>();
        final LinkedHashSet<String> savedItemNames = new LinkedHashSet<>();
        final ArrayList<Map<String, Object>> exactItems = new ArrayList<>();
        boolean any = false;
        for (final String rawPart : inputText.split(",")) {
            final String part = rawPart.trim();
            if (part.isBlank()) {
                continue;
            }
            if (part.equalsIgnoreCase("any")) {
                any = true;
                continue;
            }
            if (part.equalsIgnoreCase("hand")) {
                final ItemSelection hand = nativeItems == null ? null : nativeItems.itemInHand();
                if (hand == null) {
                    throw new ParseException(ParseProblem.HAND_REQUIRES_PLAYER, part);
                }
                materialIds.addAll(hand.materialIds());
                exactItems.addAll(hand.exactItems());
                continue;
            }
            final SavedItem savedItem = savedItemLookup == null ? null : savedItemLookup.apply(part);
            if (savedItem != null) {
                savedItemNames.add(savedItem.getName());
                continue;
            }
            final String materialId = nativeItems == null ? null : nativeItems.materialId(part);
            if (materialId == null || materialId.isBlank()) {
                throw new ParseException(ParseProblem.UNKNOWN_MATERIAL, part);
            }
            materialIds.add(materialId);
        }
        return materialIds.isEmpty() && savedItemNames.isEmpty() && exactItems.isEmpty() && !any
                ? null
                : ItemStackSelection.of(materialIds, savedItemNames, exactItems, any, 1);
    }

    public List<ItemChoice> resolve(final ItemSelection selection) {
        return resolve(selection, this::get);
    }

    /**
     * Expands saved-item references recursively while preserving their order and the root amount.
     * A saved item's display name follows its expanded choices; an outer alias display name wins.
     */
    public static List<ItemChoice> resolve(
            final ItemSelection selection,
            final Function<String, SavedItem> savedItemLookup) {
        if (selection == null) {
            return List.of();
        }
        final ArrayList<ItemChoice> choices = new ArrayList<>();
        resolve(
                selection,
                Math.max(1, selection.amount()),
                "",
                savedItemLookup,
                new LinkedHashSet<>(),
                choices);
        return List.copyOf(choices);
    }

    private static void resolve(
            final ItemSelection selection,
            final int amount,
            final String displayName,
            final Function<String, SavedItem> savedItemLookup,
            final Set<String> resolving,
            final List<ItemChoice> choices) {
        if (selection.any()) {
            choices.add(new ItemChoice(
                    ItemStackSelection.of(List.of(), List.of(), List.of(), true, amount),
                    displayName));
            return;
        }

        for (final String savedItemName : selection.savedItemNames()) {
            final SavedItem savedItem = savedItemLookup == null ? null : savedItemLookup.apply(savedItemName);
            if (savedItem == null) {
                throw new ParseException(ParseProblem.MISSING_SAVED_ITEM, savedItemName);
            }
            final String savedItemKey = key(savedItem.getName());
            if (!resolving.add(savedItemKey)) {
                final String cycle = String.join(" -> ", resolving) + " -> " + savedItem.getName();
                throw new ParseException(ParseProblem.CIRCULAR_SAVED_ITEM, cycle);
            }
            final String resolvedDisplayName = displayName.isBlank()
                    ? savedItem.getDisplayName()
                    : displayName;
            resolve(
                    savedItem.getItemSelection(),
                    amount,
                    resolvedDisplayName,
                    savedItemLookup,
                    resolving,
                    choices);
            resolving.remove(savedItemKey);
        }

        if (!selection.materialIds().isEmpty() || !selection.exactItems().isEmpty()) {
            choices.add(new ItemChoice(
                    ItemStackSelection.of(
                            selection.materialIds(),
                            List.of(),
                            selection.exactItems(),
                            false,
                            amount),
                    displayName));
        }
    }

    public void journalPlayerJoined(
            final List<String> enabledWorlds,
            final int inventorySlot,
            final String worldName,
            final IntConsumer replaceJournal) {
        if (journalEnabled(enabledWorlds, worldName) && replaceJournal != null) {
            replaceJournal.accept(inventorySlot);
        }
    }

    public boolean journalItemUsed(final boolean rightClick, final boolean journalItem) {
        return rightClick && journalItem;
    }

    public void journalPlayerRespawned(
            final List<String> enabledWorlds,
            final int inventorySlot,
            final String deathWorld,
            final String respawnWorld,
            final IntPredicate journalInSlot,
            final IntConsumer replaceJournal) {
        if ((journalInSlot == null || !journalInSlot.test(inventorySlot))
                && journalEnabled(enabledWorlds, deathWorld)
                && journalEnabled(enabledWorlds, respawnWorld)
                && replaceJournal != null) {
            replaceJournal.accept(inventorySlot);
        }
    }

    public void journalPlayerDied(
            final List<String> enabledWorlds,
            final String worldName,
            final boolean journalInDrops,
            final Runnable removeJournalDrop) {
        if (journalInDrops && journalEnabled(enabledWorlds, worldName) && removeJournalDrop != null) {
            removeJournalDrop.run();
        }
    }

    public void journalItemPickedUpOrDropped(
            final List<String> enabledWorlds,
            final String worldName,
            final boolean journalItem,
            final Runnable cancel) {
        if (journalItem && journalEnabled(enabledWorlds, worldName) && cancel != null) {
            cancel.run();
        }
    }

    public void journalInventoryClicked(
            final List<String> enabledWorlds,
            final int inventorySlot,
            final String worldName,
            final boolean clickedJournal,
            final IntPredicate journalInSlot,
            final Runnable cancel,
            final Runnable refreshCreativeInventory,
            final IntConsumer replaceJournal) {
        if (!journalEnabled(enabledWorlds, worldName)) {
            return;
        }
        if (clickedJournal && cancel != null) {
            cancel.run();
            if (refreshCreativeInventory != null) {
                refreshCreativeInventory.run();
            }
        }
        if ((journalInSlot == null || !journalInSlot.test(inventorySlot)) && replaceJournal != null) {
            replaceJournal.accept(inventorySlot);
        }
    }

    private static boolean journalEnabled(final List<String> enabledWorlds, final String worldName) {
        if (enabledWorlds == null || enabledWorlds.isEmpty()) {
            return false;
        }
        if (enabledWorlds.contains("*")) {
            return true;
        }
        final String checkedWorld = worldName == null ? "" : worldName;
        return enabledWorlds.stream()
                .filter(Objects::nonNull)
                .anyMatch(checkedWorld::equalsIgnoreCase);
    }

    private static String key(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
