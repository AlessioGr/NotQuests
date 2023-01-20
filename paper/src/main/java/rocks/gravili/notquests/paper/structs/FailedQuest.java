package rocks.gravili.notquests.paper.structs;

public class FailedQuest {
    private final Quest quest;

    private final long timeFailed;

    private final QuestPlayer questPlayer;

    public FailedQuest(final Quest quest, final QuestPlayer questPlayer) {
        this.quest = quest;
        this.questPlayer = questPlayer;
        timeFailed = System.currentTimeMillis();
    }

    public FailedQuest(
            final Quest quest, final QuestPlayer questPlayer, final long timeFailed) {
        this.quest = quest;
        this.questPlayer = questPlayer;
        this.timeFailed = timeFailed;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final QuestPlayer getQuestPlayer() {
        return questPlayer;
    }

    public final long getTimeFailed() {
        return timeFailed;
    }

    public final String getQuestIdentifier() {
        return quest.getIdentifier();
    }
}