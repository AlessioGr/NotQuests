package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

public class OtherQuestObjective extends Objective {
    private final NotQuests main;
    private final String otherQuestName;
    private final int amountOfCompletionsNeeded;
    private final boolean countPreviousCompletions;


    public OtherQuestObjective(NotQuests main, final Quest quest, final int objectiveID, String otherQuestName, int amountOfCompletionsNeeded, boolean countPreviousCompletions) {
        super(main, quest, objectiveID, ObjectiveType.OtherQuest, amountOfCompletionsNeeded);
        this.main = main;
        this.otherQuestName = otherQuestName;
        this.amountOfCompletionsNeeded = amountOfCompletionsNeeded;
        this.countPreviousCompletions = countPreviousCompletions;

    }


    public final String getOtherQuestName() {
        return otherQuestName;
    }

    public final Quest getOtherQuest() {
        return main.getQuestManager().getQuest(otherQuestName);
    }

    public final int getAmountOfCompletionsNeeded() {
        return amountOfCompletionsNeeded;
    }

    public final boolean isCountPreviousCompletions() {
        return countPreviousCompletions;
    }


}
