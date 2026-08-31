package com.notquests.paper.integrations.betonquest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class BetonQuestArchitectureTest {
  @Test
  void betonQuestCallbacksOnlyParseFactsAndCallCore() throws IOException {
    final String callbacks = javaSources(Path.of(
        "src/main/java/com/notquests/paper/integrations/betonquest/events"));
    final String conditions = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/integrations/betonquest/conditions/BQConditionsCondition.java"));
    final String sources = callbacks + conditions;

    assertFalse(sources.contains("activePaperPlayer"));
    assertFalse(sources.contains("getOrCreatePlatformPlayer"));
    assertFalse(sources.contains("getOrLoadPlatformPlayer"));
    assertFalse(sources.contains("PaperNotQuestsAdapter"));
    assertFalse(sources.contains("GiveOptions"));
    assertFalse(sources.contains(".setQuestPoints("));
    assertFalse(sources.contains(".addQuestPoints("));
    assertFalse(sources.contains(".removeQuestPoints("));
    assertFalse(sources.contains(".removeActiveQuest("));
    assertFalse(sources.contains(".triggerCommandObjectiveProgress("));
    assertFalse(sources.contains("new QuestException(\""));

    assertTrue(sources.contains("executeBetonQuestActionLine("));
    assertTrue(sources.contains("checkBetonQuestConditionLine("));
    assertTrue(sources.contains("betonQuestStartQuest("));
    assertTrue(sources.contains("betonQuestFailQuest("));
    assertTrue(sources.contains("betonQuestAbortQuest("));
    assertTrue(sources.contains("betonQuestChangeQuestPoints("));
    assertTrue(sources.contains("betonQuestTriggerObjective("));
  }

  @Test
  void betonQuestNativeCallsLeaveValidationFallbackAndLogsInCore() throws IOException {
    final String fireEvent = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/builtin/actions/BetonQuestFireEventAction.java"));
    final String fireInline = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/builtin/actions/BetonQuestFireInlineEventAction.java"));
    final String conditionVariable = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/builtin/variables/hooks/BetonQuestConditionVariable.java"));
    final String sources = fireEvent + fireInline + conditionVariable;

    assertFalse(sources.contains(".warn("));
    assertFalse(sources.contains("Tried to "));
    assertFalse(sources.contains("could not run"));
    assertFalse(sources.contains("could not test"));
    assertTrue(fireEvent.contains("executeBetonQuestNamedAction("));
    assertTrue(fireInline.contains("executeBetonQuestInlineAction("));
    assertTrue(conditionVariable.contains("checkBetonQuestConditionVariable("));
  }

  @Test
  void betonQuestIntegrationHasNoDuplicateCorePolicyOrPresentation() throws IOException {
    final String integration = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/integrations/betonquest/BetonQuestIntegration.java"));
    final String objective = Files.readString(Path.of(
        "src/main/java/com/notquests/paper/builtin/objectives/BetonQuestObjectiveStateChangeObjective.java"));

    assertFalse(integration.contains("getOrLoadPlatformPlayer"));
    assertFalse(integration.contains("Could not enable BetonQuest support"));
    assertFalse(integration.contains("Registered BetonQuest interceptor"));
    assertFalse(integration.contains("objectiveIdentifier("));
    assertFalse(integration.contains("objectiveStates("));
    assertFalse(objective.contains("matchesStateChange("));
    assertFalse(objective.contains("BetonQuestIntegration betonQuest("));
  }

  private static String javaSources(final Path directory) throws IOException {
    final StringBuilder sources = new StringBuilder();
    try (var files = Files.walk(directory)) {
      for (final Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        sources.append(Files.readString(file));
      }
    }
    return sources.toString();
  }
}
