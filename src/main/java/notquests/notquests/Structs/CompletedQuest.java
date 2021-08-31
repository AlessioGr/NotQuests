package notquests.notquests.Structs;


public class CompletedQuest {
    private final Quest quest;


    private final long timeCompleted;

    private final QuestPlayer questPlayer;


    public CompletedQuest(final Quest quest, final QuestPlayer questPlayer) {
        this.quest = quest;
        this.questPlayer = questPlayer;
        timeCompleted = System.currentTimeMillis();
    }

    public CompletedQuest(final Quest quest, final QuestPlayer questPlayer, final long timeCompleted) {
        this.quest = quest;
        this.questPlayer = questPlayer;
        this.timeCompleted = timeCompleted;
    }

    public final Quest getQuest() {
        return quest;
    }

    public final QuestPlayer getQuestPlayer() {
        return questPlayer;
    }

    public final long getTimeCompleted() {
        return timeCompleted;
    }

}
