package rocks.gravili.Structs.Objectives;


import org.bukkit.entity.Player;
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
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breed.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%ENTITYTOBREED%", getEntityToBreedType());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.mobToBreed", getEntityToBreedType());

    }

    public final String getEntityToBreedType(){
        return entityToBreedType;
    }

}
