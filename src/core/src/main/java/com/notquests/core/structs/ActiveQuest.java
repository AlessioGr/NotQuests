package com.notquests.core.structs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** One accepted quest and the active-objective tree that belongs to it. */
public final class ActiveQuest {
  private final String questIdentifier;
  private final CopyOnWriteArrayList<ActiveObjective> activeObjectives = new CopyOnWriteArrayList<>();
  private Quest quest;

  public ActiveQuest(final String questIdentifier) {
    if (questIdentifier == null || questIdentifier.isBlank()) {
      throw new IllegalArgumentException("Active quest identifier cannot be blank.");
    }
    this.questIdentifier = questIdentifier;
  }

  public ActiveQuest(final Quest quest) {
    this(Objects.requireNonNull(quest, "quest").getIdentifier());
    this.quest = quest;
  }

  public String getQuestIdentifier() {
    return questIdentifier;
  }

  public Quest getQuest() {
    return quest;
  }

  public void attachQuest(final Quest quest) {
    if (quest != null && questIdentifier.equalsIgnoreCase(quest.getIdentifier())) {
      this.quest = quest;
    }
  }

  public List<ActiveObjective> getActiveObjectives() {
    final ArrayList<ActiveObjective> flattened = new ArrayList<>();
    for (final ActiveObjective objective : activeObjectives) {
      addTree(flattened, objective);
    }
    return List.copyOf(flattened);
  }

  public ActiveObjective getActiveObjective(final int[] objectivePath) {
    if (objectivePath == null || objectivePath.length == 0) {
      return null;
    }
    ActiveObjective current = activeObjectives.stream()
        .filter(objective -> objective.getObjectiveID() == objectivePath[0])
        .findFirst()
        .orElse(null);
    for (int index = 1; index < objectivePath.length && current != null; index++) {
      current = current.child(objectivePath[index]);
    }
    return current;
  }

  public boolean addActiveObjective(final ActiveObjective objective) {
    if (objective == null || !questIdentifier.equalsIgnoreCase(objective.getQuestIdentifier())) {
      return false;
    }
    final int[] path = objective.getObjectivePath();
    if (path.length <= 1) {
      removeActiveObjective(path);
      activeObjectives.add(objective);
      return true;
    }
    final ActiveObjective parent = getActiveObjective(Arrays.copyOf(path, path.length - 1));
    return parent != null && parent.addChild(objective);
  }

  public ActiveObjective removeActiveObjective(final int[] objectivePath) {
    if (objectivePath == null || objectivePath.length == 0) {
      return null;
    }
    if (objectivePath.length == 1) {
      final ActiveObjective objective = getActiveObjective(objectivePath);
      if (objective != null) {
        activeObjectives.remove(objective);
      }
      return objective;
    }
    final ActiveObjective parent = getActiveObjective(
        Arrays.copyOf(objectivePath, objectivePath.length - 1));
    return parent == null ? null : parent.removeChild(objectivePath[objectivePath.length - 1]);
  }

  public void clearActiveObjectives() {
    activeObjectives.clear();
  }

  private static void addTree(
      final List<ActiveObjective> flattened,
      final ActiveObjective objective) {
    flattened.add(objective);
    for (final ActiveObjective child : objective.getActiveObjectives()) {
      addTree(flattened, child);
    }
  }
}
