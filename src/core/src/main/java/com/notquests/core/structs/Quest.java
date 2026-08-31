package com.notquests.core.structs;

import com.notquests.core.actions.Action;
import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.managers.ConfigurationManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments.NpcAttachment;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.triggers.Trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class Quest {
  private final String identifier;
  private final List<Objective> objectives = new ArrayList<>();
  private final List<Condition> requirements = new ArrayList<>();
  private final List<Action> rewards = new ArrayList<>();
  private final List<Trigger> triggers = new ArrayList<>();
  private int maxCompletions = -1;
  private int maxAccepts = -1;
  private int maxFails = -1;
  private long acceptCooldownComplete = -1;
  private boolean takeEnabled = true;
  private boolean abortEnabled = true;
  private String displayName = "";
  private String description = "";
  private String category = Category.DEFAULT_NAME;
  private ItemSelection guiItemSelection;
  private boolean guiItemGlow;
  private String objectiveProgressOrder = "";
  private final List<NpcAttachment> npcAttachments = new ArrayList<>();

  public Quest(final String identifier) {
    if (identifier == null || identifier.isBlank()) {
      throw new IllegalArgumentException("Quest identifier cannot be blank.");
    }
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return identifier;
  }

  public int getMaxCompletions() {
    return maxCompletions;
  }

  public void setMaxCompletions(final int maxCompletions) {
    this.maxCompletions = maxCompletions;
  }

  public int getMaxAccepts() {
    return maxAccepts;
  }

  public void setMaxAccepts(final int maxAccepts) {
    this.maxAccepts = maxAccepts;
  }

  public int getMaxFails() {
    return maxFails;
  }

  public void setMaxFails(final int maxFails) {
    this.maxFails = maxFails;
  }

  public long getAcceptCooldownComplete() {
    return acceptCooldownComplete;
  }

  public void setAcceptCooldownComplete(final long acceptCooldownComplete) {
    this.acceptCooldownComplete = acceptCooldownComplete;
  }

  public boolean isTakeEnabled() {
    return takeEnabled;
  }

  public void setTakeEnabled(final boolean takeEnabled) {
    this.takeEnabled = takeEnabled;
  }

  public boolean isAbortEnabled() {
    return abortEnabled;
  }

  public void setAbortEnabled(final boolean abortEnabled) {
    this.abortEnabled = abortEnabled;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName == null ? "" : displayName;
  }

  public void clearDisplayName() {
    displayName = "";
  }

  public String getDisplayNameOrIdentifier() {
    return displayName.isBlank() ? identifier : displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description == null ? "" : description;
  }

  public void clearDescription() {
    description = "";
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(final String category) {
    this.category = category == null || category.isBlank() ? Category.DEFAULT_NAME : category;
  }

  public String getGuiItem() {
    return guiItemSelection == null ? "" : guiItemSelection.listedMaterials("");
  }

  public void setGuiItem(final String guiItem) {
    setGuiItem(guiItem == null || guiItem.isBlank() ? null : ItemStackSelection.parse(guiItem));
  }

  public ItemSelection getGuiItemSelection() {
    return guiItemSelection;
  }

  public void setGuiItem(final ItemSelection guiItemSelection) {
    this.guiItemSelection = guiItemSelection;
  }

  public boolean isGuiItemGlow() {
    return guiItemGlow;
  }

  public void setGuiItemGlow(final boolean guiItemGlow) {
    this.guiItemGlow = guiItemGlow;
  }

  public String getObjectiveProgressOrder() {
    return objectiveProgressOrder;
  }

  public void setObjectiveProgressOrder(final String objectiveProgressOrder) {
    this.objectiveProgressOrder = objectiveProgressOrder == null ? "" : objectiveProgressOrder;
  }

  public void clearObjectiveProgressOrder() {
    objectiveProgressOrder = "";
  }

  public synchronized Objective addObjective(
      final String typeId,
      final Objectives.Data data,
      final String taskDescription) {
    return addObjective(nextObjectiveId(objectives), typeId, data, taskDescription);
  }

  public synchronized Objective addObjective(
      final int id,
      final String typeId,
      final Objectives.Data data,
      final String taskDescription) {
    final Objective objective = new Objective(id, typeId, data);
    objective.setTaskDescription(taskDescription);
    objectives.add(objective);
    return objective;
  }

  public synchronized Condition addRequirement(
      final String typeId,
      final Conditions.Data data) {
    return addRequirement(nextConditionId(requirements), typeId, data);
  }

  public synchronized Condition addRequirement(
      final int id,
      final String typeId,
      final Conditions.Data data) {
    final Condition condition = new Condition(id, typeId, data);
    requirements.add(condition);
    return condition;
  }

  public synchronized Action addReward(
      final String typeId,
      final Actions.Data data) {
    return addReward(nextActionId(rewards), typeId, data);
  }

  public synchronized Action addReward(
      final int id,
      final String typeId,
      final Actions.Data data) {
    final Action action = new Action(id, typeId, data);
    rewards.add(action);
    return action;
  }

  public synchronized Trigger addTrigger(
      final String typeId,
      final Triggers.Data data) {
    return addTrigger(nextTriggerId(triggers), typeId, data);
  }

  public synchronized Trigger addTrigger(
      final int id,
      final String typeId,
      final Triggers.Data data) {
    final Trigger trigger = new Trigger(id, typeId, data);
    triggers.add(trigger);
    return trigger;
  }

  public synchronized Trigger setTrigger(
      final int id,
      final String typeId,
      final Triggers.Data data) {
    final Trigger existing = getTriggerFromID(id);
    if (existing != null) {
      triggers.remove(existing);
    }
    final Trigger trigger = new Trigger(id, typeId, data);
    triggers.add(trigger);
    return trigger;
  }

  public synchronized List<Objective> getObjectives() {
    return List.copyOf(objectives);
  }

  public synchronized List<Condition> getRequirements() {
    return List.copyOf(requirements);
  }

  public synchronized List<Action> getRewards() {
    return List.copyOf(rewards);
  }

  public synchronized List<Trigger> getTriggers() {
    return List.copyOf(triggers);
  }

  public synchronized List<NpcAttachment> getNpcAttachments() {
    return List.copyOf(npcAttachments);
  }

  public synchronized void addNpcAttachment(
      final String npcType,
      final NQNPCID npcId,
      final String npcName,
      final boolean questShowing) {
    if (npcType == null || npcType.isBlank() || npcId == null) {
      return;
    }
    removeNpcAttachment(npcType, npcId);
    npcAttachments.add(new NpcAttachment(npcType, npcId, npcName, questShowing));
  }

  public synchronized boolean removeNpcAttachment(final String npcType, final NQNPCID npcId) {
    if (npcType == null || npcId == null) {
      return false;
    }
    return npcAttachments.removeIf(attachment ->
        attachment.npcType().equalsIgnoreCase(npcType) && npcId.equals(attachment.npcId()));
  }

  public synchronized int clearNpcAttachments() {
    final int size = npcAttachments.size();
    npcAttachments.clear();
    return size;
  }

  public synchronized int getFreeRequirementID() {
    return firstFreeId(requirements);
  }

  public synchronized int getFreeRewardID() {
    return firstFreeId(rewards);
  }

  public synchronized int getFreeTriggerID() {
    return firstFreeId(triggers);
  }

  public synchronized Objective getObjectiveFromID(final int id) {
    return objectives.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
  }

  public synchronized Condition getRequirementFromID(final int id) {
    return requirements.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
  }

  public synchronized Action getRewardFromID(final int id) {
    return rewards.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
  }

  public synchronized Trigger getTriggerFromID(final int id) {
    return triggers.stream().filter(entry -> entry.id() == id).findFirst().orElse(null);
  }

  public synchronized int clearObjectives() {
    final int size = objectives.size();
    objectives.clear();
    return size;
  }

  public synchronized int clearRequirements() {
    final int size = requirements.size();
    requirements.clear();
    return size;
  }

  public synchronized int clearRewards() {
    final int size = rewards.size();
    rewards.clear();
    return size;
  }

  public synchronized int clearTriggers() {
    final int size = triggers.size();
    triggers.clear();
    return size;
  }

  public synchronized boolean removeObjective(final int id) {
    return objectives.removeIf(entry -> entry.id() == id);
  }

  public synchronized boolean removeRequirement(final int id) {
    return requirements.removeIf(entry -> entry.id() == id);
  }

  public synchronized boolean removeReward(final int id) {
    return rewards.removeIf(entry -> entry.id() == id);
  }

  public synchronized boolean removeTrigger(final int id) {
    return triggers.removeIf(entry -> entry.id() == id);
  }

  private static int nextObjectiveId(final List<Objective> entries) {
    return entries.stream().mapToInt(Objective::id).max().orElse(0) + 1;
  }

  private static int nextConditionId(final List<Condition> entries) {
    return entries.stream().mapToInt(Condition::id).max().orElse(0) + 1;
  }

  private static int nextActionId(final List<Action> entries) {
    return entries.stream().mapToInt(Action::id).max().orElse(0) + 1;
  }

  private static int nextTriggerId(final List<Trigger> entries) {
    return entries.stream().mapToInt(Trigger::id).max().orElse(0) + 1;
  }

  private static int firstFreeId(final List<?> entries) {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      final int id = i;
      if (entries.stream().noneMatch(entry -> entryId(entry) == id)) {
        return id;
      }
    }
    return entries.size() + 1;
  }

  private static int entryId(final Object entry) {
    if (entry instanceof Objective objective) {
      return objective.id();
    }
    if (entry instanceof Condition condition) {
      return condition.id();
    }
    if (entry instanceof Action action) {
      return action.id();
    }
    return entry instanceof Trigger trigger ? trigger.id() : -1;
  }

  public static AcceptCheck acceptCheck(
      final Quest quest,
      final int maxActiveQuestsPerPlayer,
      final Collection<String> activeQuestIdentifiers,
      final Collection<QuestPlayer.CompletedQuest> completedQuests,
      final Collection<QuestPlayer.FailedQuest> failedQuests,
      final long nowMillis) {
    final String questIdentifier = normalizeQuestIdentifier(quest.getIdentifier());
    int completedAmount = 0;
    long mostRecentCompleteTime = 0;
    int failedAmount = 0;
    int acceptedAmount = 0;
    boolean alreadyActive = false;

    for (final QuestPlayer.CompletedQuest completedQuest : completedQuests) {
      if (questIdentifier.equals(normalizeQuestIdentifier(completedQuest.questIdentifier()))) {
        completedAmount += 1;
        acceptedAmount += 1;
        mostRecentCompleteTime = Math.max(mostRecentCompleteTime, completedQuest.timeCompleted());
      }
    }
    for (final QuestPlayer.FailedQuest failedQuest : failedQuests) {
      if (questIdentifier.equals(normalizeQuestIdentifier(failedQuest.questIdentifier()))) {
        failedAmount += 1;
        acceptedAmount += 1;
      }
    }
    for (final String activeQuestIdentifier : activeQuestIdentifiers) {
      if (questIdentifier.equals(normalizeQuestIdentifier(activeQuestIdentifier))) {
        alreadyActive = true;
        acceptedAmount += 1;
      }
    }

    final long completeTimeDifferenceMinutes =
        TimeUnit.MILLISECONDS.toMinutes(Math.max(0, nowMillis - mostRecentCompleteTime));
    final long timeToWaitInMinutes = Math.max(
        0,
        quest.getAcceptCooldownComplete() - completeTimeDifferenceMinutes);

    if (maxActiveQuestsPerPlayer != -1
        && activeQuestIdentifiers.size() >= maxActiveQuestsPerPlayer) {
      return acceptCheckResult(
          AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_PLAYER,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }

    if (alreadyActive) {
      return acceptCheckResult(
          AcceptCheck.Status.ALREADY_ACCEPTED,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }
    if (quest.getMaxCompletions() > -1 && completedAmount >= quest.getMaxCompletions()) {
      return acceptCheckResult(
          AcceptCheck.Status.MAX_COMPLETIONS,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }
    if (quest.getMaxAccepts() > -1 && acceptedAmount >= quest.getMaxAccepts()) {
      return acceptCheckResult(
          AcceptCheck.Status.MAX_ACCEPTS,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }
    if (quest.getMaxFails() > -1 && failedAmount >= quest.getMaxFails()) {
      return acceptCheckResult(
          AcceptCheck.Status.MAX_FAILS,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }
    if (timeToWaitInMinutes > 0) {
      return acceptCheckResult(
          AcceptCheck.Status.COOLDOWN,
          completedAmount,
          failedAmount,
          acceptedAmount,
          timeToWaitInMinutes);
    }
    return acceptCheckResult(AcceptCheck.Status.ACCEPTABLE, completedAmount, failedAmount, acceptedAmount, 0);
  }

  public static List<String> visibleQuestIdentifiers(
      final Collection<Quest> quests,
      final QuestPlayer player,
      final ConfigurationManager configuration,
      final long nowMillis,
      final Predicate<Quest> requirementsFulfilled) {
    final ConfigurationManager effectiveSettings = configuration == null ? new ConfigurationManager() : configuration;
    return (quests == null ? List.<Quest>of() : quests).stream()
        .filter(quest -> visible(quest, player, effectiveSettings, nowMillis, requirementsFulfilled))
        .map(Quest::getIdentifier)
        .toList();
  }

  private static boolean visible(
      final Quest quest,
      final QuestPlayer player,
      final ConfigurationManager configuration,
      final long nowMillis,
      final Predicate<Quest> requirementsFulfilled) {
    if (quest == null) {
      return false;
    }
    if (configuration.questVisibilityEvaluationAlreadyAccepted()
        && player != null
        && player.getActiveQuestIdentifiers().contains(quest.getIdentifier())) {
      return false;
    }
    final boolean evaluateAcceptRules =
        configuration.questVisibilityEvaluationLimits() || configuration.questVisibilityEvaluationAcceptCooldown();
    if (evaluateAcceptRules && player != null) {
      final Quest.AcceptCheck acceptCheck = Quest.acceptCheck(
          quest,
          -1,
          player.getActiveQuestIdentifiers(),
          player.getCompletedQuests(),
          player.getFailedQuests(),
          nowMillis);
      if (configuration.questVisibilityEvaluationLimits()) {
        if (quest.getMaxCompletions() > -1 && acceptCheck.completedAmount() >= quest.getMaxCompletions()) {
          return false;
        }
        if (quest.getMaxAccepts() > -1 && acceptCheck.acceptedAmount() >= quest.getMaxAccepts()) {
          return false;
        }
        if (quest.getMaxFails() > -1 && acceptCheck.failedAmount() >= quest.getMaxFails()) {
          return false;
        }
      }
      if (configuration.questVisibilityEvaluationAcceptCooldown()
          && acceptCheck.status() == Quest.AcceptCheck.Status.COOLDOWN) {
        return false;
      }
    }
    return !configuration.questVisibilityEvaluationConditions()
        || requirementsFulfilled == null
        || requirementsFulfilled.test(quest);
  }

  private static AcceptCheck acceptCheckResult(
      final AcceptCheck.Status status,
      final int completedAmount,
      final int failedAmount,
      final int acceptedAmount,
      final long timeToWaitInMinutes) {
    return new AcceptCheck(status, completedAmount, failedAmount, acceptedAmount, timeToWaitInMinutes);
  }

  private static String normalizeQuestIdentifier(final String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  public record AcceptCheck(
      Status status,
      int completedAmount,
      int failedAmount,
      int acceptedAmount,
      long timeToWaitInMinutes) {
    public enum Status {
      ACCEPTABLE,
      MAX_ACTIVE_QUESTS_PER_PLAYER,
      ALREADY_ACCEPTED,
      MAX_COMPLETIONS,
      MAX_ACCEPTS,
      MAX_FAILS,
      COOLDOWN
    }

    public double timeToWaitInHours() {
      return Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
    }

    public double timeToWaitInDays() {
      return Math.round((timeToWaitInHours() / 24f) * 10) / 10.0;
    }
  }

  public record CooldownDisplay(Bucket bucket, String value) {
    public CooldownDisplay {
      bucket = bucket == null ? Bucket.NO_COOLDOWN : bucket;
      value = value == null ? "" : value;
    }

    public static CooldownDisplay from(final AcceptCheck acceptCheck) {
      if (acceptCheck == null || acceptCheck.status() != AcceptCheck.Status.COOLDOWN) {
        return new CooldownDisplay(Bucket.NO_COOLDOWN, "");
      }
      final long minutes = acceptCheck.timeToWaitInMinutes();
      if (minutes < 60) {
        return minutes == 1
            ? new CooldownDisplay(Bucket.MINUTE, "")
            : new CooldownDisplay(Bucket.MINUTES, String.valueOf(minutes));
      }
      final double hours = acceptCheck.timeToWaitInHours();
      if (hours < 24) {
        return hours == 1
            ? new CooldownDisplay(Bucket.HOUR, "")
            : new CooldownDisplay(Bucket.HOURS, String.valueOf(hours));
      }
      final double days = acceptCheck.timeToWaitInDays();
      return days == 1
          ? new CooldownDisplay(Bucket.DAY, "")
          : new CooldownDisplay(Bucket.DAYS, String.valueOf(days));
    }

    public String format(final Text text) {
      if (text == null) {
        return "";
      }
      final String prefix = text.prefix();
      return switch (bucket) {
        case NO_COOLDOWN -> prefix + text.noCooldown();
        case MINUTE -> prefix + text.minute();
        case MINUTES -> prefix + text.minutes(value);
        case HOUR -> prefix + text.hour();
        case HOURS -> text.hours(value);
        case DAY -> prefix + text.day();
        case DAYS -> text.days(value);
      };
    }

    public enum Bucket {
      NO_COOLDOWN,
      MINUTE,
      MINUTES,
      HOUR,
      HOURS,
      DAY,
      DAYS
    }

    public interface Text {
      String prefix();
      String noCooldown();
      String minute();
      String minutes(String minutes);
      String hour();
      String hours(String hours);
      String day();
      String days(String days);
    }
  }

  public static final class GiveOptions {
    private final boolean forceGive;
    private final boolean sendQuestInfo;
    private final boolean triggerAcceptQuestTrigger;
    private final boolean callPlatformAcceptEvent;

    private GiveOptions(
        final boolean forceGive,
        final boolean sendQuestInfo,
        final boolean triggerAcceptQuestTrigger,
        final boolean callPlatformAcceptEvent) {
      this.forceGive = forceGive;
      this.sendQuestInfo = sendQuestInfo;
      this.triggerAcceptQuestTrigger = triggerAcceptQuestTrigger;
      this.callPlatformAcceptEvent = callPlatformAcceptEvent;
    }

    public static GiveOptions normal() {
      return new GiveOptions(false, true, true, true);
    }

    public static GiveOptions forcedSilent() {
      return new GiveOptions(true, false, true, true);
    }

    public GiveOptions forceGive(final boolean forceGive) {
      return new GiveOptions(forceGive, sendQuestInfo, triggerAcceptQuestTrigger, callPlatformAcceptEvent);
    }

    public GiveOptions sendQuestInfo(final boolean sendQuestInfo) {
      return new GiveOptions(forceGive, sendQuestInfo, triggerAcceptQuestTrigger, callPlatformAcceptEvent);
    }

    public GiveOptions triggerAcceptQuestTrigger(final boolean triggerAcceptQuestTrigger) {
      return new GiveOptions(forceGive, sendQuestInfo, triggerAcceptQuestTrigger, callPlatformAcceptEvent);
    }

    public GiveOptions callPlatformAcceptEvent(final boolean callPlatformAcceptEvent) {
      return new GiveOptions(forceGive, sendQuestInfo, triggerAcceptQuestTrigger, callPlatformAcceptEvent);
    }

    public boolean forceGive() {
      return forceGive;
    }

    public boolean sendQuestInfo() {
      return sendQuestInfo;
    }

    public boolean triggerAcceptQuestTrigger() {
      return triggerAcceptQuestTrigger;
    }

    public boolean callPlatformAcceptEvent() {
      return callPlatformAcceptEvent;
    }

  }

  public static final class OrderRequirements {
    private OrderRequirements() {}

    public static String missingQuestDisplayName(
        final PredefinedProgressOrder progressOrder,
        final String questName,
        final int questIndex,
        final List<OrderEntry> orderedQuests,
        final Predicate<String> completedQuest) {
      if (progressOrder == null || completedQuest == null) {
        return "";
      }
      final List<OrderEntry> quests = orderedQuests == null ? List.of() : orderedQuests;
      if (progressOrder.isFirstToLast()) {
        for (int i = 0; i < questIndex && i < quests.size(); i++) {
          final OrderEntry quest = quests.get(i);
          if (!completedQuest.test(quest.identifier())) {
            return quest.displayName();
          }
        }
        return "";
      }
      if (progressOrder.isLastToFirst()) {
        for (int i = Math.max(questIndex + 1, 0); i < quests.size(); i++) {
          final OrderEntry quest = quests.get(i);
          if (!completedQuest.test(quest.identifier())) {
            return quest.displayName();
          }
        }
        return "";
      }
      if (progressOrder.getCustomOrder() != null && !progressOrder.getCustomOrder().isEmpty()) {
        for (final String requiredQuestName : progressOrder.getCustomOrder()) {
          if (requiredQuestName.equalsIgnoreCase(questName)) {
            break;
          }
          if (!completedQuest.test(requiredQuestName)) {
            return requiredQuestName;
          }
        }
      }
      return "";
    }

    public static String requirementMessage(
        final PredefinedProgressOrder progressOrder,
        final String questName,
        final int questIndex,
        final List<OrderEntry> orderedQuests,
        final Predicate<String> completedQuest) {
      final String missingQuest = missingQuestDisplayName(
          progressOrder,
          questName,
          questIndex,
          orderedQuests,
          completedQuest);
      return missingQuest.isBlank() ? "" : "Quest " + missingQuest + " needs to be completed first";
    }
  }

  public record OrderEntry(String identifier, String displayName) {
    public OrderEntry {
      identifier = identifier == null ? "" : identifier;
      displayName = displayName == null || displayName.isBlank() ? identifier : displayName;
    }
  }

  public record ObjectiveSettings(
      String displayName,
      String description,
      String taskDescription,
      String childObjectiveProgressOrder,
      boolean locationEnabled,
      String completionNpc,
      NQLocation location) {

    public ObjectiveSettings {
      displayName = displayName == null ? "" : displayName;
      description = description == null ? "" : description;
      taskDescription = taskDescription == null ? "" : taskDescription;
      childObjectiveProgressOrder = childObjectiveProgressOrder == null ? "" : childObjectiveProgressOrder;
      completionNpc = completionNpc == null ? "" : completionNpc;
    }

    public static ObjectiveSettings from(final Objective objective) {
      if (objective == null) {
        return new ObjectiveSettings("", "", "", "", false, "", null);
      }
      return new ObjectiveSettings(
          objective.getDisplayName(),
          objective.getDescription(),
          objective.getTaskDescription(),
          objective.getChildObjectiveProgressOrder(),
          objective.isLocationEnabled(),
          objective.getCompletionNPC(),
          objective.getLocation());
    }
  }

  public record ConditionSettings(
      long progressNeeded,
      boolean negated,
      String description,
      String hiddenExpression,
      boolean allowProgressDecreaseIfNotFulfilled) {
    public ConditionSettings {
      description = description == null ? "" : description;
      hiddenExpression = hiddenExpression == null ? "" : hiddenExpression;
    }

    public static ConditionSettings from(final Condition condition) {
      if (condition == null) {
        return new ConditionSettings(1, false, "", "", false);
      }
      return new ConditionSettings(
          condition.getProgressNeeded(),
          condition.isNegated(),
          condition.getDescription(),
          condition.getHiddenExpression(),
          condition.isAllowProgressDecreaseIfNotFulfilled());
    }
  }

  public record TriggerSettings(
      String actionName,
      int applyOn,
      long amountNeeded,
      String worldName) {
    public TriggerSettings {
      actionName = actionName == null ? "" : actionName;
      worldName = worldName == null || worldName.isBlank() ? "ALL" : worldName;
    }

    public void applyTo(final Trigger trigger) {
      if (trigger == null) {
        return;
      }
      trigger.setValue("action", actionName);
      trigger.setValue("applyOn", applyOn);
      trigger.setValue("amount", (int) Math.max(1L, Math.min(Integer.MAX_VALUE, amountNeeded)));
      trigger.setValue("worldName", worldName);
    }
  }

}
