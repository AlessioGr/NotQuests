package com.notquests.core.structs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.actions.SavedActions.ActionSettings;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class QuestTest {
  @Test
  void copyPreservesConfigurationAndDoesNotShareMutableData() {
    final Quest source = new Quest("Tutorial");
    source.setCategory("Story");
    source.setDisplayName("<gold>Tutorial");
    source.setDescription("Meet the guide");
    source.setMaxCompletions(2);
    source.setMaxAccepts(3);
    source.setMaxFails(4);
    source.setAcceptCooldownComplete(60);
    source.setTakeEnabled(false);
    source.setAbortEnabled(false);
    source.setGuiItem(ItemStackSelection.of(List.of("book"), List.of("GuideBook"), List.of(
        Map.of("platform", "paper", "data", Map.of("lore", List.of("Read me")))), false, 2));
    source.setGuiItemGlow(true);
    source.setObjectiveProgressOrder("firstToLast");
    source.addNpcAttachment("citizens", NQNPCID.fromInteger(7), "Guide", true);
    final var parent = source.addObjective(3, "Objective", null, "Complete these steps");
    parent.apply(new Quest.ObjectiveSettings("Steps", "In order", "Complete these steps",
        "firstToLast", true, "citizens:7", NQLocation.at("world", 1, 2, 3, 90, 45)));
    final var child = parent.addChildObjective(8, "Objective", null, "A nested group");
    final var grandchild = child.addChildObjective(12, "BreakBlocks", null, "Break stone");
    grandchild.setValue("items", ItemStackSelection.parse("stone").withAmount(4));
    grandchild.setValue("nested", new LinkedHashMap<>(Map.of("messages", new ArrayList<>(List.of("hello")))));
    for (final String group : List.of("unlock", "progress", "complete")) {
      final var condition = parent.addCondition(group, 5, "Sneaking", null);
      condition.apply(new Quest.ConditionSettings(2, true, "A condition", "true", true));
      condition.setValue("values", new ArrayList<>(List.of("original")));
    }
    final var reward = parent.addReward(6, "SendMessage", null);
    reward.setDisplayName("Greeting");
    reward.setDescription("Say hello");
    reward.setValue("delay", Duration.ofSeconds(2));
    reward.addCondition(4, "Flying", null).setDescription("Reward condition");
    source.addRequirement(9, "Flying", null).setNegated(true);
    source.addReward(10, "GiveItem", null).setValue("items", source.getGuiItemSelection());
    source.addTrigger(11, "BEGIN", null).setValue("action", "Welcome");

    final Quest copy = source.copy("TutorialHard");

    assertEquals("TutorialHard", copy.getIdentifier());
    assertEquals(source.getCategory(), copy.getCategory());
    assertEquals(source.getDisplayName(), copy.getDisplayName());
    assertEquals(source.getDescription(), copy.getDescription());
    assertEquals(source.getMaxCompletions(), copy.getMaxCompletions());
    assertEquals(source.getMaxAccepts(), copy.getMaxAccepts());
    assertEquals(source.getMaxFails(), copy.getMaxFails());
    assertEquals(source.getAcceptCooldownComplete(), copy.getAcceptCooldownComplete());
    assertEquals(source.isTakeEnabled(), copy.isTakeEnabled());
    assertEquals(source.isAbortEnabled(), copy.isAbortEnabled());
    assertEquals(source.isGuiItemGlow(), copy.isGuiItemGlow());
    assertEquals(ItemStackSelection.toYamlValue(source.getGuiItemSelection()),
        ItemStackSelection.toYamlValue(copy.getGuiItemSelection()));
    assertNotSame(source.getGuiItemSelection(), copy.getGuiItemSelection());
    assertEquals(source.getObjectiveProgressOrder(), copy.getObjectiveProgressOrder());
    assertEquals(source.getNpcAttachments(), copy.getNpcAttachments());
    final var copiedParent = copy.getObjectiveFromID(3);
    assertEquals(Quest.ObjectiveSettings.from(parent), Quest.ObjectiveSettings.from(copiedParent));
    final var copiedGrandchild = copiedParent.getObjectiveFromID(8).getObjectiveFromID(12);
    assertEquals("BreakBlocks", copiedGrandchild.typeId());
    assertEquals("Break stone", copiedGrandchild.getTaskDescription());
    assertEquals(4, copiedGrandchild.itemSelection("items").amount());
    assertNotSame(grandchild.itemSelection("items"), copiedGrandchild.itemSelection("items"));
    assertInstanceOf(List.class, assertInstanceOf(Map.class, copiedGrandchild.value("nested")).get("messages")).clear();
    assertEquals(List.of("hello"), assertInstanceOf(Map.class, grandchild.value("nested")).get("messages"));
    for (final String group : List.of("unlock", "progress", "complete")) {
      final var copiedCondition = copiedParent.getConditionFromID(group, 5);
      assertEquals(Quest.ConditionSettings.from(parent.getConditionFromID(group, 5)),
          Quest.ConditionSettings.from(copiedCondition));
      assertInstanceOf(List.class, copiedCondition.value("values")).clear();
      assertEquals(List.of("original"), parent.getConditionFromID(group, 5).value("values"));
    }
    final var copiedReward = copiedParent.getRewardFromID(6);
    assertEquals(reward.values(), copiedReward.values());
    assertEquals(reward.getDisplayName(), copiedReward.getDisplayName());
    assertEquals(reward.getDescription(), copiedReward.getDescription());
    assertEquals(4, copiedReward.getConditions().getFirst().id());
    copiedReward.getConditions().getFirst().setDescription("Changed");
    assertEquals("Reward condition", reward.getConditions().getFirst().getDescription());
    assertTrue(copy.getRequirementFromID(9).isNegated());
    copy.getRequirementFromID(9).setNegated(false);
    assertTrue(source.getRequirementFromID(9).isNegated());
    assertNotSame(source.getRewards().getFirst().value("items"), copy.getRewards().getFirst().value("items"));
    assertEquals(11, copy.getTriggers().getFirst().id());
    assertEquals("Welcome", copy.getTriggers().getFirst().text("action"));
    copy.getTriggers().getFirst().setValue("action", "Changed");
    assertEquals("Welcome", source.getTriggers().getFirst().text("action"));
    copy.clearNpcAttachments();
    copiedParent.clearChildObjectives();
    assertEquals(1, source.getNpcAttachments().size());
    assertEquals(1, parent.getObjectives().size());
  }

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
