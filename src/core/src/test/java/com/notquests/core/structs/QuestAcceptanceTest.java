package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestAcceptanceTest {
  private static final long NOW = 1_000_000L;

  @Test
  void rejectsWhenPlayerAlreadyHasTooManyActiveQuests() {
    final Quest quest = new Quest("QuestA");

    final Quest.AcceptCheck check =
        Quest.acceptCheck(quest, 1, List.of("OtherQuest"), List.of(), List.of(), NOW);

    assertEquals(Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_PLAYER, check.status());
  }

  @Test
  void rejectsDuplicateActiveQuestButStillReportsHistoryCounts() {
    final Quest quest = new Quest("QuestA");

    final Quest.AcceptCheck check =
        Quest.acceptCheck(
            quest,
            -1,
            List.of("questa"),
            List.of(new QuestPlayer.CompletedQuest("QuestA", "player", 1)),
            List.of(new QuestPlayer.FailedQuest("QuestA", "player", 2)),
            NOW);

    assertEquals(Quest.AcceptCheck.Status.ALREADY_ACCEPTED, check.status());
    assertEquals(1, check.completedAmount());
    assertEquals(1, check.failedAmount());
    assertEquals(3, check.acceptedAmount());
  }

  @Test
  void appliesQuestCompletionAcceptAndFailLimits() {
    final Quest completedLimited = new Quest("QuestA");
    completedLimited.setMaxCompletions(1);
    assertEquals(
        Quest.AcceptCheck.Status.MAX_COMPLETIONS,
        Quest.acceptCheck(
                completedLimited,
                -1,
                List.of(),
                List.of(new QuestPlayer.CompletedQuest("QuestA", "player", 1)),
                List.of(),
                NOW)
            .status());

    final Quest acceptLimited = new Quest("QuestA");
    acceptLimited.setMaxAccepts(2);
    assertEquals(
        Quest.AcceptCheck.Status.MAX_ACCEPTS,
        Quest.acceptCheck(
                acceptLimited,
                -1,
                List.of(),
                List.of(new QuestPlayer.CompletedQuest("QuestA", "player", 1)),
                List.of(new QuestPlayer.FailedQuest("QuestA", "player", 2)),
                NOW)
            .status());

    final Quest failLimited = new Quest("QuestA");
    failLimited.setMaxFails(1);
    assertEquals(
        Quest.AcceptCheck.Status.MAX_FAILS,
        Quest.acceptCheck(
                failLimited,
                -1,
                List.of(),
                List.of(),
                List.of(new QuestPlayer.FailedQuest("QuestA", "player", 2)),
                NOW)
            .status());
  }

  @Test
  void appliesCompletionCooldownInMinutes() {
    final Quest quest = new Quest("QuestA");
    quest.setAcceptCooldownComplete(20);

    final Quest.AcceptCheck check =
        Quest.acceptCheck(
            quest,
            -1,
            List.of(),
            List.of(new QuestPlayer.CompletedQuest("QuestA", "player", NOW - 600_000L)),
            List.of(),
            NOW);

    assertEquals(Quest.AcceptCheck.Status.COOLDOWN, check.status());
    assertEquals(10, check.timeToWaitInMinutes());
  }

  @Test
  void acceptsWhenNoLimitsBlockQuest() {
    final Quest quest = new Quest("QuestA");

    final Quest.AcceptCheck check = Quest.acceptCheck(quest, -1, List.of(), List.of(), List.of(), NOW);

    assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, check.status());
  }
}
