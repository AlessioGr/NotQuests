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

package rocks.gravili.Structs;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import rocks.gravili.Hooks.Citizens.QuestGiverNPCTrait;
import rocks.gravili.NotQuests;
import rocks.gravili.Structs.Objectives.Objective;
import rocks.gravili.Structs.Requirements.*;
import rocks.gravili.Structs.Rewards.*;
import rocks.gravili.Structs.Triggers.Trigger;
import rocks.gravili.Structs.Triggers.TriggerTypes.NPCDeathTrigger;
import rocks.gravili.Structs.Triggers.TriggerTypes.WorldEnterTrigger;
import rocks.gravili.Structs.Triggers.TriggerTypes.WorldLeaveTrigger;

import java.util.ArrayList;
import java.util.logging.Level;

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
    private final ArrayList<Reward> rewards;
    private final ArrayList<Objective> objectives;
    private final ArrayList<Requirement> requirements; //Requirements to accept the quest
    private final ArrayList<Trigger> triggers; //Triggers for the quest
    private final ArrayList<NPC> attachedNPCsWithQuestShowing;
    private final ArrayList<NPC> attachedNPCsWithoutQuestShowing;
    private int maxAccepts = -1; //-1 or smaller => unlimited accepts
    private long acceptCooldown = -1; //Cooldown in minute. -1 or smaller => no cooldown.
    private boolean takeEnabled = true;
    private String description = "";
    private String displayName = "";

    public Quest(NotQuests main, String questName) {
        this.main = main;
        this.questName = questName;
        rewards = new ArrayList<>();
        objectives = new ArrayList<>();
        requirements = new ArrayList<>();
        attachedNPCsWithQuestShowing = new ArrayList<>();
        attachedNPCsWithoutQuestShowing = new ArrayList<>();
        triggers = new ArrayList<>();
    }

    public final String getQuestName() {
        return questName;
    }

    public final ArrayList<Reward> getRewards() {
        return rewards;
    }

    public void addReward(Reward reward) {
        rewards.add(reward);
        main.getDataManager().getQuestsData().set("quests." + questName + ".rewards." + rewards.size() + ".rewardType", reward.getRewardType().toString());
        if (reward instanceof CommandReward commandReward) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".rewards." + rewards.size() + ".specifics.consoleCommand", commandReward.getConsoleCommand());
        } else if (reward instanceof QuestPointsReward commandReward) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".rewards." + rewards.size() + ".specifics.rewardedQuestPoints", commandReward.getRewardedQuestPoints());
        } else if (reward instanceof ItemReward itemReward) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".rewards." + rewards.size() + ".specifics.rewardItem", itemReward.getItemReward());
        } else if (reward instanceof MoneyReward moneyReward) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".rewards." + rewards.size() + ".specifics.rewardedMoneyAmount", moneyReward.getRewardedMoney());
        }

    }

    public void removeAllRewards() {
        rewards.clear();
        main.getDataManager().getQuestsData().set("quests." + questName + ".rewards", null);
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
                objective.save();

                main.getDataManager().getQuestsData().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".objectiveType", main.getObjectiveManager().getObjectiveType(objective.getClass()));
                main.getDataManager().getQuestsData().set("quests." + questName + ".objectives." + objective.getObjectiveID() + ".progressNeeded", objective.getProgressNeeded());



            }

        } else {
            main.getLogManager().log(Level.WARNING, "ERROR: Tried to add objective to quest §b" + getQuestName() + " §cwith the ID §b" + objective.getObjectiveID() + " §cbut the ID was a DUPLICATE!");

        }


    }

    public void removeAllObjectives() {
        objectives.clear();
        main.getDataManager().getQuestsData().set("quests." + questName + ".objectives", null);
    }


    public final int getMaxAccepts() {
        return maxAccepts;
    }

    public void setMaxAccepts(int maxAccepts) {
        this.maxAccepts = maxAccepts;
        main.getDataManager().getQuestsData().set("quests." + questName + ".maxAccepts", maxAccepts);
    }

    public final boolean isTakeEnabled() {
        return takeEnabled;
    }

    public void setTakeEnabled(boolean takeEnabled) {
        this.takeEnabled = takeEnabled;
        main.getDataManager().getQuestsData().set("quests." + questName + ".takeEnabled", takeEnabled);
    }

    public final long getAcceptCooldown() {
        return acceptCooldown;
    }

    public void setAcceptCooldown(long cooldownInMinutes) {
        this.acceptCooldown = cooldownInMinutes;
        main.getDataManager().getQuestsData().set("quests." + questName + ".acceptCooldown", cooldownInMinutes);
    }

    public final String getQuestDescription() {
        return description;
    }

    public final String getQuestDescription(final int maxLengthPerLine) {
        final StringBuilder descriptionWithLineBreaks = new StringBuilder();
        int count = 0;
        for (char character : description.toCharArray()) {
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


    public void setQuestDescription(String newQuestDescription) {
        this.description = newQuestDescription;
        main.getDataManager().getQuestsData().set("quests." + questName + ".description", newQuestDescription);

    }

    public final String getQuestDisplayName() {
        return displayName;
    }

    public void setQuestDisplayName(String newQuestDisplayName) {
        this.displayName = newQuestDisplayName;
        main.getDataManager().getQuestsData().set("quests." + questName + ".displayName", newQuestDisplayName);

    }


    public final ArrayList<Requirement> getRequirements() {
        return requirements;
    }

    public void addRequirement(Requirement requirement) {
        requirements.add(requirement);
        main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".requirementType", requirement.getRequirementType().toString());
        main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".progressNeeded", requirement.getProgressNeeded());


        if (requirement instanceof OtherQuestRequirement otherQuestRequirement) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.otherQuestRequirememt", otherQuestRequirement.getOtherQuestName());
        } else if (requirement instanceof QuestPointsRequirement questPointsRequirement) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.questPointRequirement", questPointsRequirement.getQuestPointRequirement());
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.deductQuestPoints", questPointsRequirement.isDeductQuestPoints());

        } else if (requirement instanceof MoneyRequirement moneyRequirement) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.moneyRequirement", moneyRequirement.getMoneyRequirement());
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.deductMoney", moneyRequirement.isDeductMoney());

        } else if (requirement instanceof PermissionRequirement permissionRequirement) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".requirements." + requirements.size() + ".specifics.requiredPermission", permissionRequirement.getRequiredPermission());

        }

    }

    public void removeAllRequirements() {
        requirements.clear();
        main.getDataManager().getQuestsData().set("quests." + questName + ".requirements", null);
    }

    public void removeAllNPCs() {
        if(!main.isCitizensEnabled()){
            main.getLogManager().log(Level.SEVERE, "§cThe removal of all NPCs from Quest " + questName + " §chas been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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
        main.getDataManager().getQuestsData().set("quests." + questName + ".npcs", null);
    }

    public void bindToNPC(NPC npc, boolean showQuest) {
        if(!main.isCitizensEnabled()){
            main.getLogManager().log(Level.SEVERE, "§cThe binding to NPC in Quest " + questName + " §chas been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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


        main.getDataManager().getQuestsData().set("quests." + questName + ".npcs." + npc.getId() + ".npcID", npc.getId());
        main.getDataManager().getQuestsData().set("quests." + questName + ".npcs." + npc.getId() + ".questShowing", showQuest);

    }

    public final ArrayList<NPC> getAttachedNPCsWithQuestShowing() {
        return attachedNPCsWithQuestShowing;
    }

    public final ArrayList<NPC> getAttachedNPCsWithoutQuestShowing() {
        return attachedNPCsWithoutQuestShowing;
    }

    public void removeNPC(final NPC npc) {
        if(!main.isCitizensEnabled()){
            main.getLogManager().log(Level.SEVERE, "§cThe NPC removal in Quest " + questName + " §chas been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
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

            main.getDataManager().getQuestsData().set("quests." + questName + ".npcs." + npc.getId(), null);
            attachedNPCsWithQuestShowing.remove(npc);
            attachedNPCsWithoutQuestShowing.remove(npc);

        }

    }


    public final ArrayList<Trigger> getTriggers() {
        return triggers;
    }

    public void addTrigger(final Trigger trigger) {
        triggers.add(trigger);
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".triggerType", trigger.getTriggerType().toString());
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".triggerActionName", trigger.getTriggerAction().getActionName());
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".applyOn", trigger.getApplyOn());
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".amountNeeded", trigger.getAmountNeeded());
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".worldName", trigger.getWorldName());

        if (trigger instanceof NPCDeathTrigger) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".specifics.npcToDie", ((NPCDeathTrigger) trigger).getNpcToDieID());

        } else if (trigger instanceof WorldEnterTrigger) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".specifics.worldToEnter", ((WorldEnterTrigger) trigger).getWorldToEnterName());

        } else if (trigger instanceof WorldLeaveTrigger) {
            main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggers.size() + ".specifics.worldToLeave", ((WorldLeaveTrigger) trigger).getWorldToLeaveName());

        }


    }

    public void removeAllTriggers() {
        triggers.clear();
        main.getDataManager().getQuestsData().set("quests." + questName + ".triggers", null);
    }

    public void removeObjective(final Objective objective) {
        objectives.remove(objective);
        main.getDataManager().getQuestsData().set("quests." + questName + ".objectives." + objective.getObjectiveID(), null);
    }

    public void removeObjective(final int objectiveID) {
        objectives.remove(objectiveID);
        main.getDataManager().getQuestsData().set("quests." + questName + ".objectives." + objectiveID, null);
    }

    public String removeTrigger(int triggerID) {
        if (triggers.get((triggerID - 1)) != null) {


            triggers.remove(triggers.get((triggerID - 1)));
            main.getDataManager().getQuestsData().set("quests." + questName + ".triggers." + triggerID, null);
            return "§aTrigger successfully removed!";

        } else {
            return "§cError: Trigger not found!";
        }
    }

}