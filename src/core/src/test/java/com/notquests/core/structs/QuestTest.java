package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.actions.SavedActions.ActionSettings;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

class QuestTest {
  @Test
  void defaultsMatchExistingQuestBehavior() {
    final Quest state = new Quest("TestQuest");

    assertEquals("TestQuest", state.getIdentifier());
    assertEquals("TestQuest", state.getDisplayNameOrIdentifier());
    assertEquals("", state.getDisplayName());
    assertEquals(-1, state.getMaxCompletions());
    assertEquals(-1, state.getMaxAccepts());
    assertEquals(-1, state.getMaxFails());
    assertEquals(-1, state.getAcceptCooldownComplete());
    assertTrue(state.isTakeEnabled());
    assertTrue(state.isAbortEnabled());
    assertEquals(Category.DEFAULT_NAME, state.getCategory());
  }

  @Test
  void blankCategoryFallsBackToDefaultCategory() {
    final Quest state = new Quest("TestQuest");

    state.setCategory("Story");
    assertEquals("Story", state.getCategory());

    state.setCategory(" ");
    assertEquals(Category.DEFAULT_NAME, state.getCategory());
  }

  @Test
  void displayNameOverridesIdentifierUntilCleared() {
    final Quest state = new Quest("TestQuest");

    state.setDisplayName("A Better Name");
    assertEquals("A Better Name", state.getDisplayNameOrIdentifier());

    state.clearDisplayName();
    assertEquals("TestQuest", state.getDisplayNameOrIdentifier());
  }

  @Test
  void limitsAndTogglesArePlainCoreState() {
    final Quest state = new Quest("TestQuest");

    state.setMaxCompletions(3);
    state.setMaxAccepts(4);
    state.setMaxFails(5);
    state.setAcceptCooldownComplete(60);
    state.setTakeEnabled(false);
    state.setAbortEnabled(false);

    assertEquals(3, state.getMaxCompletions());
    assertEquals(4, state.getMaxAccepts());
    assertEquals(5, state.getMaxFails());
    assertEquals(60, state.getAcceptCooldownComplete());
    assertFalse(state.isTakeEnabled());
    assertFalse(state.isAbortEnabled());
  }

  @Test
  void forcedSilentGiveKeepsEventsTriggersAndSavingButSuppressesQuestInfo() {
    final Quest.GiveOptions options = Quest.GiveOptions.forcedSilent();

    assertTrue(options.forceGive());
    assertFalse(options.sendQuestInfo());
    assertTrue(options.triggerAcceptQuestTrigger());
    assertTrue(options.callPlatformAcceptEvent());
  }

  @Test
  void rejectsBlankIdentifiers() {
    assertThrows(IllegalArgumentException.class, () -> new Quest(" "));
  }

  @Test
  void conditionSettingsUpdateCondition() {
    final Quest state = new Quest("TestQuest");
    final com.notquests.core.objectives.Objective objective =
        state.addObjective(1, "BreakBlocks", mapData(), "");
    final com.notquests.core.conditions.Condition condition =
        objective.addCondition("unlock", 1, "QuestPoints", mapData());

    condition.apply(new Quest.ConditionSettings(
        7,
        true,
        "Need more points",
        "PointsHidden",
        true));

    assertEquals(7, condition.getProgressNeeded());
    assertTrue(condition.isNegated());
    assertEquals("Need more points", condition.getDescription());
    assertEquals("PointsHidden", condition.getHiddenExpression());
    assertTrue(condition.isAllowProgressDecreaseIfNotFulfilled());
  }

  @Test
  void conditionSettingsOwnConditionDefaults() {
    final com.notquests.core.conditions.Condition condition =
        new Quest("TestQuest").addRequirement(1, "QuestPoints", mapData());

    final Quest.ConditionSettings defaults = Quest.ConditionSettings.from(condition);
    assertEquals(1, defaults.progressNeeded());
    assertFalse(defaults.negated());
    assertEquals("", defaults.description());
    assertEquals("", defaults.hiddenExpression());
    assertFalse(defaults.allowProgressDecreaseIfNotFulfilled());

    condition.apply(new Quest.ConditionSettings(3, true, "Need points", "Hidden", true));
    final Quest.ConditionSettings details = Quest.ConditionSettings.from(condition);
    assertEquals(3, details.progressNeeded());
    assertTrue(details.negated());
    assertEquals("Need points", details.description());
    assertEquals("Hidden", details.hiddenExpression());
    assertTrue(details.allowProgressDecreaseIfNotFulfilled());
  }

  @Test
  void objectiveSettingsOwnObjectiveMetadata() {
    final com.notquests.core.objectives.Objective objective =
        new Quest("TestQuest").addObjective(1, "ReachLocation", mapData(), "Go there");
    final NQLocation location = NQLocation.at("world", 1, 2, 3);
    objective.setDisplayName("Reach Spawn");
    objective.setDescription("Find the spawn.");
    objective.setChildObjectiveProgressOrder("firstToLast");
    objective.setCompletionNpc("armorstand:abc");
    objective.setLocation(location);
    objective.setLocationEnabled(true);

    final Quest.ObjectiveSettings details = Quest.ObjectiveSettings.from(objective);
    assertEquals("Reach Spawn", details.displayName());
    assertEquals("Find the spawn.", details.description());
    assertEquals("Go there", details.taskDescription());
    assertEquals("firstToLast", details.childObjectiveProgressOrder());
    assertEquals("armorstand:abc", details.completionNpc());
    assertTrue(details.locationEnabled());
    assertEquals(location, details.location());
  }

  @Test
  void actionSettingsOwnNamePrecedenceAndDelayCoercion() {
    final com.notquests.core.actions.Action durationAction =
        new Quest("TestQuest").addReward(1, "Action", data("executionDelay", Duration.ofMillis(250)));
    final com.notquests.core.actions.Action numericAction =
        new Quest("TestQuest").addReward(1, "Action", data("executionDelayMillis", 400L));
    final com.notquests.core.actions.Action stringAction =
        new Quest("TestQuest").addReward(1, "Action", data("executionDelay", "750"));

    durationAction.setDisplayName("Visible Action");

    assertEquals("Visible Action", ActionSettings.from(durationAction, "savedName").name());
    assertEquals(250, ActionSettings.from(durationAction, "savedName").executionDelayMillis());
    assertEquals("savedName", ActionSettings.from(numericAction, "savedName").name());
    assertEquals(400, ActionSettings.from(numericAction, "savedName").executionDelayMillis());
    assertEquals(750, ActionSettings.from(stringAction, "savedName").executionDelayMillis());
    assertEquals(-1, ActionSettings.from(null, "savedName").executionDelayMillis());
  }

  private static com.notquests.core.TestData mapData() {
    return new com.notquests.core.TestData(new LinkedHashMap<>(Map.of()));
  }

  private static com.notquests.core.TestData data(final String key, final Object value) {
    final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put(key, value);
    return new com.notquests.core.TestData(values);
  }
}
