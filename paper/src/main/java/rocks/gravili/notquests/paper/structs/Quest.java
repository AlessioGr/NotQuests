/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

/**
 * The Quest object is loaded at the start from whatever is defined in the quests.yml. It contains
 * all data which defines a quest, but no data of active quests (like the progress). The data it
 * contains consists of the name, rewards, objectives, requirements, triggers, quest npcs and much
 * more - basically everything which can be configured in the /questsadmin command.
 *
 * <p>This data is saved into the quests.yml - not into the database.
 *
 * @author Alessio Gravili
 */
public class Quest extends ObjectiveHolder {
  private final NotQuests main;
  private final String questName;
  private final ArrayList<Action> rewards;
  private final ArrayList<Condition> conditions; // Requirements to accept the quest

  private ArrayList<Condition> conditionsWithSpecialConditions;
  private final ArrayList<Trigger> triggers; // Triggers for the quest
  private final CopyOnWriteArrayList<NQNPC> attachedNPCsWithQuestShowing;
  private final CopyOnWriteArrayList<NQNPC> attachedNPCsWithoutQuestShowing;
  private int maxCompletions = -1; // -1 or smaller => unlimited completions
  private int maxAccepts = -1; // -1 or smaller => unlimited accepts
  private int maxFails = -1; // -1 or smaller => unlimited fails


  private long acceptCooldownComplete = -1; // Cooldown in minutes. -1 or smaller => no cooldown.
  private boolean takeEnabled = true;
  private boolean abortEnabled = true;

  private String displayName = "";
  private ItemStack takeItem = new ItemStack(Material.BOOK);
  private Category category;


  public Quest(final NotQuests main, final String questName) {
    this.main = main;
    this.questName = questName;
    rewards = new ArrayList<>();
    conditions = new ArrayList<>();
    conditionsWithSpecialConditions = new ArrayList<>();
    attachedNPCsWithQuestShowing = new CopyOnWriteArrayList<>();
    attachedNPCsWithoutQuestShowing = new CopyOnWriteArrayList<>();
    triggers = new ArrayList<>();
    category = main.getDataManager().getDefaultCategory();
  }

  public Quest(NotQuests main, String questName, final Category category) {
    this.main = main;
    this.questName = questName;
    rewards = new ArrayList<>();
    conditions = new ArrayList<>();
    conditionsWithSpecialConditions = new ArrayList<>();
    attachedNPCsWithQuestShowing = new CopyOnWriteArrayList<>();
    attachedNPCsWithoutQuestShowing = new CopyOnWriteArrayList<>();
    triggers = new ArrayList<>();
    this.category = category;
  }


  public final Category getCategory() {
    return category;
  }


  public void setCategory(final Category category) {
    this.category = category;
  }


  public final ArrayList<Action> getRewards() {
    return rewards;
  }

  public void clearRewards() {
    rewards.clear();
    category.getQuestsConfig().set("quests." + questName + ".rewards", null);
    category.saveQuestsConfig();
  }




  public final Condition getRequirementFromID(final int id) {
    for (final Condition condition : conditions) {
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
      updateConditionsWithSpecial();
      if (save) {
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".requirements."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".requirements."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".requirements." + condition.getConditionID() + ".negated",
                condition.isNegated());
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".requirements."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        condition.save(
            category.getQuestsConfig(),
            "quests." + questName + ".requirements." + condition.getConditionID());
        category.saveQuestsConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add requirement to quest <highlight>"
                  + getIdentifier()
                  + "</highlight> with the ID <highlight>"
                  + condition.getConditionID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

  public void addReward(final Action action, final boolean save) {
    boolean dupeID = false;
    for (final Action action1 : rewards) {
      if (action.getActionID() == action1.getActionID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      rewards.add(action);
      if (save) {
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".rewards." + action.getActionID() + ".actionType",
                action.getActionType());
        if (!action.getActionName().isBlank()) {
          category
              .getQuestsConfig()
              .set(
                  "quests." + questName + ".rewards." + action.getActionID() + ".displayName",
                  action.getActionName());
        }

        action.save(
            category.getQuestsConfig(), "quests." + questName + ".rewards." + action.getActionID());
        category.saveQuestsConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add reward to quest <highlight>"
                  + getIdentifier()
                  + "</highlight> with the ID <highlight>"
                  + action.getActionID()
                  + "</highlight> but the ID was a DUPLICATE!");
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
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".triggers." + trigger.getTriggerID() + ".triggerType",
                trigger.getTriggerType());
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".triggers."
                    + trigger.getTriggerID()
                    + ".triggerActionName",
                trigger.getTriggerAction().getActionName());
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".triggers." + trigger.getTriggerID() + ".applyOn",
                trigger.getApplyOn());
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".triggers." + trigger.getTriggerID() + ".amountNeeded",
                trigger.getAmountNeeded());
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".triggers." + trigger.getTriggerID() + ".worldName",
                trigger.getWorldName());

        trigger.save(
            category.getQuestsConfig(),
            "quests." + questName + ".triggers." + trigger.getTriggerID());
        category.saveQuestsConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add trigger to quest <highlight>"
                  + getIdentifier()
                  + "</highlight> with the ID <highlight>"
                  + trigger.getTriggerID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }



  public final int getMaxCompletions() {
    return maxCompletions;
  }

  public void setMaxCompletions(final int maxCompletions, final boolean save) {
    this.maxCompletions = maxCompletions;
    if(save){
      category.getQuestsConfig().set("quests." + questName + ".limits.completions", maxCompletions);
      category.saveQuestsConfig();
    }
  }

  public final int getMaxAccepts() {
    return maxAccepts;
  }

  public void setMaxAccepts(int maxAccepts, final boolean save) {
    this.maxAccepts = maxAccepts;
    if(save){
      category.getQuestsConfig().set("quests." + questName + ".limits.accepts", maxAccepts);
      category.saveQuestsConfig();
    }
  }

  public final int getMaxFails() {
    return maxFails;
  }

  public void setMaxFails(int maxFails, final boolean save) {
    this.maxFails = maxFails;
    if(save){
      category.getQuestsConfig().set("quests." + questName + ".limits.fails", maxFails);
      category.saveQuestsConfig();
    }
  }

  public final boolean isAbortEnabled() {
    return abortEnabled;
  }

  public void setAbortEnabled(final boolean abortEnabled, final boolean save) {
    this.abortEnabled = abortEnabled;
    if(save){
      category.getQuestsConfig().set("quests." + questName + ".abortEnabled", abortEnabled);
      category.saveQuestsConfig();
    }
  }
  public final boolean isTakeEnabled() {
    return takeEnabled;
  }

  public void setTakeEnabled(final boolean takeEnabled, final boolean save) {
    this.takeEnabled = takeEnabled;
    if(save){
      category.getQuestsConfig().set("quests." + questName + ".takeEnabled", takeEnabled);
      category.saveQuestsConfig();
    }
  }

  public final long getAcceptCooldownComplete() {
    return acceptCooldownComplete;
  }

  public void setAcceptCooldownComplete(long cooldownInMinutes, final boolean save) {
    this.acceptCooldownComplete = cooldownInMinutes;
    if(save) {
      category.getQuestsConfig().set("quests." + questName + ".acceptCooldown.complete", cooldownInMinutes);
      category.saveQuestsConfig();
    }
  }


  public void setQuestDescription(String newQuestDescription, boolean save) {
    newQuestDescription = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDescription);

    this.setObjectiveHolderDescription(newQuestDescription);
    if (save) {
      category.getQuestsConfig().set("quests." + questName + ".description", newQuestDescription);
      category.saveQuestsConfig();
    }
  }

  public void removeQuestDescription(boolean save) {
    setObjectiveHolderDescription("");
    if (save) {
      category.getQuestsConfig().set("quests." + questName + ".description", null);
      category.saveQuestsConfig();
    }
  }

  public final String getQuestDescription(final int maxLengthPerLine) {
    return main.getUtilManager().wrapText(getObjectiveHolderDescription(), maxLengthPerLine);
  }

  public final List<String> getQuestDescriptionList(final int maxLengthPerLine) {
    return main.getUtilManager().wrapTextToList(getObjectiveHolderDescription(), maxLengthPerLine);
  }

  public final String getQuestDisplayName() {
    return displayName;
  }

  /**
   * Returns the Quest displayname if it's not blank. Otherwise, it just returns the Quest Name
   *
   * @return either the displayname or the quest name
   */
  @Override
  public final String getDisplayNameOrIdentifier() {
    if (!getQuestDisplayName().isBlank()) {
      return getQuestDisplayName();
    } else {
      return questName;
    }
  }

  public void setQuestDisplayName(String newQuestDisplayName, boolean save) {
    newQuestDisplayName = main.getUtilManager().replaceLegacyWithMiniMessage(newQuestDisplayName);

    this.displayName = newQuestDisplayName;
    if (save) {
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
    return conditionsWithSpecialConditions;
  }

  public void clearRequirements() {
    conditions.clear();
    category.getQuestsConfig().set("quests." + questName + ".requirements", null);
    category.saveQuestsConfig();
    updateConditionsWithSpecial();
  }

  public void clearNPCs() {
    final ArrayList<NQNPC> arrayList = new ArrayList<>(attachedNPCsWithQuestShowing);
    arrayList.addAll(attachedNPCsWithoutQuestShowing);
    for (final NQNPC npc : arrayList) {
      if (main.getQuestManager().getAllQuestsAttachedToNPC(npc).size() == 1) {
        npc.removeQuestGiverNPCTrait(null, this);
      }
    }
    attachedNPCsWithQuestShowing.clear();
    attachedNPCsWithoutQuestShowing.clear();
    category.getQuestsConfig().set("quests." + questName + ".npcs", null);
    category.saveQuestsConfig();
  }

  public String bindToNPC(@NotNull final NQNPC npc, final boolean showQuestInNPC) {
    final String result = npc.addQuestGiverNPCTrait(showQuestInNPC, this);
    if(!result.isBlank()){
      return result;
    }

    if (!attachedNPCsWithQuestShowing.contains(npc)
        && !attachedNPCsWithoutQuestShowing.contains(npc)) {
      if (showQuestInNPC) {
        attachedNPCsWithQuestShowing.add(npc);
      } else {
        attachedNPCsWithoutQuestShowing.add(npc);
      }
    }


    npc.saveToConfig(category.getQuestsConfig(), "quests." + questName + ".npcs." + npc.getIdentifyingString() + ".npcData");
    category
        .getQuestsConfig()
        .set("quests." + questName + ".npcs." + npc.getIdentifyingString() + ".questShowing", showQuestInNPC);
    category.saveQuestsConfig();
    return "";
  }

  public final CopyOnWriteArrayList<NQNPC> getAttachedNPCsWithQuestShowing() {
    return attachedNPCsWithQuestShowing;
  }

  public final CopyOnWriteArrayList<NQNPC> getAttachedNPCsWithoutQuestShowing() {
    return attachedNPCsWithoutQuestShowing;
  }

  public void removeNPC(final NQNPC npc) {
    // System.out.println("§e-2");
    if (attachedNPCsWithoutQuestShowing.contains(npc)
        || attachedNPCsWithQuestShowing.contains(npc)) {

      final ArrayList<NQNPC> arrayList = new ArrayList<>(attachedNPCsWithQuestShowing);
      arrayList.addAll(attachedNPCsWithoutQuestShowing);



      for (final NQNPC attachedNPC : arrayList) {
        // System.out.println("§e-1");
        if (attachedNPC.equals(npc)) {
          // System.out.println("§e0");
          if (main.getQuestManager().getAllQuestsAttachedToNPC(npc).size() == 1) {
            // npc.removeTrait(QuestGiverNPCTrait.class);
            // System.out.println("§e1");
             npc.removeQuestGiverNPCTrait(null, this);
          }
        }
      }

      category.getQuestsConfig().set("quests." + questName + ".npcs." + npc.getIdentifyingString(), null);
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


  public void removeReward(final Action action) {
    category
        .getQuestsConfig()
        .set("quests." + questName + ".rewards." + action.getActionID(), null);
    category.saveQuestsConfig();
    rewards.remove(action);
  }

  public void removeRequirement(final Condition requirement) {
    category
        .getQuestsConfig()
        .set("quests." + questName + ".requirements." + requirement.getConditionID(), null);
    category.saveQuestsConfig();
    conditions.remove(requirement);
    updateConditionsWithSpecial();
  }

  public String removeTrigger(final Trigger trigger) {
    category
        .getQuestsConfig()
        .set("quests." + questName + ".triggers." + trigger.getTriggerID(), null);
    category.saveQuestsConfig();
    triggers.remove(trigger);
    return "<highlight>Trigger successfully removed!";
  }

  public final ItemStack getTakeItem() {
    return takeItem;
  }

  public void setTakeItem(final ItemStack takeItem, final boolean save) {
    if (takeItem != null) {
      this.takeItem = takeItem;
      if(save) {
        category.getQuestsConfig().set("quests." + questName + ".takeItem", takeItem);
        category.saveQuestsConfig();
      }
    }
  }

  public void switchCategory(final Category category) {

    final ConfigurationSection questsConfigurationSection =
        getCategory().getQuestsConfig().getConfigurationSection("quests." + questName);

    getCategory().getQuestsConfig().set("quests." + questName, null);
    getCategory().saveQuestsConfig();

    setCategory(category);

    category.getQuestsConfig().set("quests." + questName, questsConfigurationSection);
    category.saveQuestsConfig();
  }



  public final int getFreeRewardID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getRewardFromID(i) == null) {
        return i;
      }
    }
    return getRewards().size() + 1;
  }

  public final int getFreeRequirementID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getRequirementFromID(i) == null) {
        return i;
      }
    }
    return conditions.size() + 1;
  }

  public final int getFreeTriggerID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getTriggerFromID(i) == null) {
        return i;
      }
    }
    return getTriggers().size() + 1;
  }

  public void updateConditionsWithSpecial(){
    conditionsWithSpecialConditions = new ArrayList<>(conditions);
    final PredefinedProgressOrder predefinedProgressOrder = category.getPredefinedProgressOrder();
    if(predefinedProgressOrder != null){
      int ourIndex = category.getQuests().indexOf(this);

      conditionsWithSpecialConditions.add(new Condition(main) {
        @Override
        protected String checkInternally(QuestPlayer questPlayer) {
          if(predefinedProgressOrder.isFirstToLast()){
            int counter = 0;
            for(final Quest otherQuest : category.getQuests()){
              if(counter < ourIndex){
                if(!questPlayer.hasCompletedQuest(otherQuest)){
                  return "Quest " + otherQuest.getDisplayNameOrIdentifier() + " needs to be completed first";
                }
              }
              counter++;
            }
          }else if(predefinedProgressOrder.isLastToFirst()){
            int counter = 0;
            for(final Quest otherQuest : category.getQuests()){
              if(counter > ourIndex){
                if(!questPlayer.hasCompletedQuest(otherQuest)){
                  return "Quest " + otherQuest.getDisplayNameOrIdentifier() + " needs to be completed first";
                }
              }
              counter++;
            }
          }else if(predefinedProgressOrder.getCustomOrder() != null && !predefinedProgressOrder.getCustomOrder().isEmpty()){

            for(final String questNameToCheck : predefinedProgressOrder.getCustomOrder()){
              if(questNameToCheck.equalsIgnoreCase(questName)){
                break;
              }
              if(!questPlayer.hasCompletedQuest(questNameToCheck)){
                return "Quest " + questNameToCheck + " needs to be completed first";
              }
            }
          }
          return "";
        }

        @Override
        protected String getConditionDescriptionInternally(QuestPlayer questPlayer,
            Object... objects) {
          return null;
        }

        @Override
        public void save(FileConfiguration configuration, String initialPath) {

        }

        @Override
        public void load(FileConfiguration configuration, String initialPath) {

        }

        @Override
        public void deserializeFromSingleLineString(ArrayList<String> arguments) {

        }
      });

    }

  }

  @Override
  public FileConfiguration getConfig() {
    return getCategory().getQuestsConfig();
  }

  @Override
  public void saveConfig() {
    getCategory().saveQuestsConfig();
  }

  @Override
  public String getInitialConfigPath() {
    return "quests." + getIdentifier();
  }

  @Override
  public String getIdentifier() {
    return this.questName;
  }

  @Override
  public void setPredefinedProgressOrder(final PredefinedProgressOrder predefinedProgressOrder, final boolean save) {
    this.predefinedProgressOrder = predefinedProgressOrder;
    if (save) {
      if(predefinedProgressOrder != null) {
        predefinedProgressOrder.saveToConfiguration(category.getQuestsConfig(),  "quests."
            + questName
            + ".predefinedProgressOrder");
      }else{
        category
            .getQuestsConfig()
            .set(
                "quests." + questName + ".predefinedProgressOrder",
                null);
      }
      category.saveQuestsConfig();
    }
  }

  @Override
  public void clearObjectives() {
    super.getObjectives().clear();
    category.getQuestsConfig().set("quests." + questName + ".objectives", null);
    category.saveQuestsConfig();
  }

  @Override
  public final Objective getObjectiveFromID(final int objectiveID) {
    for (final Objective objective : super.getObjectives()) {
      if (objective.getObjectiveID() == objectiveID) {
        return objective;
      }
    }
    return null;
  }

  @Override
  public void removeObjective(final Objective objective) {
    category
        .getQuestsConfig()
        .set("quests." + questName + ".objectives." + objective.getObjectiveID(), null);
    category.saveQuestsConfig();
    super.getObjectives().remove(objective);
  }


  public void addObjective(Objective objective, boolean save) {
    boolean dupeID = false;
    for (Objective objective1 : super.getObjectives()) {
      if (objective.getObjectiveID() == objective1.getObjectiveID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      super.getObjectives().add(objective);
      if (save) {
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".objectives."
                    + objective.getObjectiveID()
                    + ".objectiveType",
                main.getObjectiveManager().getObjectiveType(objective.getClass()));
        category
            .getQuestsConfig()
            .set(
                "quests."
                    + questName
                    + ".objectives."
                    + objective.getObjectiveID()
                    + ".progressNeededExpression",
                objective.getProgressNeededExpression().getRawExpression());

        objective.save(
            category.getQuestsConfig(),
            "quests." + questName + ".objectives." + objective.getObjectiveID());
        category.saveQuestsConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add objective to quest <highlight>"
                  + getIdentifier()
                  + "</highlight> with the ID <highlight>"
                  + objective.getObjectiveID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

}
