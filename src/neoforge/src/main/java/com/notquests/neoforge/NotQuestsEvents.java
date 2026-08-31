package com.notquests.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import com.notquests.core.objectives.Objective;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;

/** NeoForge events for cancellable NotQuests lifecycle decisions. */
public final class NotQuestsEvents {
    private NotQuestsEvents() {}

    public static final class Loaded extends Event {}

    public static final class QuestAccept extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final String questName;
        private final Quest quest;
        private final boolean triggerAcceptQuestTrigger;

        public QuestAccept(
                final ServerPlayer player,
                final String questName,
                final Quest quest,
                final boolean triggerAcceptQuestTrigger) {
            this.player = player;
            this.questName = questName == null ? "" : questName;
            this.quest = quest;
            this.triggerAcceptQuestTrigger = triggerAcceptQuestTrigger;
        }

        public ServerPlayer player() { return player; }
        public String questName() { return questName; }
        public Quest quest() { return quest; }
        public boolean triggerAcceptQuestTrigger() { return triggerAcceptQuestTrigger; }
    }

    public static final class QuestPointsChange extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final long newQuestPoints;

        public QuestPointsChange(final ServerPlayer player, final long newQuestPoints) {
            this.player = player;
            this.newQuestPoints = newQuestPoints;
        }

        public ServerPlayer player() { return player; }
        public long newQuestPoints() { return newQuestPoints; }
    }

    public static final class QuestComplete extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final String questName;
        private final Quest quest;
        private final boolean forced;

        public QuestComplete(final ServerPlayer player, final String questName, final Quest quest, final boolean forced) {
            this.player = player;
            this.questName = questName == null ? "" : questName;
            this.quest = quest;
            this.forced = forced;
        }

        public ServerPlayer player() { return player; }
        public String questName() { return questName; }
        public Quest quest() { return quest; }
        public boolean forced() { return forced; }
    }

    public static final class QuestFail extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final String questName;
        private final Quest quest;

        public QuestFail(final ServerPlayer player, final String questName, final Quest quest) {
            this.player = player;
            this.questName = questName == null ? "" : questName;
            this.quest = quest;
        }

        public ServerPlayer player() { return player; }
        public String questName() { return questName; }
        public Quest quest() { return quest; }
    }

    public static final class ObjectiveComplete extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final String questName;
        private final Quest quest;
        private final ActiveObjective objective;
        private final int[] objectivePath;
        private final int objectiveId;
        private final String objectiveHolderPath;

        public ObjectiveComplete(
                final ServerPlayer player,
                final String questName,
                final Quest quest,
                final ActiveObjective objective,
                final int[] objectivePath,
                final int objectiveId,
                final String objectiveHolderPath) {
            this.player = player;
            this.questName = questName == null ? "" : questName;
            this.quest = quest;
            this.objective = objective;
            this.objectivePath = objectivePath == null ? new int[0] : objectivePath.clone();
            this.objectiveId = objectiveId;
            this.objectiveHolderPath = objectiveHolderPath == null ? "" : objectiveHolderPath;
        }

        public ServerPlayer player() { return player; }
        public String questName() { return questName; }
        public Quest quest() { return quest; }
        public ActiveObjective objective() { return objective; }
        public int[] objectivePath() { return objectivePath.clone(); }
        public int objectiveId() {
            return objectiveId;
        }
        public Objective configuredObjective() {
            return objective == null ? null : objective.getObjective();
        }
        public String objectiveHolderPath() { return objectiveHolderPath; }
    }

    public static final class ObjectiveUnlock extends Event implements ICancellableEvent {
        private final ServerPlayer player;
        private final String questName;
        private final Quest quest;
        private final ActiveObjective objective;
        private final int[] objectivePath;
        private final int objectiveId;
        private final String objectiveHolderPath;
        private final boolean triggerAcceptQuestTrigger;

        public ObjectiveUnlock(
                final ServerPlayer player,
                final String questName,
                final Quest quest,
                final ActiveObjective objective,
                final int[] objectivePath,
                final int objectiveId,
                final String objectiveHolderPath,
                final boolean triggerAcceptQuestTrigger) {
            this.player = player;
            this.questName = questName == null ? "" : questName;
            this.quest = quest;
            this.objective = objective;
            this.objectivePath = objectivePath == null ? new int[0] : objectivePath.clone();
            this.objectiveId = objectiveId;
            this.objectiveHolderPath = objectiveHolderPath == null ? "" : objectiveHolderPath;
            this.triggerAcceptQuestTrigger = triggerAcceptQuestTrigger;
        }

        public ServerPlayer player() { return player; }
        public String questName() { return questName; }
        public Quest quest() { return quest; }
        public ActiveObjective objective() { return objective; }
        public int[] objectivePath() { return objectivePath.clone(); }
        public int objectiveId() {
            return objectiveId;
        }
        public Objective configuredObjective() {
            return objective == null ? null : objective.getObjective();
        }
        public String objectiveHolderPath() { return objectiveHolderPath; }
        public boolean triggerAcceptQuestTrigger() { return triggerAcceptQuestTrigger; }
    }
}
