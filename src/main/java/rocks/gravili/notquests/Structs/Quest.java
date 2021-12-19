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

package rocks.gravili.notquests.Structs;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.Material;
import rocks.gravili.notquests.Managers.Integrations.Citizens.QuestGiverNPCTrait;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Actions.Action;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

import java.util.ArrayList;

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
    private Material takeItem = Material.BOOK;

    public Quest(NotQuests main, String questName) {
        this.main = main;
        this.questName = questName;
        rewards = new ArrayList<>();
        objectives = new ArrayList<>();
        conditions = new ArrayList<>();
        attachedNPCsWithQuestShowing = new ArrayList<>();
        attachedNPCsWithoutQuestShowing = new ArrayList<>();
        triggers = new ArrayList<>();
    }

    public final String getQuestName() {
        return questName;
    }

    public final ArrayList<Action> getRewards() {
        return rewards;
    }



    public void removeAllRewards() {
        rewards.clear();
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards", null);
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
                main.getDataManager().getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".objectiveType", main.getObjectiveManager().getObjectiveType(objective.getClass()));
                main.getDataManager().getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".progressNeeded", objective.getProgressNeeded());

                objective.save(main.getDataManager().getQuestsConfig(), "quests." + questName + ".objectives." + objective.getObjectiveID());
            }
        } else {
            main.getLogManager().warn("ERROR: Tried to add objective to quest <AQUA>" + getQuestName() + "</AQUA> with the ID <AQUA>" + objective.getObjectiveID() + "</AQUA> but the ID was a DUPLICATE!");
        }
    }


    public void addRequirement(final Condition condition, final boolean save) {
        conditions.add(condition);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".requirements." + conditions.size() + ".conditionType", condition.getConditionType());
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".requirements." + conditions.size() + ".progressNeeded", condition.getProgressNeeded());

            condition.save(main.getDataManager().getQuestsConfig(), "quests." + questName + ".requirements." + conditions.size());
        }

    }

    public void addReward(Action action, final boolean save) {
        rewards.add(action);
        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards." + rewards.size() + ".actionType", action.getActionType());
            if (!action.getActionName().isBlank()) {
                main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards." + rewards.size() + ".displayName", action.getActionName());
            }

            action.save(main.getDataManager().getQuestsConfig(), "quests." + questName + ".rewards." + rewards.size());
        }
    }

    public void addTrigger(final Trigger trigger, final boolean save) {
        triggers.add(trigger);

        if (save) {
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggers.size() + ".triggerType", trigger.getTriggerType());
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggers.size() + ".triggerActionName", trigger.getTriggerAction().getActionName());
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggers.size() + ".applyOn", trigger.getApplyOn());
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggers.size() + ".amountNeeded", trigger.getAmountNeeded());
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggers.size() + ".worldName", trigger.getWorldName());

            trigger.save(main.getDataManager().getQuestsConfig(), "quests." + questName + ".triggers." + triggers.size());
        }

    }

    public void removeAllObjectives() {
        objectives.clear();
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".objectives", null);
    }


    public final int getMaxAccepts() {
        return maxAccepts;
    }

    public void setMaxAccepts(int maxAccepts) {
        this.maxAccepts = maxAccepts;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".maxAccepts", maxAccepts);
    }

    public final boolean isTakeEnabled() {
        return takeEnabled;
    }

    public void setTakeEnabled(boolean takeEnabled) {
        this.takeEnabled = takeEnabled;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".takeEnabled", takeEnabled);
    }

    public final long getAcceptCooldown() {
        return acceptCooldown;
    }

    public void setAcceptCooldown(long cooldownInMinutes) {
        this.acceptCooldown = cooldownInMinutes;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".acceptCooldown", cooldownInMinutes);
    }

    public final String getQuestDescription() {
        return description;
    }

    public void setQuestDescription(String newQuestDescription) {
        newQuestDescription = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDescription);

        this.description = newQuestDescription;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".description", newQuestDescription);

    }

    public final String getQuestDescription(final int maxLengthPerLine) {
        return main.getUtilManager().wrapText(description, maxLengthPerLine);
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

    public void setQuestDisplayName(String newQuestDisplayName) {
        newQuestDisplayName = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDisplayName);

        this.displayName = newQuestDisplayName;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".displayName", newQuestDisplayName);

    }


    public final ArrayList<Condition> getRequirements() {
        return conditions;
    }

    public void removeAllRequirements() {
        conditions.clear();
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".requirements", null);
    }

    public void removeAllNPCs() {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The removal of all NPCs from Quest <AQUA>" + questName + "</AQUA> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".npcs", null);
    }

    public void bindToNPC(NPC npc, boolean showQuest) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The binding to NPC in Quest <AQUA>" + questName + "</AQUA> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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


        main.getDataManager().getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId() + ".npcID", npc.getId());
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId() + ".questShowing", showQuest);

    }

    public final ArrayList<NPC> getAttachedNPCsWithQuestShowing() {
        return attachedNPCsWithQuestShowing;
    }

    public final ArrayList<NPC> getAttachedNPCsWithoutQuestShowing() {
        return attachedNPCsWithoutQuestShowing;
    }

    public void removeNPC(final NPC npc) {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().severe("The NPC removal in Quest <AQUA>" + questName + "</AQUA> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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

            main.getDataManager().getQuestsConfig().set("quests." + questName + ".npcs." + npc.getId(), null);
            attachedNPCsWithQuestShowing.remove(npc);
            attachedNPCsWithoutQuestShowing.remove(npc);

        }

    }


    public final ArrayList<Trigger> getTriggers() {
        return triggers;
    }



    public void removeAllTriggers() {
        triggers.clear();
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers", null);
    }

    public void removeObjective(final Objective objective) {
        objectives.remove(objective);
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".objectives." + objective.getObjectiveID(), null);
    }


    public void removeReward(final Action action) {
        rewards.remove(action);
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards." + rewards.indexOf(action), null);
    }

    public String removeTrigger(int triggerID) {
        if (triggers.get((triggerID - 1)) != null) {


            triggers.remove(triggers.get((triggerID - 1)));
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".triggers." + triggerID, null);
            return "<AQUA>Trigger successfully removed!";

        } else {
            return "<RED>Error: Trigger not found!";
        }
    }

    public final Material getTakeItem() {
        return takeItem;
    }

    public void setTakeItem(final Material takeItem) {
        this.takeItem = takeItem;
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".takeItem", takeItem.name());
    }

}