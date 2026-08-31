package com.notquests.builtin.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestNotQuestsAdapter;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.items.SavedItems;
import com.notquests.core.npc.NpcAttachments.Detachments;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Registry;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;

import java.util.LinkedHashMap;
import java.util.List;

class VariableConditionTest {
    @Test
    void portableVariableConditionsReadValuesThroughTheAdapter() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        adapter.variables()
                .listVariable("TestList")
                .displayName("Test List")
                .description("Test list.")
                .singular("test value")
                .plural("test values")
                .get((questPlayer, objects) -> List.of("Alpha", "Beta"))
                .register();
        adapter.variables()
                .itemStackListVariable("TestItems")
                .displayName("Test Items")
                .description("Test item stack list.")
                .singular("test item")
                .plural("test items")
                .get((questPlayer, objects) -> List.of(
                        ItemStackSelection.parse("acacia_boat"),
                        ItemStackSelection.parse("stone")))
                .register();
        final PlatformPlayer player = new TestPlayer();

        assertEquals("", check(registry, "Number", data -> {
            data.setValue("variableName", "Health");
            data.setValue("operator", "moreOrEqualThan");
            data.setValue("expression", "10");
        }, player));
        assertEquals("", check(registry, "Boolean", data -> {
            data.setValue("variableName", "True");
            data.setValue("operator", "equals");
            data.setValue("expression", "true");
        }, player));
        assertEquals("", check(registry, "List", data -> {
            data.setValue("variableName", "TestList");
            data.setValue("operator", "containsIgnoreCase");
            data.setValue("expression", "alpha");
        }, player));
        assertEquals("", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("acacia_boat"));
        }, player));
    }

    @Test
    void conditionInputParserReadsBuiltInConditionCommandArguments() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);

        final Condition worldTime =
                Conditions.parse(adapter, condition(registry, "WorldTime"), "6 18");
        assertEquals(6, worldTime.integer("minTime"));
        assertEquals(18, worldTime.integer("maxTime"));

        final Condition booleanValue =
                Conditions.parse(adapter, condition(registry, "Boolean"), "True equals true");
        assertEquals("True", booleanValue.text("variableName"));
        assertEquals("equals", booleanValue.text("operator"));
        assertEquals("true", booleanValue.text("expression"));

        final Condition stringValue = Conditions.parse(
                adapter,
                condition(registry, "String"),
                "CurrentWorld equals overworld with spaces");
        assertEquals("CurrentWorld", stringValue.text("variableName"));
        assertEquals("equals", stringValue.text("operator"));
        assertEquals("overworld with spaces", stringValue.text("expression"));

        final Condition blockValue = Conditions.parse(
                adapter,
                condition(registry, "String"),
                "Block overworld 12 64 -8 equals stone");
        assertEquals("Block", blockValue.text("variableName"));
        assertEquals("equals", blockValue.text("operator"));
        assertEquals("stone", blockValue.text("expression"));
        assertEquals("overworld", map(blockValue, "additionalStrings").get("world"));
        assertEquals("12", map(blockValue, "additionalNumbers").get("x"));
        assertEquals("64", map(blockValue, "additionalNumbers").get("y"));
        assertEquals("-8", map(blockValue, "additionalNumbers").get("z"));

        adapter.variables()
                .itemStackListVariable("TestItems")
                .displayName("Test Items")
                .description("Test item stack list.")
                .singular("test item")
                .plural("test items")
                .get((questPlayer, objects) -> List.of())
                .register();
        final Condition itemStackListValue = Conditions.parse(
                adapter,
                condition(registry, "ItemStackList"),
                "TestItems contains acacia_boat 4");
        assertEquals("TestItems", itemStackListValue.text("variableName"));
        assertEquals("contains", itemStackListValue.text("operator"));
        assertInstanceOf(com.notquests.core.items.ItemSelection.class, itemStackListValue.value("expression"));
        assertEquals(4, itemStackListValue.integer("amount"));
        assertEquals(4, ((com.notquests.core.items.ItemSelection) itemStackListValue.value("expression")).amount());
    }

    @Test
    void itemStackListConditionRequiresTheConfiguredAmount() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        adapter.variables()
                .itemStackListVariable("TestItems")
                .displayName("Test Items")
                .description("Test item stack list.")
                .singular("test item")
                .plural("test items")
                .get((questPlayer, objects) -> List.of(ItemStackSelection.parse("acacia_boat").withAmount(2)))
                .register();

        assertEquals("", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("acacia_boat").withAmount(2));
            data.setValue("amount", 2);
        }, new TestPlayer()));
        assertEquals("<yellow>test items must satisfy contains.", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("acacia_boat").withAmount(3));
            data.setValue("amount", 3);
        }, new TestPlayer()));
    }

    @Test
    void numberAndBooleanConditionsEvaluateExpressionsAndTypedArguments() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        adapter.variables()
                .numberVariable("ArgumentNumber")
                .displayName("Argument Number")
                .description("Returns its number-expression argument.")
                .singular("Argument Number")
                .plural("Argument Numbers")
                .field("offset", adapter.fields().numberExpression(), "Number expression.")
                .get(context -> context.number("offset", 0))
                .register();
        adapter.variables()
                .booleanVariable("ArgumentBoolean")
                .displayName("Argument Boolean")
                .description("Returns its boolean-expression argument.")
                .singular("Argument Boolean")
                .plural("Argument Booleans")
                .field("enabled", adapter.fields().booleanExpression(), "Boolean expression.")
                .get(context -> context.bool("enabled", false))
                .register();
        final TestPlayer player = new TestPlayer();

        assertEquals("", check(registry, "Number", data -> {
            data.setValue("variableName", "ArgumentNumber");
            data.setValue("operator", "equals");
            data.setValue("expression", "Health - 12");
            data.setValue("additionalNumbers", java.util.Map.of("offset", "5 + 3"));
        }, player));

        assertEquals("", check(registry, "Boolean", data -> {
            data.setValue("variableName", "ArgumentBoolean");
            data.setValue("operator", "equals");
            data.setValue("expression", "Health - 19.02");
            data.setValue("additionalBooleans", java.util.Map.of("enabled", "0.98"));
        }, player));

        assertEquals("", check(registry, "Boolean", data -> {
            data.setValue("variableName", "ArgumentBoolean");
            data.setValue("operator", "equals");
            data.setValue("expression", "false");
            data.setValue("additionalBooleans", java.util.Map.of("enabled", "0.97"));
        }, player));
    }

    @Test
    void itemStackListAnyMatchesOrdinaryEntriesAndAggregatesMatchingStacksOnly() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        adapter.variables()
                .itemStackListVariable("TestItems")
                .displayName("Test Items")
                .description("Test item stack list.")
                .singular("test item")
                .plural("test items")
                .get((questPlayer, objects) -> List.of(
                        ItemStackSelection.parse("diamond_sword").withAmount(2),
                        ItemStackSelection.parse("stone").withAmount(9),
                        ItemStackSelection.parse("diamond_sword").withAmount(3)))
                .register();

        assertEquals("", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("diamond_sword").withAmount(5));
        }, new TestPlayer()));
        assertEquals("<yellow>test items must satisfy contains.", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("diamond_sword").withAmount(6));
        }, new TestPlayer()));
        assertEquals("", check(registry, "ItemStackList", data -> {
            data.setValue("variableName", "TestItems");
            data.setValue("operator", "contains");
            data.setValue("expression", ItemStackSelection.parse("any").withAmount(1));
        }, new TestPlayer()));
    }

    @Test
    void completedObjectiveConditionUsesCoreQuestState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter base = plugin.createRegistryAdapter(null);
        BuiltInPack.register(plugin, base);
        final var completedObjective = plugin.getOrCreateQuest("daily")
                .addObjective(2, "BreakBlocks", new Objective(0, "BreakBlocks", null), "");
        completedObjective.setDisplayName("Break dirt");
        final var incompleteObjective = plugin.getOrCreateQuest("daily")
                .addObjective(3, "BreakBlocks", new Objective(0, "BreakBlocks", null), "");
        incompleteObjective.setDisplayName("Break dirt");
        final TestPlayer player = new TestPlayer();
        plugin.activateQuest(player, "daily", ignored -> {});
        plugin.setCompletedObjectiveIds(player, "daily", List.of("2"));

        assertEquals("", check(registry, "CompletedObjective", data ->
                {
                    data.setValue("__questName", "daily");
                    data.setValue("dependingObjectiveId", 2);
                }, player));
        assertEquals("<yellow>Finish the following objective first: <highlight>Break dirt", check(registry, "CompletedObjective", data ->
                {
                    data.setValue("__questName", "daily");
                    data.setValue("dependingObjectiveId", 3);
                }, player));
    }

    private static String check(
            final NotQuestsRegistry registry,
            final String conditionId,
            final java.util.function.Consumer<Condition> setup,
            final PlatformPlayer player) {
        final Conditions.Type condition = condition(registry, conditionId);
        final Condition data = new Condition(0, conditionId, null);
        setup.accept(data);
        return condition.checker().check(data, player);
    }

    private static Conditions.Type condition(
            final NotQuestsRegistry registry,
            final String conditionId) {
        return registry.conditions().stream()
                .filter(candidate -> candidate.id().equals(conditionId))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, String> map(final Condition data, final String name) {
        return assertInstanceOf(java.util.Map.class, data.value(name));
    }

    private static final class TestPlayer implements TestPlatformPlayer {
        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "player";
        }

        @Override
        public double health() {
            return 20;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private static class DelegatingAdapter implements TestNotQuestsAdapter {
        private final NotQuestsAdapter delegate;

        private DelegatingAdapter(final NotQuestsAdapter delegate) {
            this.delegate = delegate;
        }

        @Override
        public FieldFactories fields() {
            return delegate.fields();
        }

        @Override
        public Actions.Registry actions() {
            return delegate.actions();
        }

        @Override
        public Conditions.Registry conditions() {
            return delegate.conditions();
        }

        @Override
        public Objectives.Registry objectives() {
            return delegate.objectives();
        }

        @Override
        public Triggers.Registry triggers() {
            return delegate.triggers();
        }

        @Override
        public Variables.Registry variables() {
            return delegate.variables();
        }

        @Override
        public List<String> variableNames(final com.notquests.core.variables.VariableDataType type) {
            return delegate.variableNames(type);
        }

        @Override
        public com.notquests.core.variables.VariableDataType variableType(final String variableName) {
            return delegate.variableType(variableName);
        }

        @Override
        public String variableSingular(final String variableName) {
            return delegate.variableSingular(variableName);
        }

        @Override
        public String variablePlural(final String variableName) {
            return delegate.variablePlural(variableName);
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            return delegate.variableFields(variableName);
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            return delegate.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public String serverBrand() {
            return delegate.serverBrand();
        }

        @Override
        public List<String> damageTypeIds() {
            return delegate.damageTypeIds();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return delegate.onlinePlayerNames();
        }

        @Override
        public PlatformPlayer onlineQuestPlayer(final String playerName) {
            return delegate.onlineQuestPlayer(playerName);
        }

        @Override
        public List<String> worldNames() {
            return delegate.worldNames();
        }

        @Override
        public List<String> itemSelectionOptions() {
            return delegate.itemSelectionOptions();
        }

        @Override
        public List<String> entityTypeIds() {
            return delegate.entityTypeIds();
        }

        @Override
        public List<String> particleTypeIds() {
            return delegate.particleTypeIds();
        }

        @Override
        public List<String> soundTypeIds() {
            return delegate.soundTypeIds();
        }

        @Override
        public List<String> soundCategoryIds() {
            return delegate.soundCategoryIds();
        }

        @Override
        public List<String> statisticIds() {
            return delegate.statisticIds();
        }

        @Override
        public List<String> advancementIds() {
            return delegate.advancementIds();
        }

        @Override
        public List<String> blockMaterialOptions() {
            return delegate.blockMaterialOptions();
        }

        @Override
        public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
            return delegate.playerStatistic(questPlayer, statisticId);
        }

        @Override
        public boolean setPlayerStatistic(
                final PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            return delegate.setPlayerStatistic(questPlayer, statisticId, value);
        }

        @Override
        public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
            return delegate.hasAdvancement(questPlayer, advancementId);
        }

        @Override
        public boolean setAdvancement(
                final PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            return delegate.setAdvancement(questPlayer, advancementId, completed);
        }

        @Override
        public String blockMaterial(final com.notquests.core.platform.NQLocation location) {
            return delegate.blockMaterial(location);
        }

        @Override
        public boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final com.notquests.core.platform.NQLocation location,
                final String materialOrKeyword) {
            return delegate.setBlockMaterial(questPlayer, location, materialOrKeyword);
        }

        @Override
        public List<com.notquests.core.items.ItemSelection> containerInventoryItems(
                final com.notquests.core.platform.NQLocation location) {
            return delegate.containerInventoryItems(location);
        }

        @Override
        public boolean addContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            return delegate.addContainerInventoryItems(location, items, dropOverflow);
        }

        @Override
        public boolean removeContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return delegate.removeContainerInventoryItems(location, items);
        }

        @Override
        public boolean setContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<SavedItems.ItemChoice> items) {
            return delegate.setContainerInventoryItems(location, items);
        }

        @Override
        public List<String> enchantmentIds() {
            return delegate.enchantmentIds();
        }

        @Override
        public boolean supportsNpcAttachments() {
            return delegate.supportsNpcAttachments();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return delegate.supportsArmorStandAttachmentTools();
        }

        @Override
        public List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
            return delegate.npcSelectorOptions(allowNone, allowRightClickSelect);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return delegate.npcSelection(npcSelector);
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return delegate.setNpcQuestGiver(selection, enabled);
        }

        @Override
        public boolean giveArmorStandTool(
                final PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return delegate.giveArmorStandTool(actor, tool);
        }

        @Override
        public boolean giveNpcSelectionTool(
                final PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            return delegate.giveNpcSelectionTool(actor, selectionId, displayName, lore);
        }

        @Override
        public List<String> inventorySlotIds() {
            return delegate.inventorySlotIds();
        }

        @Override
        public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
            return delegate.inventoryItemEnchantments(questPlayer, slotId);
        }

        @Override
        public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
            return delegate.hasPermission(questPlayer, permission);
        }

        @Override
        public boolean setPermission(final PlatformPlayer questPlayer, final String permission, final boolean value) {
            return delegate.setPermission(questPlayer, permission, value);
        }

        @Override
        public com.notquests.core.platform.NQLocation location(
                final String worldName,
                final double x,
                final double y,
                final double z) {
            return delegate.location(worldName, x, y, z);
        }

        @Override
        public com.notquests.core.items.ItemSelection parseItemSelection(final String input) {
            return delegate.parseItemSelection(input);
        }

        @Override
        public void schedule(final java.time.Duration delay, final Runnable action) {
            delegate.schedule(delay, action);
        }

        @Override
        public boolean isServerThread() {
            return delegate.isServerThread();
        }

        @Override
        public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
            return delegate.callOnServerThread(action);
        }

        @Override
        public void warn(final String message) {
            delegate.warn(message);
        }

        @Override
        public void logInfo(final String message) {
            delegate.logInfo(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            delegate.broadcast(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            delegate.dispatchConsoleCommand(command);
        }

        @Override
        public String resolveActionText(
                final com.notquests.core.registry.NotQuestsRegistry.Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return delegate.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final com.notquests.core.structs.ActiveObjective activeObjective,
                final java.util.Map<String, String> replacements) {
            return delegate.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }
}
