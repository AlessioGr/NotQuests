package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuestPlayerTest {
  @Test
  void storesProfileAndNonNegativeQuestPoints() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    assertEquals("player-id", state.getPlayerIdentifier());
    assertEquals("default", state.getProfile());

    state.setQuestPoints(10);
    state.removeQuestPoints(12);
    assertEquals(0, state.getQuestPoints());

    state.addQuestPoints(5);
    assertEquals(5, state.getQuestPoints());
  }

  @Test
  void storesTagsCaseInsensitivelyAndRemovesOnNull() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    state.setTagValue("Reputation", 3);
    assertEquals(3, state.getTagValue("reputation"));
    assertEquals(3, state.getTagValue("REPUTATION"));

    state.setTagValue("reputation", null);
    assertFalse(state.getTags().containsKey("reputation"));
  }

  @Test
  void ownsRuntimeLoadingFlagsForThisProfile() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    assertTrue(state.isCurrentlyLoading());
    assertFalse(state.isFinishedLoadingGeneralData());
    assertFalse(state.isFinishedLoadingTags());

    state.setCurrentlyLoading(false);
    state.setFinishedLoadingGeneralData(true);
    state.setFinishedLoadingTags(true);

    assertFalse(state.isCurrentlyLoading());
    assertTrue(state.isFinishedLoadingGeneralData());
    assertTrue(state.isFinishedLoadingTags());
  }

  @Test
  void ownsActiveCompletedAndFailedQuestTransitions() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");
    final Quest quest = new Quest("QuestA");

    assertTrue(state.acceptQuest(quest));
    assertFalse(state.acceptQuest(quest));
    assertTrue(state.hasActiveQuest("QuestA"));
    assertTrue(state.hasActiveQuest("questa"));
    assertEquals(java.util.Set.of("QuestA"), state.getActiveQuestIdentifiers());

    assertTrue(state.completeQuest("QuestA", 1000));
    assertFalse(state.hasActiveQuest("QuestA"));
    assertTrue(state.hasCompletedQuest("questa"));
    assertEquals(1, state.getCompletedQuests().size());
    assertEquals("QuestA", state.getCompletedQuests().getFirst().questIdentifier());

    assertTrue(state.acceptQuest(quest));
    assertTrue(state.failQuest("QuestA", 2000));
    assertTrue(state.hasFailedQuest("questa"));
    assertEquals(1, state.getFailedQuests().size());
    assertEquals("QuestA", state.getFailedQuests().getFirst().questIdentifier());
  }

  @Test
  void importsStoredQuestHistoryWithoutRequiringAnActiveQuest() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    state.addActiveQuest("QuestA");
    state.addCompletedQuest(new QuestPlayer.CompletedQuest("QuestB", "player-id", 1000));
    state.addFailedQuest(new QuestPlayer.FailedQuest("QuestC", "player-id", 2000));

    assertEquals(java.util.Set.of("QuestA"), state.getActiveQuestIdentifiers());
    assertEquals("QuestB", state.getCompletedQuests().getFirst().questIdentifier());
    assertEquals("QuestC", state.getFailedQuests().getFirst().questIdentifier());
    assertThrows(UnsupportedOperationException.class, () -> state.getActiveQuestIdentifiers().clear());
    assertThrows(UnsupportedOperationException.class, () -> state.getCompletedQuests().clear());
    assertThrows(UnsupportedOperationException.class, () -> state.getFailedQuests().clear());
  }

  @Test
  void recordsQuestHistoryEvenWhenQuestIsNotActive() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    state.recordCompletedQuest("QuestA", 1000);
    state.recordFailedQuest("QuestB", 2000);

    assertEquals(0, state.getActiveQuestIdentifiers().size());
    assertEquals("QuestA", state.getCompletedQuests().getFirst().questIdentifier());
    assertEquals("player-id", state.getCompletedQuests().getFirst().questPlayerIdentifier());
    assertEquals(1000, state.getCompletedQuests().getFirst().timeCompleted());
    assertEquals("QuestB", state.getFailedQuests().getFirst().questIdentifier());
    assertEquals("player-id", state.getFailedQuests().getFirst().questPlayerIdentifier());
    assertEquals(2000, state.getFailedQuests().getFirst().timeFailed());
  }

  @Test
  void recordQuestHistoryRemovesActiveQuestWhenPresent() {
    final QuestPlayer state = new QuestPlayer("player-id", "default");

    state.addActiveQuest("QuestA");
    state.recordCompletedQuest("QuestA", 1000);

    assertFalse(state.hasActiveQuest("QuestA"));
    assertTrue(state.hasCompletedQuest("QuestA"));
  }

  @Test
  void rejectsBlankPlayerIdentifier() {
    assertThrows(IllegalArgumentException.class, () -> new QuestPlayer(" ", "default"));
  }
}
