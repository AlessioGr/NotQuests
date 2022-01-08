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

package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.ArrayList;
import java.util.UUID;

public abstract class Objective {
    private final ArrayList<Condition> conditions;
    protected final NotQuests main;
    private final ArrayList<Action> rewards;
    private long progressNeeded = 1;
    private Quest quest;
    private int objectiveID = -1;
    private String objectiveDisplayName = "";
    private String objectiveDescription = "";
    private int completionNPCID = -1;
    private UUID completionArmorStandUUID = null;

    public Objective(NotQuests main) {
        this.main = main;
        conditions = new ArrayList<>();
        rewards = new ArrayList<>();
    }

    public void setQuest(final Quest quest) {
        this.quest = quest;
    }

    public void setProgressNeeded(final long progressNeeded) {
        this.progressNeeded = progressNeeded;
    }

    public void setObjectiveID(final int objectiveID) {
        this.objectiveID = objectiveID;
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
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionNPCID", completionNPCID);
        }
    }

    public final void setCompletionArmorStandUUID(final UUID completionArmorStandUUID, final boolean save) {
        this.completionArmorStandUUID = completionArmorStandUUID;
        if (save) {
            if (completionArmorStandUUID != null) {
                main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", completionArmorStandUUID.toString());
            } else {
                main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", null);
            }
        }
    }

    public final int getObjectiveID() {
        return objectiveID;
    }

    public final long getProgressNeeded() {
        return progressNeeded;
    }

    public final ArrayList<Condition> getConditions() {
        return conditions;
    }

    public final ArrayList<Action> getRewards() {
        return rewards;
    }


    public void addCondition(final Condition condition, final boolean save) {
        conditions.add(condition);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditions.size() + ".conditionType", condition.getConditionType());
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditions.size() + ".progressNeeded", condition.getProgressNeeded());
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditions.size() + ".negated", condition.isNegated());

            condition.save(main.getDataManager().getQuestsConfig(), "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditions.size());
        }
    }

    public void addReward(final Action action, final boolean save) {
        rewards.add(action);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + rewards.size() + ".actionType", action.getActionType());
            if (!action.getActionName().isBlank()) {
                main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + rewards.size() + ".displayName", action.getActionName());
            }
            action.save(main.getDataManager().getQuestsConfig(), "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + rewards.size());
        }
    }

    public void removeCondition(final Condition condition, final boolean save) {
        int conditionID = conditions.indexOf(condition);
        conditions.remove(condition);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditionID, null);
        }
    }

    public void removeReward(final Action action, final boolean save) {
        int rewardID = rewards.indexOf(action);
        rewards.remove(action);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + rewardID, null);
        }
    }

    public void clearRewards() {
        rewards.clear();
        main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards", null);
    }

    public void clearConditions() {
        conditions.clear();
        main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions", null);
    }

    public final String getObjectiveDisplayName() {
        return objectiveDisplayName;
    }

    public final String getObjectiveFinalName() {
        if (!objectiveDisplayName.isBlank()) {
            return getObjectiveDisplayName();
        } else {
            return main.getObjectiveManager().getObjectiveType(this.getClass());
        }
    }

    public void setObjectiveDisplayName(String newObjectiveDisplayName, boolean save) {
        newObjectiveDisplayName = main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDisplayName);
        this.objectiveDisplayName = newObjectiveDisplayName;
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", newObjectiveDisplayName);
        }
    }

    public void removeObjectiveDisplayName(boolean save) {
        this.objectiveDisplayName = "";
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", null);
        }
    }


    public final String getObjectiveDescription() { //MiniMessage
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
        return main.getUtilManager().wrapText(getObjectiveDescription(), maxLengthPerLine);
    }


    public void setObjectiveDescription(String newObjectiveDescription, boolean save) {
        newObjectiveDescription = main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDescription);
        this.objectiveDescription = newObjectiveDescription;
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", newObjectiveDescription);
        }
    }

    public void removeObjectiveDescription(boolean save) {
        this.objectiveDescription = "";
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", null);
        }
    }

    public final Quest getQuest() {
        return quest;
    }

    public abstract String getObjectiveTaskDescription(final String eventualColor, final Player player);

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);

    public abstract void onObjectiveUnlock(final ActiveObjective activeObjective);

}
