package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.notquests.paper.PaperPlayer;

public class QuestPointsChangeEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final PaperPlayer questPlayer;
  private final long newQuestPointsAmount;
  private boolean isCancelled;

  public QuestPointsChangeEvent(final PaperPlayer questPlayer, final long newQuestPointsAmount) {
    super(false);

    this.questPlayer = questPlayer;
    this.newQuestPointsAmount = newQuestPointsAmount;

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

  public long getNewQuestPointsAmount() {
    return this.newQuestPointsAmount;
  }
}
