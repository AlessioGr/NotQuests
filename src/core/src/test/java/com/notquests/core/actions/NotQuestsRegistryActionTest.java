package com.notquests.core.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class NotQuestsRegistryActionTest {
    @Test
    void parsesCommaSeparatedItemSelectionsAndAmountsFromRegistryFields() {
        final NotQuestsRegistry registry = registry();
        final Action data = Actions.parse(
                registry.createAdapter(hooks()),
                action(registry, "GiveItem"),
                "acacia_boat,hanging_roots 4");

        assertTrue(data.itemSelection("material").includesMaterial("acacia_boat"));
        assertTrue(data.itemSelection("material").includesMaterial("hanging_roots"));
        assertEquals(4, data.integer("amount"));
    }

    @Test
    void parsesGreedyTextBeforeDurationFlags() {
        final NotQuestsRegistry registry = registry();
        final Action data = Actions.parse(
                registry.createAdapter(hooks()),
                action(registry, "ShowTitle"),
                "\"Quest Complete|Good work\" --fadeIn 250ms --stay 2s --fadeOut 500ms");

        assertEquals("Quest Complete|Good work", data.text("title"));
        assertEquals(Duration.ofMillis(250), data.duration("fadeIn", Duration.ZERO));
        assertEquals(Duration.ofSeconds(2), data.duration("stay", Duration.ZERO));
        assertEquals(Duration.ofMillis(500), data.duration("fadeOut", Duration.ZERO));
    }

    @Test
    void commandTextKeepsDoubleDashTokensAsPartOfTheCommand() {
        final NotQuestsRegistry registry = registry();
        final Action data = Actions.parse(
                registry.createAdapter(hooks()),
                action(registry, "ConsoleCommand"),
                "say hello --this-is-not-an-action-flag");

        assertEquals("/say hello --this-is-not-an-action-flag", data.text("command"));
    }

    @Test
    void itemSelectionsAreParsedThroughTheProvidedAdapter() {
        final NotQuestsRegistry registry = registry();
        final AtomicReference<String> parsedInput = new AtomicReference<>();
        final var base = registry.createAdapter(hooks());
        final var adapter = new com.notquests.core.test.TestNotQuestsAdapter() {
            @Override
            public com.notquests.core.registry.fields.FieldFactories fields() {
                return base.fields();
            }

            @Override
            public com.notquests.core.registry.NotQuestsRegistry.Actions.Registry actions() {
                return base.actions();
            }

            @Override
            public com.notquests.core.registry.NotQuestsRegistry.Conditions.Registry conditions() {
                return base.conditions();
            }

            @Override
            public com.notquests.core.registry.NotQuestsRegistry.Objectives.Registry objectives() {
                return base.objectives();
            }

            @Override
            public com.notquests.core.registry.NotQuestsRegistry.Triggers.Registry triggers() {
                return base.triggers();
            }

            @Override
            public com.notquests.core.registry.NotQuestsRegistry.Variables.Registry variables() {
                return base.variables();
            }

            @Override
            public java.util.List<String> variableNames(final com.notquests.core.variables.VariableDataType type) {
                return base.variableNames(type);
            }

            @Override
            public com.notquests.core.variables.VariableDataType variableType(final String variableName) {
                return base.variableType(variableName);
            }

            @Override
            public String variableSingular(final String variableName) {
                return base.variableSingular(variableName);
            }

            @Override
            public String variablePlural(final String variableName) {
                return base.variablePlural(variableName);
            }

            @Override
            public java.util.List<com.notquests.core.registry.fields.RegistryField.Definition> variableFields(
                    final String variableName) {
                return base.variableFields(variableName);
            }

            @Override
            public Object variableValue(
                    final String variableName,
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final Object... objects) {
                return base.variableValue(variableName, questPlayer, objects);
            }

            @Override
            public String serverBrand() {
                return base.serverBrand();
            }

            @Override
            public java.util.List<String> damageTypeIds() {
                return base.damageTypeIds();
            }

            @Override
            public java.util.List<String> onlinePlayerNames() {
                return base.onlinePlayerNames();
            }

            @Override
            public com.notquests.core.platform.PlatformPlayer onlineQuestPlayer(final String playerName) {
                return base.onlineQuestPlayer(playerName);
            }

            @Override
            public java.util.List<String> worldNames() {
                return base.worldNames();
            }

            @Override
            public java.util.List<String> itemSelectionOptions() {
                return base.itemSelectionOptions();
            }

            @Override
            public java.util.List<String> entityTypeIds() {
                return base.entityTypeIds();
            }

            @Override
            public java.util.List<String> particleTypeIds() {
                return base.particleTypeIds();
            }

            @Override
            public java.util.List<String> soundTypeIds() {
                return base.soundTypeIds();
            }

            @Override
            public java.util.List<String> soundCategoryIds() {
                return base.soundCategoryIds();
            }

            @Override
            public java.util.List<String> statisticIds() {
                return base.statisticIds();
            }

            @Override
            public java.util.List<String> advancementIds() {
                return base.advancementIds();
            }

            @Override
            public java.util.List<String> blockMaterialOptions() {
                return base.blockMaterialOptions();
            }

            @Override
            public int playerStatistic(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String statisticId) {
                return base.playerStatistic(questPlayer, statisticId);
            }

            @Override
            public boolean setPlayerStatistic(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String statisticId,
                    final int value) {
                return base.setPlayerStatistic(questPlayer, statisticId, value);
            }

            @Override
            public boolean hasAdvancement(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String advancementId) {
                return base.hasAdvancement(questPlayer, advancementId);
            }

            @Override
            public boolean setAdvancement(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String advancementId,
                    final boolean completed) {
                return base.setAdvancement(questPlayer, advancementId, completed);
            }

            @Override
            public String blockMaterial(final com.notquests.core.platform.NQLocation location) {
                return base.blockMaterial(location);
            }

            @Override
            public boolean setBlockMaterial(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final com.notquests.core.platform.NQLocation location,
                    final String materialOrKeyword) {
                return base.setBlockMaterial(questPlayer, location, materialOrKeyword);
            }

            @Override
            public java.util.List<ItemSelection> containerInventoryItems(
                    final com.notquests.core.platform.NQLocation location) {
                return base.containerInventoryItems(location);
            }

            @Override
            public boolean addContainerInventoryItems(
                    final com.notquests.core.platform.NQLocation location,
                    final java.util.List<com.notquests.core.items.SavedItems.ItemChoice> items,
                    final boolean dropOverflow) {
                return base.addContainerInventoryItems(location, items, dropOverflow);
            }

            @Override
            public boolean removeContainerInventoryItems(
                    final com.notquests.core.platform.NQLocation location,
                    final java.util.List<com.notquests.core.items.SavedItems.ItemChoice> items) {
                return base.removeContainerInventoryItems(location, items);
            }

            @Override
            public boolean setContainerInventoryItems(
                    final com.notquests.core.platform.NQLocation location,
                    final java.util.List<com.notquests.core.items.SavedItems.ItemChoice> items) {
                return base.setContainerInventoryItems(location, items);
            }

            @Override
            public java.util.List<String> enchantmentIds() {
                return base.enchantmentIds();
            }

            @Override
            public com.notquests.core.platform.NQLocation location(
                    final String worldName,
                    final double x,
                    final double y,
                    final double z) {
                return base.location(worldName, x, y, z);
            }

            @Override
            public boolean supportsNpcAttachments() {
                return base.supportsNpcAttachments();
            }

            @Override
            public boolean supportsArmorStandAttachmentTools() {
                return base.supportsArmorStandAttachmentTools();
            }

            @Override
            public java.util.List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
                return base.npcSelectorOptions(allowNone, allowRightClickSelect);
            }

            @Override
            public NpcSelection npcSelection(final String npcSelector) {
                return base.npcSelection(npcSelector);
            }

            @Override
            public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
                return base.setNpcQuestGiver(selection, enabled);
            }

            @Override
            public boolean giveArmorStandTool(
                    final com.notquests.core.platform.PlatformPlayer actor,
                    final ArmorStandToolItem tool) {
                return base.giveArmorStandTool(actor, tool);
            }

            @Override
            public boolean giveNpcSelectionTool(
                    final com.notquests.core.platform.PlatformPlayer actor,
                    final int selectionId,
                    final String displayName,
                    final java.util.List<String> lore) {
                return base.giveNpcSelectionTool(actor, selectionId, displayName, lore);
            }

            @Override
            public boolean hasPermission(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String permission) {
                return base.hasPermission(questPlayer, permission);
            }

            @Override
            public boolean setPermission(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String permission,
                    final boolean value) {
                return base.setPermission(questPlayer, permission, value);
            }

            @Override
            public java.util.List<String> inventorySlotIds() {
                return base.inventorySlotIds();
            }

            @Override
            public java.util.List<String> inventoryItemEnchantments(
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String slotId) {
                return base.inventoryItemEnchantments(questPlayer, slotId);
            }

            @Override
            public void warn(final String message) {}

            @Override
            public void logInfo(final String message) {}

            @Override
            public void broadcast(final String miniMessage) {}

            @Override
            public void dispatchConsoleCommand(final String command) {}

            @Override
            public void schedule(final java.time.Duration delay, final Runnable action) {
                base.schedule(delay, action);
            }

            @Override
            public boolean isServerThread() {
                return base.isServerThread();
            }

            @Override
            public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
                return base.callOnServerThread(action);
            }

            @Override
            public ItemSelection parseItemSelection(final String input) {
                parsedInput.set(input);
                return new ItemSelection() {
                    @Override
                    public boolean includesMaterial(final String materialId) {
                        return materialId.equals("adapter_only_item");
                    }

                    @Override
                    public String listedMaterials(final String miniMessageTag) {
                        return "adapter_only_item";
                    }

                    @Override
                    public boolean any() {
                        return false;
                    }

                    @Override
                    public List<String> materialIds() {
                        return List.of("adapter_only_item");
                    }

                    @Override
                    public List<String> savedItemNames() {
                        return List.of();
                    }

                    @Override
                    public List<Map<String, Object>> exactItems() {
                        return List.of();
                    }

                    @Override
                    public int amount() {
                        return 1;
                    }

                    @Override
                    public ItemSelection withAmount(final int amount) {
                        return com.notquests.core.items.ItemStackSelection.of(
                                materialIds(), savedItemNames(), exactItems(), any(), amount);
                    }
                };
            }

            @Override
            public String resolveActionText(
                    final com.notquests.core.registry.NotQuestsRegistry.Actions.Data action,
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final String text,
                    final Object... objects) {
                return text;
            }

            @Override
            public String objectiveTaskText(
                    final String translationKey,
                    final com.notquests.core.platform.PlatformPlayer questPlayer,
                    final com.notquests.core.structs.ActiveObjective activeObjective,
                    final java.util.Map<String, String> replacements) {
                return translationKey;
            }
        };

        final Action data =
                Actions.parse(adapter, action(registry, "GiveItem"), "adapter_only_item 1");

        assertEquals("adapter_only_item", parsedInput.get());
        assertTrue(data.itemSelection("material").includesMaterial("adapter_only_item"));
    }

    private static NotQuestsRegistry registry() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final var adapter = registry.createAdapter(hooks());
        adapter.actions()
                .action("GiveItem")
                .displayName("Give Item")
                .description("Test action.")
                .field("material", adapter.fields().itemSelection(), "Material selection.")
                .field("amount", adapter.fields().integer(1), "Amount.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("ShowTitle")
                .displayName("Show Title")
                .description("Test action.")
                .field("title", adapter.fields().greedyText(), "Title.")
                .flag("fadeIn", adapter.fields().duration(Duration.ZERO), "Fade in.")
                .flag("stay", adapter.fields().duration(Duration.ZERO), "Stay.")
                .flag("fadeOut", adapter.fields().duration(Duration.ZERO), "Fade out.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("ConsoleCommand")
                .displayName("Console Command")
                .description("Test action.")
                .field("command", adapter.fields().commandText(), "Command.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        return registry;
    }

    private static Actions.Type action(final NotQuestsRegistry registry, final String id) {
        return registry.actions().stream()
                .filter(action -> action.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static NotQuestsRegistry.PlatformHooks hooks() {
        return new NotQuestsRegistry.PlatformHooks(ignored -> {}, ignored -> {}, ignored -> {});
    }
}
