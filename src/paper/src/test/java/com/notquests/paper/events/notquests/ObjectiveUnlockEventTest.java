package com.notquests.paper.events.notquests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ObjectiveUnlockEventTest {
  @Test
  void eventIsSynchronousCancellableAndProtectsItsObjectivePath() {
    final int[] sourcePath = {1, 2};
    final ObjectiveUnlockEvent event = new ObjectiveUnlockEvent(
        null,
        "Quest",
        sourcePath,
        2,
        "Quest.1",
        null,
        null,
        true);

    sourcePath[0] = 99;
    final int[] returnedPath = event.getObjectivePath();
    returnedPath[1] = 99;
    event.setCancelled(true);

    assertFalse(event.isAsynchronous());
    assertTrue(event.isCancelled());
    assertTrue(event.isTriggerAcceptQuestTrigger());
    assertArrayEquals(new int[] {1, 2}, event.getObjectivePath());
  }
}
