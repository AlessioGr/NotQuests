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

package rocks.gravili.notquests.paper.structs;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.integrations.citizens.QuestGiverNPCTrait;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.List;

/**
 * The Quest object is loaded at the start from whatever is defined in the quests.yml. It contains all data which defines
 * a quest, but no data of active quests (like the progress). The data it contains consists of the name, rewards, objectives,
 * requirements, triggers, quest npcs and much more - basically everything which can be configured in the /questsadmin command.
 * <p>
 * This data is saved into the quests.yml - not into the database.
 *
 * @author Alessio Gravili
 */
public class Quest {
    private final NotQuests main;
    private final String questName;
    private final ArrayList<Action> rewards;
    private final ArrayList<Objective> objectives;
    private final ArrayList<Condition> conditions; //Requirements to accept the quest
    private final ArrayList<Trigger> triggers; //Triggers for the quest
    private final ArrayList<NPC> attachedNPCsWithQuestShowing;
    private final ArrayList<NPC> attachedNPCsWithoutQuestShowing;
    private int maxAccepts = -1; //-1 or smaller => unlimited accepts
    private long acceptCooldown = -1; //Cooldown in minute. -1 or smaller => no cooldown.
    private boolean takeEnabled = true;
    private String description = "";
    private String displayName = "";
    private ItemStack takeItem = new ItemStack(Material.BOOK);
    private Category category;

    public Quest(NotQuests main, String questName) {
        this.main = main;
        this.questName = questName;
        rewards = new ArrayList<>();
        objectives = new ArrayList<>();
        conditions = new ArrayList<>();
        attachedNPCsWithQuestShowing = new ArrayList<>();
        attachedNPCsWithoutQuestShowing = new ArrayList<>();
        triggers = new ArrayList<>();
        category = main.getDataManager().getDefaultCategory();
    }

    public Quest(NotQuests main, String questName, final Category category) {
        this.main = main;
        this.questName = questName;
        rewards = new ArrayList<>();
        objectives = new ArrayList<>();
        conditions = new ArrayList<>();
        attachedNPCsWithQuestShowing = new ArrayList<>();
        attachedNPCsWithoutQuestShowing = new ArrayList<>();
        triggers = new ArrayList<>();
        this.category = category;
    }

    public final Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public final String getQuestName() {
        return questName;
    }

    public final ArrayList<Action> getRewards() {
        return rewards;
    }



    public void clearRewards() {
        rewards.clear();
        category.getQuestsConfig().set("quests." + questName + ".rewards", null);
        category.saveQuestsConfig();
    }

    public final ArrayList<Objective> getObjectives() {
        return objectives;
    }

    public final Objective getObjectiveFromID(final int objectiveID) {
        for (final Objective objective : objectives) {
            if (objective.getObjectiveID() == objectiveID) {
                return objective;
            }
        }
        return null;
    }

    public final Condition getRequirementFromID(final int id) {
        for (final Condition condition : getRequirements()) {
            if (condition.getConditionID() == id) {
                return condition;
            }
        }
        return null;
    }
    public final Action getRewardFromID(final int id) {
        for (final Action action : getRewards()) {
            if (action.getActionID() == id) {
                return action;
            }
        }
        return null;
    }


    /*public final Reward getRewardFromID(final int rewardID) {
        for (final Reward reward : rewards) {
            if (reward.getRewardID() == rewardID) {
                return reward;
            }
        }
        return null;
    }*/

    public final Trigger getTriggerFromID(final int triggerID) {
        for (final Trigger trigger : triggers) {
            if (trigger.getTriggerID() == triggerID) {
                return trigger;
            }
        }
        return null;
    }

    public void addObjective(Objective objective, boolean save) {
        boolean dupeID = false;
        for (Objective objective1 : objectives) {
            if (objective.getObjectiveID() == objective1.getObjectiveID()) {
                dupeID = true;
                break;
            }
        }
        if (!dupeID) {
            objectives.add(objective);
            if (save) {
                category.getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".objectiveType", main.getObjectiveManager().getObjectiveType(objective.getClass()));
                category.getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".progressNeeded", objective.getProgressNeeded());

                objective.save(category.getQuestsConfig(), "quests." + questName + ".objectives." + objective.getObjectiveID());
                category.saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add objective to quest <highlight>" + getQuestName() + "</highlight> with the ID <highlight>" + objective.getObjectiveID() + "</highlight> but the ID was a DUPLICATE!");
        }
    }


    public void addRequirement(final Condition condition, final boolean save) {
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
                category.getQuestsConfig().set("quests." + questName + ".requirements." + condition.getConditionID() + ".conditionType", condition.getConditionType());
                category.getQuestsConfig().set("quests." + questName + ".requirements." + condition.getConditionID() + ".progressNeeded", condition.getProgressNeeded());
                category.getQuestsConfig().set("quests." + questName + ".requirements." + condition.getConditionID() + ".negated", condition.isNegated());
                category.getQuestsConfig().set("quests." + questName + ".requirements." + condition.getConditionID() + ".description", condition.getDescription());

                condition.save(category.getQuestsConfig(), "quests." + questName + ".requirements." + condition.getConditionID());
                category.saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add requirement to quest <highlight>" + getQuestName() + "</highlight> with the ID <highlight>" + condition.getConditionID() + "</highlight> but the ID was a DUPLICATE!");
        }

    }

    public void addReward(Action action, final boolean save) {
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
                category.getQuestsConfig().set("quests." + questName + ".rewards." + action.getActionID() + ".actionType", action.getActionType());
                if (!action.getActionName().isBlank()) {
                    category.getQuestsConfig().set("quests." + questName + ".rewards." + action.getActionID() + ".displayName", action.getActionName());
                }

                action.save(category.getQuestsConfig(), "quests." + questName + ".rewards." + action.getActionID());
                category.saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add reward to quest <highlight>" + getQuestName() + "</highlight> with the ID <highlight>" + action.getActionID() + "</highlight> but the ID was a DUPLICATE!");
        }
    }

    public void addTrigger(final Trigger trigger, final boolean save) {
        boolean dupeID = false;
        for (Trigger trigger1 : triggers) {
            if (trigger.getTriggerID() == trigger1.getTriggerID()) {
                dupeID = true;
                break;
            }
        }
        if (!dupeID) {
            triggers.add(trigger);

            if (save) {
                category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID() + ".triggerType", trigger.getTriggerType());
                category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID() + ".triggerActionName", trigger.getTriggerAction().getActionName());
                category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID() + ".applyOn", trigger.getApplyOn());
                category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID() + ".amountNeeded", trigger.getAmountNeeded());
                category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID() + ".worldName", trigger.getWorldName());

                trigger.save(category.getQuestsConfig(), "quests." + questName + ".triggers." + trigger.getTriggerID());
                category.saveQuestsConfig();
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add trigger to quest <highlight>" + getQuestName() + "</highlight> with the ID <highlight>" + trigger.getTriggerID() + "</highlight> but the ID was a DUPLICATE!");
        }
    }

    public void clearObjectives() {
        objectives.clear();
        category.getQuestsConfig().set("quests." + questName + ".objectives", null);
        category.saveQuestsConfig();
    }


    public final int getMaxAccepts() {
        return maxAccepts;
    }

    public void setMaxAccepts(int maxAccepts) {
        this.maxAccepts = maxAccepts;
        category.getQuestsConfig().set("quests." + questName + ".maxAccepts", maxAccepts);
        category.saveQuestsConfig();
    }

    public final boolean isTakeEnabled() {
        return takeEnabled;
    }

    public void setTakeEnabled(boolean takeEnabled) {
        this.takeEnabled = takeEnabled;
        category.getQuestsConfig().set("quests." + questName + ".takeEnabled", takeEnabled);
        category.saveQuestsConfig();
    }

    public final long getAcceptCooldown() {
        return acceptCooldown;
    }

    public void setAcceptCooldown(long cooldownInMinutes) {
        this.acceptCooldown = cooldownInMinutes;
        category.getQuestsConfig().set("quests." + questName + ".acceptCooldown", cooldownInMinutes);
        category.saveQuestsConfig();
    }

    public final String getQuestDescription() {
        return description;
    }

    public void setQuestDescription(String newQuestDescription, boolean save) {
        newQuestDescription = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDescription);

        this.description = newQuestDescription;
        if(save){
            category.getQuestsConfig().set("quests." + questName + ".description", newQuestDescription);
            category.saveQuestsConfig();
        }
    }

    public void removeQuestDescription(boolean save) {
        this.description = "";
        if(save){
            category.getQuestsConfig().set("quests." + questName + ".description", null);
            category.saveQuestsConfig();
        }
    }

    public final String getQuestDescription(final int maxLengthPerLine) {
        return main.getUtilManager().wrapText(description, maxLengthPerLine);
    }

    public final List<String> getQuestDescriptionList(final int maxLengthPerLine) {
        return main.getUtilManager().wrapTextToList(description, maxLengthPerLine);
    }

    public final String getQuestDisplayName() {
        return displayName;
    }


    /**
     * Returns the Quest displayname if it's not blank. Otherwise, it just returns the Quest Name
     *
     * @return either the displayname or the quest name
     */
    public final String getQuestFinalName() {
        if (!displayName.isBlank()) {
            return getQuestDisplayName();
        } else {
            return questName;
        }
    }

    public void setQuestDisplayName(String newQuestDisplayName, boolean save) {
        newQuestDisplayName = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDisplayName);

        this.displayName = newQuestDisplayName;
        if(save){
            category.getQuestsConfig().set("quests." + questName + ".displayName", newQuestDisplayName);
            category.saveQuestsConfig();
        }
    }

    public void removeQuestDisplayName(boolean save) {
        this.displayName = "";
        if (save) {
            category.getQuestsConfig().set("quests." + questName + ".displayName", null);
            category.saveQuestsConfig();
        }
    }


    public final ArrayList<Condition> getRequirements() {
        return conditions;
    }

    public void clearRequirements() {
        conditions.clear();
        category.getQuestsConfig().set("quests." + questName + ".requirements", null);
        category.saveQuestsConfig();
    }

    public void clearNPCs() {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The removal of all NPCs from Quest <highlight>" + questName + "</highlight> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
            return;
        }
        final ArrayList<NPC> arrayList = new ArrayList<>(attachedNPCsWithQuestShowing);
        arrayList.addAll(attachedNPCsWithoutQuestShowing);
        for (NPC npc : arrayList) {
            if (main.getQuestManager().getAllQuestsAttachedToNPC(npc).size() == 1) {
                npc.removeTrait(QuestGiverNPCTrait.class);
            }

        }
        attachedNPCsWithQuestShowing.clear();
        attachedNPCsWithoutQuestShowing.clear();
        category.getQuestsConfig().set("quests." + questName + ".npcs", null);
        category.saveQuestsConfig();
    }

    public void bindToNPC(NPC npc, boolean showQuest) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The binding to NPC in Quest <highlight>" + questName + "</highlight> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
            return;
        }
        if (!attachedNPCsWithQuestShowing.contains(npc) && !attachedNPCsWithoutQuestShowing.contains(npc)) {
            if (showQuest) {
                attachedNPCsWithQuestShowing.add(npc);
            } else {
                attachedNPCsWithoutQuestShowing.add(npc);
            }

        }


        boolean hasTrait = false;
        for (Trait trait : npc.getTraits()) {
            if (trait.getName().contains("questgiver")) {
                hasTrait = true;
                break;
            }
        }
        if (!npc.hasTrait(QuestGiverNPCTrait.class) && !hasTrait) {
            //System.out.println("§2NPC doesnt have trait. giving him trait... Cur traits: " + npc.getTraits().toString());
            npc.addTrait(QuestGiverNPCTrait.class);
        }


        category.getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId() + ".npcID", npc.getId());
        category.getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId() + ".questShowing", showQuest);
        category.saveQuestsConfig();
    }

    public final ArrayList<NPC> getAttachedNPCsWithQuestShowing() {
        return attachedNPCsWithQuestShowing;
    }

    public final ArrayList<NPC> getAttachedNPCsWithoutQuestShowing() {
        return attachedNPCsWithoutQuestShowing;
    }

    public void removeNPC(final NPC npc) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The NPC removal in Quest <highlight>" + questName + "</highlight> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
            return;
        }
        // System.out.println("§e-2");
        if (attachedNPCsWithoutQuestShowing.contains(npc) || attachedNPCsWithQuestShowing.contains(npc)) {

            final ArrayList<NPC> arrayList = new ArrayList<>(attachedNPCsWithQuestShowing);
            arrayList.addAll(attachedNPCsWithoutQuestShowing);
            final ArrayList<Trait> npcTraitsToRemove = new ArrayList<>();
            for (final NPC attachedNPC : arrayList) {
                // System.out.println("§e-1");
                if (attachedNPC.equals(npc)) {
                    // System.out.println("§e0");
                    if (main.getQuestManager().getAllQuestsAttachedToNPC(npc).size() == 1) {
                        //npc.removeTrait(QuestGiverNPCTrait.class);
                        // System.out.println("§e1");
                        for (final Trait trait : npc.getTraits()) {
                            if (trait.getName().equalsIgnoreCase("nquestgiver")) {
                                npcTraitsToRemove.add(trait);
                            }
                        }
                    }
                }
                for (final Trait trait : npcTraitsToRemove) {
                    npc.removeTrait(trait.getClass());
                    // System.out.println("§e2");
                }
                npcTraitsToRemove.clear();
            }

            category.getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId(), null);
            category.saveQuestsConfig();

            attachedNPCsWithQuestShowing.remove(npc);
            attachedNPCsWithoutQuestShowing.remove(npc);
        }

    }


    public final ArrayList<Trigger> getTriggers() {
        return triggers;
    }



    public void clearTriggers() {
        triggers.clear();
        category.getQuestsConfig().set("quests." + questName + ".triggers", null);
        category.saveQuestsConfig();
    }

    public void removeObjective(final Objective objective) {
        category.getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID(), null);
        category.saveQuestsConfig();
        objectives.remove(objective);
    }


    public void removeReward(final Action action) {
        category.getQuestsConfig().set("quests." + questName + ".rewards." + action.getActionID(), null);
        category.saveQuestsConfig();
        rewards.remove(action);
    }

    public void removeRequirement(final Condition requirement) {
        category.getQuestsConfig().set("quests." + questName + ".requirements." + requirement.getConditionID(), null);
        category.saveQuestsConfig();
        conditions.remove(requirement);
    }

    public String removeTrigger(final Trigger trigger) {
        category.getQuestsConfig().set("quests." + questName + ".triggers." + trigger.getTriggerID(), null);
        category.saveQuestsConfig();
        triggers.remove(trigger);
        return "<highlight>Trigger successfully removed!";
    }

    public final ItemStack getTakeItem() {
        return takeItem;
    }

    public void setTakeItem(final ItemStack takeItem) {
        if (takeItem != null) {
            this.takeItem = takeItem;
            category.getQuestsConfig().set("quests." + questName + ".takeItem", takeItem);
            category.saveQuestsConfig();
        }

    }

    public void switchCategory(final Category category) {

        final ConfigurationSection questsConfigurationSection = getCategory().getQuestsConfig().getConfigurationSection("quests." + questName);

        getCategory().getQuestsConfig().set("quests." + questName, null);
        getCategory().saveQuestsConfig();

        setCategory(category);

        category.getQuestsConfig().set("quests." + questName, questsConfigurationSection);
        category.saveQuestsConfig();

    }

    public final int getFreeObjectiveID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getObjectiveFromID(i) == null){
                return i;
            }
        }
        return getObjectives().size()+1;
    }
    public final int getFreeRewardID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getRewardFromID(i) == null){
                return i;
            }
        }
        return getRewards().size()+1;
    }
    public final int getFreeRequirementID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getRequirementFromID(i) == null){
                return i;
            }
        }
        return getRequirements().size()+1;
    }
    public final int getFreeTriggerID(){
        for(int i = 1; i< Integer.MAX_VALUE; i++){
            if(getTriggerFromID(i) == null){
                return i;
            }
        }
        return getTriggers().size()+1;
    }
}