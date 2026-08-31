package com.notquests.paper.events.notquests;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.notquests.paper.NotQuests;

public class NotQuestsFullyLoadedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();
  private final NotQuests main;

  public NotQuestsFullyLoadedEvent(final NotQuests main) {
    this.main = main;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public final NotQuests getNotQuests() {
    return this.main;
  }
}
