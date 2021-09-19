package notquests.notquests.Structs;

/**
 * This is a special object for completed quests. Unlike the ActiveQuest object, it does not need to contain the progress, as it's already expected
 * that progress = complete. Apart from, obviously, the quest object, to know what quest it was, it additionally contains the time it was completed
 * (System.currentTimeMilis thingy) and the questPlayer object, to know who finished the active quest.
 * <p>
 * The timeCompleted is needed for the quest cooldown to work. All completed quests for a player are saved in the Database.
 *
 * @author Alessio Gravili
 */
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
