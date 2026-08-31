package com.notquests.paper.events.notquests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Quest;

class NotQuestsEventTest {

  @Test
  void questEventsExposeCurrentPlayerAndQuestContext() {
    final Quest quest = new Quest("AQuest");

    final QuestFinishAcceptEvent accepted =
        new QuestFinishAcceptEvent(null, quest.getIdentifier(), quest, true);
    final QuestCompletedEvent completed =
        new QuestCompletedEvent(null, quest.getIdentifier(), quest, true);
    final QuestFailEvent failed = new QuestFailEvent(null, quest.getIdentifier(), quest);

    assertNull(accepted.getPaperPlayer());
    assertSame(quest, accepted.getQuest());
    assertSame(quest, completed.getQuest());
    assertSame(quest, failed.getQuest());
    assertEquals("AQuest", completed.getQuestName());
    assertTrue(accepted.isTriggerAcceptQuestTrigger());
    assertTrue(completed.isForced());
    assertFalse(accepted.isAsynchronous());
    assertFalse(completed.isAsynchronous());
    assertFalse(failed.isAsynchronous());
  }

  @Test
  void objectiveEventsExposeImmutableCoreContext() {
    final Quest quest = new Quest("AQuest");
    final ActiveObjective objective = ActiveObjective.configured(4);
    final int[] sourcePath = {1, 3};
    final ObjectiveCompleteEvent completed =
        new ObjectiveCompleteEvent(
            null, quest.getIdentifier(), sourcePath, 3, "AQuest.1", quest, objective);
    final ObjectiveUnlockEvent unlocked =
        new ObjectiveUnlockEvent(
            null, quest.getIdentifier(), sourcePath, 3, "AQuest.1", quest, objective, false);

    sourcePath[0] = 99;

    assertNull(completed.getPaperPlayer());
    assertSame(quest, completed.getQuest());
    assertSame(quest, unlocked.getQuest());
    assertArrayEquals(new int[] {1, 3}, completed.getObjectivePath());
    assertArrayEquals(new int[] {1, 3}, unlocked.getObjectivePath());
    assertEquals(3, completed.getObjectiveId());
    assertEquals(3, unlocked.getObjectiveId());
    assertFalse(completed.isAsynchronous());
    assertFalse(unlocked.isAsynchronous());

    final ObjectiveCompleteEvent withoutRuntimeObjective =
        new ObjectiveCompleteEvent(
            null, quest.getIdentifier(), new int[] {1, 3}, 3, "AQuest.1", quest, null);
    assertEquals("AQuest.1", withoutRuntimeObjective.getObjectiveHolderPath());
  }
}
