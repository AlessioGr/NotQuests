package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer.FailedQuest;

import java.util.List;
import java.util.Map;

class PlayerDatabaseSnapshotTest {
    @Test
    void buildsSqlSnapshotFromNeutralRuntimeViews() {
        final TestObjectiveData activeChild = new TestObjectiveData("BreakBlocks", "QuestA/1", 2, 3.0, false, 5.0);
        final TestObjectiveData activeRoot = new TestObjectiveData("ObjectiveGroup", "QuestA", 1, 1.0, false, 1.0)
                .withActive(activeChild);
        final TestObjectiveData completedChild = new TestObjectiveData("Jump", "QuestA/3", 4, 2.0, true, 2.0);
        final TestObjectiveData completedRoot = new TestObjectiveData("ObjectiveGroup", "QuestA", 3, 1.0, true, 1.0)
                .withCompleted(completedChild);
        final TestPlayerData player = new TestPlayerData(List.of(new TestActiveQuestData(
                "QuestA",
                List.of(new TestTriggerData("BEGIN", 4, 7)),
                List.of(activeRoot),
                List.of(completedRoot))));

        final PlayerDatabase.PlayerSnapshot snapshot =
                PlayerDatabase.snapshot(player);

        assertEquals("player-1", snapshot.playerIdentifier());
        assertEquals("default", snapshot.profile());
        assertEquals("builder", snapshot.activeProfile());
        assertEquals(10, snapshot.questPoints());
        assertEquals(List.of("QuestA"), snapshot.activeQuestNames());
        assertEquals(1, snapshot.activeTriggers().size());
        assertEquals("BEGIN", snapshot.activeTriggers().getFirst().triggerType());
        assertEquals(4, snapshot.activeTriggers().getFirst().currentProgress());
        assertEquals(7, snapshot.activeTriggers().getFirst().triggerId());
        assertEquals(4, snapshot.activeObjectives().size());
        assertEquals(List.of("ObjectiveGroup", "BreakBlocks", "ObjectiveGroup", "Jump"), snapshot.activeObjectives().stream()
                .map(PlayerDatabase.ActiveObjectiveRow::objectiveType)
                .toList());
        assertEquals("Done", snapshot.completedQuests().getFirst().questName());
        assertEquals("Failed", snapshot.failedQuests().getFirst().questName());
        assertEquals(Map.of("reputation", 12), snapshot.tags());
    }

    private record TestPlayerData(List<TestActiveQuestData> quests)
            implements PlayerDatabase.StoredPlayer {
        @Override
        public String playerIdentifier() {
            return "player-1";
        }

        @Override
        public String profile() {
            return "default";
        }

        @Override
        public String activeProfile() {
            return "builder";
        }

        @Override
        public long questPoints() {
            return 10;
        }

        @Override
        public List<TestActiveQuestData> activeQuests() {
            return quests;
        }

        @Override
        public List<CompletedQuest> completedQuests() {
            return List.of(new CompletedQuest("Done", "player-1", 100));
        }

        @Override
        public List<FailedQuest> failedQuests() {
            return List.of(new FailedQuest("Failed", "player-1", 200));
        }

        @Override
        public Map<String, Object> tags() {
            return Map.of("reputation", 12);
        }
    }

    private record TestActiveQuestData(
            String questIdentifier,
            List<TestTriggerData> activeTriggers,
            List<TestObjectiveData> activeObjectives,
            List<TestObjectiveData> completedObjectives)
            implements PlayerDatabase.StoredActiveQuest {}

    private record TestTriggerData(String triggerType, long currentProgress, int triggerId)
            implements PlayerDatabase.StoredActiveTrigger {}

    private record TestObjectiveData(
            String objectiveType,
            String holderPath,
            int objectiveId,
            double currentProgress,
            boolean completed,
            double progressNeeded,
            List<TestObjectiveData> activeObjectives,
            List<TestObjectiveData> completedObjectives)
            implements PlayerDatabase.StoredActiveObjective {
        TestObjectiveData(
                final String objectiveType,
                final String holderPath,
                final int objectiveId,
                final double currentProgress,
                final boolean completed,
                final double progressNeeded) {
            this(objectiveType, holderPath, objectiveId, currentProgress, completed, progressNeeded, List.of(), List.of());
        }

        TestObjectiveData withActive(final TestObjectiveData objective) {
            return new TestObjectiveData(
                    objectiveType,
                    holderPath,
                    objectiveId,
                    currentProgress,
                    completed,
                    progressNeeded,
                    List.of(objective),
                    completedObjectives);
        }

        TestObjectiveData withCompleted(final TestObjectiveData objective) {
            return new TestObjectiveData(
                    objectiveType,
                    holderPath,
                    objectiveId,
                    currentProgress,
                    completed,
                    progressNeeded,
                    activeObjectives,
                    List.of(objective));
        }
    }
}
