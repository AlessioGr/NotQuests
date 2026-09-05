package com.notquests.builtin.objectives;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.builtin.TestNotQuestsAdapter;
import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.managers.QuestPlayerManager;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
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
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.ActiveObjectives;
import com.notquests.core.structs.Quest;
import com.notquests.core.variables.VariableDataType;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class PortableObjectiveProgressTest {
    private static final String PLAYER = "player";
    private static final AtomicInteger QUEST_NUMBER = new AtomicInteger();

    @Test
    void eatingTenApplesRequiresTenConsumptionsRegardlessOfStackSize() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(plugin, adapter);
        final PlatformPlayer player = new TestPlayer(PLAYER);
        final Objectives.Type type = type(plugin.registry(), "ConsumeItems");
        assertTrue(plugin.createQuest("EatApples").success());
        plugin.quest("EatApples").addObjective("ConsumeItems", Objectives.parse(adapter, type, "apple 10"), "");
        assertTrue(plugin.giveQuest(player, "EatApples", false, ignored -> {}));
        final ActiveObjective objective = plugin.activeObjectives(PLAYER).getFirst();

        plugin.playerConsumedItem(player, new ItemEvent("bread", 16));
        assertEquals(0, objective.getCurrentProgress());
        for (int consumed = 1; consumed <= 10; consumed++) {
            plugin.playerConsumedItem(player, new ItemEvent("apple", 17 - consumed));
            assertEquals(consumed, objective.getCurrentProgress());
            assertEquals(consumed == 10, plugin.activeQuestPlayer(PLAYER).hasCompletedQuest("EatApples"));
        }
    }

    @Test
    void requestedNeoForgeObjectivesProgressThroughPortableHandlers() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final ActiveObjectives engine = activeObjectives();
        final PlatformPlayer player = new TestPlayer(PLAYER);

        final ActiveObjective harvest = activate(registry, adapter, engine, "Harvest", "wheat 2");
        engine.onPlayerHarvestBlock(player, new HarvestEvent("wheat", false, false));
        engine.onPlayerHarvestBlock(player, new HarvestEvent("wheat", true, true));
        engine.onPlayerHarvestBlock(player, new HarvestEvent("wheat", true, false));
        assertEquals(1, harvest.getCurrentProgress());

        final ActiveObjective pickup = activate(registry, adapter, engine, "PickupItems", "acacia_boat 3");
        engine.onPlayerPickupItem(player, new ItemEvent("acacia_boat", 2));
        assertEquals(2, pickup.getCurrentProgress());

        final ActiveObjective killMobs = activate(registry, adapter, engine, "KillMobs", "zombie 1");
        engine.onPlayerKillEntity(player, new EntityEvent("skeleton", "", false));
        engine.onPlayerKillEntity(player, new EntityEvent("zombie", "", true));
        assertEquals(0, killMobs.getCurrentProgress());
        engine.onPlayerKillEntity(player, new EntityEvent("zombie", "", false));
        assertEquals(1, killMobs.getCurrentProgress());

        final NQLocation target = NQLocation.at("overworld", 10, 64, 10);
        final ActiveObjective interact = activate(
                registry,
                adapter,
                engine,
                "Interact",
                "1 overworld 10 64 10 --rightClick --maxDistance 1");
        engine.onPlayerInteractBlock(player, new InteractionEvent(target, false, true));
        assertEquals(1, interact.getCurrentProgress());

        final ActiveObjective reach = activate(
                registry,
                adapter,
                engine,
                "ReachLocation",
                "overworld 5 59 5 overworld 15 69 15 Test Target");
        engine.onPlayerMove(player, () -> target);
        assertEquals(1, reach.getCurrentProgress());

        final ActiveObjective shootArrow = activate(
                registry,
                adapter,
                engine,
                "ShootArrow",
                "1 overworld 10 64 10 none none 2");
        engine.onPlayerShootProjectileHit(player, () -> NQLocation.at("overworld", 11, 64, 10));
        assertEquals(1, shootArrow.getCurrentProgress());

        final ActiveObjective brew = activate(registry, adapter, engine, "BrewItems", "potion 1");
        engine.onPlayerTakeBrewedItem(player, new ItemEvent("potion", 1));
        assertEquals(1, brew.getCurrentProgress());

        final ActiveObjective smith =
                activate(registry, adapter, engine, "SmithItems", "netherite_sword 1");
        engine.onPlayerTakeSmithingResult(player, new ItemEvent("netherite_sword", 1));
        assertEquals(1, smith.getCurrentProgress());

        final ActiveObjective triggerCommand =
                activate(registry, adapter, engine, "TriggerCommand", "dailyReward 1");
        assertEquals(java.util.List.of("dailyReward"), engine.activeTriggerCommandNames());
        engine.onPlayerRunCommand(player, new CommandEvent("dailyReward"));
        assertEquals(1, triggerCommand.getCurrentProgress());

        final ActiveObjective runCommand =
                activate(registry, adapter, engine, "RunCommand", "1 spawn --ignoreCase");
        assertEquals("/spawn", runCommand.text("command"));
        engine.onPlayerRunCommand(player, new CommandEvent("/SPAWN"));
        assertEquals(1, runCommand.getCurrentProgress());
    }

    @Test
    void conditionAndNumberVariableObjectivesProgressThroughPortableRefresh() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(plugin, adapter);
        adapter.conditions()
                .condition("AlwaysTrue")
                .displayName("Always True")
                .description("Always succeeds.")
                .conditionDescription((condition, questPlayer, objects) -> "Condition description for can_enter")
                .check((condition, questPlayer) -> "")
                .register();
        plugin.saveCondition("can_enter", conditionType(registry, "AlwaysTrue"), new Condition(0, "AlwaysTrue", null));
        adapter.variables()
                .numberVariable("TestNumber")
                .displayName("Test Number")
                .description("Test number.")
                .singular("test number")
                .plural("test numbers")
                .get((questPlayer, objects) -> 12)
                .register();
        final ActiveObjectives engine = activeObjectives();
        final PlatformPlayer player = new TestPlayer(PLAYER);

        final ActiveObjective condition = activate(registry, adapter, engine, "Condition", "can_enter");
        engine.refreshObjectives(player, Objectives.ObjectiveRefresh.periodic());
        assertEquals(1, condition.getCurrentProgress());
        assertEquals(
                "Condition description for can_enter",
                type(registry, "Condition")
                        .taskDescriptionRenderer()
                        .render(condition, player, condition));

        final ActiveObjective number =
                activate(registry, adapter, engine, "NumberVariable", "TestNumber moreOrEqualThan 10");
        engine.refreshObjectives(player, Objectives.ObjectiveRefresh.periodic());
        assertEquals(12, number.getCurrentProgress());

        final ActiveObjective moreThan =
                activate(registry, adapter, engine, "NumberVariable", "TestNumber moreThan 12");
        assertEquals(13, moreThan.progressNeeded());
        engine.refreshObjectives(player, Objectives.ObjectiveRefresh.periodic());
        assertEquals(12, moreThan.getCurrentProgress());
        assertEquals(false, moreThan.isComplete());
    }

    @Test
    void completionHandlersCanRemoveActiveObjectivesDuringDispatch() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final ActiveObjectives engine = activeObjectives();
        engine.completionHandler(engine::removeActiveObjective);
        final PlatformPlayer player = new TestPlayer(PLAYER);

        final ActiveObjective first = activate(registry, adapter, engine, "Jump", "1");
        final ActiveObjective second = activate(registry, adapter, engine, "Jump", "1");

        assertDoesNotThrow(() -> engine.onPlayerJump(player));
        assertTrue(first.isComplete());
        assertTrue(second.isComplete());
        assertEquals(List.of(), engine.activeObjectives(PLAYER));
    }

    @Test
    void livestockObjectivesProgressAndHonorTheirCancellationFlags() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final ActiveObjectives engine = activeObjectives();
        final PlatformPlayer player = new TestPlayer(PLAYER);

        final ActiveObjective milkCow = activate(registry, adapter, engine, "MilkCow", "1 --cancelMilking");
        final MilkCowEvent milkEvent = new MilkCowEvent();
        engine.onPlayerMilkCow(player, milkEvent);
        assertEquals(1, milkCow.getCurrentProgress());
        assertTrue(milkEvent.cancelled);

        final ActiveObjective shearSheep = activate(registry, adapter, engine, "ShearSheep", "1 --cancelShearing");
        final ShearSheepEvent shearEvent = new ShearSheepEvent();
        engine.onPlayerShearSheep(player, shearEvent);
        assertEquals(1, shearSheep.getCurrentProgress());
        assertTrue(shearEvent.cancelled);

        final ActiveObjectives uncancelledEngine = activeObjectives();
        activate(registry, adapter, uncancelledEngine, "MilkCow", "1");
        activate(registry, adapter, uncancelledEngine, "ShearSheep", "1");
        final MilkCowEvent uncancelledMilkEvent = new MilkCowEvent();
        final ShearSheepEvent uncancelledShearEvent = new ShearSheepEvent();
        uncancelledEngine.onPlayerMilkCow(player, uncancelledMilkEvent);
        uncancelledEngine.onPlayerShearSheep(player, uncancelledShearEvent);
        assertEquals(false, uncancelledMilkEvent.cancelled);
        assertEquals(false, uncancelledShearEvent.cancelled);
    }

    private static ActiveObjective activate(
            final NotQuestsRegistry registry,
            final NotQuestsAdapter adapter,
            final ActiveObjectives engine,
            final String type,
            final String arguments) {
        final Objectives.Type objectiveType = registry.objectives().stream()
                .filter(objective -> objective.id().equals(type))
                .findFirst()
                .orElseThrow();
        final Objective data = Objectives.parse(adapter, objectiveType, arguments);
        final Quest quest = new Quest("test-" + QUEST_NUMBER.incrementAndGet());
        final com.notquests.core.objectives.Objective entry = quest.addObjective(objectiveType.id(), data, "");
        return engine.activateObjective(
                new TestPlayer(PLAYER),
                quest.getIdentifier(),
                entry,
                "",
                objectiveType);
    }

    private static ActiveObjectives activeObjectives() {
        return new QuestPlayerManager().getActiveObjectives();
    }

    private static Objectives.Type type(
            final NotQuestsRegistry registry,
            final String type) {
        return registry.objectives().stream()
                .filter(objective -> objective.id().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private static Conditions.Type conditionType(
            final NotQuestsRegistry registry,
            final String type) {
        return registry.conditions().stream()
                .filter(condition -> condition.id().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private record TestPlayer(String playerIdentifier) implements TestPlatformPlayer {
        @Override
        public boolean hasPlayer() {
            return true;
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
        public NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private record HarvestEvent(String materialId, boolean fullyGrownHarvestable, boolean playerPlaced)
            implements Objectives.HarvestBlockEvent {
        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record ItemEvent(String materialId, int amount) implements Objectives.ItemEvent {
        @Override
        public boolean matches(final ItemSelection selection) {
            return selection != null && selection.includesMaterial(materialId);
        }
    }

    private record EntityEvent(
            String entityTypeId,
            String plainCustomName,
            boolean selfAttributedDeath) implements Objectives.EntityEvent {}

    private record InteractionEvent(NQLocation location, boolean leftClick, boolean rightClick)
            implements Objectives.InteractionEvent {
        @Override
        public void cancel() {}
    }

    private record CommandEvent(String command) implements Objectives.CommandEvent {
        @Override
        public void cancel() {}
    }

    private static final class MilkCowEvent implements Objectives.MilkCowEvent {
        private boolean cancelled;

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private static final class ShearSheepEvent implements Objectives.ShearSheepEvent {
        private boolean cancelled;

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    private abstract static class DelegatingAdapter implements TestNotQuestsAdapter {
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
        public java.util.List<String> variableNames(final VariableDataType type) {
            return delegate.variableNames(type);
        }

        @Override
        public VariableDataType variableType(final String variableName) {
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
        public java.util.List<RegistryField.Definition> variableFields(final String variableName) {
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
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final java.util.Map<String, String> stringArguments,
                final java.util.Map<String, ?> numberArguments,
                final java.util.Map<String, ?> booleanArguments) {
            return delegate.variableValue(variableName, questPlayer, stringArguments, numberArguments, booleanArguments);
        }

        @Override
        public void warn(final String message) {
            delegate.warn(message);
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
        public void schedule(final Duration delay, final Runnable action) {
            delegate.schedule(delay, action);
        }

        @Override
        public String resolveActionText(
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return delegate.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final ActiveObjective activeObjective,
                final java.util.Map<String, String> replacements) {
            return delegate.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }
}
