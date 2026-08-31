package com.notquests.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class NotQuestsRegistryTest {
    @Test
    void replacingARegistryIdKeepsTheLatestDefinition() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        adapter.actions().action("SendMessage")
                .displayName("Send Message")
                .description("Sends a message.")
                .register();

        adapter.actions().action("sendmessage")
                .displayName("Duplicate Send Message")
                .description("The latest registration wins so rebuilt dynamic commands stay in sync.")
                .register();

        assertEquals(1, registry.actions().size());
        assertEquals("Duplicate Send Message", registry.actions().getFirst().displayName());
    }

    @Test
    void coreAdapterExecutesListVariableActions() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final List<String> value = new ArrayList<>(List.of("existing"));
        final List<String> warnings = new ArrayList<>();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(warnings::add, null, null));

        adapter.variables()
                .listVariable("QuestSteps")
                .displayName("Quest Steps")
                .description("Portable list variable used by registry tests.")
                .singular("Quest Step")
                .plural("Quest Steps")
                .get((questPlayer, objects) -> value)
                .set((newValue, questPlayer, objects) -> {
                    value.clear();
                    value.addAll(newValue);
                    return true;
                })
                .register();

        final com.notquests.core.TestData action = data(
                "variableName", "QuestSteps",
                "operator", "add",
                "expression", "first,second");

        registry.executeVariableAction(
                new NotQuestsRegistry.PlatformHooks(warnings::add, null, null),
                com.notquests.core.variables.VariableDataType.LIST,
                action,
                null);

        assertTrue(warnings.isEmpty(), "list variable action should not use a warning fallback: " + warnings);
        org.junit.jupiter.api.Assertions.assertEquals(List.of("first", "second", "existing"), value);
    }

    @Test
    void coreAdapterExecutesItemStackListVariableActions() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final AtomicReference<List<com.notquests.core.items.ItemSelection>> value =
                new AtomicReference<>(new ArrayList<>());
        final List<String> warnings = new ArrayList<>();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(warnings::add, null, null));

        adapter.variables()
                .itemStackListVariable("InventoryItems")
                .displayName("Inventory Items")
                .description("Portable item-list variable used by registry tests.")
                .singular("Inventory Item")
                .plural("Inventory Items")
                .get((questPlayer, objects) -> value.get())
                .set((newValue, questPlayer, objects) -> {
                    value.set(new ArrayList<>(newValue));
                    return true;
                })
                .register();

        final com.notquests.core.TestData action = data(
                "variableName", "InventoryItems",
                "operator", "add",
                "itemStack", ItemStackSelection.parse("acacia_boat"));

        registry.executeVariableAction(
                new NotQuestsRegistry.PlatformHooks(warnings::add, null, null),
                com.notquests.core.variables.VariableDataType.ITEMSTACKLIST,
                action,
                null);

        assertTrue(warnings.isEmpty(), "item-list variable action should not use a warning fallback: " + warnings);
        assertTrue(value.get().getFirst().includesMaterial("acacia_boat"));
    }

    @Test
    void simpleItemSelectionsMatchVanillaAliasesWithoutDroppingModNamespaces() {
        final ItemStackSelection vanilla = ItemStackSelection.parse("minecraft:acacia_boat");
        assertTrue(vanilla.includesMaterial("acacia_boat"));
        assertTrue(vanilla.includesMaterial("minecraft:acacia_boat"));
        assertEquals("acacia_boat", vanilla.listedMaterials(""));

        final ItemStackSelection modded = ItemStackSelection.parse("examplemod:acacia_boat");
        assertTrue(modded.includesMaterial("examplemod:acacia_boat"));
        org.junit.jupiter.api.Assertions.assertFalse(modded.includesMaterial("acacia_boat"));
        assertEquals("examplemod:acacia_boat", modded.listedMaterials(""));
    }

    @Test
    void simpleItemSelectionsPreserveListedMaterialOrder() {
        assertEquals(
                "acacia_boat,stone",
                ItemStackSelection.parse("acacia_boat,stone").listedMaterials(""));
    }

    @Test
    void simpleItemSelectionsPreserveSavedItemNamesSeparatelyFromMaterials() {
        final var selection = ItemStackSelection.parse("acacia_boat,QuestKey", name -> name.equals("QuestKey"));

        assertEquals(List.of("acacia_boat"), selection.materialIds());
        assertEquals(List.of("QuestKey"), selection.savedItemNames());
        assertEquals("acacia_boat,QuestKey", selection.listedMaterials(""));
    }

    @Test
    void simpleItemSelectionYamlKeepsSavedItemsAndAmountStructured() {
        final var selection = ItemStackSelection
                .of(List.of("stone"), List.of("QuestKey"), false, 4);

        final Object yaml = ItemStackSelection.toYamlValue(selection);
        final Map<?, ?> map = assertInstanceOf(Map.class, yaml);
        assertEquals(List.of("stone"), map.get("materials"));
        assertEquals(List.of("QuestKey"), map.get("nqItems"));
        assertEquals(4, map.get("amount"));

        final var loaded = ItemStackSelection.fromYamlValue(map, name -> name.equals("QuestKey"));
        assertEquals(List.of("stone"), loaded.materialIds());
        assertEquals(List.of("QuestKey"), loaded.savedItemNames());
        assertEquals(4, loaded.amount());
    }

    @Test
    void exactItemSelectionsRoundTripTheirCompletePlatformPayload() {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("==", "org.bukkit.inventory.ItemStack");
        payload.put("type", "DIAMOND_SWORD");
        payload.put("amount", 1);
        payload.put("meta", Map.of("display-name", "Relic", "custom-model-data", 42));
        final Map<String, Object> exactItem = new LinkedHashMap<>();
        exactItem.put("platform", "paper");
        exactItem.put("material", "minecraft:diamond_sword");
        exactItem.put("data", payload);
        final var selection = ItemStackSelection.of(
                List.of("diamond_sword"), List.of(), List.of(exactItem), false, 1);

        final Object yaml = ItemStackSelection.toYamlValue(selection);
        final ItemSelection loaded = ItemStackSelection.fromYamlValue(yaml, ignored -> false);

        assertEquals(List.of("diamond_sword"), loaded.materialIds());
        assertEquals(List.of(exactItem), loaded.exactItems());
    }

    @Test
    void mapDataCopiesInputIntoMutableCoreState() {
        final com.notquests.core.TestData data = new com.notquests.core.TestData(java.util.Map.of("material", "stone"));

        data.setValue("amount", 4);

        assertEquals("stone", data.text("material"));
        assertEquals(4, data.integer("amount"));
    }

    private static com.notquests.core.TestData data(final Object... pairs) {
        final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return new com.notquests.core.TestData(values);
    }
}
