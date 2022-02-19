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

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.util.ArrayList;
import java.util.List;
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

    private boolean showLocation = false;
    private Location location = null;

    public Objective(NotQuests main) {
        this.main = main;
        conditions = new ArrayList<>();
        rewards = new ArrayList<>();
    }

    public boolean isShowLocation() {
        return showLocation;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location, boolean save) {
        this.location = location;
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".location", location);
            quest.getCategory().saveQuestsConfig();
        }

    }

    public void setShowLocation(boolean showLocation, boolean save) {
        this.showLocation = showLocation;
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".showLocation", showLocation);
            quest.getCategory().saveQuestsConfig();
        }
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
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionNPCID", completionNPCID);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public final void setCompletionArmorStandUUID(final UUID completionArmorStandUUID, final boolean save) {
        this.completionArmorStandUUID = completionArmorStandUUID;
        if (save) {
            if (completionArmorStandUUID != null) {
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", completionArmorStandUUID.toString());
            } else {
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".completionArmorStandUUID", null);
            }
            quest.getCategory().saveQuestsConfig();
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

    public final Condition getConditionFromID(int id){
        for(Condition condition : getConditions()){
            if(condition.getConditionID() == id){
                return condition;
            }
        }
        return null;
    }

    public final Action getRewardFromID(int id){
        for(Action action : getRewards()){
            if(action.getActionID() == id){
                return action;
            }
        }
        return null;
    }

    public final ArrayList<Action> getRewards() {
        return rewards;
    }




    public void addCondition(final Condition condition, final boolean save) {
        boolean dupeID = false;
        for (Condition condition1 : conditions) {
            if (condition.getConditionID() == condition1.getConditionID()) {
                dupeID = true;
                break;
            }
        }
        if (!dupeID) {
            conditions.add(condition);
            if (save) {
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + condition.getConditionID() + ".conditionType", condition.getConditionType());
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + condition.getConditionID() + ".progressNeeded", condition.getProgressNeeded());
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + condition.getConditionID() + ".negated", condition.isNegated());
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + condition.getConditionID() + ".description", condition.getDescription());

                condition.save(quest.getCategory().getQuestsConfig(), "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + condition.getConditionID());
                quest.getCategory().saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add condition to objective with the ID <highlight>" + getObjectiveID() + "</highlight> with the ID <highlight>" + condition.getConditionID() + "</highlight> but the ID was a DUPLICATE!");
        }
    }

    public void addReward(final Action action, final boolean save) {
        boolean dupeID = false;
        for (Action action1 : rewards) {
            if (action.getActionID() == action1.getActionID()) {
                dupeID = true;
                break;
            }
        }
        if (!dupeID) {
            rewards.add(action);
            if (save) {
                quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + action.getActionID() + ".actionType", action.getActionType());
                if (!action.getActionName().isBlank()) {
                    quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + action.getActionID() + ".displayName", action.getActionName());
                }
                action.save(quest.getCategory().getQuestsConfig(), "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + action.getActionID());
                quest.getCategory().saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add reward to objective with the ID <highlight>" + getObjectiveID() + "</highlight> with the ID <highlight>" + action.getActionID() + "</highlight> but the ID was a DUPLICATE!");
        }
    }

    public void removeCondition(final Condition condition, final boolean save) {
        int conditionID = condition.getConditionID();
        conditions.remove(condition);
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions." + conditionID, null);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public void removeReward(final Action action, final boolean save) {
        int rewardID = action.getActionID();
        rewards.remove(action);
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards." + rewardID, null);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public void clearRewards() {
        rewards.clear();
        quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards", null);
        quest.getCategory().saveQuestsConfig();
    }

    public void clearConditions() {
        conditions.clear();
        quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions", null);
        quest.getCategory().saveQuestsConfig();
    }

    public final String getDisplayName() {
        return objectiveDisplayName;
    }

    public final String getFinalName() {
        if (!objectiveDisplayName.isBlank()) {
            return getDisplayName();
        } else {
            return main.getObjectiveManager().getObjectiveType(this.getClass());
        }
    }

    public void setDisplayName(String newObjectiveDisplayName, boolean save) {
        newObjectiveDisplayName = main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDisplayName);
        this.objectiveDisplayName = newObjectiveDisplayName;
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", newObjectiveDisplayName);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public void removeDisplayName(boolean save) {
        this.objectiveDisplayName = "";
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName", null);
            quest.getCategory().saveQuestsConfig();
        }
    }


    public final String getDescription() { //MiniMessage
        return objectiveDescription;
    }


    /**
     * Gets the objective description, but also adds line-breaks so the description is not bigger than the screen
     * (useful for the GUI)
     *
     * @param maxLengthPerLine how long the description can be per-line
     * @return the description of the objective with proper line-breaks
     */
    public final String getDescription(final int maxLengthPerLine) {
        return main.getUtilManager().wrapText(getDescription(), maxLengthPerLine);
    }

    public final List<String> getDescriptionLines(final int maxLengthPerLine) {
        return main.getUtilManager().wrapTextToList(getDescription(), maxLengthPerLine);
    }


    public void setDescription(String newObjectiveDescription, boolean save) {
        newObjectiveDescription = main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDescription);
        this.objectiveDescription = newObjectiveDescription;
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", newObjectiveDescription);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public void removeDescription(boolean save) {
        this.objectiveDescription = "";
        if (save) {
            quest.getCategory().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description", null);
            quest.getCategory().saveQuestsConfig();
        }
    }

    public final Quest getQuest() {
        return quest;
    }

    public abstract String getObjectiveTaskDescription(final Player player);

    public abstract void save(final FileConfiguration configuration, final String initialPath);

    public abstract void load(final FileConfiguration configuration, final String initialPath);

    public abstract void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess);

    public abstract void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed);


    public final int getFreeRewardID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getRewardFromID(i) == null){
                return i;
            }
        }
        return getRewards().size()+1;
    }
    public final int getFreeConditionID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getConditionFromID(i) == null){
                return i;
            }
        }
        return getConditions().size()+1;
    }

}
