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


import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.events.notquests.ObjectiveUnlockEvent;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.OtherQuestObjective;

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
public class ActiveObjective extends ActiveObjectiveHolder {
    private final NotQuests main;
    private final Objective objective;
    private final ActiveObjectiveHolder activeObjectiveHolder;
    private final int objectiveID;
    private double currentProgress;
    private boolean unlocked = false;
    private boolean hasBeenCompleted = false;

    private double progressNeeded;

    public ActiveObjective(final NotQuests main, final int objectiveID, final Objective objective, final ActiveObjectiveHolder activeObjectiveHolder) {
        super(main, activeObjectiveHolder.getQuestPlayer(), objective, activeObjectiveHolder.getLevel()+1);

        this.main = main;
        this.objectiveID = objectiveID;
        this.objective = objective;
        this.activeObjectiveHolder = activeObjectiveHolder;
        this.currentProgress = 0;

        this.progressNeeded = objective.getProgressNeededExpression().calculateValue(activeObjectiveHolder.getQuestPlayer());
    }

    public final double getProgressNeeded() {
        return progressNeeded;
    }

    public void setProgressNeeded(final double progressNeeded){
        this.progressNeeded = progressNeeded;
    }

    public final void setUnlocked(final boolean unlocked, final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {
        if (this.unlocked != unlocked) {
            getQuestPlayer().sendDebugMessage("Changed objective unlock status to " + unlocked);


            this.unlocked = unlocked;
            if (unlocked) {

                ObjectiveUnlockEvent objectiveUnlockEvent = new ObjectiveUnlockEvent(getQuestPlayer(), this, activeObjectiveHolder, triggerAcceptQuestTrigger);
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> Bukkit.getPluginManager().callEvent(objectiveUnlockEvent));
                } else {
                    Bukkit.getPluginManager().callEvent(objectiveUnlockEvent);
                }

                if (objectiveUnlockEvent.isCancelled()) {
                    return;
                }

                objective.onObjectiveUnlock(this, main.getDataManager().isCurrentlyLoading() || getQuestPlayer().isCurrentlyLoading());

                getQuestPlayer().setTrackingObjective(this);



                //TODO: What?
                if (objective instanceof final OtherQuestObjective otherQuestObjective) {
                    if (otherQuestObjective.isCountPreviousCompletions()) {
                        for (CompletedQuest completedQuest : getQuestPlayer().getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(otherQuestObjective.getOtherQuest())) {
                                addProgress(1, (NQNPC) null);
                            }
                        }
                    }
                }
                if (notifyPlayer) {
                    main.getQuestManager().sendActiveObjective(getQuestPlayer(), this, 0);
                }

            }else{
                objective.onObjectiveCompleteOrLock(this, main.getDataManager().isCurrentlyLoading() || getQuestPlayer().isCurrentlyLoading() , isCompleted((NQNPC) null));
            }
        }

    }

    public final boolean isUnlocked() {
        return unlocked;
    }

    public void updateUnlocked(final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {
        getQuestPlayer().sendDebugMessage("Updating if objective is unlocked...");

        //First check the quests PredefinedProgressOrder
        final PredefinedProgressOrder predefinedProgressOrder = activeObjectiveHolder.getObjectiveHolder().getPredefinedProgressOrder();
        if(predefinedProgressOrder != null){
            if(predefinedProgressOrder.isFirstToLast()){
                for(final Objective objective1 : activeObjectiveHolder.getObjectiveHolder().getObjectives()){
                    if(objective1.getObjectiveID() < objectiveID){
                        if(activeObjectiveHolder.getActiveObjectiveFromID(objective1.getObjectiveID()) != null){
                            getQuestPlayer().sendDebugMessage("Active objective locked due to firstToLast PredefinedProgressOrder: BecauseActive objective with ID " + objective1.getObjectiveID() + " is still active.");
                            setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                            return;
                        }
                    }
                }
            }else if(predefinedProgressOrder.isLastToFirst()){
                for(final Objective objective1 : activeObjectiveHolder.getObjectiveHolder().getObjectives()){
                    if(objective1.getObjectiveID() > objectiveID){
                        if(activeObjectiveHolder.getActiveObjectiveFromID(objective1.getObjectiveID()) != null){
                            getQuestPlayer().sendDebugMessage("Active objective locked due to lastToFirst PredefinedProgressOrder: BecauseActive objective with ID " + objective1.getObjectiveID() + " is still active.");
                            setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                            return;
                        }
                    }
                }
            }else if(predefinedProgressOrder.getCustomOrder() != null && !predefinedProgressOrder.getCustomOrder().isEmpty()){

                for(final String objectiveIDToCheckString : predefinedProgressOrder.getCustomOrder()){
                    final int objectiveIDToCheck = Integer.parseInt(objectiveIDToCheckString);
                    if(objectiveIDToCheck == objectiveID){
                        break;
                    }
                    if(activeObjectiveHolder.getActiveObjectiveFromID(objectiveIDToCheck) != null){
                        getQuestPlayer().sendDebugMessage("Active objective locked due to custom PredefinedProgressOrder: BecauseActive objective with ID " + objectiveIDToCheck + " is still active.");
                        setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                        return;
                    }
                }
            }
        }

        for (final Condition condition : objective.getUnlockConditions()){
            ConditionResult check = condition.check(getQuestPlayer());
            getQuestPlayer().sendDebugMessage("Condition status for " + objective.getDisplayNameOrIdentifier() + ": " + check.message());

            if(!check.fulfilled()) {
                getQuestPlayer().sendDebugMessage("Following objective unlock condition is still unfinished (there may be more than what's listed here): " + condition.getConditionDescription(getQuestPlayer()));
                setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                return;
            }
        }


        //If it didn't return; and reaches this, it means all conditions are met!
        getQuestPlayer().sendDebugMessage("Active objective " + objective.getDisplayNameOrIdentifier() + " has been set to unlocked!");
        setUnlocked(true, notifyPlayer, triggerAcceptQuestTrigger);

        updateObjectivesUnlocked(notifyPlayer, triggerAcceptQuestTrigger);

    }

    public final boolean canProgress(final boolean checkForProgressDecrease) {
        getQuestPlayer().sendDebugMessage("Checking if objective can progress...");


        //Make it so it cannot progress if sub-objectives aren't completed! //TODO: Maybe make this configurable
        if(!getActiveObjectives().isEmpty()){
            return false;
        }

        for (final Condition condition : objective.getProgressConditions()){
            if(checkForProgressDecrease && condition.isObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled()) {
                continue;
            }
            ConditionResult check = condition.check(getQuestPlayer());
            getQuestPlayer().sendDebugMessage("Condition status for " + objective.getDisplayNameOrIdentifier() + " and condition " + condition.getConditionType() + ": " + check.message());

            if(!check.fulfilled()) {
                getQuestPlayer().sendDebugMessage("Following objective progress condition is still unfinished (there may be more than what's listed here): " + condition.getConditionDescription(getQuestPlayer()));
                return false;
            }
        }
        //If it didn't return; and reaches this, it means all conditions are met!
        getQuestPlayer().sendDebugMessage("Active objective " + objective.getDisplayNameOrIdentifier() + " can progress!");
        return true;
    }

    public final boolean canComplete() {
        getQuestPlayer().sendDebugMessage("Checking if objective can be completed...");

        for (final Condition condition : objective.getCompleteConditions()){
            ConditionResult check = condition.check(getQuestPlayer());
            getQuestPlayer().sendDebugMessage("Condition status for " + objective.getDisplayNameOrIdentifier() + ": " + check.message());

            if(!check.fulfilled()) {
                getQuestPlayer().sendDebugMessage("Following objective complete condition is still unfinished (there may be more than what's listed here): " + condition.getConditionDescription(getQuestPlayer()));
                return false;
            }
        }
        //If it didn't return; and reaches this, it means all conditions are met!
        getQuestPlayer().sendDebugMessage("Active objective " + objective.getDisplayNameOrIdentifier() + " can complete!");
        return true;
    }


    public final Objective getObjective() {
        return objective;
    }

    public final double getCurrentProgress() {
        return currentProgress;
    }


    public void setProgress(double newProgress, final boolean capAtZero){
        if(newProgress == currentProgress){
            return;
        }
        if(newProgress > currentProgress){
            addProgress(newProgress - currentProgress);
        } else {
            removeProgress(currentProgress - newProgress, capAtZero);
        }
    }
    public void addProgress(double progressToAdd) {
        addProgress(progressToAdd, null, false);
    }
    public void addProgress(double progressToAdd, boolean silent) {
        addProgress(progressToAdd, null, silent);
    }
    //For Citizens NPCs
    public void addProgress(double progressToAdd, final NQNPC nqnpc) {
        addProgress(progressToAdd, nqnpc, false);
    }

    public void addProgress(double progressToAdd, final NQNPC nqnpc, boolean silent) {
        if(main.getDataManager().isDisabled() || !canProgress(false)){
            return;
        }
        currentProgress += progressToAdd;
        getQuestPlayer().setTrackingObjective(this);


        if ( (isCompleted(nqnpc))) {
            getQuestPlayer().sendDebugMessage("Objective completed: " + NotQuestColors.debugHighlightGradient + getObjective().getDisplayNameOrIdentifier() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveObjectiveHolder().getObjectiveHolder().getDisplayNameOrIdentifier() + "</gradient>. Silent: " + silent);

            setHasBeenCompleted(true);
            activeObjectiveHolder.notifyActiveObjectiveCompleted(this, silent, nqnpc);
        }
        if(main.getConfiguration().isDebug()){
            main.getLogManager().debug("+" + progressToAdd + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getDisplayNameOrIdentifier() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveObjectiveHolder().getObjectiveHolder().getDisplayNameOrIdentifier() + "</gradient>. Silent: " + silent);
        }
        getQuestPlayer().sendDebugMessage("+" + progressToAdd + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getDisplayNameOrIdentifier() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveObjectiveHolder().getObjectiveHolder().getDisplayNameOrIdentifier() + "</gradient>. Silent: " + silent);
    }



    public void removeProgress(double i, boolean capAtZero) {
        if(main.getDataManager().isDisabled()){
            return;
        }
        if(i < 0) {
            main.getLogManager().severe("Tried to remove negative progress (=> add progress) from objective " + getObjective().getDisplayNameOrIdentifier() + " of quest " + getActiveObjectiveHolder().getObjectiveHolder().getDisplayNameOrIdentifier() + "!");
            return;
        }

        //Setting the first argument to true only checks for progress decrease. If the "--allowProgressDecreaseIfNotFulfilled" flag is set for that condition, it would be skipped
        if(!canProgress(true)) {
            return;
        }

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

        getQuestPlayer().sendDebugMessage("-" + i + " progress for objective " + NotQuestColors.debugHighlightGradient + getObjective().getDisplayNameOrIdentifier() + "</gradient> of quest " + NotQuestColors.debugHighlightGradient + getActiveObjectiveHolder().getObjectiveHolder().getDisplayNameOrIdentifier() + "</gradient>.");

    }

    public final boolean isCompleted(@Nullable final NQNPC nqnpc) {
        if (getObjective().getCompletionNPC() == null || getObjective().getCompletionNPC().equals(nqnpc)) {
            return canComplete() && currentProgress >= getProgressNeeded() && super.isCompleted();
        } else {
            return false;
        }

    }

    public final ActiveObjectiveHolder getActiveObjectiveHolder() {
        return activeObjectiveHolder;
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
            objective.onObjectiveCompleteOrLock(this, main.getDataManager().isCurrentlyLoading() || getQuestPlayer().isCurrentlyLoading(), true);
        }
    }
}
