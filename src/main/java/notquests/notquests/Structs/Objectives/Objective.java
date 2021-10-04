/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

import java.util.ArrayList;
import java.util.UUID;

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
    private UUID completionArmorStandUUID = null;

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

    public final UUID getCompletionArmorStandUUID() {
        return completionArmorStandUUID;
    }

    public final void setCompletionNPCID(final int completionNPCID, final boolean save) {
        this.completionNPCID = completionNPCID;
        if (save) {
            main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionNPCID", completionNPCID);
        }
    }

    public final void setCompletionArmorStandUUID(final UUID completionArmorStandUUID, final boolean save) {
        this.completionArmorStandUUID = completionArmorStandUUID;
        if (save) {
            if (completionArmorStandUUID != null) {
                main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", completionArmorStandUUID.toString());
            } else {
                main.getDataManager().getQuestsData().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", null);
            }
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


    /**
     * Gets the objective description, but also adds line-breaks so the description is not bigger than the screen
     * (useful for the GUI)
     *
     * @param maxLengthPerLine how long the description can be per-line
     * @return the description of the objective with proper line-breaks
     */
    public final String getObjectiveDescription(final int maxLengthPerLine) {

        StringBuilder descriptionWithLineBreaks = new StringBuilder();
        int count = 0;
        for (char character : objectiveDescription.toCharArray()) {
            count++;
            if (count > maxLengthPerLine) {
                count = 0;
                descriptionWithLineBreaks.append("\n§8");
            } else {
                descriptionWithLineBreaks.append(character);
            }
        }

        return descriptionWithLineBreaks.toString();
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
