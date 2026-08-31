package com.notquests.core.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.items.ItemStackSelection;
import com.notquests.core.managers.DataManager.ReloadTarget;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Category;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DataManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void unconfiguredManagerSafelyDoesNoPersistenceWork() {
        final DataManager dataManager = new DataManager();

        assertTrue(dataManager.save());
        assertTrue(dataManager.saveConfiguredData());
        assertTrue(dataManager.savePlayerRuntime());
        assertTrue(dataManager.loadPlayerRuntime(true));
        assertTrue(dataManager.reload(ReloadTarget.ALL));
        assertTrue(dataManager.saveCategory(null));
    }

    @Test
    void reloadCreatesDefaultCategoryWhenNoCoreDataExistsYet() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());
    }

    @Test
    void saveWritesNewQuestsInsideTheDefaultCategory() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);

        plugin.getOrCreateQuest("TestQuest");

        assertTrue(persistence.save());
        assertFalse(Files.exists(tempDir.resolve("data").resolve("core.yml")));
        final Map<String, Object> saved = YamlConfig.load(tempDir.resolve("default").resolve("quests.yml")).asMap();
        final Map<?, ?> quests = assertInstanceOf(Map.class, saved.get("quests"));
        assertInstanceOf(Map.class, quests.get("TestQuest"));
        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());
    }

    @Test
    void savePersistsRuntimeDataByDefault() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);
        plugin.activeQuestPlayer("player-one").setQuestPoints(12);

        assertTrue(persistence.save());

        assertTrue(Files.exists(tempDir.resolve("data").resolve("runtime.yml")));
    }

    @Test
    void saveCanSkipRuntimeDataForPlatformsWithTheirOwnPlayerStore() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir,
                false);
        plugin.activeQuestPlayer("player-one").setQuestPoints(12);

        assertTrue(persistence.save());

        assertFalse(Files.exists(tempDir.resolve("data").resolve("runtime.yml")));
    }

    @Test
    void reloadWithPlatformOwnedRuntimeKeepsLoadedPlayerState() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", """
                id: default
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.activeQuestPlayer("player-one").setQuestPoints(12);
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir,
                false);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertEquals(12, plugin.activeQuestPlayer("player-one").getQuestPoints());
    }

    @Test
    void saveCategoryWritesOnlyThatCategoryMetadataAndCreatesCategoryFiles() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);
        plugin.createCategory("Story");
        plugin.setCategoryDisplayName("Story", "Story Quests");
        plugin.setCategoryProgressOrder("Story", "firstToLast");
        plugin.setCategoryGuiItem("Story", "diamond", true);
        plugin.setCategoryConversationDelayMillis("Story", 750);

        assertTrue(persistence.saveCategory(plugin.category("Story")));

        final Map<String, Object> saved = load("Story", "category.yml");
        assertEquals("Story Quests", saved.get("displayName"));
        final Map<?, ?> conversations = assertInstanceOf(Map.class, saved.get("conversations"));
        assertEquals(750, conversations.get("delay"));
        final Map<?, ?> progressOrder = assertInstanceOf(Map.class, saved.get("predefinedProgressOrder"));
        assertEquals(true, progressOrder.get("firstToLast"));
        assertEquals("diamond", saved.get("guiItem"));
        assertTrue(Files.exists(tempDir.resolve("Story").resolve("quests.yml")));
        assertTrue(Files.exists(tempDir.resolve("Story").resolve("conversations")));
    }

    @Test
    void reloadNormalizesBlankQuestCategoriesToDefault() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", """
                id: default
                """);
        writeCategoryData(Category.DEFAULT_NAME, "quests.yml", """
                quests:
                  - id: TestQuest
                    category: ''
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertEquals(Category.DEFAULT_NAME, plugin.quest("TestQuest").getCategory());
        assertEquals(List.of(Category.DEFAULT_NAME), plugin.categoryNames());
    }

    @Test
    void reloadUsesCategoryFolderAsTheCategoryScope() throws Exception {
        writeCategoryData("Story", "category.yml", "");
        writeCategoryData("Story", "quests.yml", """
                quests:
                  - id: TestQuest
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertEquals("Story", plugin.quest("TestQuest").getCategory());
        assertNotNull(plugin.category("Story"));
        assertNotNull(plugin.category(Category.DEFAULT_NAME));
    }

    @Test
    void reloadKeepsCategoryConversationDelayInCoreState() throws Exception {
        writeCategoryData("Story", "category.yml", """
                conversations:
                  delay: 1250
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final DataManager persistence = new DataManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertEquals(1250, plugin.category("Story").getConversationDelayInMS());
    }

    @Test
    void saveWritesCategoryOwnedContentIntoThatCategoryFolder() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsRegistry registry = plugin.registry();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .field("message", adapter.fields().greedyText(), "Message text.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .check((condition, questPlayer) -> "")
                .register();

        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        plugin.getOrCreateCategory("Story");
        plugin.getOrCreateQuest("StoryQuest").setCategory("Story");
        plugin.getOrCreateQuest("StoryQuest").addNpcAttachment("citizens", NQNPCID.fromInteger(7), "Trent", true);
        plugin.savedActions().save("Notify", registry.actions().getFirst(), data("message", "Hello"));
        plugin.savedActions().action("Notify").setCategory("Story");
        final int actionConditionId = plugin.savedActions().action("Notify")
                .addCondition(registry.conditions().getFirst(), data("reason", "blocked"));
        plugin.savedActions().action("Notify").getConditionFromID(actionConditionId).setProgressNeeded(3);
        plugin.savedActions().action("Notify").getConditionFromID(actionConditionId).setNegated(true);
        final var condition = NotQuestsPlugin.StoredCondition.restore(
                "Requirement",
                registry.conditions().getFirst(),
                new com.notquests.core.TestData(new LinkedHashMap<>()));
        condition.setCategory("Story");
        condition.setProgressNeeded(2);
        condition.setNegated(true);
        plugin.putSavedCondition(condition);
        plugin.createTag(TagType.STRING, "chapter", "Story");
        plugin.putSavedItem("Key", ItemStackSelection.parse("stone"), "Story", "Story Key");
        plugin.conversationManager().save("Intro", List.of("Hello"), "Story");

        assertTrue(persistence.save());

        assertFalse(Files.exists(tempDir.resolve("data").resolve("core.yml")));
        final Map<?, ?> quests = assertInstanceOf(Map.class, load("Story", "quests.yml").get("quests"));
        final Map<?, ?> actions = assertInstanceOf(Map.class, load("Story", "actions.yml").get("actions"));
        final Map<?, ?> conditions = assertInstanceOf(Map.class, load("Story", "conditions.yml").get("conditions"));
        final Map<?, ?> tags = assertInstanceOf(Map.class, load("Story", "tags.yml").get("tags"));
        final Map<?, ?> items = assertInstanceOf(Map.class, load("Story", "items.yml").get("items"));
        assertEquals(1, quests.size());
        assertEquals(1, actions.size());
        assertEquals(1, conditions.size());
        assertEquals(1, tags.size());
        assertEquals(1, items.size());
        final Map<?, ?> action = assertInstanceOf(Map.class, actions.get("Notify"));
        assertEquals("Echo", action.get("actionType"));
        assertEquals("Hello", action.get("message"));
        final Map<?, ?> actionConditions = assertInstanceOf(Map.class, action.get("conditions"));
        final Map<?, ?> actionCondition = assertInstanceOf(Map.class, actionConditions.get("1"));
        assertEquals(3, actionCondition.get("progressNeeded"));
        assertEquals(true, actionCondition.get("negated"));
        assertFalse(action.containsKey("data"));
        final Map<?, ?> quest = assertInstanceOf(Map.class, quests.get("StoryQuest"));
        final Map<?, ?> npcs = assertInstanceOf(Map.class, quest.get("npcs"));
        final Map<?, ?> npc = assertInstanceOf(Map.class, npcs.get("citizens-7"));
        assertEquals(true, npc.get("questShowing"));
        final Map<?, ?> npcData = assertInstanceOf(Map.class, npc.get("npcData"));
        assertEquals("citizens", npcData.get("type"));
        assertEquals(7, npcData.get("integerID"));
        final Map<?, ?> savedCondition = assertInstanceOf(Map.class, conditions.get("Requirement"));
        assertEquals(2, savedCondition.get("progressNeeded"));
        assertEquals(true, savedCondition.get("negated"));
        final Map<?, ?> tag = assertInstanceOf(Map.class, tags.get("chapter"));
        assertEquals("STRING", tag.get("tagType"));
        final Map<?, ?> item = assertInstanceOf(Map.class, items.get("Key"));
        assertEquals("stone", item.get("material"));
        assertTrue(Files.exists(tempDir.resolve("Story").resolve("conversations").resolve("Intro.yml")));
        final Map<String, Object> conversation = YamlConfig.load(
                tempDir.resolve("Story").resolve("conversations").resolve("Intro.yml")).asMap();
        assertEquals("NotQuests.line1", conversation.get("start"));
        assertInstanceOf(Map.class, conversation.get("Lines"));
        assertFalse(conversation.containsKey("lines"));

        final NotQuestsPlugin reloaded = NotQuestsPlugin.create();
        final var reloadedAdapter = reloaded.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        reloadedAdapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .field("message", reloadedAdapter.fields().greedyText(), "Message text.")
                .execute((actionData, questPlayer, objects) -> {})
                .register();
        reloadedAdapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .check((conditionData, questPlayer) -> "")
                .register();
        assertTrue(new DataManager(reloaded, reloadedAdapter, tempDir).reload(ReloadTarget.ALL));
        assertEquals(1, reloaded.quest("StoryQuest").getNpcAttachments().size());
        assertEquals("citizens-7", reloaded.quest("StoryQuest").getNpcAttachments().getFirst().identifyingString());
        assertEquals(2, reloaded.savedCondition("Requirement").getProgressNeeded());
        assertTrue(reloaded.savedCondition("Requirement").isNegated());
        final var reloadedActionCondition = reloaded.savedActions().action("Notify").getConditionFromID(1);
        assertEquals(3, reloadedActionCondition.getProgressNeeded());
        assertTrue(reloadedActionCondition.isNegated());
    }

    @Test
    void saveWritesParentFoldersForNestedCategories() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        plugin.getOrCreateQuest("NestedQuest").setCategory("Story.ChapterOne");

        assertTrue(persistence.save());

        assertTrue(Files.exists(tempDir.resolve("Story").resolve("category.yml")));
        assertTrue(Files.exists(tempDir.resolve("Story").resolve("ChapterOne").resolve("category.yml")));

        final NotQuestsPlugin reloaded = NotQuestsPlugin.create();
        final DataManager reloadedPersistence = new DataManager(
                reloaded,
                reloaded.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                tempDir);
        assertTrue(reloadedPersistence.reload(ReloadTarget.ALL));
        assertEquals("Story.ChapterOne", reloaded.quest("NestedQuest").getCategory());
    }

    @Test
    void saveDoesNotOverwriteExistingRichConversationFiles() throws Exception {
        final Path conversationFile = tempDir.resolve("default").resolve("conversations").resolve("demo.yml");
        Files.createDirectories(conversationFile.getParent());
        Files.writeString(conversationFile, """
                start: NotQuests.line1
                npcs:
                  npc1:
                    type: armorstand
                    identifier: marker
                Lines:
                  NotQuests:
                    color: <gray>
                    line1:
                      text: Keep this rich line
                      actions: ActionA
                      conditions: ConditionA
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        plugin.conversationManager().save("demo", List.of("A simple replacement"), "default");

        assertTrue(persistence.save());

        final String saved = Files.readString(conversationFile);
        assertTrue(saved.contains("npcs:"));
        assertTrue(saved.contains("Keep this rich line"));
        assertTrue(saved.contains("actions: ActionA"));
        assertFalse(saved.contains("A simple replacement"));
    }

    @Test
    void saveMovesExistingRichConversationFileWhenCategoryChanges() throws Exception {
        final Path oldConversationFile = tempDir.resolve("default").resolve("conversations").resolve("demo.yml");
        Files.createDirectories(oldConversationFile.getParent());
        Files.writeString(oldConversationFile, """
                start: NotQuests.line1
                Lines:
                  NotQuests:
                    color: <gray>
                    line1:
                      text: Keep this rich line
                      actions: ActionA
                      conditions: ConditionA
                """);
        writeCategoryData("Story", "category.yml", "id: Story\n");
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        plugin.createCategory("Story");
        plugin.conversationManager().save("demo", List.of("A simple replacement"), "Story");

        assertTrue(persistence.save());

        final Path newConversationFile = tempDir.resolve("Story").resolve("conversations").resolve("demo.yml");
        assertFalse(Files.exists(oldConversationFile));
        assertTrue(Files.exists(newConversationFile));
        final String saved = Files.readString(newConversationFile);
        assertTrue(saved.contains("Keep this rich line"));
        assertTrue(saved.contains("actions: ActionA"));
        assertFalse(saved.contains("A simple replacement"));
    }

    @Test
    void reloadReadsCanonicalItemSelectionLists() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", """
                id: default
                """);
        writeCategoryData(Category.DEFAULT_NAME, "items.yml", """
                items:
                  Pickaxe:
                    material:
                      materials: [STONE]
                      any: false
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertNotNull(plugin.savedItem("Pickaxe"));
        assertTrue(plugin.savedItem("Pickaxe").getItemSelection().includesMaterial("stone"));
    }

    @Test
    void reloadReadsCanonicalItemSelectionListsInsideEntryData() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", """
                id: default
                """);
        writeCategoryData(Category.DEFAULT_NAME, "quests.yml", """
                quests:
                  TestQuest:
                    objectives:
                      1:
                        objectiveType: BreakBlocks
                        item:
                          materials: [DIRT]
                          any: false
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives().objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Break blocks.")
                .register();
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        final var objective = plugin.quest("TestQuest").getObjectiveFromID(1);
        assertNotNull(objective);
        assertTrue(objective.data().itemSelection("item").includesMaterial("dirt"));
    }

    @Test
    void registryConfigPathsAreTheOnlyPersistedEntryShape() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", "id: default\n");
        writeCategoryData(Category.DEFAULT_NAME, "quests.yml", """
                quests:
                  TestQuest:
                    objectives:
                      1:
                        objectiveType: BreakBlocks
                        progressNeededExpression: 64 + 1
                        specifics:
                          itemStackSelection:
                            materials: [dirt]
                          deductIfBlockPlaced: false
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives().objective("BreakBlocks")
                .displayName("Break Blocks")
                .description("Break configured blocks.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Blocks to break.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Required progress.")
                .flag(
                        "doNotDeductIfBlockPlaced",
                        adapter.fields().presenceFlag()
                                .invertedBooleanConfig("specifics.deductIfBlockPlaced"),
                        "Do not deduct placed blocks.")
                .register();
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));
        final var objective = plugin.quest("TestQuest").getObjectiveFromID(1);
        assertTrue(objective.data().itemSelection("materials").includesMaterial("dirt"));
        assertEquals("64 + 1", objective.data().text("amount"));
        assertTrue(objective.data().flag("doNotDeductIfBlockPlaced"));

        assertTrue(persistence.save());
        final Map<?, ?> quests = assertInstanceOf(
                Map.class, load(Category.DEFAULT_NAME, "quests.yml").get("quests"));
        final Map<?, ?> quest = assertInstanceOf(Map.class, quests.get("TestQuest"));
        final Map<?, ?> objectives = assertInstanceOf(Map.class, quest.get("objectives"));
        final Map<?, ?> savedObjective = assertInstanceOf(Map.class, objectives.get("1"));
        final Map<?, ?> specifics = assertInstanceOf(Map.class, savedObjective.get("specifics"));
        assertNotNull(specifics.get("itemStackSelection"));
        assertEquals(false, specifics.get("deductIfBlockPlaced"));
        assertEquals("64 + 1", savedObjective.get("progressNeededExpression"));
        assertFalse(savedObjective.containsKey("materials"));
        assertFalse(savedObjective.containsKey("amount"));
        assertFalse(savedObjective.containsKey("doNotDeductIfBlockPlaced"));
    }

    @Test
    void reloadReadsCanonicalItemSelectionsForCategoryAndQuestIcons() throws Exception {
        writeCategoryData(Category.DEFAULT_NAME, "category.yml", """
                id: default
                guiItem:
                  materials:
                    - CHEST
                  any: false
                """);
        writeCategoryData(Category.DEFAULT_NAME, "quests.yml", """
                quests:
                  TestQuest:
                    takeItem:
                      materials:
                        - DIAMOND_SWORD
                      any: false
                """);
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);

        assertTrue(persistence.reload(ReloadTarget.ALL));

        assertTrue(plugin.category(Category.DEFAULT_NAME).getGuiItemSelection().includesMaterial("chest"));
        assertTrue(plugin.quest("TestQuest").getGuiItemSelection().includesMaterial("diamond_sword"));
    }

    @Test
    void saveQuestWritesSimpleTakeItemSelectionInOriginalShape() throws Exception {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final DataManager persistence = new DataManager(plugin, adapter, tempDir);
        plugin.getOrCreateQuest("TestQuest").setGuiItem(ItemStackSelection.parse("diamond_sword"));

        assertTrue(persistence.save());

        final Map<?, ?> quests = assertInstanceOf(Map.class, load(Category.DEFAULT_NAME, "quests.yml").get("quests"));
        final Map<?, ?> quest = assertInstanceOf(Map.class, quests.get("TestQuest"));
        assertEquals("diamond_sword", quest.get("takeItem"));
    }

    private Map<String, Object> load(final String category, final String fileName) throws Exception {
        return YamlConfig.load(tempDir.resolve(category).resolve(fileName)).asMap();
    }

    private static com.notquests.core.TestData data(final String key, final Object value) {
        final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put(key, value);
        return new com.notquests.core.TestData(values);
    }

    private void writeCategoryData(final String categoryName, final String fileName, final String yaml) throws Exception {
        final Path file = tempDir.resolve(categoryName).resolve(fileName);
        Files.createDirectories(file.getParent());
        Files.writeString(file, yaml);
    }
}
