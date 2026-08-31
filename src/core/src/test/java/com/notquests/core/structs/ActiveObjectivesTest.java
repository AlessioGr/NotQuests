package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.managers.QuestPlayerManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ActiveObjectivesTest {
    @Test
    void coreOwnsBlockBeamPlacementAndReachedDecision() {
        final ActiveObjectives.BlockBeam nearbySolid = ActiveObjectives.blockBeam(
                new ActiveObjectives.BeamPoint(0, 64, 0),
                new ActiveObjectives.BeamPoint(10, 64, 10),
                true,
                false,
                ignored -> 0,
                ignored -> false,
                -64);

        assertTrue(nearbySolid.targetReached());
        assertEquals(new ActiveObjectives.BeamBlock(10, 63, 10), nearbySolid.block());

        final ActiveObjectives.BlockBeam atWorldFloor = ActiveObjectives.blockBeam(
                new ActiveObjectives.BeamPoint(0, -64, 0),
                new ActiveObjectives.BeamPoint(10, -64, 10),
                true,
                false,
                ignored -> 0,
                ignored -> false,
                -64);
        assertEquals(new ActiveObjectives.BeamBlock(10, -64, 10), atWorldFloor.block());

        final ActiveObjectives.BlockBeam distant = ActiveObjectives.blockBeam(
                new ActiveObjectives.BeamPoint(0, 64, 0),
                new ActiveObjectives.BeamPoint(200, 20, 0),
                true,
                false,
                ignored -> 0,
                ignored -> true,
                -64);

        assertFalse(distant.targetReached());
        assertEquals(new ActiveObjectives.BeamBlock(85, 192, 0), distant.block());
        assertEquals(null, ActiveObjectives.blockBeam(
                new ActiveObjectives.BeamPoint(0, 64, 0),
                new ActiveObjectives.BeamPoint(10, 64, 10),
                false,
                false,
                ignored -> 0,
                ignored -> true,
                -64));
    }

    @Test
    void coreOwnsClientBeamProjection() {
        final ActiveObjectives.ClientBeam nearby = ActiveObjectives.clientBeam(
                new ActiveObjectives.BeamPoint(0, 64, 0),
                new ActiveObjectives.BeamPoint(10, 64, 10));
        final ActiveObjectives.ClientBeam distant = ActiveObjectives.clientBeam(
                new ActiveObjectives.BeamPoint(0, 64, 0),
                new ActiveObjectives.BeamPoint(500, 64, 0));

        assertFalse(nearby.proxy());
        assertTrue(distant.proxy());
        assertTrue(distant.x() < 100);
    }

    @Test
    void inventoryResultMathUsesPortableClickAndCapacityFacts() {
        final ActiveObjectives.InventoryItem result = ActiveObjectives.InventoryItem.target(4, 64);

        assertEquals(4, ActiveObjectives.takenResultAmount(
                result,
                ActiveObjectives.InventoryItem.empty(64),
                ActiveObjectives.InventoryClick.LEFT,
                false,
                false,
                64));
        assertEquals(2, ActiveObjectives.takenResultAmount(
                result,
                ActiveObjectives.InventoryItem.empty(64),
                ActiveObjectives.InventoryClick.RIGHT,
                false,
                false,
                64));
        assertEquals(3, ActiveObjectives.takenResultAmount(
                result,
                ActiveObjectives.InventoryItem.empty(64),
                ActiveObjectives.InventoryClick.SHIFT_LEFT,
                false,
                false,
                3));
        assertEquals(0, ActiveObjectives.takenResultAmount(
                result,
                new ActiveObjectives.InventoryItem(1, 64, false, false),
                ActiveObjectives.InventoryClick.LEFT,
                false,
                false,
                64));
    }

    @Test
    void craftingMathUsesPortableInventoryFacts() {
        final ActiveObjectives.InventoryItem result = ActiveObjectives.InventoryItem.target(2, 64);
        final List<ActiveObjectives.InventoryItem> inventory = List.of(
                ActiveObjectives.InventoryItem.empty(64),
                new ActiveObjectives.InventoryItem(60, 64, false, true),
                new ActiveObjectives.InventoryItem(32, 64, false, false));

        assertEquals(68, ActiveObjectives.inventorySpaceLeft(inventory, result));
        assertEquals(6, ActiveObjectives.maxCraftAmount(2, List.of(5, 3, 8)));
        assertEquals(6, ActiveObjectives.craftAmount(
                result,
                ActiveObjectives.InventoryItem.empty(64),
                ActiveObjectives.InventoryClick.SHIFT_LEFT,
                false,
                false,
                6,
                68));
    }

    @Test
    void freshlyBrewedItemsAreMatchedAndConsumedInCore() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Brew")
                .displayName("Brew")
                .description("Counts freshly brewed items.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Items required.")
                .onPlayerTakeBrewedItem((event, objective) -> objective.addProgress(event.amount()))
                .register();
        final ActiveObjectives activeObjectives = activeObjectives();
        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective entry = quest.addObjective("Brew", data("amount", 10), "");
        final TestPlayer player = new TestPlayer("player-1");
        final ActiveObjective objective = activeObjectives.activateObjective(
                player, quest.getIdentifier(), entry, "", registry.objectives().getFirst());
        activeObjectives.brewingFinished(
                "world:1:2:3",
                List.of(
                        new ActiveObjectives.BrewedItem("potion-a", 2),
                        new ActiveObjectives.BrewedItem("potion-a", 1),
                        new ActiveObjectives.BrewedItem("potion-b", 4)));

        activeObjectives.onPlayerTakeInventoryItem(
                player,
                ActiveObjectives.TakenItem.BREWED,
                "world:1:2:3",
                "potion-a",
                itemEvent("potion", 2));
        activeObjectives.onPlayerTakeInventoryItem(
                player,
                ActiveObjectives.TakenItem.BREWED,
                "world:1:2:3",
                "potion-a",
                itemEvent("potion", 5));
        activeObjectives.onPlayerTakeInventoryItem(
                player,
                ActiveObjectives.TakenItem.BREWED,
                "world:1:2:3",
                "missing",
                itemEvent("potion", 5));

        assertEquals(3, objective.currentProgress());
    }

    @Test
    void blockAndPlayerTickStateAreOwnedByActiveObjectives() {
        final ActiveObjectives activeObjectives = activeObjectives();
        final TestPlayer player = new TestPlayer("player-1");
        final String blockKey = ActiveObjectives.blockKey("world", 1, 2, 3);

        activeObjectives.onBlockPlaced(player, blockKey, "wheat", true);
        assertTrue(activeObjectives.isPlayerPlacedHarvestBlock(blockKey));
        activeObjectives.onBlockBroken(player, blockKey, "wheat", true, true);
        assertFalse(activeObjectives.isPlayerPlacedHarvestBlock(blockKey));

        activeObjectives.playerTick(player, NQLocation.at("world", 0, 64, 0), false);
        activeObjectives.playerTick(player, NQLocation.at("world", 1, 64, 0), true);
        activeObjectives.playerTick(player, NQLocation.at("world", 1, 64, 0), true);
        activeObjectives.removePlayerObservations(player.playerIdentifier());
        activeObjectives.playerTick(player, NQLocation.at("world", 1, 64, 0), true);

        assertFalse(activeObjectives.objectiveMarkersDue());
        assertFalse(activeObjectives.objectiveMarkersDue());
        assertFalse(activeObjectives.objectiveMarkersDue());
        assertTrue(activeObjectives.objectiveMarkersDue());
    }

    @Test
    void cancelledUnlockGateKeepsObjectiveLockedAndSkipsUnlockHandler() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final AtomicInteger objectiveUnlocks = new AtomicInteger();
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .onUnlock((objective, questPlayer, loading) -> objectiveUnlocks.incrementAndGet())
                .register();
        final ActiveObjectives progressEngine = activeObjectives();
        final AtomicInteger platformEvents = new AtomicInteger();
        progressEngine.unlockHandler(objective -> {
            platformEvents.incrementAndGet();
            return false;
        });
        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective entry = quest.addObjective("Jump", data("amount", 1), "");

        final ActiveObjective objective = progressEngine.activateObjective(
                new TestPlayer("player-1"),
                quest.getIdentifier(),
                entry,
                "",
                registry.objectives().getFirst());

        assertFalse(objective.isUnlocked());
        assertEquals(1, platformEvents.get());
        assertEquals(0, objectiveUnlocks.get());
    }

    @Test
    void dispatchToleratesObjectiveRemovalDuringCompletionCallback() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .onPlayerJump(objective -> objective.addProgress(1))
                .register();
        final ActiveObjectives progressEngine = activeObjectives();
        progressEngine.completionHandler(progressEngine::removeActiveObjective);

        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective entry = quest.addObjective("Jump", data("amount", 1), "");
        progressEngine.activateObjective(
                new TestPlayer("player-1"),
                quest.getIdentifier(),
                entry,
                "",
                registry.objectives().getFirst());

        assertDoesNotThrow(() -> progressEngine.onPlayerJump(new TestPlayer("player-1")));
        assertEquals(0, progressEngine.activeObjectives("player-1").size());
    }

    @Test
    void nestedObjectiveTreesAreActivatedBeforeUnlockHandlersRun() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("ObjectiveGroup")
                .displayName("Objective Group")
                .description("Contains child objectives.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .onUnlock((objective, questPlayer, loading) -> {
                    if (objective.childObjectiveCount() == 0
                            && objective.currentProgress() < objective.progressNeeded()) {
                        objective.setProgress(objective.progressNeeded(), false);
                    }
                })
                .register();
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .onPlayerJump(objective -> objective.addProgress(1))
                .register();

        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective group = quest.addObjective("ObjectiveGroup", data("amount", 1), "");
        final com.notquests.core.objectives.Objective child = group.addChildObjective("Jump", data("amount", 1), "");
        final ActiveObjectives progressEngine = activeObjectives();
        final List<String> completedPaths = new ArrayList<>();
        progressEngine.completionHandler(progress -> {
            completedPaths.add(progress.getObjectivePathKey());
            progressEngine.removeActiveObjective(progress);
        });
        final TestPlayer player = new TestPlayer("player-1");
        final ActiveObjective groupProgress = progressEngine.activateObjective(
                player,
                quest.getIdentifier(),
                group,
                new int[] {group.id()},
                "",
                registry.objectives().stream()
                        .filter(objective -> objective.id().equals("ObjectiveGroup"))
                        .findFirst()
                        .orElseThrow(),
                false);
        progressEngine.activateObjective(
                player,
                quest.getIdentifier(),
                child,
                new int[] {group.id(), child.id()},
                group.getChildObjectiveProgressOrder(),
                registry.objectives().stream()
                        .filter(objective -> objective.id().equals("Jump"))
                        .findFirst()
                        .orElseThrow(),
                false);

        progressEngine.refreshObjectiveUnlocks(player);

        assertEquals(1, groupProgress.childObjectiveCount());
        assertFalse(groupProgress.isComplete());
        assertEquals(2, progressEngine.activeObjectives("player-1").size());

        progressEngine.onPlayerJump(player);

        assertEquals(List.of("1.1", "1"), completedPaths);
        assertEquals(0, progressEngine.activeObjectives("player-1").size());
    }

    @Test
    void databaseRestoreMarksUnlockCallbacksAsLoadingAndDoesNotRunCompletion() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final List<Boolean> loadingModes = new ArrayList<>();
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .onUnlock((objective, questPlayer, loading) -> {
                    loadingModes.add(loading);
                    if (!loading) {
                        objective.addProgress(1);
                    }
                })
                .register();
        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective entry = quest.addObjective("Jump", data("amount", 1), "");
        final ActiveObjectives activeObjectives = activeObjectives();
        final AtomicInteger completions = new AtomicInteger();
        activeObjectives.completionHandler(ignored -> completions.incrementAndGet());
        final TestPlayer player = new TestPlayer("player-1");

        final ActiveObjective objective = activeObjectives.activateObjective(
                player,
                quest.getIdentifier(),
                entry,
                new int[] {entry.id()},
                "",
                registry.objectives().getFirst(),
                false);
        activeObjectives.refreshObjectiveUnlocks(player, "default", true);

        assertEquals(List.of(true), loadingModes);
        assertEquals(0, objective.currentProgress());
        assertEquals(0, completions.get());
    }

    @Test
    void databaseRestoreDoesNotCompleteParentWhenRemovingSavedCompletedChild() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Group")
                .displayName("Group")
                .description("Contains child objectives.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .register();
        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective group = quest.addObjective("Group", data("amount", 0), "");
        final com.notquests.core.objectives.Objective child = group.addChildObjective("Jump", data("amount", 1), "");
        final ActiveObjectives activeObjectives = activeObjectives();
        final AtomicInteger completions = new AtomicInteger();
        activeObjectives.completionHandler(ignored -> completions.incrementAndGet());
        final TestPlayer player = new TestPlayer("player-1");
        activeObjectives.activateObjective(
                player,
                quest.getIdentifier(),
                group,
                new int[] {group.id()},
                "",
                registry.objectives().stream().filter(type -> type.id().equals("Group")).findFirst().orElseThrow(),
                false);
        activeObjectives.activateObjective(
                player,
                quest.getIdentifier(),
                child,
                new int[] {group.id(), child.id()},
                "",
                registry.objectives().stream().filter(type -> type.id().equals("Jump")).findFirst().orElseThrow(),
                false);
        activeObjectives.refreshObjectiveUnlocks(player, "default", true);

        assertTrue(activeObjectives.restoreObjectiveProgress(
                "player-1", "default", "Quest", new int[] {group.id(), child.id()}, 1, true, true));

        final ActiveObjective restoredParent = activeObjectives.activeObjective(
                "player-1", "default", "Quest", new int[] {group.id()});
        assertFalse(restoredParent.hasBeenCompleted());
        assertEquals(0, completions.get());
    }

    @Test
    void completionNpcObjectivesStayActiveUntilMatchingNpcCompletesThem() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("TalkToNPC")
                .displayName("Talk To NPC")
                .description("Counts talking to a specific NPC.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Talks required.")
                .onPlayerInteractNpc((event, objective) -> objective.addProgress(1))
                .register();

        final Quest quest = new Quest("Quest");
        final com.notquests.core.objectives.Objective entry = quest.addObjective("TalkToNPC", data("amount", 1), "");
        entry.setCompletionNpc("citizens-1");
        final ActiveObjectives progressEngine = activeObjectives();
        final List<String> completedPaths = new ArrayList<>();
        progressEngine.completionHandler(progress -> {
            completedPaths.add(progress.getObjectivePathKey());
            progressEngine.removeActiveObjective(progress);
        });
        final TestPlayer player = new TestPlayer("player-1");
        final ActiveObjective progress = progressEngine.activateObjective(
                player,
                quest.getIdentifier(),
                entry,
                "",
                registry.objectives().getFirst());

        assertTrue(progressEngine.onPlayerInteractNpc(player, npcEvent(player, "citizens-2")));
        assertEquals(1, progress.currentProgress());
        assertFalse(progress.hasBeenCompleted());
        assertEquals(List.of(), completedPaths);
        assertEquals(1, progressEngine.activeObjectives("player-1").size());

        assertTrue(progressEngine.onPlayerInteractNpc(player, npcEvent(player, "citizens-1")));
        assertTrue(progress.hasBeenCompleted());
        assertEquals(List.of("1"), completedPaths);
        assertEquals(0, progressEngine.activeObjectives("player-1").size());
    }

    @Test
    void forcedQuestCompletionMarksAndRemovesRemainingObjectivesWithoutObjectiveEffects() {
        final NotQuestsRegistry registry = new NotQuestsRegistry();
        final NotQuestsAdapter adapter = registry.createAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .register();
        final ActiveObjectives activeObjectives = activeObjectives();
        final AtomicInteger objectiveCompletions = new AtomicInteger();
        activeObjectives.completionHandler(ignored -> objectiveCompletions.incrementAndGet());
        final TestPlayer player = new TestPlayer("player-1");
        final Quest forcedQuest = new Quest("ForcedQuest");
        final Quest otherQuest = new Quest("OtherQuest");
        final ActiveObjective first = activeObjectives.activateObjective(
                player,
                forcedQuest.getIdentifier(),
                forcedQuest.addObjective("Jump", data("amount", 3), ""),
                "",
                registry.objectives().getFirst());
        final ActiveObjective second = activeObjectives.activateObjective(
                player,
                forcedQuest.getIdentifier(),
                forcedQuest.addObjective("Jump", data("amount", 7), ""),
                "",
                registry.objectives().getFirst());
        final ActiveObjective unrelated = activeObjectives.activateObjective(
                player,
                otherQuest.getIdentifier(),
                otherQuest.addObjective("Jump", data("amount", 2), ""),
                "",
                registry.objectives().getFirst());

        assertEquals(
                List.of(first, second),
                activeObjectives.forceCompleteActiveObjectives(player.playerIdentifier(), forcedQuest.getIdentifier()));

        assertTrue(first.hasBeenCompleted());
        assertEquals(3, first.currentProgress());
        assertTrue(second.hasBeenCompleted());
        assertEquals(7, second.currentProgress());
        assertFalse(unrelated.hasBeenCompleted());
        assertEquals(List.of(unrelated), activeObjectives.activeObjectives(player.playerIdentifier()));
        assertEquals(0, objectiveCompletions.get());
    }

    private static com.notquests.core.TestData data(final String key, final Object value) {
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put(key, value);
        return new com.notquests.core.TestData(values);
    }

    private static ActiveObjectives activeObjectives() {
        return new QuestPlayerManager().getActiveObjectives();
    }

    private static Objectives.NpcInteractionEvent npcEvent(
            final PlatformPlayer questPlayer,
            final String npcSelector) {
        return new Objectives.NpcInteractionEvent() {
            @Override
            public PlatformPlayer questPlayer() {
                return questPlayer;
            }

            @Override
            public String npcSelector() {
                return npcSelector;
            }

            @Override
            public String npcName() {
                return npcSelector;
            }
        };
    }

    private static Objectives.ItemEvent itemEvent(final String materialId, final int amount) {
        return new Objectives.ItemEvent() {
            @Override
            public String materialId() {
                return materialId;
            }

            @Override
            public int amount() {
                return amount;
            }

            @Override
            public boolean matches(final com.notquests.core.items.ItemSelection selection) {
                return false;
            }
        };
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
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }
}
