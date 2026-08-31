package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.structs.Quest;
import com.notquests.paper.PaperPlayer;

public class QuestCompletedEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final PaperPlayer questPlayer;
  private final String questName;
  private final Quest quest;
  private final boolean forced;
  private boolean isCancelled;

  public QuestCompletedEvent(
      final PaperPlayer questPlayer,
      final String questName,
      final Quest quest,
      final boolean forced) {
    super(false);

    this.questPlayer = questPlayer;
    this.questName = questName == null ? "" : questName;
    this.quest = quest;
    this.forced = forced;

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

  public PaperPlayer getPaperPlayer() {
    return this.questPlayer;
  }

  public String getQuestName() {
    return this.questName;
  }

  /** Returns the core-owned configured quest, when it was available at dispatch time. */
  @Nullable
  public Quest getQuest() {
    return this.quest;
  }

  public final boolean isForced() {
    return forced;
  }
}
