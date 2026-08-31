package com.notquests.builtin.variables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.TestNotQuestsAdapter;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NpcAttachments.Detachments;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.variables.VariableDataType;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ContainerInventoryVariableTest {
    @Test
    void itemStackListAddUsesContainerLeafAtConfiguredLocation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final FakeAdapter adapter = new FakeAdapter(plugin.registry());
        ContainerInventoryVariable.register(plugin, adapter);

        plugin.executeVariableAction(
                VariableDataType.ITEMSTACKLIST,
                data(
                        "variableName", "ContainerInventory",
                        "operator", "add",
                        "itemStack", ItemStackSelection.parse("chest").withAmount(2),
                        "additionalStrings", Map.of("world", "world"),
                        "additionalNumbers", Map.of("x", "10", "y", "64", "z", "20"),
                        "additionalBooleans", Map.of("add", "1", "skipItemIfInventoryFull", "1")),
                null);

        assertTrue(adapter.added);
        assertFalse(adapter.dropOverflow);
        assertEquals("world", adapter.location.worldName());
        assertEquals(10, adapter.location.x());
        assertEquals(64, adapter.location.y());
        assertEquals(20, adapter.location.z());
        assertEquals(1, adapter.items.size());
        assertTrue(adapter.items.getFirst().selection().includesMaterial("chest"));
        assertEquals(2, adapter.items.getFirst().selection().amount());
    }

    private static Action data(final Object... pairs) {
        final Action values = new Action(0, "VariableAction", null);
        for (int i = 0; i < pairs.length; i += 2) {
            values.setValue(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return values;
    }

    private static final class FakeAdapter implements TestNotQuestsAdapter {
        private final NotQuestsAdapter registryAdapter;
        private boolean added;
        private boolean dropOverflow;
        private NQLocation location;
        private List<SavedItems.ItemChoice> items = List.of();

        private FakeAdapter(final NotQuestsRegistry registry) {
            registryAdapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        }

        @Override
        public FieldFactories fields() {
            return registryAdapter.fields();
        }

        @Override
        public com.notquests.core.registry.NotQuestsRegistry.Actions.Registry actions() {
            return registryAdapter.actions();
        }

        @Override
        public com.notquests.core.registry.NotQuestsRegistry.Conditions.Registry conditions() {
            return registryAdapter.conditions();
        }

        @Override
        public com.notquests.core.registry.NotQuestsRegistry.Objectives.Registry objectives() {
            return registryAdapter.objectives();
        }

        @Override
        public com.notquests.core.registry.NotQuestsRegistry.Triggers.Registry triggers() {
            return registryAdapter.triggers();
        }

        @Override
        public com.notquests.core.registry.NotQuestsRegistry.Variables.Registry variables() {
            return registryAdapter.variables();
        }

        @Override
        public List<String> variableNames(final VariableDataType type) {
            return registryAdapter.variableNames(type);
        }

        @Override
        public VariableDataType variableType(final String variableName) {
            return registryAdapter.variableType(variableName);
        }

        @Override
        public String variableSingular(final String variableName) {
            return registryAdapter.variableSingular(variableName);
        }

        @Override
        public String variablePlural(final String variableName) {
            return registryAdapter.variablePlural(variableName);
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            return registryAdapter.variableFields(variableName);
        }

        @Override
        public Object variableValue(
                final String variableName,
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final Object... objects) {
            return registryAdapter.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public String serverBrand() {
            return registryAdapter.serverBrand();
        }

        @Override
        public List<String> damageTypeIds() {
            return registryAdapter.damageTypeIds();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return registryAdapter.onlinePlayerNames();
        }

        @Override
        public com.notquests.core.platform.PlatformPlayer onlineQuestPlayer(final String playerName) {
            return registryAdapter.onlineQuestPlayer(playerName);
        }

        @Override
        public List<String> worldNames() {
            return registryAdapter.worldNames();
        }

        @Override
        public List<String> itemSelectionOptions() {
            return registryAdapter.itemSelectionOptions();
        }

        @Override
        public List<String> entityTypeIds() {
            return registryAdapter.entityTypeIds();
        }

        @Override
        public List<String> particleTypeIds() {
            return registryAdapter.particleTypeIds();
        }

        @Override
        public List<String> soundTypeIds() {
            return registryAdapter.soundTypeIds();
        }

        @Override
        public List<String> soundCategoryIds() {
            return registryAdapter.soundCategoryIds();
        }

        @Override
        public List<String> statisticIds() {
            return registryAdapter.statisticIds();
        }

        @Override
        public List<String> advancementIds() {
            return registryAdapter.advancementIds();
        }

        @Override
        public List<String> blockMaterialOptions() {
            return registryAdapter.blockMaterialOptions();
        }

        @Override
        public List<String> enchantmentIds() {
            return registryAdapter.enchantmentIds();
        }

        @Override
        public boolean supportsNpcAttachments() {
            return registryAdapter.supportsNpcAttachments();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return registryAdapter.supportsArmorStandAttachmentTools();
        }

        @Override
        public List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
            return registryAdapter.npcSelectorOptions(allowNone, allowRightClickSelect);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return registryAdapter.npcSelection(npcSelector);
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return registryAdapter.setNpcQuestGiver(selection, enabled);
        }

        @Override
        public boolean giveArmorStandTool(
                final com.notquests.core.platform.PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return registryAdapter.giveArmorStandTool(actor, tool);
        }

        @Override
        public boolean giveNpcSelectionTool(
                final com.notquests.core.platform.PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            return registryAdapter.giveNpcSelectionTool(actor, selectionId, displayName, lore);
        }

        @Override
        public List<String> inventorySlotIds() {
            return registryAdapter.inventorySlotIds();
        }

        @Override
        public List<String> inventoryItemEnchantments(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String slotId) {
            return registryAdapter.inventoryItemEnchantments(questPlayer, slotId);
        }

        @Override
        public boolean hasPermission(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String permission) {
            return registryAdapter.hasPermission(questPlayer, permission);
        }

        @Override
        public boolean setPermission(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String permission,
                final boolean value) {
            return registryAdapter.setPermission(questPlayer, permission, value);
        }

        @Override
        public int playerStatistic(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String statisticId) {
            return registryAdapter.playerStatistic(questPlayer, statisticId);
        }

        @Override
        public boolean setPlayerStatistic(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            return registryAdapter.setPlayerStatistic(questPlayer, statisticId, value);
        }

        @Override
        public boolean hasAdvancement(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String advancementId) {
            return registryAdapter.hasAdvancement(questPlayer, advancementId);
        }

        @Override
        public boolean setAdvancement(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            return registryAdapter.setAdvancement(questPlayer, advancementId, completed);
        }

        @Override
        public NQLocation location(final String worldName, final double x, final double y, final double z) {
            return NQLocation.at(worldName, x, y, z);
        }

        @Override
        public String blockMaterial(final NQLocation location) {
            return registryAdapter.blockMaterial(location);
        }

        @Override
        public boolean setBlockMaterial(
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final NQLocation location,
                final String materialOrKeyword) {
            return registryAdapter.setBlockMaterial(questPlayer, location, materialOrKeyword);
        }

        @Override
        public void schedule(final Duration delay, final Runnable action) {
            registryAdapter.schedule(delay, action);
        }

        @Override
        public boolean isServerThread() {
            return registryAdapter.isServerThread();
        }

        @Override
        public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
            return registryAdapter.callOnServerThread(action);
        }

        @Override
        public void warn(final String message) {
            registryAdapter.warn(message);
        }

        @Override
        public void logInfo(final String message) {
            registryAdapter.logInfo(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            registryAdapter.broadcast(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            registryAdapter.dispatchConsoleCommand(command);
        }

        @Override
        public boolean addContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            this.added = true;
            this.location = location;
            this.items = List.copyOf(items);
            this.dropOverflow = dropOverflow;
            return true;
        }

        @Override
        public List<ItemSelection> containerInventoryItems(final NQLocation location) {
            return registryAdapter.containerInventoryItems(location);
        }

        @Override
        public boolean removeContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return registryAdapter.removeContainerInventoryItems(location, items);
        }

        @Override
        public boolean setContainerInventoryItems(
                final NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return registryAdapter.setContainerInventoryItems(location, items);
        }

        @Override
        public ItemSelection parseItemSelection(final String input) {
            return registryAdapter.parseItemSelection(input);
        }

        @Override
        public String resolveActionText(
                final com.notquests.core.registry.NotQuestsRegistry.Actions.Data action,
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return registryAdapter.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final com.notquests.core.platform.PlatformPlayer questPlayer,
                final com.notquests.core.structs.ActiveObjective activeObjective,
                final Map<String, String> replacements) {
            return registryAdapter.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }
}
