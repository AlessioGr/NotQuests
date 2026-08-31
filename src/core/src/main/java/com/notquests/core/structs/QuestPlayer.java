package com.notquests.core.structs;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class QuestPlayer {
  private final String playerIdentifier;
  private final String profile;
  private final Map<String, Object> tags = new ConcurrentHashMap<>();
  private final List<ActiveQuest> activeQuests = new CopyOnWriteArrayList<>();
  private final Map<String, Set<String>> completedObjectiveIdsByQuest = new ConcurrentHashMap<>();
  private final Map<String, Map<String, QuestPlayer.CompletedObjective>> completedObjectivesByQuest =
      new ConcurrentHashMap<>();
  private final List<QuestPlayer.CompletedQuest> completedQuests = new CopyOnWriteArrayList<>();
  private final List<QuestPlayer.FailedQuest> failedQuests = new CopyOnWriteArrayList<>();
  private long questPoints;
  private volatile boolean currentlyLoading = true;
  private volatile boolean finishedLoadingGeneralData;
  private volatile boolean finishedLoadingTags;

  public QuestPlayer(final String playerIdentifier, final String profile) {
    if (playerIdentifier == null || playerIdentifier.isBlank()) {
      throw new IllegalArgumentException("Player identifier cannot be blank.");
    }
    this.playerIdentifier = playerIdentifier;
    this.profile = profile == null || profile.isBlank() ? "default" : profile;
  }

  public String getPlayerIdentifier() {
    return playerIdentifier;
  }

  public String getProfile() {
    return profile;
  }

  public Object getTagValue(final String tagIdentifier) {
    return tags.get(normalizeTag(tagIdentifier));
  }

  public void setTagValue(final String tagIdentifier, final Object newValue) {
    final String key = normalizeTag(tagIdentifier);
    if (key.isBlank()) {
      return;
    }
    if (newValue == null) {
      tags.remove(key);
      return;
    }
    tags.put(key, newValue);
  }

  public Map<String, Object> getTags() {
    return Collections.unmodifiableMap(tags);
  }

  public void replaceTags(final Map<String, Object> loadedTags) {
    tags.clear();
    if (loadedTags != null) {
      loadedTags.forEach(this::setTagValue);
    }
  }

  public boolean isCurrentlyLoading() {
    return currentlyLoading;
  }

  public void setCurrentlyLoading(final boolean currentlyLoading) {
    this.currentlyLoading = currentlyLoading;
  }

  public boolean isFinishedLoadingGeneralData() {
    return finishedLoadingGeneralData;
  }

  public void setFinishedLoadingGeneralData(final boolean finishedLoadingGeneralData) {
    this.finishedLoadingGeneralData = finishedLoadingGeneralData;
  }

  public boolean isFinishedLoadingTags() {
    return finishedLoadingTags;
  }

  public void setFinishedLoadingTags(final boolean finishedLoadingTags) {
    this.finishedLoadingTags = finishedLoadingTags;
  }

  public boolean acceptQuest(final Quest quest) {
    if (quest == null) {
      return false;
    }
    return addActiveQuest(quest);
  }

  public boolean addActiveQuest(final String questIdentifier) {
    final String normalizedIdentifier = normalizeQuestIdentifier(questIdentifier);
    if (normalizedIdentifier.isBlank() || hasActiveQuest(normalizedIdentifier)) {
      return false;
    }
    activeQuests.add(new ActiveQuest(normalizedIdentifier));
    return true;
  }

  public boolean addActiveQuest(final Quest quest) {
    if (quest == null) {
      return false;
    }
    final ActiveQuest activeQuest = getActiveQuest(quest.getIdentifier());
    if (activeQuest != null) {
      activeQuest.attachQuest(quest);
      return false;
    }
    activeQuests.add(new ActiveQuest(quest));
    return true;
  }

  public ActiveQuest ensureActiveQuest(final String questIdentifier) {
    ActiveQuest activeQuest = getActiveQuest(questIdentifier);
    if (activeQuest == null && addActiveQuest(questIdentifier)) {
      activeQuest = getActiveQuest(questIdentifier);
    }
    return activeQuest;
  }

  public ActiveQuest getActiveQuest(final String questIdentifier) {
    final String normalized = normalizeQuestIdentifier(questIdentifier);
    return activeQuests.stream()
        .filter(activeQuest -> activeQuest.getQuestIdentifier().equalsIgnoreCase(normalized))
        .findFirst()
        .orElse(null);
  }

  public List<ActiveQuest> getActiveQuests() {
    return List.copyOf(activeQuests);
  }

  public boolean removeActiveQuest(final String questIdentifier) {
    final ActiveQuest activeQuest = getActiveQuest(questIdentifier);
    return activeQuest != null && activeQuests.remove(activeQuest);
  }

  public boolean completeQuest(final String questIdentifier, final long timeCompleted) {
    if (!removeActiveQuest(questIdentifier)) {
      return false;
    }
    completedObjectiveIdsByQuest.remove(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    completedObjectivesByQuest.remove(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    addCompletedQuest(new QuestPlayer.CompletedQuest(questIdentifier, playerIdentifier, timeCompleted));
    return true;
  }

  public void recordCompletedQuest(final String questIdentifier, final long timeCompleted) {
    if (!completeQuest(questIdentifier, timeCompleted)) {
      addCompletedQuest(new QuestPlayer.CompletedQuest(questIdentifier, playerIdentifier, timeCompleted));
    }
  }

  public boolean failQuest(final String questIdentifier, final long timeFailed) {
    if (!removeActiveQuest(questIdentifier)) {
      return false;
    }
    completedObjectiveIdsByQuest.remove(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    completedObjectivesByQuest.remove(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    addFailedQuest(new QuestPlayer.FailedQuest(questIdentifier, playerIdentifier, timeFailed));
    return true;
  }

  public void recordFailedQuest(final String questIdentifier, final long timeFailed) {
    if (!failQuest(questIdentifier, timeFailed)) {
      addFailedQuest(new QuestPlayer.FailedQuest(questIdentifier, playerIdentifier, timeFailed));
    }
  }

  public void addCompletedQuest(final QuestPlayer.CompletedQuest completedQuest) {
    if (completedQuest != null) {
      completedQuests.add(completedQuest);
    }
  }

  public void removeCompletedQuest(final QuestPlayer.CompletedQuest completedQuest) {
    if (completedQuest != null) {
      completedQuests.remove(completedQuest);
    }
  }

  public void addFailedQuest(final QuestPlayer.FailedQuest failedQuest) {
    if (failedQuest != null) {
      failedQuests.add(failedQuest);
    }
  }

  public void removeFailedQuest(final QuestPlayer.FailedQuest failedQuest) {
    if (failedQuest != null) {
      failedQuests.remove(failedQuest);
    }
  }

  public boolean hasActiveQuest(final String questIdentifier) {
    final String normalized = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    return activeQuests.stream()
        .map(ActiveQuest::getQuestIdentifier)
        .map(identifier -> identifier.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::equals);
  }

  public boolean hasCompletedQuest(final String questIdentifier) {
    final String normalized = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    return completedQuests.stream()
        .map(QuestPlayer.CompletedQuest::questIdentifier)
        .map(identifier -> identifier.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::equals);
  }

  public boolean hasFailedQuest(final String questIdentifier) {
    final String normalized = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    return failedQuests.stream()
        .map(QuestPlayer.FailedQuest::questIdentifier)
        .map(identifier -> identifier.toLowerCase(Locale.ROOT))
        .anyMatch(normalized::equals);
  }

  public Set<String> getActiveQuestIdentifiers() {
    return activeQuests.stream()
        .map(ActiveQuest::getQuestIdentifier)
        .collect(Collectors.toUnmodifiableSet());
  }

  public List<ActiveObjective> getActiveObjectives() {
    return activeQuests.stream().flatMap(activeQuest -> activeQuest.getActiveObjectives().stream()).toList();
  }

  public void clearActiveObjectives() {
    activeQuests.forEach(ActiveQuest::clearActiveObjectives);
  }

  public List<String> getCompletedObjectiveIDs(final String questIdentifier) {
    final Set<String> ids = completedObjectiveIdsByQuest.get(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    return ids == null ? List.of() : ids.stream().sorted().toList();
  }

  public void setCompletedObjectiveIds(final String questIdentifier, final List<String> objectiveIds) {
    final String questKey = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    if (questKey.isBlank()) {
      return;
    }
    final Set<String> ids = ConcurrentHashMap.newKeySet();
    if (objectiveIds != null) {
      for (final String objectiveId : objectiveIds) {
        if (objectiveId != null && !objectiveId.isBlank()) {
          ids.add(objectiveId);
        }
      }
    }
    if (ids.isEmpty()) {
      completedObjectiveIdsByQuest.remove(questKey);
      completedObjectivesByQuest.remove(questKey);
      return;
    }
    completedObjectiveIdsByQuest.put(questKey, ids);
  }

  public void addCompletedObjectiveId(final String questIdentifier, final int objectiveId) {
    addCompletedObjectiveId(questIdentifier, String.valueOf(objectiveId));
  }

  public void addCompletedObjectiveId(final String questIdentifier, final String objectiveId) {
    final String questKey = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    if (questKey.isBlank() || objectiveId == null || objectiveId.isBlank()) {
      return;
    }
    completedObjectiveIdsByQuest
        .computeIfAbsent(questKey, ignored -> ConcurrentHashMap.newKeySet())
        .add(objectiveId);
  }

  public void addCompletedObjective(final QuestPlayer.CompletedObjective completedObjective) {
    if (completedObjective == null
        || completedObjective.questIdentifier().isBlank()
        || completedObjective.objectivePath().isBlank()) {
      return;
    }
    addCompletedObjectiveId(completedObjective.questIdentifier(), completedObjective.objectivePath());
    completedObjectivesByQuest
        .computeIfAbsent(
            normalizeQuestIdentifier(completedObjective.questIdentifier()).toLowerCase(Locale.ROOT),
            ignored -> new ConcurrentHashMap<>())
        .put(completedObjective.objectivePath(), completedObjective);
  }

  public List<QuestPlayer.CompletedObjective> getCompletedObjectives(final String questIdentifier) {
    final Map<String, QuestPlayer.CompletedObjective> records =
        completedObjectivesByQuest.get(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    return records == null
        ? List.of()
        : records.values().stream()
            .sorted(Comparator.comparing(QuestPlayer.CompletedObjective::objectivePath))
            .toList();
  }

  public QuestPlayer.CompletedObjective getCompletedObjective(final String questIdentifier, final String objectivePath) {
    final Map<String, QuestPlayer.CompletedObjective> records =
        completedObjectivesByQuest.get(normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT));
    return records == null ? null : records.get(objectivePath);
  }

  public void clearCompletedObjectives(final String questIdentifier) {
    final String questKey = normalizeQuestIdentifier(questIdentifier).toLowerCase(Locale.ROOT);
    completedObjectiveIdsByQuest.remove(questKey);
    completedObjectivesByQuest.remove(questKey);
  }

  public Map<String, List<String>> getCompletedObjectiveIDsByQuest() {
    final Map<String, List<String>> copy = new LinkedHashMap<>();
    completedObjectiveIdsByQuest.forEach((quest, ids) -> copy.put(quest, ids.stream().sorted().toList()));
    return Collections.unmodifiableMap(copy);
  }

  public List<QuestPlayer.CompletedQuest> getCompletedQuests() {
    return List.copyOf(completedQuests);
  }

  public List<QuestPlayer.FailedQuest> getFailedQuests() {
    return List.copyOf(failedQuests);
  }

  public long getQuestPoints() {
    return questPoints;
  }

  public void setQuestPoints(final long questPoints) {
    this.questPoints = Math.max(0, questPoints);
  }

  public void addQuestPoints(final long amount) {
    setQuestPoints(questPoints + amount);
  }

  public void removeQuestPoints(final long amount) {
    setQuestPoints(questPoints - amount);
  }

  private static String normalizeTag(final String tagIdentifier) {
    return tagIdentifier == null ? "" : tagIdentifier.toLowerCase(Locale.ROOT);
  }

  private static String normalizeQuestIdentifier(final String questIdentifier) {
    return questIdentifier == null ? "" : questIdentifier;
  }

  public record CompletedQuest(String questIdentifier, String questPlayerIdentifier, long timeCompleted) {
    public CompletedQuest {
      if (questIdentifier == null || questIdentifier.isBlank()) {
        throw new IllegalArgumentException("Completed quest identifier cannot be blank.");
      }
      if (questPlayerIdentifier == null || questPlayerIdentifier.isBlank()) {
        throw new IllegalArgumentException("Completed quest player identifier cannot be blank.");
      }
    }
  }

  public record FailedQuest(String questIdentifier, String questPlayerIdentifier, long timeFailed) {
    public FailedQuest {
      if (questIdentifier == null || questIdentifier.isBlank()) {
        throw new IllegalArgumentException("Failed quest identifier cannot be blank.");
      }
      if (questPlayerIdentifier == null || questPlayerIdentifier.isBlank()) {
        throw new IllegalArgumentException("Failed quest player identifier cannot be blank.");
      }
    }
  }

  public record CompletedObjective(
      String questIdentifier,
      String objectivePath,
      String holderPath,
      String objectiveType,
      double currentProgress,
      double progressNeeded) {
    public CompletedObjective {
      questIdentifier = questIdentifier == null ? "" : questIdentifier;
      objectivePath = objectivePath == null ? "" : objectivePath;
      holderPath = holderPath == null ? "" : holderPath;
      objectiveType = objectiveType == null ? "" : objectiveType;
    }
  }
}
