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


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.events.notquests.ObjectiveUnlockEvent;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.OtherQuestObjective;

import java.util.UUID;

/**
 * This is a special object for active objectives. Apart from the main Objective object which stores information about what defines the objective itself,
 * it contains other information like the ActiveQuest for which the objective is (the ActiveQuest object ALSO stores this ActiveObjective object, so they
 * can access each other).
 * <p>
 * It also contains the progress and information about if this active objective has been completed - because there is NO CompletedObjective object. Completed
 * objectives will still be instances of the ActiveObjective class.
 *
 * @author Alessio Gravili
 */
public class ActiveObjective {
    private final NotQuests main;
    private final Objective objective;
    private final ActiveQuest activeQuest;
    private final int objectiveID;
    private long currentProgress;
    private boolean unlocked = false;
    private boolean hasBeenCompleted = false;

    public ActiveObjective(final NotQuests main, final int objectiveID, final Objective objective, final ActiveQuest activeQuest) {
        this.main = main;
        this.objectiveID = objectiveID;
        this.objective = objective;
        this.activeQuest = activeQuest;
        currentProgress = 0;

    }

    public final void setUnlocked(final boolean unlocked, final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {
        if (this.unlocked != unlocked) {
            getQuestPlayer().sendDebugMessage("Changed objective unlock status to " + unlocked);


            this.unlocked = unlocked;
            if (unlocked) {

                ObjectiveUnlockEvent objectiveUnlockEvent = new ObjectiveUnlockEvent(getQuestPlayer(), this, activeQuest, triggerAcceptQuestTrigger);
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                        Bukkit.getPluginManager().callEvent(objectiveUnlockEvent);
                    });
                } else {
                    Bukkit.getPluginManager().callEvent(objectiveUnlockEvent);
                }

                if (objectiveUnlockEvent.isCancelled()) {
                    return;
                }

                objective.onObjectiveUnlock(this);

                getQuestPlayer().setTrackingObjective(this);




                if (objective instanceof EscortNPCObjective escortNPCObjective) {
                    if (main.getIntegrationsManager().isCitizensEnabled()) {
                        main.getIntegrationsManager().getCitizensManager().handleEscortNPCObjectiveForActiveObjective(escortNPCObjective, activeQuest);
                    }

                }


                //TODO: What?
                if (objective instanceof OtherQuestObjective otherQuestObjective) {
                    if (otherQuestObjective.isCountPreviousCompletions()) {
                        for (CompletedQuest completedQuest : getQuestPlayer().getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(otherQuestObjective.getOtherQuest())) {
                                addProgress(1, -1);
                            }
                        }
                    }
                }
                if (notifyPlayer) {
                    final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                    if (player != null) {
                        main.getQuestManager().sendActiveObjective(player, this);
                    }
                }

            }
        }

    }

    public final boolean isUnlocked() {
        return unlocked;
    }

    public void updateUnlocked(final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {
        getQuestPlayer().sendDebugMessage("Updating if objective is unlocked...");

        boolean foundStillFalseConditions = false;
        for (final Condition condition : objective.getConditions()){
            String check = condition.check(getQuestPlayer());
            getQuestPlayer().sendDebugMessage("Condition status for " + objective.getObjectiveFinalName() + ": " + check);

            if(!check.isBlank()) {
                foundStillFalseConditions = true;
                getQuestPlayer().sendDebugMessage("Following objective condition is still unfinished: " + condition.getConditionDescription(getQuestPlayer().getPlayer()));
                setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
            }

            if (foundStillFalseConditions) {
                break;
            }
        }
        if (!foundStillFalseConditions) {
            getQuestPlayer().sendDebugMessage("Active objective " + objective.getObjectiveFinalName() + " has been set to unlocked!");
            setUnlocked(true, notifyPlayer, triggerAcceptQuestTrigger);

        }


    }


    public final Objective getObjective() {
        return objective;
    }

    public final long getProgressNeeded() {
        return objective.getProgressNeeded();
    }

    public final long getCurrentProgress() {
        return currentProgress;
    }


    public void addProgress(long progressToAdd) {
        addProgress(progressToAdd, -1, null, false);
    }
    public void addProgress(long progressToAdd, boolean silent) {
        addProgress(progressToAdd, -1, null, silent);
    }
    //For Citizens NPCs
    public void addProgress(long progressToAdd, final int NPCID) {
        addProgress(progressToAdd, NPCID, null, false);
    }
    public void addProgress(long progressToAdd, final int NPCID, final boolean silent) {
        addProgress(progressToAdd, NPCID, null, silent);

    }
    //For Armor Stands
    public void addProgress(long progressToAdd, final UUID armorStandUUID) {
        addProgress(progressToAdd, -1, armorStandUUID, false);
    }
    public void addProgress(long progressToAdd, final UUID armorStandUUID, final boolean silent) {
        addProgress(progressToAdd, -1, armorStandUUID, silent);
    }

    public void addProgress(long progressToAdd, final int NPCID, final UUID armorStandUUID, boolean silent) {
        currentProgress += progressToAdd;
        getQuestPlayer().setTrackingObjective(this);

        if(main.getConfiguration().isVisualObjectiveTrackingShowProgressInActionBar()){
            getQuestPlayer().sendObjectiveProgress(this);
        }

        if (isCompleted(armorStandUUID)) {
            setHasBeenCompleted(true);
            if(armorStandUUID != null){
                activeQuest.notifyActiveObjectiveCompleted(this, silent, armorStandUUID);
            }else{
                activeQuest.notifyActiveObjectiveCompleted(this, silent, NPCID);
            }
        }
        getQuestPlayer().sendDebugMessage("+" + progressToAdd + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>. Silent: " + silent);
    }



    public void removeProgress(int i, boolean capAtZero) {
        if (capAtZero) {
            if (currentProgress - i < 0) {
                if (currentProgress > 0) {
                    currentProgress = 0;
                }
            } else {
                currentProgress -= i;
            }
        } else {
            currentProgress -= i;
        }

        getQuestPlayer().sendDebugMessage("-" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>.");

    }

    //For Citizens NPCs
    public final boolean isCompleted(final int NPCID) {
        if (getObjective().getCompletionNPCID() == -1 || getObjective().getCompletionNPCID() == NPCID) {
            return currentProgress >= objective.getProgressNeeded();
        } else {
            return false;
        }

    }

    //For Armor Stands
    public final boolean isCompleted(final UUID armorStandUUID) {
        if (getObjective().getCompletionArmorStandUUID() == null || getObjective().getCompletionArmorStandUUID().equals(armorStandUUID)) {
            return currentProgress >= objective.getProgressNeeded();
        } else {
            return false;
        }

    }

    public final QuestPlayer getQuestPlayer() {
        return activeQuest.getQuestPlayer();
    }

    public final ActiveQuest getActiveQuest() {
        return activeQuest;
    }

    public final int getObjectiveID() {
        return objectiveID;
    }

    public final boolean hasBeenCompleted() {
        return hasBeenCompleted;
    }

    public void setHasBeenCompleted(final boolean hasBeenCompleted) {
        // System.out.println("§4§lSet has been completed to: §b" + hasBeenCompleted + " §cfor objective with ID §b" + getObjectiveID());
        this.hasBeenCompleted = hasBeenCompleted;
        if(hasBeenCompleted){
            getQuestPlayer().disableTrackingObjective(this);
        }
    }
}
