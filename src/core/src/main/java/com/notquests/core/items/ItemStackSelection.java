package com.notquests.core.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ItemStackSelection implements ItemSelection {
    private Set<String> materialIds;
    private Set<String> savedItemNames;
    private List<Map<String, Object>> exactItems;
    private boolean any;
    private int amount;

    public ItemStackSelection() {
        this(Set.of(), Set.of(), List.of(), false, 1);
    }

    protected ItemStackSelection(final int amount) {
        this(Set.of(), Set.of(), List.of(), false, amount);
    }

    private ItemStackSelection(final Set<String> materialIds, final boolean any) {
        this(materialIds, Set.of(), List.of(), any, 1);
    }

    private ItemStackSelection(
            final Set<String> materialIds,
            final Set<String> savedItemNames,
            final List<Map<String, Object>> exactItems,
            final boolean any,
            final int amount) {
        this.materialIds = Collections.unmodifiableSet(new LinkedHashSet<>(materialIds));
        this.savedItemNames = Collections.unmodifiableSet(new LinkedHashSet<>(savedItemNames));
        this.exactItems = exactItems(exactItems);
        this.any = any;
        this.amount = Math.max(1, amount);
    }

    public static ItemStackSelection parse(final String input) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("any")) {
            return new ItemStackSelection(Set.of(), true);
        }
        final Set<String> ids = Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .map(ItemStackSelection::normalize)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ItemStackSelection(
                ids,
                Set.of(),
                List.of(),
                ids.stream().anyMatch("any"::equalsIgnoreCase),
                1);
    }

    public static ItemStackSelection parse(
            final String input,
            final Predicate<String> savedItemExists) {
        if (input == null || input.isBlank() || input.equalsIgnoreCase("any")) {
            return new ItemStackSelection(Set.of(), Set.of(), List.of(), true, 1);
        }
        final LinkedHashSet<String> materialIds = new LinkedHashSet<>();
        final LinkedHashSet<String> savedItemNames = new LinkedHashSet<>();
        boolean any = false;
        for (final String rawPart : input.split(",")) {
            final String part = rawPart.trim();
            if (part.isBlank()) {
                continue;
            }
            if (part.equalsIgnoreCase("any")) {
                any = true;
                continue;
            }
            if (savedItemExists != null && savedItemExists.test(part)) {
                savedItemNames.add(part);
                continue;
            }
            materialIds.add(normalize(part));
        }
        return new ItemStackSelection(materialIds, savedItemNames, List.of(), any, 1);
    }

    public static ItemStackSelection of(
            final Iterable<String> materialIds,
            final Iterable<String> savedItemNames,
            final boolean any,
            final int amount) {
        return of(materialIds, savedItemNames, List.of(), any, amount);
    }

    public static ItemStackSelection of(
            final Iterable<String> materialIds,
            final Iterable<String> savedItemNames,
            final Iterable<? extends Map<String, Object>> exactItems,
            final boolean any,
            final int amount) {
        return new ItemStackSelection(
                normalizedSet(materialIds),
                orderedSet(savedItemNames),
                exactItems(exactItems),
                any,
                amount);
    }

    /**
     * Platform-neutral fallback used when an item selection has no native item payload. Platform
     * adapters replace this with their native item similarity check when metadata is available.
     */
    public static boolean areSimilar(final ItemSelection required, final ItemSelection actual) {
        if (required == null || actual == null) {
            return false;
        }
        if (required.any()) {
            return actual.any()
                    || !actual.materialIds().isEmpty()
                    || !actual.savedItemNames().isEmpty()
                    || !actual.exactItems().isEmpty();
        }
        for (final Map<String, Object> requiredItem : required.exactItems()) {
            if (actual.exactItems().contains(requiredItem)) {
                return true;
            }
        }
        // An exact saved/native item must retain its metadata semantics. Its accompanying material
        // id is only useful for materializing the item; accepting that id here would make a named,
        // enchanted, or otherwise customized item match every plain stack of the same material.
        if (!required.exactItems().isEmpty()) {
            return false;
        }
        for (final String materialId : actual.materialIds()) {
            if (required.includesMaterial(materialId)) {
                return true;
            }
        }
        for (final String savedItemName : actual.savedItemNames()) {
            if (required.savedItemNames().stream().anyMatch(savedItemName::equalsIgnoreCase)) {
                return true;
            }
        }
        return false;
    }

    public static Object toYamlValue(final ItemSelection itemSelection) {
        if (itemSelection == null) {
            return "";
        }
        if (itemSelection.any()) {
            if (itemSelection.amount() <= 1) {
                return "any";
            }
            final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("any", true);
            map.put("amount", itemSelection.amount());
            return map;
        }
        if (itemSelection.savedItemNames().isEmpty()
                && itemSelection.exactItems().isEmpty()
                && itemSelection.amount() <= 1) {
            return String.join(",", itemSelection.materialIds());
        }
        final LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (!itemSelection.materialIds().isEmpty()) {
            map.put("materials", itemSelection.materialIds());
        }
        if (!itemSelection.savedItemNames().isEmpty()) {
            map.put("nqItems", itemSelection.savedItemNames());
        }
        if (!itemSelection.exactItems().isEmpty()) {
            map.put("exactItems", itemSelection.exactItems());
        }
        if (itemSelection.amount() > 1) {
            map.put("amount", itemSelection.amount());
        }
        return map;
    }

    public static ItemSelection fromYamlValue(
            final Object yamlValue,
            final Predicate<String> savedItemExists) {
        if (yamlValue instanceof final ItemSelection itemSelection) {
            return itemSelection;
        }
        if (yamlValue instanceof final Map<?, ?> map) {
            if (Boolean.TRUE.equals(map.get("any"))) {
                return of(List.of(), List.of(), List.of(), true, amount(map.get("amount")));
            }
            if (looksLikeExactItem(map)) {
                final Map<String, Object> exactItem = normalizeExactItem(map);
                return of(
                        materialFromExactItem(exactItem),
                        List.of(),
                        List.of(exactItem),
                        false,
                        amount(map.get("amount")));
            }
            final LinkedHashSet<String> materialIds = selectionParts(map.get("materials"));
            final LinkedHashSet<String> savedItemNames = selectionParts(map.get("nqItems"));
            final List<Map<String, Object>> exactItems = exactItems(map.get("exactItems"));
            for (final Map<String, Object> exactItem : exactItems) {
                materialIds.addAll(materialFromExactItem(exactItem));
            }
            if (!materialIds.isEmpty() || !savedItemNames.isEmpty() || !exactItems.isEmpty()) {
                return of(materialIds, savedItemNames, exactItems, false, amount(map.get("amount")));
            }
        }
        if (yamlValue instanceof final Iterable<?> iterable) {
            final String input = selectionParts(iterable).stream()
                    .collect(Collectors.joining(","));
            return parse(input, savedItemExists);
        }
        return parse(String.valueOf(yamlValue == null ? "" : yamlValue), savedItemExists);
    }

    @Override
    public boolean includesMaterial(final String materialId) {
        return any || materialIds.contains(normalize(materialId));
    }

    @Override
    public String listedMaterials(final String miniMessageTag) {
        if (any) {
            return "any";
        }
        final LinkedHashSet<String> ids = new LinkedHashSet<>(materialIds);
        ids.addAll(savedItemNames);
        for (final Map<String, Object> exactItem : exactItems) {
            ids.addAll(materialFromExactItem(exactItem));
        }
        return String.join(",", ids);
    }

    @Override
    public boolean any() {
        return any;
    }

    @Override
    public List<String> materialIds() {
        return List.copyOf(materialIds);
    }

    @Override
    public List<String> savedItemNames() {
        return List.copyOf(savedItemNames);
    }

    @Override
    public List<Map<String, Object>> exactItems() {
        return exactItems(exactItems);
    }

    @Override
    public int amount() {
        return amount;
    }

    protected final void setAmount(final int amount) {
        this.amount = Math.max(1, amount);
    }

    @Override
    public ItemSelection withAmount(final int amount) {
        return new ItemStackSelection(materialIds, savedItemNames, exactItems, any, amount);
    }

    public ItemStackSelection withAny(final boolean any) {
        return new ItemStackSelection(materialIds, savedItemNames, exactItems, any, amount);
    }

    public ItemStackSelection withMaterial(final String materialId) {
        final LinkedHashSet<String> materials = new LinkedHashSet<>(materialIds);
        if (materialId != null && !materialId.isBlank()) {
            materials.add(normalize(materialId));
        }
        return new ItemStackSelection(materials, savedItemNames, exactItems, any, amount);
    }

    public ItemStackSelection withSavedItem(final String savedItemName) {
        final LinkedHashSet<String> names = new LinkedHashSet<>(savedItemNames);
        if (savedItemName != null && !savedItemName.isBlank()) {
            names.add(savedItemName.trim());
        }
        return new ItemStackSelection(materialIds, names, exactItems, any, amount);
    }

    public ItemStackSelection withExactItem(final Map<String, Object> exactItem) {
        final ArrayList<Map<String, Object>> items = new ArrayList<>(exactItems);
        if (exactItem != null && !exactItem.isEmpty()) {
            items.add(exactItem);
        }
        return new ItemStackSelection(materialIds, savedItemNames, items, any, amount);
    }

    public boolean empty() {
        return !any && materialIds.isEmpty() && savedItemNames.isEmpty() && exactItems.isEmpty();
    }

    public boolean isAny() {
        return any;
    }

    public void setAny(final boolean any) {
        this.any = any;
    }

    public void addNqItemName(final String savedItemName) {
        savedItemNames = withSavedItem(savedItemName).savedItemNames;
    }

    public void addMaterialName(final String materialId) {
        materialIds = withMaterial(materialId).materialIds;
    }

    public void addExactItem(final Map<String, Object> exactItem) {
        exactItems = withExactItem(exactItem).exactItems;
    }

    public String getAllMaterialsListed() {
        if (any) {
            return "Any";
        }
        final LinkedHashSet<String> values = new LinkedHashSet<>(materialIds);
        values.addAll(savedItemNames);
        for (final Map<String, Object> exactItem : exactItems) {
            values.addAll(materialFromExactItem(exactItem));
        }
        return String.join(", ", values);
    }

    public boolean hasSavedItem() {
        return !savedItemNames.isEmpty();
    }

    public List<String> getNqItemNames() {
        return savedItemNames();
    }

    public List<String> getMaterialNames() {
        return materialIds();
    }

    public boolean isEmptyOrAny() {
        return any || empty();
    }

    @Override
    public String toString() {
        return "ItemStackSelection{" +
                "savedItems=" + savedItemNames +
                ", exactItems=" + exactItems +
                ", materials=" + materialIds +
                ", any=" + any +
                '}';
    }

    private static List<Map<String, Object>> exactItems(final Object value) {
        final ArrayList<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            if (looksLikeExactItem(map)) {
                result.add(normalizeExactItem(map));
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (final Object entry : iterable) {
                if (entry instanceof Map<?, ?> item) {
                    result.add(normalizeExactItem(item));
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> exactItems(
            final Iterable<? extends Map<String, Object>> values) {
        final ArrayList<Map<String, Object>> result = new ArrayList<>();
        if (values != null) {
            for (final Map<String, Object> value : values) {
                if (value != null) {
                    result.add(normalizeExactItem(value));
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean looksLikeExactItem(final Map<?, ?> map) {
        return map.containsKey("platform") && map.containsKey("data");
    }

    private static Map<String, Object> normalizeExactItem(final Map<?, ?> source) {
        return Collections.unmodifiableMap(mutableMap(source));
    }

    private static List<String> materialFromExactItem(final Map<String, Object> exactItem) {
        final Object material = exactItem.get("material");
        return material == null || material.toString().isBlank()
                ? List.of()
                : List.of(normalize(material.toString()));
    }

    private static Map<String, Object> mutableMap(final Map<?, ?> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), mutableValue(entry.getValue()));
        }
        return copy;
    }

    private static Object mutableValue(final Object value) {
        if (value instanceof Map<?, ?> map) {
            return mutableMap(map);
        }
        if (value instanceof Iterable<?> iterable) {
            final ArrayList<Object> copy = new ArrayList<>();
            for (final Object entry : iterable) {
                copy.add(mutableValue(entry));
            }
            return copy;
        }
        return value;
    }

    private static String normalize(final String materialId) {
        if (materialId == null) {
            return "";
        }
        final String normalized = materialId.toLowerCase(Locale.ROOT);
        return normalized.startsWith("minecraft:") ? normalized.substring("minecraft:".length()) : normalized;
    }

    private static LinkedHashSet<String> normalizedSet(final Iterable<String> values) {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values == null) {
            return result;
        }
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(normalize(value.trim()));
            }
        }
        return result;
    }

    private static LinkedHashSet<String> orderedSet(final Iterable<String> values) {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        if (values == null) {
            return result;
        }
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(value.trim());
            }
        }
        return result;
    }

    private static LinkedHashSet<String> selectionParts(final Object value) {
        final LinkedHashSet<String> parts = new LinkedHashSet<>();
        if (value instanceof final Iterable<?> iterable) {
            for (final Object entry : iterable) {
                addSelectionPart(parts, entry);
            }
            return parts;
        }
        addSelectionPart(parts, value);
        return parts;
    }

    private static void addSelectionPart(final LinkedHashSet<String> parts, final Object value) {
        if (value == null) {
            return;
        }
        final String part = String.valueOf(value).trim();
        if (!part.isBlank()) {
            parts.add(part);
        }
    }

    private static int amount(final Object rawAmount) {
        if (rawAmount instanceof Number number) {
            return number.intValue();
        }
        if (rawAmount instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (final NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }
}
