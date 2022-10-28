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

package rocks.gravili.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveObjectiveHolder;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ObjectiveUnlockEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final QuestPlayer questPlayer;
  private final ActiveObjective activeObjective;
  private final ActiveObjectiveHolder activeObjectiveHolder;
  private final boolean triggerAcceptQuestTrigger;

  private boolean isCancelled;

  public ObjectiveUnlockEvent(
      final QuestPlayer questPlayer,
      final ActiveObjective activeObjective,
      final ActiveObjectiveHolder activeObjectiveHolder,
      final boolean triggerAcceptQuestTrigger) {
    super(true);

    this.questPlayer = questPlayer;
    this.activeObjective = activeObjective;
    this.activeObjectiveHolder = activeObjectiveHolder;
    this.triggerAcceptQuestTrigger = triggerAcceptQuestTrigger;

    this.isCancelled = false;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @Override
  public boolean isCancelled() {
    return this.isCancelled;
  }

  @Override
  public void setCancelled(boolean isCancelled) {
    this.isCancelled = isCancelled;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public QuestPlayer getQuestPlayer() {
    return this.questPlayer;
  }

  public ActiveObjective getActiveObjective() {
    return this.activeObjective;
  }

  public ActiveObjectiveHolder getActiveObjectiveHolder() {
    return this.activeObjectiveHolder;
  }

  public final boolean isTriggerAcceptQuestTrigger() {
    return triggerAcceptQuestTrigger;
  }
}
