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
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.events.notquests.ObjectiveCompleteEvent;
import rocks.gravili.notquests.paper.events.notquests.QuestFailEvent;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

/**
 * This is a special object for active quests. Apart from the Quest itself, it stores additional
 * objects to track the quest progress. This includes the active objectives and completed
 * objectives, as well as triggers and the quest player who accepted the Quest.
 *
 * <p>All this information is saved in the Database, so the Player can continue from where they left
 * off if the server or the plugin restarts.
 *
 * @author Alessio Gravili
 */
public class ActiveQuest {
  private final NotQuests main;

  private final Quest quest;

  private final ArrayList<ActiveObjective> activeObjectives;
  private final ArrayList<ActiveObjective> completedObjectives;
  private final ArrayList<ActiveObjective> toRemove;
  private final ArrayList<ActiveTrigger> activeTriggers;

  private final QuestPlayer questPlayer;

  public ActiveQuest(NotQuests main, Quest quest, QuestPlayer questPlayer) {
    this.main = main;
    this.quest = quest;
    this.questPlayer = questPlayer;
    activeObjectives = new ArrayList<>();
    toRemove = new ArrayList<>();
    completedObjectives = new ArrayList<>();
    activeTriggers = new ArrayList<>();

    int triggerID = 1;
    for (final Trigger trigger : quest.getTriggers()) {
      ActiveTrigger activeTrigger = new ActiveTrigger(triggerID, trigger, this);
      activeTriggers.add(activeTrigger);
      triggerID++;
    }

    int objectiveID = 1;
    for (final Objective objective : quest.getObjectives()) {
      ActiveObjective activeObjective = new ActiveObjective(main, objectiveID, objective, this);
      activeObjectives.add(activeObjective);
      objectiveID++;
    }
  }

  public final Quest getQuest() {
    return quest;
  }

  public final ArrayList<ActiveTrigger> getActiveTriggers() {
    return activeTriggers;
  }

  public final ArrayList<ActiveObjective> getActiveObjectives() {
    return activeObjectives;
  }

  public final ArrayList<ActiveObjective> getCompletedObjectives() {
    return completedObjectives;
  }

  public final boolean isCompleted() {
    return activeObjectives.size() == 0;
  }

  public final QuestPlayer getQuestPlayer() {
    return questPlayer;
  }

  // For Citizens NPCs
  public void notifyActiveObjectiveCompleted(
      final ActiveObjective activeObjective, final boolean silent, final int NPCID) {
    notifyActiveObjectiveCompleted(activeObjective, silent, NPCID, null);
  }
  // For Armor Stands
  public void notifyActiveObjectiveCompleted(
      final ActiveObjective activeObjective, final boolean silent, final UUID armorStandUUID) {
    notifyActiveObjectiveCompleted(activeObjective, silent, -1, armorStandUUID);
  }

  public void notifyActiveObjectiveCompleted(
      final ActiveObjective activeObjective,
      final boolean silent,
      final int NPCID,
      final UUID armorStandUUID) {
    if (!main.getDataManager().isCurrentlyLoading() && !questPlayer.isCurrentlyLoading()) {
      ObjectiveCompleteEvent objectiveCompleteEvent =
          new ObjectiveCompleteEvent(getQuestPlayer(), activeObjective, this);
      if (Bukkit.isPrimaryThread()) {
        Bukkit.getScheduler()
            .runTaskAsynchronously(
                main.getMain(),
                () -> {
                  Bukkit.getPluginManager().callEvent(objectiveCompleteEvent);
                });
      } else {
        Bukkit.getPluginManager().callEvent(objectiveCompleteEvent);
      }

      if (objectiveCompleteEvent.isCancelled()) {
        return;
      }
      final Player player = Bukkit.getPlayer(questPlayer.getUniqueId());
      // Now execute the objective reward actions:
      String fullRewardString = "";
      int counterWithRewardNames = 0;
      for (Action rewardAction : activeObjective.getObjective().getRewards()) {
        questPlayer.sendDebugMessage("Executing a rewardAction for an objective");
        main.getActionManager()
            .executeActionWithConditions(rewardAction, questPlayer, null, true, getQuest());

        if (main.getConfiguration().showRewardsAfterObjectiveCompletion) {
          if (!rewardAction.getActionName().isBlank()) {
            counterWithRewardNames++;
            if (counterWithRewardNames == 1) {
              fullRewardString +=
                  main.getLanguageManager()
                      .getString(
                          "chat.objectives.successfully-completed-rewards-prefix",
                          player,
                          this,
                          activeObjective,
                          rewardAction);
            }
            fullRewardString +=
                "\n"
                    + main.getLanguageManager()
                        .getString(
                            "chat.objectives.successfully-completed-rewards-rewardformat",
                            player,
                            this,
                            activeObjective,
                            rewardAction,
                            Map.of("%reward%", rewardAction.getActionName()));
          }
        }
      }

      if (counterWithRewardNames > 0) {
        fullRewardString +=
            "\n"
                + main.getLanguageManager()
                    .getString(
                        "chat.objectives.successfully-completed-rewards-suffix",
                        player,
                        this,
                        activeObjective);
      }

      if (!silent) {

        if (player != null) {
          player.playSound(
              player.getLocation(), Sound.BLOCK_ANVIL_LAND, SoundCategory.MASTER, 75, 1.4f);

          if (fullRewardString.isBlank()) {
            questPlayer.sendMessage(
                main.getLanguageManager()
                    .getString(
                        "chat.objectives.successfully-completed",
                        questPlayer.getPlayer(),
                        this,
                        activeObjective));
          } else {
            main.sendMessage(
                player,
                main.getLanguageManager()
                        .getString(
                            "chat.objectives.successfully-completed",
                            questPlayer.getPlayer(),
                            this,
                            activeObjective)
                    + "<RESET>"
                    + fullRewardString);
          }
        }
      }
    }

    completedObjectives.add(activeObjective);

    // Add to completed Objectives list. This list will then be used in removeCompletedObjectives()
    // to remove all its contests also from the activeObjectives lists
    // (Without a concurrentmodificationexception)
    toRemove.add(activeObjective);
  }

  public void removeCompletedObjectives(final boolean notifyPlayer) {
    if (main.getDataManager().isDisabled()) {
      return;
    }
    if (toRemove.size() == 0) {
      return;
    }
    questPlayer.sendDebugMessage("Executing removeCompletedObjectives");

    activeObjectives.removeAll(toRemove);
    toRemove.clear();

    // Other active objectives might be unlocked if this objective is completed. This will re-check
    // them all. (This is either due to a dependency or OtherQuest condition (for v3))
    for (final ActiveObjective activeObjectiveToCheckForIfUnlocked : activeObjectives) {
      activeObjectiveToCheckForIfUnlocked.updateUnlocked(notifyPlayer, true);
    }

    if (activeObjectives.size() == 0) {
      questPlayer.notifyActiveQuestCompleted(this);
    }
  }

  public void fail() {

    QuestFailEvent questFailEvent = new QuestFailEvent(getQuestPlayer(), this);
    if (Bukkit.isPrimaryThread()) {
      Bukkit.getScheduler()
          .runTaskAsynchronously(
              main.getMain(), () -> Bukkit.getPluginManager().callEvent(questFailEvent));
    } else {
      Bukkit.getPluginManager().callEvent(questFailEvent);
    }

    if (questFailEvent.isCancelled()) {
      return;
    }

    questPlayer.sendMessage(
        main.getLanguageManager().getString("chat.quest-failed", questPlayer.getPlayer(), this));

    for (final ActiveObjective activeObjective : getActiveObjectives()) {
      getQuestPlayer().disableTrackingObjective(activeObjective);
      if (activeObjective.getObjective() instanceof EscortNPCObjective) {
        if (main.getIntegrationsManager().isCitizensEnabled()
            && main.getIntegrationsManager().getCitizensManager() != null) {
          main.getIntegrationsManager().getCitizensManager().handleEscortObjective(activeObjective);
        }
      }
    }
  }

  public void updateObjectivesUnlocked(
      final boolean sendUpdateObjectivesUnlocked, final boolean triggerAcceptQuestTrigger) {
    for (final ActiveObjective activeObjective : activeObjectives) {
      activeObjective.updateUnlocked(sendUpdateObjectivesUnlocked, triggerAcceptQuestTrigger);
    }
  }

  public final ActiveObjective getActiveObjectiveFromID(final int objectiveID) {
    for (final ActiveObjective objective : activeObjectives) {
      if (objective.getObjectiveID() == objectiveID) {
        return objective;
      }
    }
    return null;
  }

  public final String getQuestName() {
    return quest.getQuestName();
  }
}
