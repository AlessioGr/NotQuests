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


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.Events.notquests.ObjectiveUnlockEvent;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Objectives.EscortNPCObjective;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Objectives.OtherQuestObjective;

import java.util.ArrayList;
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
            this.unlocked = unlocked;
            if (unlocked) {

                ObjectiveUnlockEvent objectiveUnlockEvent = new ObjectiveUnlockEvent(getQuestPlayer(), this, activeQuest, triggerAcceptQuestTrigger);
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                        Bukkit.getPluginManager().callEvent(objectiveUnlockEvent);
                    });
                } else {
                    Bukkit.getPluginManager().callEvent(objectiveUnlockEvent);
                }

                if (objectiveUnlockEvent.isCancelled()) {
                    return;
                }

                objective.onObjectiveUnlock(this);

                if (objective instanceof EscortNPCObjective escortNPCObjective) {
                    if (main.isCitizensEnabled()) {
                        activeQuest.getCitizensHandler().handleEscortNPCObjectiveForActiveObjective(escortNPCObjective, activeQuest);
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

        boolean foundStillDependant = false;
        for (final Objective dependantObjective : objective.getDependantObjectives()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjectiveID() == dependantObjective.getObjectiveID()) {
                    foundStillDependant = true;
                    if (!isUnlocked()) {
                        setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                    }

                    break;
                }
            }
            if (foundStillDependant) {
                break;
            }
        }
        if (!foundStillDependant) {
            setUnlocked(true, notifyPlayer, triggerAcceptQuestTrigger);

        }


    }

    public final ArrayList<ActiveObjective> getObjectivesWhichStillNeedToBeCompletedBeforeUnlock() {
        final ArrayList<ActiveObjective> stillDependantObjectives = new ArrayList<>();
        for (final Objective dependantObjective : objective.getDependantObjectives()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjectiveID() == dependantObjective.getObjectiveID()) {
                    stillDependantObjectives.add(activeObjective);
                    break;
                }
            }


        }

        return stillDependantObjectives;
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

    //For Citizens NPCs
    public void addProgress(long i, final int NPCID) {
        currentProgress += i;
        if (isCompleted(NPCID)) {
            getQuestPlayer().sendDebugMessage("Objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient> has been registered as completed.");
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, false, NPCID);
        }
        getQuestPlayer().sendDebugMessage("+" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>.");
    }

    //For Armor Stands
    public void addProgress(long i, final UUID armorStandUUID) {
        currentProgress += i;
        if (isCompleted(armorStandUUID)) {
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, false, armorStandUUID);
        }
        getQuestPlayer().sendDebugMessage("+" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>.");
    }

    //For Citizens NPCs
    public void addProgressSilent(long i, final int NPCID) {
        currentProgress += i;
        if (isCompleted(NPCID)) {
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, true, NPCID);
        }
        getQuestPlayer().sendDebugMessage("[Silent] +" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>.");
    }

    //For Armor Stands
    public void addProgressSilent(long i, final UUID armorStandUUID) {
        currentProgress += i;
        if (isCompleted(armorStandUUID)) {
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, true, armorStandUUID);
        }
        getQuestPlayer().sendDebugMessage("[Silent] +" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getObjectiveFinalName() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveQuest().getQuest().getQuestFinalName() + "</gradient>.");
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
    }
}
