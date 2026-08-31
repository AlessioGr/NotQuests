package com.notquests.builtin.variables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.TestPlatformPlayer;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.BooleanVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.NumberVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class QuestVariableRegressionTest {
    @Test
    void activeQuestsUsesFailureAndForcedSilentAcceptanceLifecycle() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        ActiveQuestsVariable.register(plugin, adapter);
        plugin.getOrCreateQuest("OldQuest");
        plugin.getOrCreateQuest("NewQuest").setMaxAccepts(0);
        final RecordingPlayer player = new RecordingPlayer();
        assertTrue(plugin.activateQuest(player, "OldQuest", ignored -> {}));

        final Variables.ListVariableHandler activeQuests = list(registry, "ActiveQuests");
        assertFalse(activeQuests.setValue(List.of("MissingQuest"), player));
        assertEquals(List.of("OldQuest"), plugin.activeQuestNames(player));
        assertEquals(0, player.failedBefore.size());

        assertTrue(activeQuests.setValue(List.of("NewQuest"), player));

        assertEquals(List.of("NewQuest"), plugin.activeQuestNames(player));
        assertEquals(List.of("OldQuest"), player.failedBefore);
        assertEquals(List.of("NewQuest"), player.accepted);
        assertEquals(
                List.of("OldQuest"),
                plugin.failedQuests(player.playerIdentifier(), "default").stream()
                        .map(com.notquests.core.structs.QuestPlayer.FailedQuest::questIdentifier)
                        .toList());
    }

    @Test
    void completedObjectiveIdsUsesConditionsRewardsChildrenTriggersAndCleanupExactlyOnce() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        CompletedObjectiveIDsOfQuestVariable.register(plugin, adapter);
        final AtomicBoolean completionAllowed = new AtomicBoolean(false);
        final AtomicInteger rewards = new AtomicInteger();
        adapter.conditions()
                .condition("CompletionGate")
                .displayName("Completion Gate")
                .description("Test completion condition.")
                .check((condition, player) -> completionAllowed.get() ? "" : "blocked")
                .register();
        adapter.actions()
                .action("Reward")
                .displayName("Reward")
                .description("Records a reward.")
                .execute((action, player, objects) -> rewards.incrementAndGet())
                .register();
        adapter.objectives()
                .objective("Progress")
                .displayName("Progress")
                .description("Test objective progress.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();

        final Quest quest = plugin.getOrCreateQuest("NestedQuest");
        final com.notquests.core.objectives.Objective parent = quest.addObjective(
                1,
                "Progress",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Parent");
        final com.notquests.core.objectives.Objective child = parent.addChildObjective(
                2,
                "Progress",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Child");
        quest.addObjective(
                3,
                "Progress",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Remaining objective");
        parent.addCondition("complete", "CompletionGate", new com.notquests.builtin.TestData(Map.of()));
        parent.addReward("Reward", new com.notquests.builtin.TestData(Map.of()));
        child.addReward("Reward", new com.notquests.builtin.TestData(Map.of()));
        quest.addTrigger(
                "COMPLETE",
                new com.notquests.builtin.TestData(Map.of("applyOn", 2, "amountNeeded", 1)));

        final RecordingPlayer player = new RecordingPlayer();
        assertTrue(plugin.giveQuest(player, quest.getIdentifier(), false, ignored -> {}));
        final Variables.ListVariableHandler completedIds = list(registry, "CompletedObjectiveIDsOfQuest");

        assertFalse(completedIds.setValue(List.of("1", "1.2"), player, quest.getIdentifier()));
        assertEquals(1, rewards.get(), "the child completes, while its blocked parent stays active");
        assertEquals(List.of("1.2"), plugin.completedObjectiveIds(player, quest.getIdentifier()));
        assertNotNull(plugin.activeObjectiveProgress(player, quest.getIdentifier(), new int[] {1}));
        assertEquals(1, plugin.activeTriggers(player.playerIdentifier()).getFirst().currentProgress());

        completionAllowed.set(true);
        assertTrue(completedIds.setValue(List.of("1"), player, quest.getIdentifier()));
        assertEquals(2, rewards.get());
        assertEquals(2, player.completedObjectives.size());
        assertEquals(Set.of("1", "1.2"), Set.copyOf(plugin.completedObjectiveIds(player, quest.getIdentifier())));
        assertEquals(null, plugin.activeObjectiveProgress(player, quest.getIdentifier(), new int[] {1}));
        assertNotNull(plugin.activeObjectiveProgress(player, quest.getIdentifier(), new int[] {3}));

        assertTrue(completedIds.setValue(List.of("1", "1.2"), player, quest.getIdentifier()));
        assertEquals(2, rewards.get(), "completed IDs must not replay rewards or completion hooks");
        assertEquals(2, player.completedObjectives.size());
        assertFalse(completedIds.setValue(List.of("99"), player, quest.getIdentifier()));

        plugin.getOrCreateQuest("InactiveQuest").addObjective(
                1,
                "Progress",
                new com.notquests.builtin.TestData(Map.of("amount", 1)),
                "Inactive");
        assertFalse(completedIds.setValue(List.of("1"), player, "InactiveQuest"));
    }

    @Test
    void questStatusVariablesRemainIndependentWhenSeveralLimitsOverlap() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        QuestAbleToAcceptVariable.register(plugin, adapter);
        QuestOnCooldownVariable.register(plugin, adapter);
        QuestReachedMaxAcceptsVariable.register(plugin, adapter);
        QuestReachedMaxCompletionsVariable.register(plugin, adapter);
        QuestReachedMaxFailsVariable.register(plugin, adapter);
        final Quest quest = plugin.getOrCreateQuest("OverlapQuest");
        quest.setMaxAccepts(1);
        quest.setMaxCompletions(1);
        quest.setMaxFails(1);
        quest.setAcceptCooldownComplete(60);
        final RecordingPlayer player = new RecordingPlayer();
        plugin.recordCompletedQuest(
                player.playerIdentifier(),
                "default",
                quest.getIdentifier(),
                System.currentTimeMillis());
        plugin.recordFailedQuest(player.playerIdentifier(), "default", quest.getIdentifier(), 1L);
        assertTrue(plugin.activateQuest(player, quest.getIdentifier(), ignored -> {}));

        assertFalse(bool(registry, "QuestAbleToAccept").getValue(player, quest.getIdentifier()));
        assertTrue(bool(registry, "QuestOnCooldown").getValue(player, quest.getIdentifier()));
        assertTrue(bool(registry, "QuestReachedMaxAccepts").getValue(player, quest.getIdentifier()));
        assertTrue(bool(registry, "QuestReachedMaxCompletions").getValue(player, quest.getIdentifier()));
        assertTrue(bool(registry, "QuestReachedMaxFails").getValue(player, quest.getIdentifier()));
        assertFalse(plugin.questCooldownLeftFormatted(
                        player,
                        quest.getIdentifier(),
                        System.currentTimeMillis())
                .isBlank());
    }

    @Test
    void questPointsNotifyPlayerFieldUsesTheNotificationAwareCoreMethod() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        QuestPointsVariable.register(plugin, adapter);
        final RecordingPlayer player = new RecordingPlayer();
        final Variables.Type variable = variable(registry, "QuestPoints");
        assertEquals(List.of("notifyPlayer"), variable.fields().stream().map(RegistryField.Definition::name).toList());

        final Variables.NumberVariableHandler questPoints = number(registry, "QuestPoints");
        assertTrue(questPoints.setValue(12, player));
        assertEquals(List.of(), player.messages);
        assertTrue(questPoints.setValue(25, player, true));
        assertEquals(25, plugin.questPoints(player));
        assertTrue(player.messages.getLast().contains("25"));
    }

    private static Variables.BooleanVariableHandler bool(final NotQuestsRegistry registry, final String id) {
        return (Variables.BooleanVariableHandler) variable(registry, id).handler();
    }

    private static Variables.ListVariableHandler list(final NotQuestsRegistry registry, final String id) {
        return (Variables.ListVariableHandler) variable(registry, id).handler();
    }

    private static Variables.NumberVariableHandler number(final NotQuestsRegistry registry, final String id) {
        return (Variables.NumberVariableHandler) variable(registry, id).handler();
    }

    private static Variables.Type variable(final NotQuestsRegistry registry, final String id) {
        return registry.variables().stream()
                .filter(variable -> variable.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static final class RecordingPlayer implements TestPlatformPlayer {
        private final List<String> messages = new ArrayList<>();
        private final List<String> accepted = new ArrayList<>();
        private final List<String> failedBefore = new ArrayList<>();
        private final List<String> completedObjectives = new ArrayList<>();

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "player";
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {
            messages.add(miniMessage);
        }

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
        public boolean beforeQuestAccepted(
                final com.notquests.core.structs.Quest quest,
                final boolean triggerAcceptQuestTrigger) {
            accepted.add(quest.getIdentifier());
            return true;
        }

        @Override
        public boolean beforeQuestFailed(final com.notquests.core.structs.Quest quest) {
            failedBefore.add(quest.getIdentifier());
            return true;
        }

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean beforeObjectiveCompleted(
                final com.notquests.core.structs.Quest quest,
                final com.notquests.core.structs.ActiveObjective objective) {
            completedObjectives.add(quest.getIdentifier() + ":"
                    + java.util.Arrays.stream(objective.getObjectivePath())
                    .mapToObj(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(".")));
            return true;
        }
    }
}
