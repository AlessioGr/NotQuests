package notquests.notquests.Structs.Requirements;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;


public class OtherQuestRequirement extends Requirement {

    private final NotQuests main;
    private final String otherQuestName;
    private final int amountOfCompletionsNeeded;


    public OtherQuestRequirement(NotQuests main, String otherQuestName, int amountOfCompletionsNeeded) {
        super(RequirementType.OtherQuest, amountOfCompletionsNeeded);
        this.main = main;
        this.otherQuestName = otherQuestName;
        this.amountOfCompletionsNeeded = amountOfCompletionsNeeded;

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

}
