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

package rocks.gravili.notquests.paper.structs.triggers;

import rocks.gravili.notquests.paper.structs.ActiveQuest;

public class ActiveTrigger {
  private final Trigger trigger;
  private final ActiveQuest activeQuest;
  private final int triggerID;
  private long currentProgress;

  public ActiveTrigger(int triggerID, Trigger trigger, ActiveQuest activeQuest) {
    this.triggerID = triggerID;
    this.trigger = trigger;
    this.activeQuest = activeQuest;
  }

  public final Trigger getTrigger() {
    return trigger;
  }

  public boolean isCompleted() {
    return currentProgress >= trigger.getAmountNeeded();
  }

  public final long getCurrentProgress() {
    return currentProgress;
  }

  public void setCurrentProgress(long newCurrentProgress) {
    this.currentProgress = newCurrentProgress;
  }

  public void addProgress(long progressToAdd) {
    setCurrentProgress(getCurrentProgress() + progressToAdd);
  }

  public void addAndCheckTrigger(ActiveQuest activeQuest) {
    addProgress(1);
    if (isCompleted()) {
      activeQuest
          .getQuestPlayer()
          .sendDebugMessage(
              "Trigger: Triggering trigger "
                  + trigger.getTriggerType()
                  + " for Quest "
                  + activeQuest.getQuest().getIdentifier());
      trigger.trigger(activeQuest);
    }
  }

  public final int getTriggerID() {
    return triggerID;
  }

  public void addProgressSilent(long progressToAdd) {
    setCurrentProgress(getCurrentProgress() + progressToAdd);
  }

  public final ActiveQuest getActiveQuest() {
    return activeQuest;
  }
}
