package com.notquests.paper.integrations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BetonQuestVersionTest {
  @Test
  void acceptsBetonQuestThreeAndNewer() {
    assertTrue(PaperIntegrations.supportsBetonQuestVersion("3.0.0"));
    assertTrue(PaperIntegrations.supportsBetonQuestVersion("3.0.0-SNAPSHOT"));
    assertTrue(PaperIntegrations.supportsBetonQuestVersion("v3.1.4"));
    assertTrue(PaperIntegrations.supportsBetonQuestVersion("4.0.0"));
  }

  @Test
  void rejectsOlderAndUnparseableVersions() {
    assertFalse(PaperIntegrations.supportsBetonQuestVersion("2.2.1"));
    assertFalse(PaperIntegrations.supportsBetonQuestVersion("1.12.0"));
    assertFalse(PaperIntegrations.supportsBetonQuestVersion(""));
    assertFalse(PaperIntegrations.supportsBetonQuestVersion("not-a-version"));
  }
}
