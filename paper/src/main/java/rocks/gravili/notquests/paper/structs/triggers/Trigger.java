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

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.Action;

public abstract class Trigger {
  protected final NotQuests main;
  private Quest quest = null;
  private int triggerID = 1;
  private Action action = null;
  private int applyOn =
      0; // 0 is for the whole quest. Positive numbers = objectives (JUST INTERNALLY HERE, NOT IN
         // THE ADMIN COMMAND)
  private String worldName = "ALL";
  private long amountNeeded = 0; // 0 or 1 means every trigger() triggers it
  private Category category;

  public Trigger(final NotQuests main) {
    this.main = main;
    category = main.getDataManager().getDefaultCategory();
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public void setAction(final Action action) {
    this.action = action;
  }

  public final Quest getQuest() {
    return quest;
  }

  public void setQuest(final Quest quest) {
    this.quest = quest;
  }

  public final int getTriggerID() {
    return triggerID;
  }

  public void setTriggerID(final int triggerID) {
    this.triggerID = triggerID;
  }

  public final String getTriggerType() {
    return main.getTriggerManager().getTriggerType(this.getClass());
  }

  public final String getWorldName() {
    return worldName;
  }

  public void setWorldName(final String worldName) {
    this.worldName = worldName;
  }

  public final Action getTriggerAction() {
    return action;
  }

  public final int getApplyOn() {
    return applyOn;
  }

  public void setApplyOn(final int applyOn) {
    this.applyOn = applyOn;
  }

  public final long getAmountNeeded() {
    return amountNeeded;
  }

  public void setAmountNeeded(final long amountNeeded) {
    this.amountNeeded = amountNeeded;
  }

  public void trigger(ActiveQuest activeQuest) { // or void completeTrigger() or finishTrigger()
    // execute action here
    final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUniqueId());

    if (player != null) {
      activeQuest
          .getQuestPlayer()
          .sendDebugMessage(
              "Trigger: Executing action "
                  + action.getActionName()
                  + " for Quest "
                  + activeQuest.getQuest().getIdentifier());
      if (quest != null) {
        main.getActionManager()
            .executeActionWithConditions(action, activeQuest.getQuestPlayer(), null, true, quest);
      } else {
        main.getActionManager()
            .executeActionWithConditions(action, activeQuest.getQuestPlayer(), null, true);
      }
    } else {
      main.getLogManager().warn("Tried to execute trigger for offline player - ABORTED!");
    }
  }

  public abstract void save(final FileConfiguration configuration, final String initialPath);

  public abstract void load(final FileConfiguration configuration, final String initialPath);

  public abstract String getTriggerDescription();
}
