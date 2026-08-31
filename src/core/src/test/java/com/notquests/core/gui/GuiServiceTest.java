package com.notquests.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.config.YamlConfig;
import com.notquests.core.gui.GuiService.GuiAction;
import com.notquests.core.gui.GuiService.GuiButton;
import com.notquests.core.gui.GuiService.GuiLayout;
import com.notquests.core.gui.GuiService.GuiSlot;
import com.notquests.core.gui.GuiService.Icon;
import com.notquests.core.gui.GuiService.IconProperty;
import com.notquests.core.gui.GuiService.ResolvedGui;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;
import com.notquests.core.test.TestPlatformPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

class GuiServiceTest {
    @Test
    void mainMenuSwitchesTabsWithoutLeavingTheConfiguredTabGui() {
        final GuiService guiService = new GuiService(NotQuestsPlugin.create());

        final ResolvedGui view = guiService.build("main-base", new Player("alessio"), "", "");

        assertEquals("main-base", view.id());
        assertTrue(view.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("main-base", GuiContext.EMPTY.withTab(1)))));
        assertFalse(view.slots().stream().anyMatch(slot -> slot.actions().stream()
                .anyMatch(action -> action.type() == GuiAction.Type.OPEN_GUI
                        && action.target().equals("main-active"))));
    }

    @Test
    void selectedTabControlsItsContentTitleAndActiveIcon() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateCategory("Story");
        plugin.languageManager().configuration().set("gui.main-take.title", "Available quests");
        plugin.languageManager().configuration().set("gui.main-take.button.categories.name", "%CATEGORYID%");
        final GuiService gui = new GuiService(plugin);

        final ResolvedGui view = gui.build(
                "main-base",
                new Player("alessio"),
                GuiContext.EMPTY.withTab(1));

        assertEquals("Available quests", view.title());
        assertTrue(view.slots().stream().anyMatch(slot -> slot.displayName().equals("Story")));
        assertTrue(view.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("main-base", GuiContext.EMPTY.withTab(0)))));
    }

    @Test
    void tabbedMainMenuPaginatesTheSelectedChildGui() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.main-active.button.quests.name", "%QUESTID%");
        final Player player = new Player("alessio");
        for (int i = 1; i <= 35; i++) {
            final String quest = "Quest%02d".formatted(i);
            plugin.getOrCreateQuest(quest);
            plugin.questPlayer("alessio", "default").addActiveQuest(quest);
        }
        final GuiService gui = new GuiService(plugin);
        final GuiContext firstPage = GuiContext.EMPTY.withTab(0);
        final GuiContext secondPage = firstPage.withPage(1);

        final ResolvedGui first = gui.build("main-base", player, firstPage);
        final ResolvedGui second = gui.build("main-base", player, secondPage);

        assertTrue(first.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest01")));
        assertFalse(first.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest29")));
        assertTrue(second.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest29")));
        assertFalse(second.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest01")));
        assertTrue(first.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("main-base", secondPage))));
        assertTrue(second.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("main-base", firstPage))));
    }

    @Test
    void pagedGuiSlicesContentAndPreservesContextInNavigation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.category-take.button.quests.name", "%QUESTID%");
        for (int i = 1; i <= 35; i++) {
            plugin.getOrCreateQuest("Quest%02d".formatted(i));
        }
        final GuiService gui = new GuiService(plugin);
        final GuiContext firstPage = GuiContext.of("", "default");
        final GuiContext secondPage = firstPage.withPage(1);

        final ResolvedGui first = gui.build("category-take", new Player("alessio"), firstPage);
        final ResolvedGui second = gui.build("category-take", new Player("alessio"), secondPage);

        assertTrue(first.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest01")));
        assertFalse(first.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest29")));
        assertTrue(second.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest29")));
        assertFalse(second.slots().stream().anyMatch(slot -> slot.displayName().equals("Quest01")));
        assertTrue(first.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("category-take", secondPage))));
        assertTrue(second.slots().stream().anyMatch(slot -> slot.actions().contains(
                GuiAction.openGui("category-take", firstPage))));
    }

    @Test
    void questLoreExpandsTheSixThreeMultilineTokens() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "gui", Map.of(
                        "quest-description-max-line-length", 7,
                        "wrap-long-words", false))));
        plugin.languageManager().configuration().set(
                "gui.quest-preview.button.info.lore",
                List.of(
                        "Rewards:",
                        "%QUESTREWARDS%",
                        "Requirements:",
                        "%QUESTREQUIREMENTS%",
                        "Description:",
                        "%WRAPPEDQUESTDESCRIPTION%"));
        plugin.getOrCreateQuest("Story").setDescription("one two three four");
        plugin.syncQuestReward(
                "Story",
                1,
                "Anything",
                new com.notquests.core.TestData(Map.of()),
                "Named reward");
        plugin.syncQuestRequirement(
                "Story",
                1,
                "NeedPoints",
                new com.notquests.core.TestData(Map.of()),
                new Quest.ConditionSettings(1, false, "Earn enough points", "", false));

        final ResolvedGui view = new GuiService(plugin).build(
                "quest-preview",
                new Player("alessio"),
                "Story",
                "");
        final GuiSlot info = view.slots().stream()
                .filter(slot -> slot.material().equalsIgnoreCase("BOOK"))
                .findFirst()
                .orElseThrow();

        assertTrue(info.lore().stream().anyMatch(line -> line.contains("Named reward")));
        assertTrue(info.lore().stream().anyMatch(line -> line.contains("Earn enough points")));
        assertTrue(info.lore().contains("one two"));
        assertTrue(info.lore().contains("three"));
        assertTrue(info.lore().contains("four"));
    }

    @Test
    void mainMenuKeepsPlayerHeadTexturesForPlatformRenderers() {
        final GuiService guiService = new GuiService(NotQuestsPlugin.create());

        final ResolvedGui view = guiService.build("main-base", new Player("alessio"), "", "");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.material().equalsIgnoreCase("PLAYER_HEAD")
                        && !slot.skullTexture().isBlank()));
    }

    @Test
    void defaultBuilderResolvesGuiTextFromRuntimeTranslations() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.main-base.title", "Runtime title for %player_name%");
        plugin.languageManager().configuration().set("gui.main-base.button.questpoints.name", "Runtime points: %QUESTPOINTS%");
        plugin.languageManager()
                .configuration()
                .set(
                        "gui.main-base.button.questpoints.lore",
                        List.of("Runtime lore for %player_name%", "Runtime points lore: %QUESTPOINTS%"));
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("main-base", new Player("alessio"), "", "");

        assertEquals("Runtime title for alessio", view.title());
        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.displayName().equals("Runtime points: 0")
                        && slot.lore().equals(List.of("Runtime lore for alessio", "Runtime points lore: 0"))),
                () -> "Generated slots: " + view.slots());
    }

    @Test
    void availableQuestListLinksToQuestPreview() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("TheVirus").setDescription("A dangerous quest.");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("category-take", new Player("alessio"), "", "");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                        GuiAction.openGui("quest-preview", "TheVirus", "").forPlayer("alessio"))),
                () -> "Generated slots: " + view.slots());
    }

    @Test
    void availableQuestListUsesQuestGuiItemFromCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("TheVirus").setGuiItem("acacia_boat,stone");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("category-take", new Player("alessio"), "", "");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                                GuiAction.openGui("quest-preview", "TheVirus", "").forPlayer("alessio"))
                        && slot.material().equalsIgnoreCase("acacia_boat")),
                () -> "Generated slots: " + view.slots());
    }

    @Test
    void categoryListUsesCategoryGuiItemFromCoreState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateCategory("Story").setGuiItem("emerald_block");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("main-take", new Player("alessio"), "", "");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                                GuiAction.openGui("category-take", "", "Story").forPlayer("alessio"))
                        && slot.material().equalsIgnoreCase("emerald_block")),
                () -> "Generated slots: " + view.slots());
    }

    @Test
    void coreOwnsInvalidGuiMaterialWarningsAndFallbacks() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final GuiService gui = customMaterialGui(plugin, "water");
        final List<String> warnings = new ArrayList<>();

        final ResolvedGui resolved = gui.withValidMaterials(
                gui.build("custom-material", new Player("alessio"), "", ""),
                material -> material.equals("stone") || material.equals("gray_stained_glass_pane"),
                warnings::add);

        assertEquals("gray_stained_glass_pane", resolved.slot(0).material());
        assertEquals(
                List.of("Invalid GUI item material 'water'. Falling back to "
                        + "GRAY_STAINED_GLASS_PANE for this server version."),
                warnings);
    }

    @Test
    void explicitAirGuiMaterialRemainsAnEmptyChoiceWithoutWarning() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final GuiService gui = customMaterialGui(plugin, "minecraft:air");
        final List<String> warnings = new ArrayList<>();

        final ResolvedGui resolved = gui.withValidMaterials(
                gui.build("custom-material", new Player("alessio"), "", ""),
                ignored -> false,
                warnings::add);

        assertEquals("minecraft:air", resolved.slot(0).material());
        assertTrue(resolved.slot(0).empty());
        assertEquals(List.of(), warnings);
    }

    @Test
    void categoryListShowsOneNamedDefaultCategoryWhenNoCategoriesAreLoaded() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.main-take.button.categories.name", "%CATEGORYID%");
        plugin.getOrCreateQuest("FirstQuest");
        plugin.getOrCreateQuest("SecondQuest");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("main-take", new Player("alessio"), "", "");
        final var categorySlots = view.slots().stream()
                .filter(slot -> slot.actions().contains(
                        GuiAction.openGui("category-take", "", "default").forPlayer("alessio")))
                .toList();

        assertEquals(1, categorySlots.size(), () -> "Generated slots: " + view.slots());
        assertTrue(categorySlots.getFirst().displayName().contains("default"));
    }

    @Test
    void categoryListDoesNotDuplicateDefaultCategoryWithDifferentCase() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.main-take.button.categories.name", "%CATEGORYID%");
        plugin.getOrCreateCategory("DEFAULT");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("main-take", new Player("alessio"), "", "");
        final var categorySlots = view.slots().stream()
                .filter(slot -> slot.actions().stream()
                        .anyMatch(action -> action.type() == GuiAction.Type.OPEN_GUI
                                && action.categoryIdentifier().equalsIgnoreCase("default")))
                .toList();

        assertEquals(1, categorySlots.size(), () -> "Generated slots: " + view.slots());
        assertTrue(categorySlots.getFirst().displayName().contains("default"));
    }

    @Test
    void defaultCategoryViewIncludesQuestsWithBlankStoredCategory() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.category-take.button.quests.name", "%QUESTID%");
        plugin.getOrCreateQuest("TheVirus");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("category-take", new Player("alessio"), "", "default");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                                GuiAction.openGui("quest-preview", "TheVirus", "").forPlayer("alessio"))
                        && slot.displayName().contains("TheVirus")),
                () -> "Generated slots: " + view.slots());
    }

    @Test
    void playerAvailableCategoryViewUsesCoreVisibilityFiltering() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        enableConditionVisibility(plugin);
        registerFailingCondition(plugin);
        plugin.languageManager().configuration().set("gui.category-take.button.quests.name", "%QUESTID%");
        plugin.getOrCreateQuest("VisibleQuest");
        plugin.getOrCreateQuest("HiddenQuest");
        plugin.syncQuestRequirement("HiddenQuest", 1, "Static", new com.notquests.core.TestData(Map.of()));
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("category-take", new Player("alessio"), "", "default");
        final List<String> questNames = view.slots().stream()
                .filter(slot -> slot.actions().stream().anyMatch(action -> action.type() == GuiAction.Type.OPEN_GUI))
                .map(GuiSlot::displayName)
                .toList();

        assertTrue(questNames.contains("VisibleQuest"), () -> "Generated slots: " + view.slots());
        assertTrue(!questNames.contains("HiddenQuest"), () -> "Generated slots: " + view.slots());
    }

    @Test
    void npcShownQuestContentOnlyIncludesShownAttachmentsForCurrentNpc() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.languageManager().configuration().set("gui.npc-available-quests.title", "NPC quests");
        plugin.languageManager()
                .configuration()
                .set("gui.npc-available-quests.button.quest.name", "NPC quest %QUESTID%");
        final NQNPCID currentNpc = NQNPCID.fromInteger(7);
        plugin.getOrCreateQuest("ShownMatching").addNpcAttachment("citizens", currentNpc, "Guide", true);
        plugin.getOrCreateQuest("HiddenMatching").addNpcAttachment("citizens", currentNpc, "Guide", false);
        plugin.getOrCreateQuest("WrongNpc").addNpcAttachment("citizens", NQNPCID.fromInteger(8), "Other", true);
        plugin.getOrCreateQuest("WrongType").addNpcAttachment("armorstand", currentNpc, "Stand", true);
        plugin.getOrCreateQuest("Unattached");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build(
                "npc-available-quests",
                new Player("alessio"),
                new GuiContext("", "", "citizens", currentNpc));

        assertEquals(List.of("NPC quest ShownMatching"), view.slots().stream()
                .filter(slot -> slot.actions().contains(GuiAction.openGui("quest-preview", "ShownMatching", ""))
                        || slot.displayName().startsWith("NPC quest "))
                .map(GuiSlot::displayName)
                .toList());
    }

    @Test
    void npcShownQuestContentUsesCoreVisibilityFiltering() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        enableConditionVisibility(plugin);
        registerFailingCondition(plugin);
        plugin.languageManager().configuration().set("gui.npc-available-quests.title", "NPC quests");
        plugin.languageManager()
                .configuration()
                .set("gui.npc-available-quests.button.quest.name", "NPC quest %QUESTID%");
        final NQNPCID currentNpc = NQNPCID.fromInteger(7);
        plugin.getOrCreateQuest("VisibleNpcQuest").addNpcAttachment("citizens", currentNpc, "Guide", true);
        plugin.getOrCreateQuest("HiddenNpcQuest").addNpcAttachment("citizens", currentNpc, "Guide", true);
        plugin.syncQuestRequirement("HiddenNpcQuest", 1, "Static", new com.notquests.core.TestData(Map.of()));
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build(
                "npc-available-quests",
                new Player("alessio"),
                new GuiContext("", "", "citizens", currentNpc));

        final List<String> questNames = view.slots().stream()
                .filter(slot -> slot.displayName().startsWith("NPC quest "))
                .map(GuiSlot::displayName)
                .toList();
        assertEquals(List.of("NPC quest VisibleNpcQuest"), questNames);
    }

    @Test
    void questPreviewCanAcceptAndClose() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("TheVirus");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui view = guiService.build("quest-preview", new Player("alessio"), "TheVirus", "");

        assertTrue(view.slots().stream()
                .anyMatch(slot -> slot.actions().contains(GuiAction.takeQuest("TheVirus"))
                        && slot.actions().contains(GuiAction.close())));
    }

    @Test
    void activeQuestInfoCanOpenAbortConfirmation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("TheVirus");
        plugin.questPlayer("alessio", "default").addActiveQuest("TheVirus");
        final GuiService guiService = new GuiService(plugin);

        final ResolvedGui active = guiService.build("main-active", new Player("alessio"), "", "");
        final ResolvedGui info = guiService.build("active-quest-info", new Player("alessio"), "TheVirus", "");
        final ResolvedGui confirm = guiService.build("active-quest-abort-confirm", new Player("alessio"), "TheVirus", "");

        assertTrue(active.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                        GuiAction.openGui("active-quest-info", "TheVirus", "").forPlayer("alessio"))),
                () -> "Generated active slots: " + active.slots());
        assertTrue(info.slots().stream()
                .anyMatch(slot -> slot.actions().contains(
                        GuiAction.openGui("active-quest-abort-confirm", "TheVirus", "").forPlayer("alessio"))));
        assertTrue(confirm.slots().stream()
                .anyMatch(slot -> slot.actions().contains(GuiAction.failQuest("TheVirus"))
                        && slot.actions().contains(GuiAction.close())));
    }

    @Test
    void coreExecutesGuiOpenAndCloseActions() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final Player player = new Player("alessio");

        assertTrue(plugin.runGuiActions(
                player,
                List.of(
                        GuiAction.openGui("quest-preview", "TheVirus", "Story"),
                        GuiAction.close()),
                player.messages::add));

        assertEquals(List.of("quest-preview"), player.openedGuis);
        assertTrue(player.closedInventory);
    }

    @Test
    void coreBuildsTheNextGuiAndDeliversItToTheNamedConnectedPlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final Player player = new Player("alessio");
        final Player bob = new Player("bob");
        plugin.registerQuestPlayer(bob, "default", true);
        final GuiContext context = new GuiContext(
                "TheVirus",
                "Story",
                "fancynpcs",
                NQNPCID.fromString("guide"),
                2,
                1);

        assertTrue(plugin.runGuiActions(
                player,
                List.of(GuiAction.openGui("quest-preview", context).forPlayer("bob")),
                player.messages::add));

        assertEquals(List.of(), player.openedGuis);
        assertEquals(List.of("quest-preview"), bob.openedGuis);
    }

    @Test
    void coreExecutesGuiQuestActions() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        plugin.getOrCreateQuest("TheVirus");
        final Player player = new Player("alessio");

        assertTrue(plugin.runGuiActions(player, List.of(GuiAction.takeQuest("TheVirus")), player.messages::add));
        assertTrue(plugin.questPlayer("alessio", "default").getActiveQuestIdentifiers().contains("TheVirus"));

        assertTrue(plugin.runGuiActions(player, List.of(GuiAction.failQuest("TheVirus")), player.messages::add));
        assertTrue(plugin.questPlayer("alessio", "default").getActiveQuestIdentifiers().isEmpty());
        assertEquals("TheVirus", plugin.questPlayer("alessio", "default").getFailedQuests().getFirst().questIdentifier());
    }

    @Test
    void guiExecutesAnyRegisteredActionWithConditionsAndPlaceholders() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        final AtomicReference<String> captured = new AtomicReference<>();
        adapter.actions()
                .action("Capture")
                .displayName("Capture")
                .description("Captures GUI action input.")
                .field("message", adapter.fields().greedyText(), "Message to capture.")
                .execute((action, questPlayer, objects) -> captured.set(action.text("message")))
                .register();
        adapter.conditions()
                .condition("Allowed")
                .displayName("Allowed")
                .description("Allows this GUI action.")
                .check((condition, questPlayer) -> "")
                .register();
        plugin.getOrCreateQuest("TheVirus");
        final GuiService gui = customActionGui(
                plugin,
                "Capture %QUESTID%:%player_name%:%external%:%NPCID%",
                "Allowed");
        final Player player = new Player("alessio");

        final ResolvedGui resolved = gui.build(
                "custom",
                player,
                "TheVirus",
                "Story",
                "citizens",
                NQNPCID.fromInteger(7));

        assertTrue(plugin.runGuiActions(player, resolved.slot(0).actions(), player.messages::add));
        assertEquals("TheVirus:alessio:platform:7", captured.get());
    }

    @Test
    void guiConditionsGateRegisteredAndBuiltInActions() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(null);
        final AtomicReference<String> captured = new AtomicReference<>();
        adapter.actions()
                .action("Capture")
                .displayName("Capture")
                .description("Captures GUI action input.")
                .field("message", adapter.fields().greedyText(), "Message to capture.")
                .execute((action, questPlayer, objects) -> captured.set(action.text("message")))
                .register();
        adapter.conditions()
                .condition("Denied")
                .displayName("Denied")
                .description("Rejects this GUI action.")
                .check((condition, questPlayer) -> "Not allowed")
                .register();
        final Player player = new Player("alessio");
        final GuiAction custom = GuiAction.registryAction("Capture should-not-run", List.of("Denied"));

        assertFalse(plugin.runGuiActions(player, List.of(custom), player.messages::add));
        assertEquals(null, captured.get());
        assertEquals(List.of("Not allowed"), player.messages);
    }

    private static GuiService customActionGui(
            final NotQuestsPlugin plugin,
            final String action,
            final String condition) {
        final Set<IconProperty> properties = Set.of(
                new IconProperty("actions", new IconProperty.ListValue(List.of(action))),
                new IconProperty("conditions", new IconProperty.ListValue(List.of(condition))));
        final GuiLayout layout = new GuiLayout(
                "custom",
                "Custom",
                "NORMAL",
                List.of("X"),
                Map.of('X', new GuiButton(GuiService.ButtonType.ACTION, properties, List.of("button"))),
                Map.of("button", new Icon("stone", "Button", "", "")),
                List.of());
        return new GuiService(plugin, Map.of("custom", layout));
    }

    private static GuiService customMaterialGui(
            final NotQuestsPlugin plugin,
            final String material) {
        final GuiLayout layout = new GuiLayout(
                "custom-material",
                "Custom material",
                "NORMAL",
                List.of("X"),
                Map.of('X', new GuiButton(GuiService.ButtonType.BLANK, Set.of(), List.of("button"))),
                Map.of("button", new Icon(material, "Button", "", "")),
                List.of());
        return new GuiService(plugin, Map.of("custom-material", layout));
    }

    private static void enableConditionVisibility(final NotQuestsPlugin plugin) {
        plugin.configuration().loadFrom(YamlConfig.fromMap(Map.of(
                "gui", Map.of(
                        "quest-visibility-evaluations", Map.of(
                                "conditions", Map.of("enabled", true))))));
    }

    private static void registerFailingCondition(final NotQuestsPlugin plugin) {
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always fails in this test.")
                .check((condition, questPlayer) -> "<negative>Requirement missing.")
                .register();
    }

    private static final class Player implements TestPlatformPlayer {
        private final String playerIdentifier;
        private final List<String> messages = new ArrayList<>();
        private final List<String> openedGuis = new ArrayList<>();
        private boolean closedInventory;

        private Player(final String playerIdentifier) {
            this.playerIdentifier = playerIdentifier;
        }

        @Override
        public String playerName() {
            return playerIdentifier;
        }

        @Override
        public String playerIdentifier() {
            return playerIdentifier;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public long currentWorldTimeTicks() {
            return 0;
        }

        @Override
        public void sendMessage(final String miniMessage) {}

        @Override
        public void sendActionBar(final String miniMessage) {}

        @Override
        public void showProgressBossBar(final String miniMessage, final double progress) {}

        @Override
        public void hideProgressBossBar() {}

        @Override
        public void showTitle(
                final String title,
                final String subtitle,
                final java.time.Duration fadeIn,
                final java.time.Duration stay,
                final java.time.Duration fadeOut) {}
        @Override
        public void chat(final String message) {}

        @Override
        public void performCommand(final String command) {}

        @Override
        public void closeInventory() {
            closedInventory = true;
        }

        @Override
        public boolean showGui(final GuiService.ResolvedGui gui) {
            openedGuis.add(gui.id());
            return true;
        }

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public String applyExternalPlaceholders(final String text) {
            return text == null ? "" : text.replace("%external%", "platform");
        }

        @Override
        public boolean supportsExternalPlaceholders() {
            return true;
        }
    }
}
