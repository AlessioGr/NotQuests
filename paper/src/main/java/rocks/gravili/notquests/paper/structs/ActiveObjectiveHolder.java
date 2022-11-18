package rocks.gravili.notquests.paper.structs;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.events.notquests.ObjectiveCompleteEvent;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveObjective;

public abstract class ActiveObjectiveHolder {
  private final CopyOnWriteArrayList<ActiveObjective> activeObjectives;
  private final ArrayList<ActiveObjective> completedObjectives;
  private final ArrayList<ActiveObjective> toRemove;

  private final ObjectiveHolder objectiveHolder;

  private final NotQuests main;
  private final QuestPlayer questPlayer;

  private final int level; //Level in the hierarchy. Quest = 0. 1. sub-objective = 1. etc.
  public ActiveObjectiveHolder(final NotQuests main, final QuestPlayer questPlayer, final ObjectiveHolder objectiveHolder, final int level){
    this.main = main;
    this.objectiveHolder = objectiveHolder;
    this.questPlayer = questPlayer;
    this.level = level;

    activeObjectives = new CopyOnWriteArrayList<>();
    completedObjectives = new ArrayList<>();
    toRemove = new ArrayList<>();

    int objectiveID = 1;
    for (final Objective objective : objectiveHolder.getObjectives()) {
      ActiveObjective activeObjective = new ActiveObjective(main, objectiveID, objective, this);
      getActiveObjectives().add(activeObjective);
      objectiveID++;
    }
  }

  public final int getLevel(){
    return level;
  }

  public final QuestPlayer getQuestPlayer(){
    return questPlayer;
  }

  public final NotQuests getMain(){
    return main;
  }

  public final CopyOnWriteArrayList<ActiveObjective> getActiveObjectives(){
    return this.activeObjectives;
  }

  public final ArrayList<ActiveObjective> getCompletedObjectives(){
    return this.completedObjectives;
  }
  public final ArrayList<ActiveObjective> getToRemoveObjectives(){
    return this.toRemove;
  }

  public final ObjectiveHolder getObjectiveHolder(){
    return objectiveHolder;
  }

  public final boolean isCompleted() {
    return getActiveObjectives().isEmpty();
  }

  public void removeCompletedObjectives(final boolean notifyPlayer) {
    if (main.getDataManager().isDisabled()) {
      return;
    }
    if (toRemove.isEmpty()) {
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

    if (activeObjectives.isEmpty() && this instanceof ActiveQuest activeQuest) {
      questPlayer.notifyActiveQuestCompleted(activeQuest);
    }

    if(this instanceof final ActiveObjective activeObjective1){
      if(!activeObjective1.isCompleted(null) && activeObjective1.getObjective() instanceof ObjectiveObjective){
        if(activeObjective1.isCompleted()){
          activeObjective1.setProgress(activeObjective1.getProgressNeeded(), false);
          activeObjective1.getActiveObjectiveHolder().removeCompletedObjectives(notifyPlayer);
          activeObjective1.getQuestPlayer().removeCompletedQuests();
        }

      }
    }
  }

  public void notifyActiveObjectiveCompleted(
      final ActiveObjective activeObjective,
      final boolean silent,
      final NQNPC nqnpc) {

    if (!getMain().getDataManager().isCurrentlyLoading() && !getQuestPlayer().isCurrentlyLoading()) {
      main.getLogManager().debug("notifyActiveObjectiveCompleted: getDataManager().isCurrentlyLoading(): " + getMain().getDataManager().isCurrentlyLoading() + " getQuestPlayer().isCurrentlyLoading(): " + getQuestPlayer().isCurrentlyLoading());
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
      for (final Action rewardAction : activeObjective.getObjective().getRewards()) {
        questPlayer.sendDebugMessage("Executing a rewardAction for an objective");
        main.getActionManager()
            .executeActionWithConditions(rewardAction, questPlayer, null, true, getObjectiveHolder());

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
}
