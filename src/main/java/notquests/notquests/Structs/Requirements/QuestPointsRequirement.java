package notquests.notquests.Structs.Requirements;

import notquests.notquests.NotQuests;

public class QuestPointsRequirement extends Requirement {

    private final NotQuests main;
    private final long questPointRequirement;
    private final boolean deductQuestPoints;


    public QuestPointsRequirement(NotQuests main, long questPointRequirement, boolean deductQuestPoints) {
        super(RequirementType.QuestPoints, questPointRequirement);
        this.main = main;
        this.questPointRequirement = questPointRequirement;
        this.deductQuestPoints = deductQuestPoints;

    }


    public final long getQuestPointRequirement() {
        return questPointRequirement;
    }


    public final boolean isDeductQuestPoints() {
        return deductQuestPoints;
    }

}
