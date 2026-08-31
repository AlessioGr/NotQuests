package com.notquests.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.actions.SavedActions.ActionCondition;
import com.notquests.core.actions.SavedActions.Chain;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.conversation.ConversationManager;
import com.notquests.core.conversation.Speaker;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.managers.DataManager;
import com.notquests.core.managers.PlayerDatabase;
import com.notquests.core.migrations.ConfigurationMigrations;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.npc.NpcAttachments;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Pack;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.structs.Category;
import com.notquests.core.structs.Quest.AcceptCheck;
import com.notquests.core.structs.Quest.ConditionSettings;
import com.notquests.core.structs.Quest.CooldownDisplay;
import com.notquests.core.structs.Quest.ObjectiveSettings;
import com.notquests.core.structs.Quest.TriggerSettings;
import com.notquests.core.structs.Quest;
import com.notquests.core.structs.QuestPlayer.CompletedObjective;
import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer.FailedQuest;
import com.notquests.core.structs.QuestPlayer;
import com.notquests.core.test.TestPlatformPlayer;
import com.notquests.core.text.NotQuestsColors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class NotQuestsPluginTest {
    @Test
    void rejectsUnknownTopLevelObjectiveTypesBeforeConfiguredDataBecomesReady() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("broken").addObjective(
                1,
                "MissingObjectiveType",
                new com.notquests.core.TestData(Map.of()),
                "");

        assertFalse(plugin.finishConfiguredDataLoad());
        assertTrue(plugin.pluginStatus().isDisabled());
        assertFalse(plugin.pluginStatus().isConfiguredDataLoaded());
    }

    @Test
    void rejectsUnknownNestedObjectiveTypesBeforeConfiguredDataBecomesReady() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.createRegistryAdapter(null).objectives().objective("Group")
                .displayName("Group")
                .description("Groups objectives.")
                .register();
        final com.notquests.core.objectives.Objective parent = plugin.getOrCreateQuest("broken").addObjective(
                1,
                "Group",
                new com.notquests.core.TestData(Map.of()),
                "");
        parent.addChildObjective(
                4,
                "MissingNestedType",
                new com.notquests.core.TestData(Map.of()),
                "");

        assertFalse(plugin.finishConfiguredDataLoad());
        assertTrue(plugin.pluginStatus().isDisabled());
        assertFalse(plugin.pluginStatus().isConfiguredDataLoaded());
    }

    @Test
    void ownsQuestStateOperationsWithoutPlatformAdapterState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");

        plugin.getOrCreateQuest("ExampleQuest");
        plugin.giveQuest(player, "ExampleQuest", false, player.messages::add);
        plugin.completeQuest(player, "ExampleQuest", player.messages::add);

        assertEquals(List.of("ExampleQuest"), plugin.questNames());
        assertEquals(List.of(true), player.forcedQuestCompletions);
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Completed]")));
    }

    @Test
    void questChangesDoNotSaveUnrelatedPlayerData() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final AtomicInteger configuredSaves = new AtomicInteger();
        final AtomicInteger playerSaves = new AtomicInteger();
        plugin.dataManager(new DataManager() {
            @Override
            public boolean saveConfiguredData() {
                configuredSaves.incrementAndGet();
                return true;
            }

            @Override
            public boolean savePlayerRuntime() {
                playerSaves.incrementAndGet();
                return true;
            }
        });

        assertTrue(plugin.createQuest("ScopedQuest", plugin.defaultCategoryName()).success());
        assertEquals(1, configuredSaves.get());
        assertEquals(0, playerSaves.get());

        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "ScopedQuest", false, player.messages::add));
        assertTrue(plugin.completeQuest(player, "ScopedQuest", player.messages::add));
        assertEquals(1, configuredSaves.get());
        assertEquals(0, playerSaves.get());
    }

    @Test
    void forcedCompletionMarksRemainingObjectivesAndReportsForcedEvent() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .register();
        final Quest quest = plugin.getOrCreateQuest("ForcedQuest");
        quest.addObjective(1, "Jump", new com.notquests.core.TestData(Map.of("amount", 5)), "");
        final TestPlayer player = new TestPlayer("forced-player");
        assertTrue(plugin.giveQuest(player, quest.getIdentifier(), false, player.messages::add));
        final ActiveObjective remaining = plugin.activeObjectiveProgress(player, quest.getIdentifier(), 1);

        assertTrue(plugin.forceCompleteQuest(player, quest.getIdentifier(), player.messages::add));

        assertEquals(List.of(true), player.forcedQuestCompletions);
        assertTrue(remaining.hasBeenCompleted());
        assertEquals(remaining.progressNeeded(), remaining.currentProgress());
        assertTrue(plugin.activeObjectives(player.playerIdentifier()).isEmpty());
        assertTrue(plugin.questPlayer(player.playerIdentifier(), "default").hasCompletedQuest(quest.getIdentifier()));
    }

    @Test
    void naturalObjectiveCompletionReportsNotForced() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Jumps required.")
                .onPlayerJump(objective -> objective.addProgress(1))
                .register();
        final Quest quest = plugin.getOrCreateQuest("NaturalQuest");
        quest.addObjective(1, "Jump", new com.notquests.core.TestData(Map.of("amount", 1)), "");
        final TestPlayer player = new TestPlayer("natural-player");
        assertTrue(plugin.giveQuest(player, quest.getIdentifier(), false, player.messages::add));

        plugin.playerJumped(player);

        assertEquals(List.of(false), player.forcedQuestCompletions);
        assertTrue(plugin.questPlayer(player.playerIdentifier(), "default").hasCompletedQuest(quest.getIdentifier()));
    }

    @Test
    void alwaysOwnsDefaultCategoryAndCreatesQuestsInsideIt() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());
        assertEquals(Category.DEFAULT_NAME, plugin.defaultCategoryName());
        assertEquals(List.of(Category.DEFAULT_NAME), plugin.topLevelCategoryNames());
        assertEquals(Category.DEFAULT_NAME, plugin.getOrCreateQuest("ExampleQuest").getCategory());

        plugin.getOrCreateQuest("ExampleQuest").setCategory("");
        assertEquals(Category.DEFAULT_NAME, plugin.getOrCreateQuest("ExampleQuest").getCategory());

        assertTrue(plugin.createCategory("story"));
        assertTrue(plugin.createCategory("story.side"));
        assertEquals(List.of(Category.DEFAULT_NAME, "story"), plugin.topLevelCategoryNames());

        plugin.clearStoredData();
        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());
    }

    @Test
    void categoriesAreCaseInsensitiveWithoutDuplicatingDisplayState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        assertFalse(plugin.createCategory("DEFAULT"));
        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());

        assertTrue(plugin.createCategory("Story"));
        assertFalse(plugin.createCategory("story"));
        assertEquals(List.of(Category.DEFAULT_NAME, "Story"), plugin.categoryNames());
        assertEquals("Story", plugin.category("story").getIdentifier());

        plugin.getOrCreateCategory("STORY").setDisplayName("Storyline");
        assertEquals("Storyline", plugin.category("Story").getDisplayName());
        assertEquals(List.of(Category.DEFAULT_NAME, "Story"), plugin.categoryNames());
    }

    @Test
    void ownsPlayerPlacedHarvestBlockTrackingForAllAdapters() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        plugin.trackPlayerPlacedHarvestBlock("world:1:2:3", "wheat", true);
        plugin.trackPlayerPlacedHarvestBlock("world:4:5:6", "wheat", false);

        assertTrue(plugin.isPlayerPlacedHarvestBlock("world:1:2:3"));
        assertFalse(plugin.isPlayerPlacedHarvestBlock("world:4:5:6"));

        plugin.clearPlayerPlacedHarvestBlock("world:1:2:3");

        assertFalse(plugin.isPlayerPlacedHarvestBlock("world:1:2:3"));
    }

    @Test
    void categoryChangesAreOwnedByCoreAndSentToPersistence() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> savedCategories = new ArrayList<>();
        plugin.dataManager(new DataManager() {
            @Override
            public boolean save() {
                return true;
            }

            @Override
            public boolean saveConfiguredData() {
                return true;
            }

            @Override
            public boolean savePlayerRuntime() {
                return true;
            }

            @Override
            public boolean loadPlayerRuntime(final boolean firstLoad) {
                return true;
            }

            @Override
            public boolean reload(final ReloadTarget target) {
                return true;
            }

            @Override
            public boolean saveCategory(final com.notquests.core.structs.Category category) {
                savedCategories.add(category.getIdentifier()
                        + ":"
                        + category.getDisplayName()
                        + ":"
                        + category.getProgressOrder()
                        + ":"
                        + category.getGuiItem());
                return true;
            }
        });

        assertTrue(plugin.createCategory("Story"));
        assertTrue(plugin.setCategoryDisplayName("Story", "Story Quests"));
        assertTrue(plugin.setCategoryProgressOrder("Story", "firstToLast"));
        assertTrue(plugin.setCategoryGuiItem("Story", "diamond", true));

        assertEquals("Story Quests", plugin.category("Story").getDisplayName());
        assertEquals("Story Quests", plugin.categoryDisplayNameOrIdentifier("Story"));
        assertEquals("firstToLast", plugin.category("Story").getProgressOrder());
        assertEquals("diamond", plugin.category("Story").getGuiItem());
        assertTrue(plugin.category("Story").isGuiItemGlow());
        assertEquals(
                List.of(
                        "Story:::",
                        "Story:Story Quests::",
                        "Story:Story Quests:firstToLast:",
                        "Story:Story Quests:firstToLast:diamond"),
                savedCategories);
    }

    @Test
    void questMetadataChangesAreOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("StoryQuest");

        assertTrue(plugin.setQuestCategory("StoryQuest", "Story"));
        assertTrue(plugin.setQuestDisplayName("StoryQuest", "Story Display"));
        assertTrue(plugin.setQuestDescription("StoryQuest", "Story description."));
        assertTrue(plugin.setQuestMaxCompletions("StoryQuest", 3));
        assertTrue(plugin.setQuestMaxAccepts("StoryQuest", 4));
        assertTrue(plugin.setQuestMaxFails("StoryQuest", 5));
        assertTrue(plugin.setQuestTakeEnabled("StoryQuest", false));
        assertTrue(plugin.setQuestAbortEnabled("StoryQuest", false));
        assertTrue(plugin.setQuestAcceptCooldownComplete("StoryQuest", 250L));
        assertTrue(plugin.setQuestGuiItem("StoryQuest", ItemStackSelection.parse("diamond"), true));

        final var quest = plugin.quest("StoryQuest");
        assertEquals("Story", quest.getCategory());
        assertEquals("Story Display", quest.getDisplayName());
        assertEquals("Story description.", quest.getDescription());
        assertEquals(3, quest.getMaxCompletions());
        assertEquals(4, quest.getMaxAccepts());
        assertEquals(5, quest.getMaxFails());
        assertFalse(quest.isTakeEnabled());
        assertFalse(quest.isAbortEnabled());
        assertEquals(250L, quest.getAcceptCooldownComplete());
        assertEquals("Story", plugin.questCategory("StoryQuest"));
        assertEquals("Story Display", plugin.questDisplayName("StoryQuest"));
        assertEquals("Story Display", plugin.questDisplayNameOrIdentifier("StoryQuest"));
        assertEquals("Story description.", plugin.questDescription("StoryQuest"));
        assertEquals(List.of("Story", "description."), plugin.questDescriptionLines("StoryQuest", 5));
        assertEquals("Story\ndescription.", plugin.questDescriptionWrapped("StoryQuest", 5));
        assertEquals(3, plugin.questMaxCompletions("StoryQuest"));
        assertEquals(4, plugin.questMaxAccepts("StoryQuest"));
        assertEquals(5, plugin.questMaxFails("StoryQuest"));
        assertFalse(plugin.questTakeEnabled("StoryQuest"));
        assertFalse(plugin.questAbortEnabled("StoryQuest"));
        assertEquals(250L, plugin.questAcceptCooldownComplete("StoryQuest"));
        assertEquals("diamond", plugin.questGuiItemSelection("StoryQuest").listedMaterials(""));

        assertTrue(plugin.clearQuestDisplayName("StoryQuest"));
        assertTrue(plugin.clearQuestDescription("StoryQuest"));
        assertEquals("", quest.getDisplayName());
        assertEquals("", quest.getDescription());
        assertEquals("StoryQuest", plugin.questDisplayNameOrIdentifier("StoryQuest"));
    }

    @Test
    void categoryQuestOrderRequirementIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.getOrCreateQuest("AlphaQuest").setDisplayName("Alpha");
        plugin.getOrCreateQuest("BetaQuest").setDisplayName("Beta");
        plugin.setCategoryProgressOrder(Category.DEFAULT_NAME, "firstToLast");

        assertFalse(plugin.canAcceptQuest(player, "BetaQuest"));
        assertEquals(
                "Quest Alpha needs to be completed first",
                plugin.questOrderRequirementMessage(player, plugin.quest("BetaQuest")));

        plugin.questPlayer("player-1", "default")
                .addCompletedQuest(new QuestPlayer.CompletedQuest("AlphaQuest", "player-1", 1000L));

        assertTrue(plugin.canAcceptQuest(player, "BetaQuest"));
        assertEquals("", plugin.questOrderRequirementMessage(player, plugin.quest("BetaQuest")));
    }

    @Test
    void npcAttachmentLookupIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID shownNpc = NQNPCID.fromInteger(12);
        final NQNPCID hiddenNpc = NQNPCID.fromInteger(13);

        plugin.getOrCreateQuest("ShownQuest").addNpcAttachment("citizens", shownNpc, "Merlin", true);
        plugin.getOrCreateQuest("HiddenQuest").addNpcAttachment("citizens", hiddenNpc, "Guard", false);
        plugin.getOrCreateQuest("OtherQuest").addNpcAttachment("fancynpcs", NQNPCID.fromString("merchant"), "Merchant", true);

        assertEquals(
                List.of("ShownQuest"),
                plugin.questsAttachedToNpc("citizens", shownNpc, true).stream()
                        .map(com.notquests.core.structs.Quest::getIdentifier)
                        .toList());
        assertEquals(
                List.of("HiddenQuest"),
                plugin.questsAttachedToNpc("citizens", hiddenNpc, false).stream()
                        .map(com.notquests.core.structs.Quest::getIdentifier)
                        .toList());
        assertEquals(
                List.of("ShownQuest"),
                plugin.questsAttachedToNpc("citizens", shownNpc).stream()
                        .map(com.notquests.core.structs.Quest::getIdentifier)
                        .toList());
        assertTrue(plugin.questsAttachedToNpc("citizens", shownNpc, false).isEmpty());
        assertEquals(List.of("merchant"), plugin.npcIdsWithShowingQuest("fancynpcs").stream()
                .map(NQNPCID::getStringID)
                .toList());
    }

    @Test
    void npcAttachmentRemovalTraitPolicyIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npc = NQNPCID.fromInteger(12);
        plugin.getOrCreateQuest("QuestA").addNpcAttachment("citizens", npc, "Guide", true);
        plugin.getOrCreateQuest("QuestB").addNpcAttachment("citizens", npc, "Guide", false);

        assertFalse(plugin.shouldRemovePlatformQuestNpcTraitAfterAttachmentRemoval("QuestA", "citizens", npc));
        assertEquals(1, plugin.questNpcDetachments("QuestA").attachments().size());
        assertFalse(plugin.questNpcDetachments("QuestA").attachments().getFirst().removePlatformTrait());

        assertTrue(plugin.removeQuestNpcAttachment("QuestA", "citizens", npc));
        assertTrue(plugin.shouldRemovePlatformQuestNpcTraitAfterAttachmentRemoval("QuestB", "citizens", npc));
        assertTrue(plugin.questNpcDetachments("QuestB").attachments().getFirst().removePlatformTrait());
    }

    @Test
    void npcAttachmentRemovalByPlatformNpcIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npc = NQNPCID.fromInteger(12);
        plugin.getOrCreateQuest("QuestA").addNpcAttachment("citizens", npc, "Guide", true);
        plugin.getOrCreateQuest("QuestB").addNpcAttachment("citizens", npc, "Guide", false);

        final var plan = plugin.detachNpcFromQuests("citizens", npc);

        assertEquals(List.of("QuestA", "QuestB"), plan.attachments().stream()
                .map(com.notquests.core.npc.NpcAttachments.Detachment::questIdentifier)
                .toList());
        assertEquals(List.of(false, true), plan.attachments().stream()
                .map(com.notquests.core.npc.NpcAttachments.Detachment::removePlatformTrait)
                .toList());
        assertTrue(plugin.questsAttachedToNpc("citizens", npc).isEmpty());
    }

    @Test
    void configuredObjectiveMetadataReadsAreOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var location = new TestLocation("world", 1, 2, 3);

        plugin.getOrCreateQuest("ObjectiveQuest");
        assertTrue(plugin.syncConfiguredObjective(
                "ObjectiveQuest",
                new int[] {1},
                "BreakBlocks",
                new com.notquests.core.TestData(Map.of("progressNeededExpression", "10")),
                new Quest.ObjectiveSettings(
                        "Break Dirt",
                        "Breaks dirt blocks.",
                        "Break 10 dirt.",
                        "firstToLast",
                        true,
                        "armorstand:123",
                        location)));
        assertTrue(plugin.syncConfiguredObjective(
                "ObjectiveQuest",
                new int[] {1, 2},
                "Jump",
                new com.notquests.core.TestData(Map.of("progressNeededExpression", "5")),
                new Quest.ObjectiveSettings("Jump", "", "Jump 5 times.", "", false, "", null)));

        assertEquals("BreakBlocks", plugin.configuredObjectiveTypeId("ObjectiveQuest", new int[] {1}));
        assertEquals("Break Dirt", plugin.configuredObjectiveDisplayName("ObjectiveQuest", new int[] {1}));
        assertEquals("Break 10 dirt.", plugin.configuredObjectiveTaskDescription("ObjectiveQuest", new int[] {1}));
        assertEquals("10", plugin.configuredObjectiveProgressNeededExpression("ObjectiveQuest", new int[] {1}));
        assertEquals("armorstand:123", plugin.configuredObjectiveCompletionNpc("ObjectiveQuest", new int[] {1}));
        assertEquals(location, plugin.configuredObjectiveLocation("ObjectiveQuest", new int[] {1}));
        assertTrue(plugin.configuredObjectiveLocationEnabled("ObjectiveQuest", new int[] {1}));
        assertEquals("Jump", plugin.configuredObjectiveTypeId("ObjectiveQuest", new int[] {1, 2}));
        assertEquals("5", plugin.configuredObjectiveProgressNeededExpression("ObjectiveQuest", new int[] {1, 2}));

        assertTrue(plugin.setConfiguredObjectiveProgressNeededExpression("ObjectiveQuest", new int[] {1, 2}, "8"));
        assertEquals("8", plugin.configuredObjectiveProgressNeededExpression("ObjectiveQuest", new int[] {1, 2}));
        assertTrue(plugin.setConfiguredObjectiveCompletionNpc("ObjectiveQuest", new int[] {1, 2}, "armorstand:456"));
        assertEquals("armorstand:456", plugin.configuredObjectiveCompletionNpc("ObjectiveQuest", new int[] {1, 2}));
    }

    @Test
    void configuredRewardDetailsAreOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var quest = plugin.getOrCreateQuest("RewardQuest");
        final var questReward = quest.addReward(4, "Notify", new com.notquests.core.TestData(Map.of(
                "executionDelayMillis", 250L)));
        questReward.setDisplayName("Quest Reward");
        final var objective = quest.addObjective(1, "BreakBlocks", new com.notquests.core.TestData(Map.of()), "");
        final var objectiveReward = objective.addReward(2, "Notify", new com.notquests.core.TestData(Map.of(
                "executionDelayMillis", 500L)));
        objectiveReward.setDisplayName("Objective Reward");

        assertEquals("Quest Reward", plugin.questRewardSettings("RewardQuest", 4, "").name());
        assertEquals(250L, plugin.questRewardSettings("RewardQuest", 4, "").executionDelayMillis());
        assertEquals(
                "Objective Reward",
                plugin.configuredObjectiveRewardSettings("RewardQuest", new int[] {1}, 2, "").name());
        assertEquals(
                500L,
                plugin.configuredObjectiveRewardSettings("RewardQuest", new int[] {1}, 2, "").executionDelayMillis());

        assertTrue(plugin.setQuestRewardDisplayName("RewardQuest", 4, "Renamed Quest Reward"));
        assertTrue(plugin.setConfiguredObjectiveRewardDisplayName(
                "RewardQuest",
                new int[] {1},
                2,
                "Renamed Objective Reward"));
        assertEquals("Renamed Quest Reward", plugin.questRewardSettings("RewardQuest", 4, "").name());
        assertEquals(
                "Renamed Objective Reward",
                plugin.configuredObjectiveRewardSettings("RewardQuest", new int[] {1}, 2, "").name());
    }

    @Test
    void metricsCountsAreOwnedByCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Notify")
                .displayName("Notify")
                .description("Sends a notification.")
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Static condition.")
                .register();
        adapter.triggers()
                .trigger("WorldEnter")
                .displayName("World Enter")
                .description("Runs when a player enters a world.")
                .register();

        final var quest = plugin.getOrCreateQuest("MetricsQuest");
        final var objective = quest.addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of()), "");
        objective.addChildObjective("Jump", new com.notquests.core.TestData(Map.of()), "");
        objective.addReward("Notify", new com.notquests.core.TestData(Map.of()));
        objective.addCondition("unlock", "Static", new com.notquests.core.TestData(Map.of()));
        quest.addReward("Number", new com.notquests.core.TestData(Map.of("variableName", "QuestPoints")));
        quest.addRequirement("Boolean", new com.notquests.core.TestData(Map.of("variableName", "Flying")));
        quest.addTrigger("WorldEnter", new com.notquests.core.TestData(Map.of()));
        assertTrue(plugin.syncSavedAction(
                "SavedNotify",
                "Notify",
                new com.notquests.core.TestData(Map.of()),
                null,
                "",
                List.of()));
        assertTrue(plugin.syncSavedCondition(
                "SavedStatic",
                "Static",
                new com.notquests.core.TestData(Map.of()),
                "",
                null));
        plugin.conversationManager().save("Intro", List.of("Hello"));

        assertEquals(1, plugin.questCount());
        assertEquals(1, plugin.conversationCount());
        assertEquals(Map.of("BreakBlocks", 1), plugin.objectiveTypeCounts());
        assertEquals(Map.of("Notify", 2, "QuestPoints", 1), plugin.actionTypeCounts());
        assertEquals(Map.of("Static", 2, "Flying", 1), plugin.conditionTypeCounts());
        assertEquals(Map.of("WorldEnter", 1), plugin.triggerTypeCounts());

        final NotQuestsPlugin.Metrics metrics = plugin.metrics();
        assertEquals(12824, metrics.pluginId());
        assertEquals(1, metrics.singleLineCharts().get("quests").get());
        assertEquals(1, metrics.singleLineCharts().get("conversations").get());
        assertEquals(Map.of("BreakBlocks", 1), metrics.advancedPieCharts().get("ObjectiveTypes").get());
        assertEquals(Map.of("Notify", 2, "QuestPoints", 1), metrics.advancedPieCharts().get("AllActionTypes").get());
    }

    @Test
    void questPreviewDetailsAreRenderedFromCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Notify")
                .displayName("Notify")
                .description("Sends a notification.")
                .actionDescription((action, questPlayer, objects) -> "Notify " + action.text("message"))
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Static condition.")
                .conditionDescription((condition, questPlayer, objects) -> "Need " + condition.text("name"))
                .register();
        final Quest quest = plugin.getOrCreateQuest("PreviewQuest");
        quest.setDisplayName("Preview Quest");
        quest.setDescription("Quest description.");
        quest.addRequirement("Static", new com.notquests.core.TestData(Map.of("name", "permission")));
        quest.addReward("Notify", new com.notquests.core.TestData(Map.of("message", "player")));

        final TestPlayer player = new TestPlayer("player-1");

        assertEquals(
                "<GREEN>1. <YELLOW>Static\nNeed permission\n",
                plugin.questRequirementsText(player, "PreviewQuest"));
        assertEquals(
                "<GREEN>1. <blue>Reward hidden</blue></GREEN>",
                plugin.questRewardsText(player, "PreviewQuest"));
        assertEquals(
                "nquests take PreviewQuest",
                plugin.singleQuestPreview(player, "PreviewQuest").acceptCommand());
        assertTrue(plugin.singleQuestPreview(player, "PreviewQuest")
                .linesBeforeAccept()
                .contains("<YELLOW>Quest description: <GRAY>Quest description."));
    }

    @Test
    void objectiveAdminDisplayIsRenderedFromCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Breaks selected blocks.")
                .taskDescription((objective, questPlayer, activeObjective) -> "Break " + objective.text("materials"))
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Static condition.")
                .conditionDescription((condition, questPlayer, objects) -> "Need " + condition.text("name"))
                .register();
        final Quest quest = plugin.getOrCreateQuest("AdminQuest");
        final com.notquests.core.objectives.Objective objective =
                quest.addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("materials", "dirt")), "Break dirt.");
        objective.setDescription("Objective details.");
        objective.addCondition("unlock", "Static", new com.notquests.core.TestData(Map.of("name", "permission")));

        final List<String> lines = plugin.objectiveAdminLines(new TestPlayer("player-1"), "AdminQuest", new int[0]);

        assertTrue(lines.contains("<highlight>1.</highlight> <main>Break Blocks"));
        assertTrue(lines.contains("   <highlight>Description:</highlight> <main>Objective details."));
        assertTrue(lines.contains("         <highlight>1.</highlight> <main>Condition:</main> <highlight2>Need permission"));
        assertTrue(lines.stream().anyMatch(line -> line.contains("Break dirt.")));
    }

    @Test
    void questHistoryRecordingIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.recordCompletedQuest("player-1", "default", "StoryQuest", 1000L);
        plugin.recordFailedQuest("player-1", "default", "DangerQuest", 2000L);

        final var state = plugin.questPlayer("player-1", "default");
        assertEquals("StoryQuest", state.getCompletedQuests().getFirst().questIdentifier());
        assertEquals(1000L, state.getCompletedQuests().getFirst().timeCompleted());
        assertEquals("DangerQuest", state.getFailedQuests().getFirst().questIdentifier());
        assertEquals(2000L, state.getFailedQuests().getFirst().timeFailed());
    }

    @Test
    void activePlayerProfileCreationIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        assertEquals("default", plugin.activeProfile("player-1"));
        assertEquals("builder", plugin.activatePlayerProfile("player-1", "builder").getProfile());
        assertEquals("builder", plugin.activeProfile("player-1"));
        assertEquals(List.of("builder"), plugin.playerProfileNames("player-1"));
    }

    @Test
    void changingProfileKeepsTheSamePlatformPlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.registerQuestPlayer(player, "default", true);
        plugin.createPlayerProfile("player-1", "builder");

        assertTrue(plugin.changePlayerProfile("player-1", "builder"));
        assertTrue(plugin.questPlayer("player-1", "builder").isFinishedLoadingGeneralData());
        assertSame(player, plugin.activePlatformPlayer("player-1"));
        assertSame(player, plugin.platformPlayer("player-1"));
    }

    @Test
    void acceptingQuestActivatesConfiguredObjectivesAndShowsInitialProgress() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        assertTrue(plugin.createQuest("ExampleQuest").success());
        plugin.getOrCreateQuest("ExampleQuest")
                .addObjective("BreakBlocks", Objectives.parse(
                        adapter, plugin.registry().objectives().getFirst(), "3"), "");
        final TestPlayer player = new TestPlayer("player-1");

        plugin.giveQuest(player, "ExampleQuest", false, player.messages::add);

        assertEquals(1, plugin.activeObjectives("player-1").size());
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("Objectives:")));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("Break Blocks")));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains("0 / 3")));
    }

    @Test
    void ownsTriggerMirrorStateById() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("ExampleQuest");

        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                2,
                "BEGIN",
                new com.notquests.core.TestData(Map.of(
                        "action", "Notify",
                        "applyOn", 1,
                        "amount", 3,
                        "worldName", "world"))));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                2,
                "COMPLETE",
                new com.notquests.core.TestData(Map.of(
                        "action", "Done",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "ALL"))));

        final com.notquests.core.triggers.Trigger trigger =
                plugin.quest("ExampleQuest").getTriggerFromID(2);
        assertEquals("COMPLETE", trigger.typeId());
        assertEquals("Done", trigger.data().text("action"));
        assertEquals(0, trigger.data().integer("applyOn", -1));
        assertEquals(1, trigger.data().integer("amount", -1));
        assertEquals("ALL", trigger.data().text("worldName"));
        assertEquals(1, plugin.quest("ExampleQuest").getTriggers().size());
    }

    @Test
    void ownsTriggerMirrorDetails() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("ExampleQuest");

        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                1,
                "BEGIN",
                new com.notquests.core.TestData(Map.of("custom", "kept")),
                new Quest.TriggerSettings("Notify", 2, 5L, "")));

        final com.notquests.core.triggers.Trigger trigger =
                plugin.quest("ExampleQuest").getTriggerFromID(1);
        assertEquals("kept", trigger.data().text("custom"));
        assertEquals("Notify", trigger.data().text("action"));
        assertEquals(2, trigger.data().integer("applyOn", -1));
        assertEquals(5, trigger.data().integer("amount", -1));
        assertFalse(trigger.data().values().containsKey("amountNeeded"));
        assertEquals("ALL", trigger.data().text("worldName"));
    }

    @Test
    void disconnectTriggerFlowIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> executions = new ArrayList<>();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Record")
                .displayName("Record")
                .description("Records that an action ran.")
                .execute((action, questPlayer, objects) -> executions.add(questPlayer.playerIdentifier()))
                .register();
        plugin.getOrCreateQuest("ExampleQuest");
        assertTrue(plugin.syncSavedAction(
                "OnDisconnect",
                "Record",
                new com.notquests.core.TestData(Map.of()),
                null,
                "default",
                List.of()));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                1,
                "DISCONNECT",
                new com.notquests.core.TestData(Map.of(
                        "action", "OnDisconnect",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "world"))));
        final TestPlayer player = new TestPlayer("player-1");

        plugin.playerDisconnected(player, "world");
        assertEquals(List.of(), executions);

        plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
        plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});
        plugin.playerDisconnected(player, "world");
        assertEquals(List.of("player-1"), executions);
    }

    @Test
    void worldChangeTriggerFlowIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> executions = new ArrayList<>();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Record")
                .displayName("Record")
                .description("Records that an action ran.")
                .execute((action, questPlayer, objects) -> executions.add(action.text("marker")))
                .register();
        plugin.getOrCreateQuest("ExampleQuest");
        assertTrue(plugin.syncSavedAction(
                "OnEnter",
                "Record",
                new com.notquests.core.TestData(Map.of("marker", "enter")),
                null,
                "default",
                List.of()));
        assertTrue(plugin.syncSavedAction(
                "OnLeave",
                "Record",
                new com.notquests.core.TestData(Map.of("marker", "leave")),
                null,
                "default",
                List.of()));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                1,
                "WORLDENTER",
                new com.notquests.core.TestData(Map.of(
                        "action", "OnEnter",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "new_world"))));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                2,
                "WORLDLEAVE",
                new com.notquests.core.TestData(Map.of(
                        "action", "OnLeave",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "old_world"))));
        final TestPlayer player = new TestPlayer("player-1");

        plugin.playerChangedWorld(player, "old_world", "new_world");
        assertEquals(List.of(), executions);

        plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
        plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});
        plugin.playerChangedWorld(player, "old_world", "new_world");
        assertEquals(List.of("enter", "leave"), executions);
    }

    @Test
    void deathTriggerFlowIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> executions = new ArrayList<>();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Record")
                .displayName("Record")
                .description("Records that an action ran.")
                .execute((action, questPlayer, objects) -> executions.add(questPlayer.playerIdentifier()))
                .register();
        plugin.getOrCreateQuest("ExampleQuest");
        assertTrue(plugin.syncSavedAction(
                "OnDeath",
                "Record",
                new com.notquests.core.TestData(Map.of()),
                null,
                "default",
                List.of()));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                1,
                "DEATH",
                new com.notquests.core.TestData(Map.of(
                        "action", "OnDeath",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "ALL"))));
        final TestPlayer player = new TestPlayer("player-1");

        plugin.playerDied(player);
        assertEquals(List.of(), executions);

        plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
        plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});
        plugin.playerDied(player);
        assertEquals(List.of("player-1"), executions);
    }

    @Test
    void npcDeathTriggerUsesCanonicalLoadedNpcField() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> executions = new ArrayList<>();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Record")
                .displayName("Record")
                .description("Records that an action ran.")
                .execute((action, questPlayer, objects) -> executions.add(questPlayer.playerIdentifier()))
                .register();
        plugin.getOrCreateQuest("ExampleQuest");
        assertTrue(plugin.syncSavedAction(
                "OnNpcDeath",
                "Record",
                new com.notquests.core.TestData(Map.of()),
                null,
                "default",
                List.of()));
        assertTrue(plugin.syncQuestTrigger(
                "ExampleQuest",
                1,
                "NPCDEATH",
                new com.notquests.core.TestData(Map.of(
                        "action", "OnNpcDeath",
                        "applyOn", 0,
                        "amount", 1,
                        "worldName", "ALL",
                        "NPC", 12))));
        final TestPlayer player = new TestPlayer("player-1");

        plugin.npcDied(player, "12", "world");
        assertEquals(List.of(), executions);

        plugin.setActiveQuestNames(player, List.of("ExampleQuest"));
        plugin.activateQuestProgress(player, "ExampleQuest", ignored -> {});
        plugin.npcDied(player, "99", "world");
        assertEquals(List.of(), executions);

        plugin.npcDied(player, "12", "world");
        assertEquals(List.of("player-1"), executions);
    }

    @Test
    void syncsSavedActionAndConditionStateByRegistryId() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Notify")
                .displayName("Notify")
                .description("Test action.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Test condition.")
                .check((condition, questPlayer) -> "")
                .register();

        assertTrue(plugin.syncSavedCondition(
                "HasStatic",
                "Static",
                new com.notquests.core.TestData(Map.of("raw", "condition")),
                "daily",
                new Quest.ConditionSettings(8, true, "Need static", "HiddenStatic", false)));
        assertTrue(plugin.syncSavedAction(
                "DailyNotify",
                "Notify",
                new com.notquests.core.TestData(Map.of("message", "hello")),
                Duration.ofMillis(250),
                "daily",
                List.of(new ActionCondition(
                        4,
                        "Static",
                        new com.notquests.core.TestData(Map.of("raw", "nested")),
                        new Quest.ConditionSettings(3, false, "Nested static", "NestedHidden", false)))));

        final NotQuestsPlugin.StoredCondition savedCondition = plugin.savedCondition("HasStatic");
        assertEquals("daily", savedCondition.getCategory());
        assertEquals("Static", savedCondition.getType().id());
        assertEquals(8, savedCondition.getProgressNeeded());
        assertTrue(savedCondition.isNegated());
        assertEquals("Need static", savedCondition.getDescription());
        assertEquals("HiddenStatic", savedCondition.getHiddenExpression());

        final com.notquests.core.actions.SavedActions.SavedAction action =
                plugin.savedActions().action("DailyNotify");
        assertEquals("daily", action.getCategory());
        assertEquals("Notify", action.getType().id());
        assertEquals(Duration.ofMillis(250), action.getExecutionDelay());
        assertEquals("hello", action.getData().text("message"));
        final com.notquests.core.actions.SavedActions.SavedCondition condition = action.getConditionFromID(4);
        assertEquals("Static", condition.getType().id());
        assertEquals(3, condition.getProgressNeeded());
        assertEquals("Nested static", condition.getDescription());
        assertEquals("NestedHidden", condition.getHiddenExpression());
    }

    @Test
    void progressUpdatesActionbarAndBossbarIndependently() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "objective-tracking",
                        Map.of(
                                "actionbar", Map.of("enabled", false),
                                "bossbar", Map.of("enabled", true, "show-if-objective-is-completed", false))))));
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        assertTrue(plugin.createQuest("ExampleQuest").success());
        plugin.getOrCreateQuest("ExampleQuest")
                .addObjective("BreakBlocks", Objectives.parse(
                        adapter, plugin.registry().objectives().getFirst(), "3"), "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "ExampleQuest", false, player.messages::add));

        plugin.addActiveObjectiveProgress(player, "ExampleQuest", new int[] {1}, 1, "Break Blocks", "ExampleQuest");

        assertEquals(List.of(), player.actionBars);
        assertEquals(1, player.bossBars.size());
        assertTrue(player.bossBars.getFirst().contains("1 <unimportant>/ <main>3"));
    }

    @Test
    void coreExpiresObjectiveBossBarsForEveryPlatform() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "objective-tracking",
                        Map.of(
                                "actionbar", Map.of("enabled", false),
                                "bossbar", Map.of(
                                        "enabled", true,
                                        "show-time", 2,
                                        "show-if-objective-is-completed", false))))));
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Progress")
                .displayName("Progress")
                .description("Counts progress.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("ExampleQuest").addObjective(
                "Progress",
                new com.notquests.core.TestData(Map.of("amount", 3)),
                "");
        final TestPlayer player = new TestPlayer("player-1");
        plugin.registerQuestPlayer(player, "default", true);
        assertTrue(plugin.giveQuest(player, "ExampleQuest", false, player.messages::add));

        plugin.addActiveObjectiveProgress(
                player,
                "ExampleQuest",
                new int[] {1},
                1,
                "Progress",
                "ExampleQuest");
        plugin.questRuntimeSecondPassed();
        assertEquals(0, player.bossBarHides);
        plugin.questRuntimeSecondPassed();

        assertEquals(1, player.bossBarHides);
    }

    @Test
    void progressUpdatesUseIndependentLocalizedActionbarAndBossbarMessages() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "visual",
                Map.of(
                        "objective-tracking",
                        Map.of(
                                "actionbar", Map.of("enabled", true),
                                "bossbar", Map.of("enabled", true, "show-if-objective-is-completed", true))))));
        plugin.languageManager().configuration().set(
                "objective-tracking.actionbar-progress-update.default",
                "ACTION MANY %QUESTNAME% %OBJECTIVENAME% %ACTIVEOBJECTIVEPROGRESS%/%OBJECTIVEPROGRESSNEEDED%");
        plugin.languageManager().configuration().set(
                "objective-tracking.bossbar-progress-update.default",
                "BOSS MANY %QUESTNAME% %OBJECTIVENAME% %ACTIVEOBJECTIVEPROGRESS%/%OBJECTIVEPROGRESSNEEDED%");
        plugin.languageManager().configuration().set(
                "objective-tracking.actionbar-progress-update.only-one-max-progress",
                "ACTION ONE %QUESTNAME% %OBJECTIVENAME% %OBJECTIVEPROGRESSPERCENTAGE%%");
        plugin.languageManager().configuration().set(
                "objective-tracking.bossbar-progress-update.only-one-max-progress",
                "BOSS ONE %QUESTNAME% %OBJECTIVENAME% %OBJECTIVEPROGRESSPERCENTAGE%%");
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Progress")
                .displayName("Localized objective")
                .description("Counts progress.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("ManyQuest").addObjective(
                "Progress",
                new com.notquests.core.TestData(Map.of("amount", 3)),
                "");
        plugin.getOrCreateQuest("OneQuest").addObjective(
                1,
                "Progress",
                new com.notquests.core.TestData(Map.of("amount", 1)),
                "");
        plugin.getOrCreateQuest("OneQuest").addObjective(
                2,
                "Progress",
                new com.notquests.core.TestData(Map.of("amount", 2)),
                "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.giveQuest(player, "ManyQuest", false, player.messages::add));
        assertTrue(plugin.giveQuest(player, "OneQuest", false, player.messages::add));
        player.actionBars.clear();
        player.bossBars.clear();

        plugin.addActiveObjectiveProgress(
                player,
                "ManyQuest",
                new int[] {1},
                1,
                "Localized objective",
                "ManyQuest");

        assertEquals(List.of("ACTION MANY ManyQuest Localized objective 1/3"), player.actionBars);
        assertEquals(1, player.bossBars.size());
        assertTrue(player.bossBars.getFirst().startsWith("BOSS MANY ManyQuest Localized objective 1/3 @ "));

        player.actionBars.clear();
        player.bossBars.clear();
        plugin.addActiveObjectiveProgress(
                player,
                "OneQuest",
                new int[] {1},
                1,
                "Localized objective",
                "OneQuest");

        assertEquals(List.of("ACTION ONE OneQuest Localized objective 100%"), player.actionBars);
        assertEquals(1, player.bossBars.size());
        assertTrue(player.bossBars.getFirst().startsWith("BOSS ONE OneQuest Localized objective 100% @ "));
    }

    @Test
    void ownsConversationStorageAndPlayback() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");

        plugin.saveConversation("intro", List.of("Hello", "Welcome"));
        plugin.startConversation(player, "intro", false, player.messages::add);

        assertEquals(List.of("intro"), plugin.conversationNames());
        assertEquals(List.of("Hello", "Welcome"), player.messages);
    }

    @Test
    void ownsConversationOptionStateWhileAdaptersRenderTheDisplay() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        final List<String> displayed = new ArrayList<>();
        plugin.conversationManager().display((questPlayer, message) -> displayed.add(message.miniMessage()));
        plugin.conversationManager().save("intro", Map.of(
                "start", "Guide.hello",
                "Lines", Map.of(
                        "Guide", Map.of(
                                "hello", Map.of("text", "Hello", "next", "Player.yes,Player.no")),
                        "Player", Map.of(
                                "player", true,
                                "yes", Map.of("text", "Yes"),
                                "no", Map.of("text", "No")))));

        assertTrue(plugin.startConversation(player, "intro", true, player.messages::add));
        assertEquals(4, displayed.size());
        assertTrue(displayed.get(0).contains("Hello"));
        assertTrue(displayed.get(1).contains("Choose your answer"));
        assertTrue(displayed.get(2).contains("Yes"));
        assertTrue(displayed.get(3).contains("No"));
        assertTrue(plugin.hasActiveConversation(player));

        assertTrue(plugin.chooseConversationOption(player, 2));
        assertFalse(plugin.hasActiveConversation(player));
    }

    @Test
    void ownsNpcConversationSessionStateAndReconcilesFinishedConversations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final UUID playerId = UUID.randomUUID();
        final TestPlayer player = new TestPlayer(playerId.toString());
        plugin.conversationManager().display((questPlayer, message) -> {});
        plugin.conversationManager().save("intro", Map.of(
                "start", "Guide.hello",
                "Lines", Map.of(
                        "Guide", Map.of(
                                "hello", Map.of("text", "Hello", "next", "Player.yes")),
                        "Player", Map.of(
                                "player", true,
                                "yes", Map.of("text", "Yes")))));

        assertTrue(plugin.startConversation(player, "intro", true, player.messages::add));
        plugin.markNpcConversationStarted(42, playerId);
        assertTrue(plugin.hasActiveConversationForNpc(42));

        assertTrue(plugin.chooseConversationOption(player, 1));
        assertFalse(plugin.hasActiveConversationForNpc(42));
        assertFalse(plugin.hasActiveConversationForNpc(42));
    }

    @Test
    void citizensSelectionRequiresAnAuthorizedLoadedPlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter registryAdapter = plugin.createRegistryAdapter(null);
        plugin.platformAdapter((NotQuestsAdapter) java.lang.reflect.Proxy.newProxyInstance(
                NotQuestsAdapter.class.getClassLoader(),
                new Class<?>[] {NotQuestsAdapter.class},
                (proxy, method, arguments) -> method.getName().equals("hasPermission")
                        ? true
                        : method.invoke(registryAdapter, arguments)));
        final TestPlayer player = new TestPlayer(UUID.randomUUID().toString());
        plugin.registerQuestPlayer(player, "default", true);
        final List<NotQuestsAdapter.NpcSelection> selections = new ArrayList<>();
        final int selectionId = plugin.registerNpcSelection(selections::add);

        final NpcAttachments.ClickEffects effects = plugin.nativeNpcClicked(
                player.playerIdentifier(),
                "citizens",
                NQNPCID.fromInteger(42),
                "Guide",
                selectionId,
                ignored -> null,
                () -> {},
                null);

        assertTrue(effects.selectionHandled());
        assertTrue(effects.handled());
        assertEquals(1, selections.size());
        assertEquals("citizens", selections.getFirst().npcType());
        assertEquals(42, selections.getFirst().npcId().getIntegerID());
    }

    @Test
    void citizensConversationClickOwnsSessionUntilConversationStops() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer(UUID.randomUUID().toString());
        final AtomicInteger npcBecameIdle = new AtomicInteger();
        plugin.registerQuestPlayer(player, "default", true);
        plugin.saveConversation("intro", List.of("Hello"));
        assertTrue(plugin.syncConversationNpcAttachment(
                "intro",
                "citizens",
                NQNPCID.fromInteger(42),
                "Guide"));

        final NpcAttachments.ClickEffects effects = plugin.nativeNpcClicked(
                player.playerIdentifier(),
                "citizens",
                NQNPCID.fromInteger(42),
                "Guide",
                -1,
                ignored -> null,
                npcBecameIdle::incrementAndGet,
                null);

        assertTrue(effects.conversationStarted());
        assertTrue(effects.pauseNavigation());
        assertTrue(effects.stopPlayerMovement());
        assertTrue(plugin.hasActiveConversationForNpc(42));
        assertEquals(0, npcBecameIdle.get());

        assertTrue(plugin.stopConversation(player));
        assertFalse(plugin.hasActiveConversationForNpc(42));
        assertEquals(1, npcBecameIdle.get());
    }

    @Test
    void citizensEscortCompletionPrecedesPreviewAndConversation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        adapter.objectives().objective("EscortNPC")
                .displayName("Escort NPC")
                .description("Escorts one NPC to another.")
                .field(
                        "npcToEscortId",
                        adapter.fields().storedInteger(-1),
                        "NPC to escort.")
                .field(
                        "destinationNpcId",
                        adapter.fields().storedInteger(-1),
                        "Destination NPC.")
                .register();
        plugin.getOrCreateQuest("EscortQuest").addObjective(
                1,
                "EscortNPC",
                new com.notquests.core.TestData(Map.of(
                        "npcToEscortId", 7,
                        "destinationNpcId", 42)),
                "");
        plugin.saveConversation("intro", List.of("Hello"));
        assertTrue(plugin.syncConversationNpcAttachment(
                "intro",
                "citizens",
                NQNPCID.fromInteger(42),
                "Destination"));
        final TestPlayer player = new TestPlayer(UUID.randomUUID().toString());
        plugin.registerQuestPlayer(player, "default", true);
        assertTrue(plugin.giveQuest(player, "EscortQuest", false, player.messages::add));
        final AtomicInteger finishedEscort = new AtomicInteger();

        final NpcAttachments.ClickEffects effects = plugin.nativeNpcClicked(
                player.playerIdentifier(),
                "citizens",
                NQNPCID.fromInteger(42),
                "Destination",
                -1,
                escortNpcId -> {
                    assertEquals(7, escortNpcId);
                    return new NpcAttachments.EscortNpc(
                            true,
                            4,
                            "Companion",
                            finishedEscort::incrementAndGet);
                },
                () -> {},
                null);

        assertTrue(effects.objectiveHandled());
        assertFalse(effects.previewShown());
        assertFalse(effects.conversationStarted());
        assertFalse(effects.pauseNavigation());
        assertEquals(1, finishedEscort.get());
    }

    @Test
    void ownsRegistryPackLifecycle() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> calls = new ArrayList<>();

        plugin.addRegistryPack(new Pack<String>() {
            @Override
            public void register(final String platform) {
                calls.add("register:" + platform);
            }

            @Override
            public void variablesChanged(final String platform) {
                calls.add("variables:" + platform);
            }

            @Override
            public void variableValueChanged(
                    final String platform,
                    final String variable,
                    final PlatformPlayer questPlayer) {
                calls.add("value:" + platform + ":" + variable + ":" + questPlayer.playerIdentifier());
            }
        });

        plugin.registerRegistryPacks("paper");
        plugin.refreshRegistryPacksAfterVariableChange("paper");
        plugin.notifyRegistryPacksAfterVariableValueChange("paper", "variable", new TestPlayer("player"));

        assertEquals(
                List.of("register:paper", "variables:paper", "value:paper:variable:player"),
                calls);
    }

    @Test
    void ownsGenericActionChainExecutionFlow() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> executed = new ArrayList<>();

        plugin.executeActionChain(
                Chain.builder()
                        .actionNames("first,second")
                        .amount(2)
                        .objects("payload")
                        .build(),
                name -> name,
                action -> true,
                (action, ignoreConditions, delay, objects) -> executed.add(action + ":" + objects[0]),
                executed::add);

        assertEquals(List.of("first:payload", "second:payload", "first:payload", "second:payload"), executed);
    }

    @Test
    void ownsPortablePlayerQuestStateQueriesAndMutations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.getOrCreateQuest("QuestA");
        plugin.getOrCreateQuest("QuestB");

        assertTrue(plugin.setActiveQuestNames(player, List.of("QuestA", "QuestB")));
        assertEquals(List.of("QuestA", "QuestB"), plugin.activeQuestNames(player));

        plugin.getOrCreateQuest("QuestC");
        assertTrue(plugin.setCompletedQuestNames(player, List.of("QuestC")));
        assertEquals(List.of("QuestC"), plugin.completedQuestNames(player));

        assertTrue(plugin.setQuestPoints(player, 42));
        assertEquals(42, plugin.questPoints(player));

        assertTrue(plugin.setPlayerTagValue("player-1", "default", "Reputation", 7));
        assertEquals(7, plugin.playerTagValue("player-1", "default", "Reputation"));
        assertTrue(plugin.setPlayerFinishedLoadingTags("player-1", "default", true));
        assertTrue(plugin.questPlayer("player-1", "default").isFinishedLoadingTags());
    }

    @Test
    void removeActiveQuestAlsoRemovesActiveObjectiveProgress() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.getOrCreateQuest("QuestA").addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 2)), "");

        plugin.giveQuest(player, "QuestA", false, player.messages::add);
        assertEquals(List.of("QuestA"), plugin.activeQuestNames(player));
        assertEquals(1, plugin.activeObjectives("player-1").size());

        assertTrue(plugin.removeActiveQuest("player-1", "default", "QuestA"));

        assertEquals(List.of(), plugin.activeQuestNames(player));
        assertEquals(List.of(), plugin.activeObjectives("player-1"));
    }

    @Test
    void activateQuestAddsActiveStateAndObjectiveProgress() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 2)), "");

        assertTrue(plugin.activateQuest(new TestPlayer("player-1"), "QuestA", ignored -> {}));

        assertEquals(List.of("QuestA"), plugin.activeQuestNames(new TestPlayer("player-1")));
        assertEquals(1, plugin.activeObjectives("player-1").size());
    }

    @Test
    void platformObjectiveUnlockCancellationKeepsObjectiveLocked() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter registryAdapter = plugin.createRegistryAdapter(null);
        registryAdapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", registryAdapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        final AtomicInteger events = new AtomicInteger();
        final AtomicBoolean triggerAcceptQuestTrigger = new AtomicBoolean();
        final NotQuestsAdapter cancellingAdapter = (NotQuestsAdapter) java.lang.reflect.Proxy.newProxyInstance(
                NotQuestsAdapter.class.getClassLoader(),
                new Class<?>[] {NotQuestsAdapter.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("allowObjectiveUnlock")) {
                        events.incrementAndGet();
                        triggerAcceptQuestTrigger.set((boolean) arguments[3]);
                        return false;
                    }
                    return method.invoke(registryAdapter, arguments);
                });
        plugin.platformAdapter(cancellingAdapter);
        plugin.getOrCreateQuest("QuestA").addObjective(
                "BreakBlocks",
                new com.notquests.core.TestData(Map.of("amount", 2)),
                "");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));

        assertEquals(1, events.get());
        assertFalse(triggerAcceptQuestTrigger.get());
        assertFalse(plugin.activeObjectiveProgress(player, "QuestA", 1).isUnlocked());
    }

    @Test
    void npcAttachmentsAreAppliedInsidePlatformThreadCall() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter registryAdapter = plugin.createRegistryAdapter(null);
        final AtomicBoolean platformThreadCall = new AtomicBoolean();
        final AtomicBoolean insidePlatformThread = new AtomicBoolean();
        final AtomicBoolean questAttachmentApplied = new AtomicBoolean();
        final NotQuestsAdapter threadCheckingAdapter = (NotQuestsAdapter) java.lang.reflect.Proxy.newProxyInstance(
                NotQuestsAdapter.class.getClassLoader(),
                new Class<?>[] {NotQuestsAdapter.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("callOnServerThread")) {
                        platformThreadCall.set(true);
                        insidePlatformThread.set(true);
                        try {
                            return ((java.util.concurrent.Callable<?>) arguments[0]).call();
                        } finally {
                            insidePlatformThread.set(false);
                        }
                    }
                    if (method.getName().equals("setNpcQuestGiver")) {
                        assertTrue(insidePlatformThread.get());
                        questAttachmentApplied.set(true);
                        return true;
                    }
                    if (method.getName().equals("nativeNpcQuestGiverTypes")) {
                        return List.of("citizens");
                    }
                    return method.invoke(registryAdapter, arguments);
                });
        plugin.platformAdapter(threadCheckingAdapter);
        plugin.getOrCreateQuest("QuestA").addNpcAttachment(
                "citizens",
                NQNPCID.fromInteger(7),
                "Guide",
                true);

        plugin.applyNpcAttachments();

        assertTrue(platformThreadCall.get());
        assertTrue(questAttachmentApplied.get());
    }

    @Test
    void currentActiveObjectiveProgressDisplayIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 4)), "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));
        assertTrue(plugin.addActiveObjectiveProgress(player, "QuestA", new int[] {1}, 1, "Break Blocks", "QuestA")
                .applied());
        player.actionBars.clear();
        player.bossBars.clear();

        assertTrue(plugin.showActiveObjectiveProgress(player, "QuestA", new int[] {1}));

        assertEquals(1, player.actionBars.size());
        assertEquals(1, player.bossBars.size());
        assertTrue(player.actionBars.getFirst().contains("1 <unimportant>/ <main>4"));
        assertTrue(player.bossBars.getFirst().contains("1 <unimportant>/ <main>4"));
    }

    @Test
    void activeObjectiveTrackingMarkerDecisionIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        final com.notquests.core.objectives.Objective objective = plugin.getOrCreateQuest("QuestA")
                .addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 4)), "");
        final TestLocation location = new TestLocation("world", 1, 2, 3);
        objective.setLocationEnabled(true);
        objective.setLocation(location);
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));
        player.actionBars.clear();
        player.bossBars.clear();

        assertTrue(plugin.trackActiveObjective(player, "QuestA", new int[] {1}));

        assertEquals(1, player.actionBars.size());
        assertEquals(1, player.bossBars.size());
        final String marker = "objective:QuestA:1:Break Blocks";
        assertEquals(location, player.beams.get(marker));

        assertTrue(plugin.untrackActiveObjective(player, "QuestA", new int[] {1}));
        assertFalse(player.beams.containsKey(marker));
    }

    @Test
    void activeObjectiveDisplayLinesAreOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .taskDescription((objective, questPlayer, activeObjective) -> "Break Blocks: <main>dirt")
                .register();
        plugin.getOrCreateQuest("QuestA")
                .addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 4)), "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));
        assertTrue(plugin.addActiveObjectiveProgress(player, "QuestA", new int[] {1}, 1, "Break Blocks", "QuestA")
                .applied());

        assertEquals(
                List.of(
                        "<highlight>1.</highlight> <main>Break Blocks:",
                        "    <veryUnimportant>└─ <unimportant>Break Blocks: <main>dirt",
                        "    <veryUnimportant>└─ <unimportant>Progress: <main>1 / 4"),
                plugin.activeObjectiveDisplayLines(player, "QuestA", new int[] {1}, 0));
    }

    @Test
    void activeObjectiveProgressMutationAndMessagesAreOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 3)), "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));

        final ActiveObjective.Update added = plugin.addActiveObjectiveProgress(
                player,
                "QuestA",
                new int[] {1},
                2,
                "Break Blocks",
                "QuestA");
        assertTrue(added.applied());
        assertEquals(2, added.currentProgress());
        assertTrue(added.unlocked());
        assertFalse(added.completed());
        assertEquals(
                List.of("+2.0 core progress for objective "
                        + NotQuestsColors.debugHighlightGradient + "Break Blocks</gradient>"
                        + " of quest " + NotQuestsColors.debugHighlightGradient + "QuestA</gradient>."),
                added.debugMessages());
        assertEquals(2, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());

        final ActiveObjective.Update removed = plugin.removeActiveObjectiveProgress(
                player,
                "QuestA",
                new int[] {1},
                1,
                true,
                "Break Blocks",
                "QuestA");
        assertTrue(removed.applied());
        assertEquals(1, removed.currentProgress());
        assertEquals(
                List.of("-1.0 core progress for objective "
                        + NotQuestsColors.debugHighlightGradient + "Break Blocks</gradient>"
                        + " of quest " + NotQuestsColors.debugHighlightGradient + "QuestA</gradient>."),
                removed.debugMessages());
        assertEquals(1, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());

        final ActiveObjective.Update rejected = plugin.removeActiveObjectiveProgress(
                player,
                "QuestA",
                new int[] {1},
                -1,
                true,
                "Break Blocks",
                "QuestA");
        assertFalse(rejected.applied());
        assertEquals(
                List.of("Tried to remove negative progress (=> add progress) from objective "
                        + "Break Blocks of quest QuestA!"),
                rejected.severeMessages());
        assertEquals(1, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
    }

    @Test
    void restoresSqlPlayerRuntimeIntoCoreStateWithoutPaperReplay() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final List<Boolean> objectiveLoadingModes = new ArrayList<>();
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .onUnlock((objective, questPlayer, loading) -> {
                    objectiveLoadingModes.add(loading);
                    if (!loading) {
                        objective.addProgress(99);
                    }
                })
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective(1, "BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 2)), "");
        plugin.getOrCreateQuest("QuestA").addObjective(2, "BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 4)), "");
        plugin.getOrCreateQuest("CompletedQuest");
        plugin.getOrCreateQuest("FailedQuest");

        final TestPlayer player = new TestPlayer("player-1");
        final PlayerDatabase.LoadedPlayer loadedPlayer = new PlayerDatabase.LoadedPlayer(
                "player-1",
                "builder",
                "builder",
                42,
                List.of(new PlayerDatabase.QuestHistoryReadRow("CompletedQuest", 1000L)),
                List.of(new PlayerDatabase.QuestHistoryReadRow("FailedQuest", 2000L)),
                List.of("QuestA"),
                Map.of(),
                Map.of("QuestA", List.of(
                        new PlayerDatabase.ActiveObjectiveReadRow(
                                "BreakBlocks",
                                "QuestA",
                                1.5,
                                1,
                                false,
                                2,
                                false),
                        new PlayerDatabase.ActiveObjectiveReadRow(
                                "BreakBlocks",
                                "QuestA",
                                4,
                                2,
                                true,
                                4,
                                false))));

        plugin.restorePlayerRuntime(loadedPlayer, player, ignored -> {});

        assertEquals("builder", plugin.activeProfile("player-1"));
        assertEquals(42, plugin.questPlayer("player-1", "builder").getQuestPoints());
        assertEquals(List.of("QuestA"), plugin.activeQuestNames(player));
        assertEquals(
                List.of(new QuestPlayer.CompletedQuest("CompletedQuest", "player-1", 1000L)),
                plugin.questPlayer("player-1", "builder").getCompletedQuests());
        assertEquals(
                List.of(new QuestPlayer.FailedQuest("FailedQuest", "player-1", 2000L)),
                plugin.questPlayer("player-1", "builder").getFailedQuests());
        assertEquals(1.5, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(List.of(true, true), objectiveLoadingModes);
        assertEquals(
                List.of(new QuestPlayer.CompletedObjective("QuestA", "2", "QuestA", "BreakBlocks", 4, 4)),
                plugin.questPlayer("player-1", "builder").getCompletedObjectives("QuestA"));
    }

    @Test
    void restoresQuestHistoryAfterTheQuestConfigurationIsMissing() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<String> warnings = new ArrayList<>();
        final PlayerDatabase.LoadedPlayer loadedPlayer = new PlayerDatabase.LoadedPlayer(
                "player-1",
                "default",
                "default",
                0,
                List.of(new PlayerDatabase.QuestHistoryReadRow("test", 1000L)),
                List.of(new PlayerDatabase.QuestHistoryReadRow("test", 2000L)),
                List.of(),
                Map.of(),
                Map.of());

        plugin.restorePlayerRuntime(loadedPlayer, null, warnings::add);

        assertEquals(
                List.of(new QuestPlayer.CompletedQuest("test", "player-1", 1000L)),
                plugin.questPlayer("player-1", "default").getCompletedQuests());
        assertEquals(
                List.of(new QuestPlayer.FailedQuest("test", "player-1", 2000L)),
                plugin.questPlayer("player-1", "default").getFailedQuests());
        assertEquals(List.of(), warnings);
    }

    @Test
    void loadsSqlPlayerRuntimeThroughCore() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective(1, "BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 3)), "");
        plugin.getOrCreateQuest("CompletedQuest");
        final String playerIdentifier = "11111111-1111-1111-1111-111111111111";

        final PlayerDatabase.PlayerSnapshot snapshot = new PlayerDatabase.PlayerSnapshot(
                playerIdentifier,
                "builder",
                "builder",
                7,
                List.of("QuestA"),
                List.of(),
                List.of(new PlayerDatabase.ActiveObjectiveRow(
                        "BreakBlocks",
                        "QuestA",
                        playerIdentifier,
                        2,
                        1,
                        false,
                        3,
                        "builder")),
                List.of(new PlayerDatabase.QuestHistoryRow("CompletedQuest", playerIdentifier, 1000L, "builder")),
                List.of());

        final TestPlayer player = new TestPlayer(playerIdentifier);
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(snapshot));

            assertEquals(
                    List.of(plugin.questPlayer(playerIdentifier, "builder")),
                    plugin.loadPlayerRuntime(connection, playerIdentifier, ignored -> player, ignored -> {}));
        }

        assertEquals("builder", plugin.activeProfile(playerIdentifier));
        assertEquals(7, plugin.questPlayer(playerIdentifier, "builder").getQuestPoints());
        assertTrue(plugin.questPlayer(playerIdentifier, "builder").isFinishedLoadingGeneralData());
        assertTrue(plugin.questPlayer(playerIdentifier, "builder").isFinishedLoadingTags());
        assertFalse(plugin.questPlayer(playerIdentifier, "builder").isCurrentlyLoading());
        assertEquals(List.of("QuestA"), plugin.activeQuestNames(player));
        assertEquals(2, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(
                List.of(new QuestPlayer.CompletedQuest("CompletedQuest", playerIdentifier, 1000L)),
                plugin.questPlayer(playerIdentifier, "builder").getCompletedQuests());
    }

    @Test
    void loadsSwitchesAndSavesObjectiveAndTriggerRuntimePerProfile() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .onPlayerJump(objective -> objective.addProgress(1))
                .register();
        final Quest quest = plugin.getOrCreateQuest("QuestA");
        quest.addObjective(1, "Jump", new com.notquests.core.TestData(Map.of("amount", 10)), "");
        quest.addTrigger(1, "BEGIN", new com.notquests.core.TestData(Map.of(
                "action", "",
                "applyOn", 0,
                "amountNeeded", 100,
                "worldName", "ALL")));
        final String playerIdentifier = "22222222-2222-2222-2222-222222222222";
        final PlayerDatabase.PlayerSnapshot defaultProfile = new PlayerDatabase.PlayerSnapshot(
                playerIdentifier,
                "default",
                "builder",
                0,
                List.of("QuestA"),
                List.of(new PlayerDatabase.ActiveTriggerRow(
                        "BEGIN", "QuestA", playerIdentifier, 3, 1, "default")),
                List.of(new PlayerDatabase.ActiveObjectiveRow(
                        "Jump", "QuestA", playerIdentifier, 2, 1, false, 10, "default")),
                List.of(),
                List.of());
        final PlayerDatabase.PlayerSnapshot builderProfile = new PlayerDatabase.PlayerSnapshot(
                playerIdentifier,
                "builder",
                "builder",
                0,
                List.of("QuestA"),
                List.of(new PlayerDatabase.ActiveTriggerRow(
                        "BEGIN", "QuestA", playerIdentifier, 8, 1, "builder")),
                List.of(new PlayerDatabase.ActiveObjectiveRow(
                        "Jump", "QuestA", playerIdentifier, 7, 1, false, 10, "builder")),
                List.of(),
                List.of());
        final TestPlayer player = new TestPlayer(playerIdentifier);

        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            PlayerDatabase.prepare(connection, null);
            PlayerDatabase.save(connection, List.of(defaultProfile, builderProfile));
            assertEquals(
                    2,
                    plugin.loadPlayerRuntime(connection, playerIdentifier, ignored -> player, ignored -> {}).size());
            assertEquals(
                    2,
                    plugin.loadPlayerRuntime(connection, playerIdentifier, ignored -> player, ignored -> {}).size());
        }

        assertEquals("builder", plugin.activeProfile(playerIdentifier));
        assertEquals(7, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(1, plugin.activeTriggers(playerIdentifier).size());
        assertEquals(8, plugin.activeTriggers(playerIdentifier).getFirst().currentProgress());

        assertTrue(plugin.changePlayerProfile(playerIdentifier, "default"));
        assertEquals(2, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(1, plugin.activeTriggers(playerIdentifier).size());
        assertEquals(3, plugin.activeTriggers(playerIdentifier).getFirst().currentProgress());
        plugin.playerJumped(player);
        plugin.triggerQuestEvent(player, "BEGIN", "QuestA");
        assertEquals(3, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(4, plugin.activeTriggers(playerIdentifier).getFirst().currentProgress());

        assertTrue(plugin.changePlayerProfile(playerIdentifier, "builder"));
        assertEquals(7, plugin.activeObjectiveProgress(player, "QuestA", 1).getCurrentProgress());
        assertEquals(8, plugin.activeTriggers(playerIdentifier).getFirst().currentProgress());

        final List<PlayerDatabase.PlayerSnapshot> saved = plugin.playerRuntimeSnapshots();
        final PlayerDatabase.PlayerSnapshot savedDefault = saved.stream()
                .filter(snapshot -> snapshot.profile().equals("default"))
                .findFirst()
                .orElseThrow();
        final PlayerDatabase.PlayerSnapshot savedBuilder = saved.stream()
                .filter(snapshot -> snapshot.profile().equals("builder"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, savedDefault.activeObjectives().size());
        assertEquals(3, savedDefault.activeObjectives().getFirst().currentProgress());
        assertEquals(4, savedDefault.activeTriggers().getFirst().currentProgress());
        assertEquals(1, savedBuilder.activeObjectives().size());
        assertEquals(7, savedBuilder.activeObjectives().getFirst().currentProgress());
        assertEquals(8, savedBuilder.activeTriggers().getFirst().currentProgress());
    }

    @Test
    void normalReloadIsSafeAndFullReloadRequiresUnsafeFlow(@TempDir final Path dataFolder) {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("Jump")
                .displayName("Jump")
                .description("Counts player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.configureData(dataFolder, "7.0.0", "1.21.11", adapter, false);
        final List<ReloadTarget> reloads = new ArrayList<>();
        plugin.dataManager(new RecordingDataManager(reloads));
        plugin.getOrCreateQuest("QuestA").addObjective(
                "Jump",
                new com.notquests.core.TestData(Map.of("amount", 5)),
                "");
        final TestPlayer player = new TestPlayer("player-1");
        assertTrue(plugin.activateQuest(player, "QuestA", ignored -> {}));
        final ActiveObjective objectiveBeforeReload = plugin.activeObjectiveProgress(player, "QuestA", 1);

        assertTrue(plugin.reloadData(ReloadTarget.ALL));
        assertEquals(List.of(ReloadTarget.CONVERSATIONS), reloads);
        assertSame(objectiveBeforeReload, plugin.activeObjectiveProgress(player, "QuestA", 1));
        assertSame(plugin.quest("QuestA"), plugin.getOrCreateQuest("QuestA"));

        reloads.clear();
        assertTrue(plugin.reloadAllDataUnsafe());
        assertEquals(List.of(ReloadTarget.ALL), reloads);
    }

    @Test
    void newInstallStartsAtTheCurrentMigrationVersion(@TempDir final Path dataFolder) {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        plugin.configureData(dataFolder, "7.0.0-beta.2", "1.21.11", adapter, false);

        assertEquals(
                "7.0.0-beta.2",
                ConfigurationMigrations.dataVersion(plugin.generalConfig()));
        final Path backups = dataFolder.resolve("backups");
        if (Files.isDirectory(backups)) {
            try (var entries = Files.list(backups)) {
                assertFalse(entries.anyMatch(path -> path.getFileName().toString()
                        .startsWith("notquests-full-before-migration-")));
            } catch (final java.io.IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        }
    }

    @Test
    void sixThreeDataIsMigratedBeforeRuntimeSettingsAreLoaded(@TempDir final Path dataFolder)
            throws Exception {
        Files.createDirectories(dataFolder.resolve("default"));
        Files.writeString(dataFolder.resolve("general.yml"), """
                config-version-do-not-edit: 6.3.0
                storage:
                  database:
                    host: localhost
                    port: 3306
                    database: notquests
                    username: nq
                    password: secret
                visual:
                  colors:
                    console:
                      info:
                        default: '<released-main>'
                """);
        Files.writeString(dataFolder.resolve("default/category.yml"), "displayName: Default\n");
        Files.writeString(dataFolder.resolve("default/quests.yml"), "quests: {}\n");
        Files.writeString(dataFolder.resolve("default/actions.yml"), "actions: {}\n");

        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        plugin.configureData(dataFolder, "7.0.0-beta.2", "1.21.11", adapter, false);

        assertTrue(plugin.configuration().databaseEnabled());
        assertEquals(
                "<released-main>",
                plugin.configuration().consoleInfoColor(
                        com.notquests.core.managers.LogManager.LogCategory.DEFAULT));
        assertEquals(
                "7.0.0-beta.2",
                ConfigurationMigrations.dataVersion(plugin.generalConfig()));
    }

    @Test
    void buildsSqlPlayerRuntimeSnapshotsFromCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Counts matching blocks broken by the player.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Progress needed.")
                .register();
        plugin.getOrCreateQuest("QuestA").addObjective("BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 3)), "");
        plugin.createPlayerProfile("player-1", "builder");
        plugin.changePlayerProfile("player-1", "builder");

        final var state = plugin.questPlayer("player-1", "builder");
        state.setQuestPoints(42);
        state.addActiveQuest("QuestA");
        state.addCompletedObjectiveId("QuestA", "1");
        state.addCompletedQuest(new QuestPlayer.CompletedQuest("CompletedQuest", "player-1", 1000L));
        state.addFailedQuest(new QuestPlayer.FailedQuest("FailedQuest", "player-1", 2000L));

        final PlayerDatabase.PlayerSnapshot snapshot = plugin.playerRuntimeSnapshots().getFirst();

        assertEquals("player-1", snapshot.playerIdentifier());
        assertEquals("builder", snapshot.profile());
        assertEquals("builder", snapshot.activeProfile());
        assertEquals(42, snapshot.questPoints());
        assertEquals(List.of("QuestA"), snapshot.activeQuestNames());
        assertEquals(1, snapshot.activeObjectives().size());

        final PlayerDatabase.ActiveObjectiveRow completedObjective = snapshot.activeObjectives().getFirst();
        assertEquals("BreakBlocks", completedObjective.objectiveType());
        assertEquals("QuestA", completedObjective.holderPath());
        assertEquals("player-1", completedObjective.playerUuid());
        assertEquals(3.0, completedObjective.currentProgress());
        assertEquals(1, completedObjective.objectiveId());
        assertTrue(completedObjective.completed());
        assertEquals(3.0, completedObjective.progressNeeded());
        assertEquals("builder", completedObjective.profile());
        assertEquals(
                List.of(new PlayerDatabase.QuestHistoryRow("CompletedQuest", "player-1", 1000L, "builder")),
                snapshot.completedQuests());
        assertEquals(
                List.of(new PlayerDatabase.QuestHistoryRow("FailedQuest", "player-1", 2000L, "builder")),
                snapshot.failedQuests());
    }

    @Test
    void ownsProfileScopedQuestHistoryMutations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final QuestPlayer.CompletedQuest completed = new QuestPlayer.CompletedQuest("QuestA", "player-1", 10L);
        final QuestPlayer.FailedQuest failed = new QuestPlayer.FailedQuest("QuestB", "player-1", 20L);

        plugin.addCompletedQuest("player-1", "builder", completed);
        plugin.addFailedQuest("player-1", "builder", failed);

        final com.notquests.core.structs.QuestPlayer state = plugin.questPlayer("player-1", "builder");
        assertEquals(List.of(completed), state.getCompletedQuests());
        assertEquals(List.of(failed), state.getFailedQuests());

        plugin.removeCompletedQuest("player-1", "builder", completed);
        plugin.removeFailedQuest("player-1", "builder", failed);

        assertEquals(List.of(), state.getCompletedQuests());
        assertEquals(List.of(), state.getFailedQuests());
    }

    @Test
    void ownsPortableQuestAcceptLimitChecks() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");

        plugin.getOrCreateQuest("QuestA").setMaxAccepts(1);
        assertEquals(Quest.AcceptCheck.Status.ACCEPTABLE, plugin.questAcceptCheck(player, "QuestA").status());

        plugin.setActiveQuestNames(player, List.of("QuestA"));

        assertEquals(Quest.AcceptCheck.Status.ALREADY_ACCEPTED, plugin.questAcceptCheck(player, "QuestA").status());
        assertFalse(plugin.questAcceptCheck(player, "QuestA").status() == Quest.AcceptCheck.Status.ACCEPTABLE);
    }

    @Test
    void ownsSharedGeneralConfigDefaultsVersionsAndSettingsLoad() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final YamlConfig configuration = YamlConfig.fromMap(Map.of(
                "general",
                Map.of("max-active-quests-per-player", 3),
                "storage",
                Map.of("database", Map.of("enabled", false))));

        final var result = plugin.configuration().load(configuration, "7.0.0-beta.2");

        assertTrue(result.changed());
        assertFalse(result.shouldDisableSaving());
        assertEquals("7.0.0-beta.2", configuration.getString("config-version-do-not-edit"));
        assertFalse(configuration.contains("data-migration-version-do-not-edit"));

        assertEquals(3, plugin.configuration().maxActiveQuestsPerPlayer());
    }

    @Test
    void ownsCooldownDisplayCheckWithoutMaxActiveQuestLimit() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "general", Map.of("max-active-quests-per-player", 1))));
        plugin.getOrCreateQuest("CooldownQuest").setAcceptCooldownComplete(10);
        plugin.getOrCreateQuest("OtherQuest");
        plugin.questPlayer("player-1", "default")
                .addCompletedQuest(new QuestPlayer.CompletedQuest("CooldownQuest", "player-1", 1_000L));
        plugin.setActiveQuestNames(player, List.of("OtherQuest"));

        final Quest.AcceptCheck generalCheck = plugin.questAcceptCheck(player, "CooldownQuest");
        final Quest.AcceptCheck cooldownCheck =
                plugin.questCooldownCheckForDisplay(player, "CooldownQuest", 61_000L);

        assertEquals(Quest.AcceptCheck.Status.MAX_ACTIVE_QUESTS_PER_PLAYER, generalCheck.status());
        assertEquals(Quest.AcceptCheck.Status.COOLDOWN, cooldownCheck.status());
        assertEquals(9, cooldownCheck.timeToWaitInMinutes());
        assertEquals(
                "prefix minutes:9",
                plugin.questCooldownLeftFormatted(
                        player,
                        "CooldownQuest",
                        61_000L,
                        new Quest.CooldownDisplay.Text() {
                            @Override
                            public String prefix() {
                                return "prefix ";
                            }

                            @Override
                            public String noCooldown() {
                                return "none";
                            }

                            @Override
                            public String minute() {
                                return "minute";
                            }

                            @Override
                            public String minutes(final String minutes) {
                                return "minutes:" + minutes;
                            }

                            @Override
                            public String hour() {
                                return "hour";
                            }

                            @Override
                            public String hours(final String hours) {
                                return "hours:" + hours;
                            }

                            @Override
                            public String day() {
                                return "day";
                            }

                            @Override
                            public String days(final String days) {
                                return "days:" + days;
                            }
                        }));
        assertEquals(
                "9 minutes",
                plugin.questCooldownLeftFormatted(player, "CooldownQuest", 61_000L));
        assertEquals(
                "",
                plugin.noQuestCooldownLeftFormatted(player));
    }

    @Test
    void ownsPlayerQuestStateQueries() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.getOrCreateQuest("ActiveQuest");
        plugin.getOrCreateQuest("CompletedQuest");
        plugin.getOrCreateQuest("FailedQuest");

        plugin.setQuestPoints(player, 12);
        plugin.setActiveQuestNames(player, List.of("ActiveQuest"));
        plugin.setCompletedQuestNames(player, List.of("CompletedQuest"));
        plugin.addFailedQuest(
                "player-1",
                "default",
                new QuestPlayer.FailedQuest("FailedQuest", "player-1", 10L));
        plugin.setPlayerTagValue("player-1", "default", "daily-counter", 3);
        plugin.setPlayerCurrentlyLoading("player-1", "default", true);
        plugin.setPlayerFinishedLoadingGeneralData("player-1", "default", true);
        plugin.setPlayerFinishedLoadingTags("player-1", "default", true);

        assertEquals(12, plugin.questPoints(player));
        assertEquals(List.of("ActiveQuest"), plugin.activeQuestIdentifiers("player-1", "default"));
        assertEquals(List.of(), plugin.activeQuestIdentifiersIfLoaded("missing-player", "default"));
        assertTrue(plugin.hasActiveQuest(player, "ActiveQuest"));
        assertTrue(plugin.hasCompletedQuest(player, "CompletedQuest"));
        assertTrue(plugin.hasFailedQuest(player, "FailedQuest"));
        assertFalse(plugin.hasActiveQuest(player, "MissingQuest"));
        assertEquals(
                List.of("CompletedQuest"),
                plugin.completedQuests("player-1", "default").stream()
                        .map(QuestPlayer.CompletedQuest::questIdentifier)
                        .toList());
        assertEquals(
                List.of("FailedQuest"),
                plugin.failedQuests("player-1", "default").stream()
                        .map(QuestPlayer.FailedQuest::questIdentifier)
                        .toList());
        assertEquals(3, plugin.playerTagValue("player-1", "default", "daily-counter"));
        assertEquals(Map.of("daily-counter", 3), plugin.playerTags("player-1", "default"));
        assertTrue(plugin.isPlayerCurrentlyLoading("player-1", "default"));
        assertTrue(plugin.isPlayerFinishedLoadingGeneralData("player-1", "default"));
        assertTrue(plugin.isPlayerFinishedLoadingTags("player-1", "default"));
    }

    @Test
    void ownsVisibleQuestIdentifierFiltering() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final Quest first = new Quest("First");
        final Quest second = new Quest("Second");
        final Quest duplicateFirst = new Quest("First");

        assertEquals(
                List.of("First", "Second", "First"),
                plugin.visibleQuestIdentifiers(
                        new TestPlayer("player-1"),
                        List.of(first, second, duplicateFirst),
                        1_000L,
                        ignored -> {}));

        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always fails in this test.")
                .check((condition, questPlayer) -> "<negative>Requirement missing.")
                .register();
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "gui", Map.of(
                        "quest-visibility-evaluations", Map.of(
                                "conditions", Map.of("enabled", true))))));
        plugin.getOrCreateQuest("NeedsRequirement");
        plugin.syncQuestRequirement("NeedsRequirement", 1, "Static", new com.notquests.core.TestData(Map.of()));

        assertEquals(
                List.of(),
                plugin.visibleQuestIdentifiers(
                        new TestPlayer("player-1"),
                        List.of(plugin.quest("NeedsRequirement")),
                        1_000L,
                        ignored -> {}));
    }

    @Test
    void ownsNpcQuestPreviewDecision() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npcId = NQNPCID.fromInteger(7);
        plugin.createQuest("TheVirus", "default");
        plugin.syncQuestNpcAttachment("TheVirus", "citizens", npcId, "Trent", true);
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "gui", Map.of(
                        "questpreview", Map.of("enabled", false),
                        "npc-gui-name", "npc-custom"))));

        final var player = new TestPlayer("player-1");
        assertTrue(plugin.showAttachedNpcQuestPreview(player, "citizens", npcId, false));
    }

    @Test
    void ownsEmptyNpcQuestPreviewDecision() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        final var player = new TestPlayer("player-1");
        assertFalse(plugin.showAttachedNpcQuestPreview(player, "citizens", NQNPCID.fromInteger(99), false));
    }

    @Test
    void ownsConversationNpcAttachmentBatchRemoval() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npcId = NQNPCID.fromInteger(12);
        plugin.conversationManager().save("demo-a", List.of("line"));
        plugin.conversationManager().save("demo-b", List.of("line"));
        plugin.syncConversationNpcAttachment("demo-a", "citizens", npcId, "Trent");
        plugin.syncConversationNpcAttachment("demo-b", "citizens", npcId, "Trent");

        assertEquals(
                List.of("demo-a", "demo-b"),
                plugin.removeConversationNpcAttachments("citizens", npcId));
        assertEquals(null, plugin.conversationAttachedToNpc("citizens", npcId));
    }

    @Test
    void ownsArmorStandConversationToolDecisions() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npcId = NQNPCID.fromUUID(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        plugin.conversationManager().save("demo", List.of("line"));

        final var missing = plugin.addArmorStandConversationAttachment("", null, npcId, "Stand");
        assertFalse(missing.storeAttachment());
        assertEquals("<error>Error: this item has no valid conversation.", missing.message());

        final var unknown = plugin.addArmorStandConversationAttachment("unknown", null, npcId, "Stand");
        assertFalse(unknown.storeAttachment());
        assertEquals("<error>Error: Conversation <highlight>unknown</highlight> does not exist.", unknown.message());

        final var added = plugin.addArmorStandConversationAttachment("demo", null, npcId, "Stand");
        assertTrue(added.storeAttachment());
        assertEquals("demo", added.conversationName());
        assertEquals("demo", plugin.conversationAttachedToNpc("armorstand", npcId));

        final var duplicate = plugin.addArmorStandConversationAttachment("demo", "demo", npcId, "Stand");
        assertFalse(duplicate.storeAttachment());
        assertEquals(
                "<RED>Error: That armor stand already has the Conversation <highlight>demo</highlight> attached to it!",
                duplicate.message());

        final var removed = plugin.removeArmorStandConversationAttachment("demo", npcId);
        assertTrue(removed.removeAttachment());
        assertEquals("demo", removed.conversationName());
        assertEquals(null, plugin.conversationAttachedToNpc("armorstand", npcId));

        final var none = plugin.removeArmorStandConversationAttachment(null, npcId);
        assertFalse(none.removeAttachment());
        assertEquals("<RED>This armorstand doesn't have the conversation attached to it.", none.message());
    }

    @Test
    void ownsArmorStandSelectorParsingAndQuestAttachmentOperations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final String selector = "armorstand:11111111-1111-1111-1111-111111111111";
        plugin.createQuest("daily");

        final NotQuestsAdapter.NpcSelection selection = plugin.armorStandNpcSelection(selector);
        assertEquals(selector, selection.selector());
        assertEquals("armorstand", selection.npcType());
        assertEquals("11111111-1111-1111-1111-111111111111", selection.npcId().getEitherAsString());

        assertEquals(
                "<success>Quest <highlight>daily</highlight> attached to <highlight2>armorstand:11111111-1111-1111-1111-111111111111</highlight2>.",
                plugin.addArmorStandQuestAttachment("daily", selector, true));
        assertTrue(plugin.armorStandAttachedQuestsMessage(selector).contains("daily"));

        assertEquals(
                "<success>Quest <highlight>daily</highlight> removed from <highlight2>armorstand:11111111-1111-1111-1111-111111111111</highlight2>.",
                plugin.removeArmorStandQuestAttachment("daily", selector));
        assertEquals(
                "<main>No NotQuests quests are attached to <highlight>armorstand:11111111-1111-1111-1111-111111111111</highlight>.",
                plugin.armorStandAttachedQuestsMessage(selector));
    }

    @Test
    void ownsArmorStandSelectorConversationOperations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final String selector = "armorstand:11111111-1111-1111-1111-111111111111";
        final NQNPCID npcId = NQNPCID.fromUUID(java.util.UUID.fromString("11111111-1111-1111-1111-111111111111"));
        plugin.conversationManager().save("demo", List.of("line"));

        assertEquals(
                "<GREEN>Conversation with the name <highlight>demo</highlight> was added to this poor little armorstand!",
                plugin.addArmorStandConversationAttachment("demo", selector));
        assertEquals("demo", plugin.conversationAttachedToNpc("armorstand", npcId));

        assertEquals(
                "<GREEN>All conversations were removed from this armorStand!",
                plugin.removeArmorStandConversationAttachment(selector));
        assertEquals(null, plugin.conversationAttachedToNpc("armorstand", npcId));
    }

    @Test
    void ownsArmorStandCompletionNpcToolDecision() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();

        final var missingQuest = plugin.setObjectiveCompletionNpcFromArmorStand("missing", 1, "stand-id", "Guide");
        assertFalse(missingQuest.applied());
        assertEquals("<error>Error: Quest <highlight>missing</highlight> does not exist.", missingQuest.message());

        plugin.getOrCreateQuest("daily");
        final var missingObjective = plugin.setObjectiveCompletionNpcFromArmorStand("daily", 3, "stand-id", "Guide");
        assertFalse(missingObjective.applied());
        assertEquals(
                "<error>Error: Objective with the ID <highlight>3</highlight> was not found for quest <highlight2>daily</highlight2>!",
                missingObjective.message());

        plugin.getOrCreateQuest("daily").addObjective(3, "Jump", new com.notquests.core.TestData(Map.of()), "");
        final var applied = plugin.setObjectiveCompletionNpcFromArmorStand("daily", 3, "stand-id", "Guide");
        assertTrue(applied.applied());
        assertEquals("armorstand:stand-id", applied.selector());
        assertEquals("armorstand:stand-id", plugin.configuredObjectiveCompletionNpc("daily", new int[] {3}));
        assertEquals(
                "<success>The completionArmorStandUUID of the objective with the ID <highlight>3</highlight> has been set to the Armor Stand with the UUID <highlight2>stand-id</highlight2> and name <highlight2>Guide</highlight2>!",
                applied.message());
    }

    @Test
    void ownsArmorStandQuestToolStateChanges() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NQNPCID npcId = NQNPCID.fromUUID(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"));

        assertEquals(
                "<error>Error: Quest <highlight>missing</highlight> does not exist.",
                plugin.addArmorStandQuestAttachment("missing", npcId, "Stand", true));

        plugin.getOrCreateQuest("daily");
        assertEquals(
                "<success>Quest <highlight>daily</highlight> attached to <highlight2>armorstand:22222222-2222-2222-2222-222222222222</highlight2>.",
                plugin.addArmorStandQuestAttachment("daily", npcId, "Stand", true));
        assertEquals(List.of("daily"), plugin.questsAttachedToNpc("armorstand", npcId).stream()
                .map(Quest::getIdentifier)
                .toList());

        assertEquals(
                "<success>Quest <highlight>daily</highlight> removed from <highlight2>armorstand:22222222-2222-2222-2222-222222222222</highlight2>.",
                plugin.removeArmorStandQuestAttachment("daily", npcId));
        assertTrue(plugin.questsAttachedToNpc("armorstand", npcId).isEmpty());
        assertEquals(
                "<error>Quest <highlight>daily</highlight> is not attached to <highlight2>armorstand:22222222-2222-2222-2222-222222222222</highlight2>.",
                plugin.removeArmorStandQuestAttachment("daily", npcId));
    }

    @Test
    void ownsFullQuestAcceptabilityDecision() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always fails in this test.")
                .check((condition, questPlayer) -> "<negative>Requirement missing.")
                .register();
        plugin.getOrCreateQuest("QuestA").setMaxAccepts(0);
        plugin.getOrCreateQuest("QuestB");
        plugin.syncQuestRequirement("QuestB", 1, "Static", new com.notquests.core.TestData(Map.of()));

        assertFalse(plugin.canAcceptQuest(null, "QuestA"));
        assertFalse(plugin.canAcceptQuest(new TestPlayer("player-1"), "QuestB"));
        assertTrue(plugin.canAcceptQuest(new TestPlayer("player-2"), "MissingQuest"));
    }

    @Test
    void ownsConversationLineFormatting() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        final Speaker speaker = new Speaker("Quest_Giver", 0);
        speaker.setColor("<aqua>");

        assertEquals(
                "<aqua>[Quest Giver] <GRAY>Hello Alex",
                plugin.conversationSpeakerLine(player, speaker, "Hello %player%"));
        assertEquals(
                " <main>2. <gray>Yes Alex",
                plugin.conversationAnswerOptionLine(player, speaker, "Yes %player%", 2));
        assertEquals(
                " \n<main>Choose your answer:</main>",
                plugin.conversationChooseAnswerPrefix(player));
        assertEquals(
                "<highlight>Click to answer",
                plugin.conversationChooseAnswerHover(player));
        assertEquals(
                "<main>You have ended your previous conversation!",
                plugin.conversationEndedPreviousMessage(player));
    }

    @Test
    void ownsPlaceholderApiIdentifierResolution() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.variables()
                .numberVariable("HealthAlias")
                .displayName("Health Alias")
                .description("Test variable.")
                .singular("point")
                .plural("points")
                .get((questPlayer, objects) -> 7)
                .register();
        adapter.objectives()
                .objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Test objective.")
                .field("amount", adapter.fields().storedNumber(1).progressNeeded(), "Needed amount.")
                .register();

        final Quest quest = plugin.getOrCreateQuest("Daily");
        quest.setDisplayName("Daily Display");
        quest.addObjective(1, "BreakBlocks", new com.notquests.core.TestData(Map.of("amount", 5)), "");
        plugin.activateQuest(player, "Daily", ignored -> {});
        plugin.addProgressToActiveObjectives(
                player.playerIdentifier(),
                "BreakBlocks",
                objective -> objective.getObjectiveID() == 1,
                2);
        plugin.addQuestPoints(player, 12);
        plugin.createTag(com.notquests.core.managers.tags.TagType.STRING, "Rank");
        plugin.setPlayerTagValue(player.playerIdentifier(), plugin.activeProfile(player.playerIdentifier()), "Rank", "S");

        assertEquals("12", plugin.resolvePlaceholderApiValue(player, "player_questpoints", 100));
        assertEquals("1", plugin.resolvePlaceholderApiValue(player, "player_active_quests_amount", 100));
        assertEquals("Daily Display", plugin.resolvePlaceholderApiValue(player, "player_active_quests_list_horizontal", 100));
        assertEquals("No", plugin.resolvePlaceholderApiValue(player, "player_has_completed_quest_Daily", 100));
        assertEquals("Yes", plugin.resolvePlaceholderApiValue(player, "player_has_current_active_quest_Daily", 100));
        assertEquals("Yes", plugin.resolvePlaceholderApiValue(
                player,
                "player_is_objective_unlocked_and_active_1_from_active_quest_Daily",
                100));
        assertEquals("2.0", plugin.resolvePlaceholderApiValue(
                player,
                "player_objective_progress_1_from_active_quest_Daily",
                100));
        assertEquals("40", plugin.resolvePlaceholderApiValue(
                player,
                "player_objective_progress_percentage_1_from_active_quest_Daily",
                100));
        assertEquals("8.0", plugin.resolvePlaceholderApiValue(player, "player_expression_5 + 3", 100));
        assertEquals("7", plugin.resolvePlaceholderApiValue(player, "player_variable_HealthAlias", 100));
        assertEquals("S", plugin.resolvePlaceholderApiValue(player, "player_tag_Rank", 100));
        assertEquals(null, plugin.resolvePlaceholderApiValue(player, "unknown_placeholder", 100));
    }

    private static final class RecordingDataManager extends DataManager {
        private final List<ReloadTarget> reloads;

        private RecordingDataManager(final List<ReloadTarget> reloads) {
            this.reloads = reloads;
        }

        @Override
        public boolean save() {
            return true;
        }

        @Override
        public boolean saveConfiguredData() {
            return true;
        }

        @Override
        public boolean savePlayerRuntime() {
            return true;
        }

        @Override
        public boolean loadPlayerRuntime(final boolean firstLoad) {
            return true;
        }

        @Override
        public boolean reload(final ReloadTarget target) {
            reloads.add(target);
            return true;
        }

        @Override
        public boolean saveCategory(final Category category) {
            return true;
        }
    }

    @Test
    void betonQuestCallbacksKeepPlayerSelectionAndQuestMutationsInCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.playerRuntime(
                TestPlayer::new,
                List::of);
        plugin.getOrCreateQuest("StoryQuest");
        final String playerIdentifier = "player-1";

        assertEquals("", plugin.betonQuestStartQuest(
                playerIdentifier, "StoryQuest", false, true, false));
        final PlatformPlayer player = plugin.activePlatformPlayer(playerIdentifier);
        assertTrue(plugin.hasActiveQuest(player, "StoryQuest"));

        assertEquals("", plugin.betonQuestChangeQuestPoints(
                playerIdentifier, "set", "10", true));
        assertEquals("", plugin.betonQuestChangeQuestPoints(
                playerIdentifier, "add", "5", true));
        assertEquals("", plugin.betonQuestChangeQuestPoints(
                playerIdentifier, "remove", "3", true));
        assertEquals(12, plugin.questPoints(player));

        assertEquals("", plugin.betonQuestAbortQuest(playerIdentifier, "StoryQuest"));
        assertFalse(plugin.hasActiveQuest(player, "StoryQuest"));

        assertEquals("", plugin.betonQuestStartQuest(
                playerIdentifier, "StoryQuest", false, true, false));
        assertEquals("", plugin.betonQuestFailQuest(playerIdentifier, "StoryQuest"));
        assertTrue(plugin.hasFailedQuest(player, "StoryQuest"));
    }

    @Test
    void betonQuestCallbacksReturnCoreOwnedValidationFailures() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        plugin.registerQuestPlayer(player, "default", true);

        assertEquals(
                "NotQuests quest 'MissingQuest' does not exist.",
                plugin.betonQuestStartQuest("player-1", "MissingQuest", false, false, true));
        assertEquals(
                "NotQuests quest-points action must be set, add, or remove.",
                plugin.betonQuestChangeQuestPoints("player-1", "multiply", "5", false));
        assertEquals(
                "Invalid NotQuests quest-points amount.",
                plugin.betonQuestChangeQuestPoints("player-1", "add", "many", false));
        assertEquals(
                "Cannot trigger NotQuests objective: missing trigger name.",
                plugin.betonQuestTriggerObjective("player-1", ""));

        final String actionFailure = plugin.executeBetonQuestActionLine(
                "player-1", "MissingAction value");
        assertTrue(actionFailure.contains("Unable to find conversation line action: MissingAction"));
        assertFalse(plugin.checkBetonQuestConditionLine(
                "player-1", "MissingCondition value"));
    }

    @Test
    void betonQuestNativeCallsUseCoreValidationFallbackAndLogging() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final TestPlayer player = new TestPlayer("player-1");
        final AtomicBoolean actionCalled = new AtomicBoolean();

        plugin.executeBetonQuestNamedAction(
                player,
                "package",
                "action",
                ignored -> actionCalled.set(true));
        assertTrue(actionCalled.get());
        assertTrue(plugin.checkBetonQuestConditionVariable(
                player,
                "package",
                "condition",
                ignored -> true));

        plugin.executeBetonQuestNamedAction(
                player,
                "package",
                "broken",
                ignored -> { throw new IllegalStateException(new RuntimeException("native failure")); });
        assertTrue(plugin.logManager().warningLogs().stream()
                .anyMatch(message -> message.contains("package.broken: native failure")));

        assertFalse(plugin.checkBetonQuestConditionVariable(
                player,
                "package",
                "broken",
                ignored -> { throw new IllegalStateException(new RuntimeException("condition failure")); }));
        assertTrue(plugin.logManager().warningLogs().stream()
                .anyMatch(message -> message.contains("package.broken: condition failure")));

        plugin.executeBetonQuestInlineAction(player, "", ignored -> actionCalled.set(false));
        assertTrue(actionCalled.get());
        assertTrue(plugin.logManager().warningLogs().stream()
                .anyMatch(message -> message.contains("empty instruction")));
    }

    @Test
    void betonQuestActivationPresentationIsOwnedByCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final List<com.notquests.core.managers.LogManager.ConsoleLine> console = new ArrayList<>();
        plugin.console(console::add);

        plugin.betonQuestIntegrationActivationFailed(
                new IllegalStateException(new RuntimeException("registration failed")));
        plugin.betonQuestInterceptorRegistered();

        assertTrue(plugin.logManager().warningLogs().contains(
                "Could not enable BetonQuest support: registration failed"));
        assertTrue(console.stream().anyMatch(line ->
                line.message().equals("Registered BetonQuest interceptor: notquests")));
    }

    private static final class TestPlayer implements TestPlatformPlayer {
        private final String id;
        private final List<String> messages = new ArrayList<>();
        private final List<String> actionBars = new ArrayList<>();
        private final List<String> bossBars = new ArrayList<>();
        private int bossBarHides;
        private final Map<String, com.notquests.core.platform.NQLocation> beams = new HashMap<>();
        private final List<Boolean> forcedQuestCompletions = new ArrayList<>();

        private TestPlayer(final String id) {
            this.id = id;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return id;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {
            messages.add(miniMessage);
        }

        @Override
        public void sendActionBar(final String miniMessage) {
            actionBars.add(miniMessage);
        }

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {
            bossBars.add(miniMessage + " @ " + progress);
        }

        @Override
        public void hideProgressBossBar() {
            bossBarHides++;
        }

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
        @Override
        public boolean beforeQuestCompleted(
                final com.notquests.core.structs.Quest quest,
                final boolean forced) {
            forcedQuestCompletions.add(forced);
            return true;
        }

        @Override
        public String applyExternalPlaceholders(final String text) {
            return text == null ? "" : text.replace("%player%", "Alex");
        }

        @Override
        public boolean supportsExternalPlaceholders() {
            return true;
        }

        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean renderObjectiveMarkers(
                final Map<String, com.notquests.core.platform.NQLocation> markers,
                final boolean useBeaconBlocks,
                final boolean force) {
            beams.clear();
            beams.putAll(markers);
            return true;
        }

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }

        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private record TestLocation(String worldName, double x, double y, double z)
            implements com.notquests.core.platform.NQLocation {
        @Override
        public float yaw() {
            return 0;
        }

        @Override
        public float pitch() {
            return 0;
        }
    }
}
