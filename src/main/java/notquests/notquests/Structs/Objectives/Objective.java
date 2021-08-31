package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

import java.util.ArrayList;

public class Objective {
    private final ObjectiveType objectiveType;
    private final ArrayList<Objective> dependantObjectives;
    private final long progressNeeded;
    private final Quest quest;
    private final NotQuests main;
    private final int objectiveID;
    private String objectiveDisplayName = "";
    private String objectiveDescription = "";
    private int completionNPCID = -1;

    public Objective(NotQuests main, Quest quest, int objectiveID, ObjectiveType objectiveType, int progressNeeded) {
        this.main = main;
        this.quest = quest;
        this.objectiveID = objectiveID;
        this.objectiveType = objectiveType;
        this.progressNeeded = progressNeeded;
        dependantObjectives = new ArrayList<>();
    }

    public final int getCompletionNPCID() {
        return completionNPCID;
    }

    public final void setCompletionNPCID(final int completionNPCID, final boolean save) {
        this.completionNPCID = completionNPCID;
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionNPCID", completionNPCID);
        }
    }

    public final int getObjectiveID() {
        return objectiveID;
    }

    public final ObjectiveType getObjectiveType() {
        return objectiveType;
    }

    public final long getProgressNeeded() {
        return progressNeeded;
    }

    public final ArrayList<Objective> getDependantObjectives() {
        return dependantObjectives;
    }


    public void addDependantObjective(final Objective objective, final boolean save) {
        dependantObjectives.add(objective);
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".dependantObjectives." + objective.getObjectiveID() + ".objectiveID", objective.getObjectiveID());
        }
    }

    public void removeDependantObjective(final Objective objective, final boolean save) {
        dependantObjectives.remove(objective);
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".dependantObjectives." + objective.getObjectiveID(), null);
        }
    }

    public void clearDependantObjectives() {
        dependantObjectives.clear();
        main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".dependantObjectives", null);
    }

    public final String getObjectiveDisplayName() {
        return objectiveDisplayName;
    }

    public void setObjectiveDisplayName(final String newObjectiveDisplayName, boolean save) {
        this.objectiveDisplayName = newObjectiveDisplayName;
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", newObjectiveDisplayName);
        }
    }

    public void removeObjectiveDisplayName(boolean save) {
        this.objectiveDisplayName = "";
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", null);
        }
    }

    public final String getObjectiveDescription() {
        return objectiveDescription;
    }

    public void setObjectiveDescription(final String newObjectiveDescription, boolean save) {
        this.objectiveDescription = newObjectiveDescription;
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", newObjectiveDescription);
        }
    }

    public void removeObjectiveDescription(boolean save) {
        this.objectiveDescription = "";
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", null);
        }
    }

    public final Quest getQuest() {
        return quest;
    }


}
