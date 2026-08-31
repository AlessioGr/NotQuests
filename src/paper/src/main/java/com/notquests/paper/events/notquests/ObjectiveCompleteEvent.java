package com.notquests.paper.events.notquests;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.notquests.core.objectives.Objective;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;
import com.notquests.paper.PaperPlayer;

public class ObjectiveCompleteEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private final PaperPlayer questPlayer;
  private final String questName;
  private final int[] objectivePath;
  private final int objectiveId;
  private final String objectiveHolderPath;
  private final Quest quest;
  private final ActiveObjective activeObjective;
  private boolean isCancelled;

  public ObjectiveCompleteEvent(
      final PaperPlayer questPlayer,
      final String questName,
      final int[] objectivePath,
      final int objectiveId,
      final String objectiveHolderPath,
      final Quest quest,
      final ActiveObjective activeObjective) {
    super(false);

    this.questPlayer = questPlayer;
    this.questName = questName == null ? "" : questName;
    this.objectivePath = objectivePath == null ? new int[0] : objectivePath.clone();
    this.objectiveId = objectiveId;
    this.objectiveHolderPath = objectiveHolderPath == null ? "" : objectiveHolderPath;
    this.quest = quest;
    this.activeObjective = activeObjective;

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

  public int[] getObjectivePath() {
    return this.objectivePath.clone();
  }

  /** Returns the objective ID at the end of {@link #getObjectivePath()}, or {@code 0}. */
  public int getObjectiveId() {
    return this.objectiveId;
  }

  /** Returns the configured objective entry represented by this event, when available. */
  @Nullable
  public Objective getObjective() {
    return this.activeObjective == null ? null : this.activeObjective.getObjective();
  }

  /** Returns the immutable core storage path of the objective holder. */
  public String getObjectiveHolderPath() {
    return this.objectiveHolderPath;
  }
}
