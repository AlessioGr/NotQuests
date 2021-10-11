package rocks.gravili.Structs.Objectives;


import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Quest;

public class BreedObjective extends Objective {
    private final NotQuests main;
    private final String entityToBreedType;

    public BreedObjective(NotQuests main, Quest quest, int objectiveID, int progressNeeded, String entityToBreedType) {
        super(main, quest, objectiveID, progressNeeded);
        this.main = main;
        this.entityToBreedType = entityToBreedType;
    }

    public BreedObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        this.main = main;
        final String questName = quest.getQuestName();

        this.entityToBreedType = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.mobToBreed");

    }

    @Override
    public String getObjectiveTaskDescription(String eventualColor) {
        return "    §7" + eventualColor + "Mob to breed: §f" + eventualColor + getEntityToBreedType();
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.mobToBreed", getEntityToBreedType());

    }

    public final String getEntityToBreedType(){
        return entityToBreedType;
    }

}
