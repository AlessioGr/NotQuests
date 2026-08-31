package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class QuestHistoryRecordTest {
  @Test
  void completedQuestRecordStoresPersistedQuestHistoryFields() {
    final QuestPlayer.CompletedQuest record = new QuestPlayer.CompletedQuest("QuestA", "player-id", 1234);

    assertEquals("QuestA", record.questIdentifier());
    assertEquals("player-id", record.questPlayerIdentifier());
    assertEquals(1234, record.timeCompleted());
  }

  @Test
  void failedQuestRecordStoresPersistedQuestHistoryFields() {
    final QuestPlayer.FailedQuest record = new QuestPlayer.FailedQuest("QuestA", "player-id", 5678);

    assertEquals("QuestA", record.questIdentifier());
    assertEquals("player-id", record.questPlayerIdentifier());
    assertEquals(5678, record.timeFailed());
  }

  @Test
  void recordsRejectBlankIdentifiers() {
    assertThrows(IllegalArgumentException.class, () -> new QuestPlayer.CompletedQuest("", "player-id", 1));
    assertThrows(IllegalArgumentException.class, () -> new QuestPlayer.CompletedQuest("QuestA", "", 1));
    assertThrows(IllegalArgumentException.class, () -> new QuestPlayer.FailedQuest("", "player-id", 1));
    assertThrows(IllegalArgumentException.class, () -> new QuestPlayer.FailedQuest("QuestA", "", 1));
  }
}
