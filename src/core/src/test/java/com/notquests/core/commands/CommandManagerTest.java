package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.DataManager;
import com.notquests.core.npc.NQNPCID;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Registry;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.FieldFactories;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.core.test.TestNotQuestsAdapter;
import com.notquests.core.test.TestPlatformPlayer;
import com.notquests.core.variables.VariableDataType;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

class CommandManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void objectiveEditSavesConfigurationWithoutSavingPlayers() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static objective")
                .description("Test objective")
                .register();
        plugin.getOrCreateQuest("ya");
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

        assertTrue(QuestObjectiveCommands.addQuestObjective(
                plugin, adapter, "ya", "StaticObjective", "", "").success());
        assertEquals(1, configuredSaves.get());
        assertEquals(0, playerSaves.get());
    }

    @Test
    void rightClickNpcSelectionAddsObjectiveOnlyAfterTheNpcIsSelected() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter base = plugin.createRegistryAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        final AtomicInteger pendingSelectionId = new AtomicInteger(-1);
        final NotQuestsAdapter adapter = adapterWithDeferredNpcSelection(
                plugin, base, pendingSelectionId);
        adapter.objectives()
                .objective("DeliverItems")
                .displayName("Deliver Items")
                .description("Test objective")
                .field("npc", adapter.fields().npcSelector(), "NPC")
                .register();
        plugin.getOrCreateQuest("ya");
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(QuestObjectiveCommands.addQuestObjective(
                plugin,
                adapter,
                "ya",
                "DeliverItems",
                "rightClickSelect",
                "",
                player).success());
        assertEquals(0, plugin.quest("ya").getObjectives().size());
        assertTrue(pendingSelectionId.get() >= 0);

        assertTrue(plugin.completeNpcSelection(
                pendingSelectionId.get(),
                "citizens",
                NQNPCID.fromInteger(7),
                "Guide"));
        assertEquals(1, plugin.quest("ya").getObjectives().size());
        assertEquals("citizens:7", plugin.quest("ya").getObjectives().getFirst().text("npc"));
        assertTrue(player.messages.stream().anyMatch(message -> message.contains(
                "DeliverItems Objective successfully added")));
    }

    @Test
    void executesRegisteredActionThroughCoreCommandGraph() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(
                new NotQuestsRegistry.PlatformHooks(null, null, null));
        final ArrayList<String> executed = new ArrayList<>();
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records the provided message.")
                .field("message", adapter.fields().greedyText(), "Message to record.")
                .execute((action, questPlayer, objects) ->
                        executed.add(questPlayer.playerIdentifier() + ":" + action.text("message")))
                .register();

        final CommandMessage messages =
                RegistryCommands.executeAction(plugin, adapter, new TestPlayer("player-1"), "Echo", "hello there", null);

        assertTrue(messages.success());
        assertEquals(List.of("player-1:hello there"), executed);
    }

    @Test
    void ownsConversationCommandMessage() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        assertEquals(List.of("<highlight>All conversations:"), ConversationCommands.listConversations(plugin).stream()
                .map(CommandMessage::message)
                .toList());
        assertTrue(ConversationCommands.saveConversation(plugin, "intro", "Hello").success());
        assertEquals(
                List.of(
                        "<highlight>All conversations:",
                        "<highlight>1.</highlight> <main>intro",
                        "<unimportant>--- Attached to NPC:</unimportant> <main>none",
                        "<unimportant>--- Amount of starting conversation lines:</unimportant> <main>1"),
                ConversationCommands.listConversations(plugin).stream()
                        .map(CommandMessage::message)
                        .toList());
    }

    @Test
    void createConversationLoadsTheRealTemplateConversation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        plugin.dataManager(new DataManager(plugin, adapter, tempDir));

        assertTrue(ConversationCommands.createConversation(plugin, () -> tempDir, "demo", true, "default").success());

        assertTrue(plugin.conversationManager().exists("demo"));
        assertTrue(plugin.conversationManager().lineCount("demo") > 1);
        assertTrue(plugin.conversationManager().lines("demo").stream()
                .noneMatch(line -> line.equals("<main>Hello! This is a demo NotQuests conversation.")));
    }

    @Test
    void commandFeedbackFormatsPlainMessagesWithoutChangingRawText() {
        final CommandMessage messages =
                CommandMessage.success("Quest created.");
        final CommandMessage alreadyTagged =
                CommandMessage.error("<error>Quest failed.");

        assertEquals("Quest created.", messages.message());
        assertEquals("<success>Quest created.", messages.formattedMessage());
        assertEquals("<error>Quest failed.", alreadyTagged.formattedMessage());
    }

    @Test
    void argumentKindSuggestionsAreOnlyUsedWithoutAnOverride() {
        final NQArgumentType itemSelection = NQArgumentType.itemSelection();
        final Function<NQArgumentType, List<String>> fallback =
                argument -> argument.kind() == NQArgumentType.Kind.ITEM_SELECTION
                        ? List.of("acacia_boat", "dirt", "hand", "any")
                        : List.of();

        assertEquals(
                List.of("acacia_boat", "dirt", "hand", "any"),
                NQCommandBuilder.resolveSuggestions(
                        itemSelection, null, new TestPlayer("player-1"), "", fallback));
        assertEquals(
                List.of(),
                NQCommandBuilder.resolveSuggestions(
                        itemSelection,
                        (context, input) -> List.of(),
                        new TestPlayer("player-1"),
                        "",
                        fallback));
    }

    @Test
    void ownsBasicQuestStateCommandMessage() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = new OnlinePlayerAdapter(
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                new TestPlayer("PlayerOne"));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertEquals("<error>Quest <highlight>Virus</highlight> already exists!", QuestLifecycleCommands.createQuest(plugin, "Virus", "").message());
        assertEquals(
                "<error>The symbol <highlight>°</highlight> cannot be used, because it's used for some important, plugin-internal stuff.",
                QuestLifecycleCommands.createQuest(plugin, "Bad°Quest", "").message());
        assertEquals(List.of("Virus"), plugin.questNames());

        final CommandMessage forcedGive =
                QuestLifecycleCommands.giveQuest(plugin, adapter, "PlayerOne", "Virus", true);
        assertTrue(forcedGive.success());
        assertEquals("<success>Successfully accepted the quest (Forced).", forcedGive.message());
        assertEquals(List.of("Virus"), plugin.questPlayer("PlayerOne", "default").getActiveQuestIdentifiers().stream().toList());

        assertTrue(QuestLifecycleCommands.completeQuest(plugin, adapter, "PlayerOne", "Virus").success());
        assertEquals(List.of(), plugin.questPlayer("PlayerOne", "default").getActiveQuestIdentifiers().stream().toList());
        assertEquals(List.of("Virus"), plugin.questPlayer("PlayerOne", "default").getCompletedQuests().stream()
                .map(com.notquests.core.structs.QuestPlayer.CompletedQuest::questIdentifier)
                .toList());

        assertTrue(QuestLifecycleCommands.deleteQuest(plugin, "Virus").success());
        assertEquals(List.of(), plugin.questNames());
    }

    @Test
    void giveQuestCanUseRealQuestPlayerForImmediatePlayerMessages() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestLifecycleCommands.giveQuest(plugin, player, "Virus", false).success());

        assertTrue(player.messages.stream().anyMatch(message -> message.contains("[Quest Accepted]")));
    }

    @Test
    void triggerObjectiveMatchesOldSilentSuccessBehaviorAndRequiresOnlinePlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter base = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager onlineRunner = new CommandManager(plugin, new OnlinePlayerAdapter(base));

        assertEquals(List.of(), QuestLifecycleCommands.triggerObjective(
                plugin, new OnlinePlayerAdapter(base), "PlayerOne", "daily"));

        final List<CommandMessage> messages =
                QuestLifecycleCommands.triggerObjective(plugin, base, "PlayerOne", "daily");
        assertEquals(1, messages.size());
        assertFalse(messages.getFirst().success());
        assertEquals("Player <highlight>PlayerOne</highlight> is not online.", messages.getFirst().message());
    }

    @Test
    void ownsQuestPointCommandMessage() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));

        assertTrue(QuestPointCommands.setQuestPoints(plugin, adapter, "PlayerOne", 10).success());
        assertEquals("<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red>: <highlight2>10</highlight2>",
                QuestPointCommands.questPoints(plugin, adapter, "PlayerOne").message());
        assertTrue(QuestPointCommands.addQuestPoints(plugin, adapter, "PlayerOne", 5).success());
        assertEquals("<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red>: <highlight2>15</highlight2>",
                QuestPointCommands.questPoints(plugin, adapter, "PlayerOne").message());
        assertEquals(
                "<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red> have been set from <unimportant>15</unimportant> to <highlight2>0</highlight2>.",
                QuestPointCommands.removeQuestPoints(plugin, adapter, "PlayerOne", 20).message());
        assertEquals("<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red>: <highlight2>0</highlight2>",
                QuestPointCommands.questPoints(plugin, adapter, "PlayerOne").message());
    }

    @Test
    void savedActionCreationMatchesOldPaperDuplicateRules() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records the provided message.")
                .field("message", adapter.fields().greedyText(), "Message to record.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "hello").success());
        assertEquals(
                "<success>Action with the name <highlight2>Notify</highlight2> has been deleted.",
                SavedActionCommands.deleteSavedAction(plugin, "Notify").message());
        assertTrue(SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "hello").success());
        assertEquals(
                "<error>Error! An action with the name <highlight>Notify</highlight> already exists!",
                SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "hello again").message());
        assertEquals(
                "<error>Action <highlight>bad.name</highlight> cannot contain a dot in its name!",
                SavedActionCommands.saveAction(plugin, adapter, "bad.name", "Echo", "hello").message());
    }

    @Test
    void savedActionAndConditionEmptyListsOnlyPrintOldHeaders() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertEquals(List.of("<highlight>All Actions:"), SavedActionCommands.listSavedActions(plugin).stream()
                .map(CommandMessage::message)
                .toList());
        assertEquals(List.of("<highlight>All Conditions:"), SavedConditionCommands.listSavedConditions(plugin).stream()
                .map(CommandMessage::message)
                .toList());
    }

    @Test
    void savedActionExecutionDoesNotReportSuccessWhenNothingRuns() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final ArrayList<String> executed = new ArrayList<>();
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .execute((action, questPlayer, objects) -> executed.add(questPlayer.playerIdentifier()))
                .register();
        adapter.conditions()
                .condition("Blocked")
                .displayName("Blocked")
                .description("Always blocks.")
                .check((condition, questPlayer) -> "<error>Blocked by test condition.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertEquals(
                List.of("Action chain references unknown action 'MissingAction'."),
                SavedActionCommands.executeSavedAction(plugin, adapter, new TestPlayer("player-1"), "MissingAction", false, false).stream()
                        .map(CommandMessage::message)
                        .toList());

        assertTrue(SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "").success());
        assertTrue(SavedActionCommands.addSavedActionCondition(plugin, adapter, "Notify", "Blocked", "").success());
        assertEquals(
                List.of("<error>Blocked by test condition."),
                SavedActionCommands.executeSavedAction(plugin, adapter, new TestPlayer("player-1"), "Notify", false, false).stream()
                        .map(CommandMessage::message)
                        .toList());
        assertEquals(List.of(), executed);
    }

    @Test
    void questPointCommandsIncludeOldOfflineBalanceLine() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        QuestPointCommands.adminCommands(
                        NQCommandBuilder.<
                                        NQArgumentType,
                                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                        NQSuggestionProvider<NQCommandContext>,
                                        NQCommandHandler>
                                root("qa", NQDescription.of("Admin commands.")),
                        plugin,
                        adapter)
                .forEach(tree::register);

        final List<String> messages = child(child(child(child(tree.root("qa"), "questpoints"), "player"), "set"), "amount")
                .handler()
                .execute(new ArgumentContext(Map.of("player", "PlayerOne", "amount", "10")))
                .stream()
                .map(CommandMessage::message)
                .toList();

        assertEquals(
                List.of(
                        "<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red> have been set from <unimportant>0</unimportant> to <highlight2>10</highlight2>.",
                        "<main>Quest points for player <highlight>PlayerOne</highlight> <red>(offline)</red>: <highlight2>10</highlight2>"),
                messages);
    }

    @Test
    void richQuestEntryListsUseRegistryDescriptions() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .conditionDescription((condition, questPlayer, objects) -> "Static condition details.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .field("message", adapter.fields().greedyText(), "Message.")
                .actionDescription((action, questPlayer, objects) -> "Echo action: " + action.text("message"))
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.triggers()
                .trigger("BEGIN")
                .displayName("Begin")
                .description("Runs when the quest begins.")
                .field("amount", adapter.fields().integer(1), "Amount.")
                .triggerDescription(trigger -> "Trigger amount: " + trigger.integer("amount", 1))
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestRequirementCommands.addQuestRequirement(plugin, adapter, "Virus", "Static", "").success());
        assertTrue(QuestRewardCommands.addQuestReward(plugin, adapter, "Virus", "Echo", "hello").success());
        assertTrue(SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "notify").success());
        assertTrue(TriggerCommands.addQuestTrigger(plugin, "Virus", "BEGIN", "Notify", "Quest", "ALL", "2").success());

        assertTrue(QuestRequirementCommands.listQuestRequirements(plugin, "Virus").stream()
                .map(CommandMessage::message)
                .anyMatch(message -> message.equals("<main>Static condition details.")));
        assertTrue(QuestRewardCommands.listQuestRewards(plugin, "Virus").stream()
                .map(CommandMessage::message)
                .anyMatch(message -> message.equals("<unimportant>--</unimportant> <main>Echo action: hello")));
        final List<String> triggerMessages = TriggerCommands.listQuestTriggers(plugin, "Virus").stream()
                .map(CommandMessage::message)
                .toList();
        assertTrue(triggerMessages.contains("<unimportant>--</unimportant> <main>Trigger amount: 2"));
        assertTrue(triggerMessages.contains("<unimportant>------ Description:</unimportant> <main>Echo action: notify"));
    }

    @Test
    void rewardInfoShowsActionDescriptionNotDisplayNameProperty() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .field("message", adapter.fields().greedyText(), "Message.")
                .actionDescription((action, questPlayer, objects) -> "Echo action: " + action.text("message"))
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestRewardCommands.addQuestReward(plugin, adapter, "Virus", "Echo", "quest").success());
        assertEquals(
                "<main>Reward <highlight>1</highlight> for Quest <highlight2>Virus</highlight2>:\n<unimportant>--</unimportant> <main>Echo action: quest",
                QuestRewardCommands.questRewardInfo(plugin, "Virus", 1).message());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());
        assertTrue(QuestObjectiveCommands.addObjectiveReward(
                        plugin, adapter, "Virus", new int[] {1}, "Echo", "objective")
                .success());
        assertEquals(
                "<main>Reward <highlight>1</highlight> for Objective with ID <highlight2>1</highlight2> of Quest <highlight2>Virus</highlight2>:\n<unimportant>--</unimportant> <main>Echo action: objective",
                QuestObjectiveCommands.objectiveRewardInfo(plugin, "Virus", new int[] {1}, 1).message());
    }

    @Test
    void actionParserHonorsSingleLineParserBeforeReadingSharedFlags() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("SingleLine")
                .displayName("Single Line")
                .description("Uses a one-line parser.")
                .field("message", adapter.fields().greedyText(), "Message.")
                .flag("enabled", adapter.fields().presenceFlag(), "Enables the action.")
                .singleLine((action, arguments) -> action.setValue("message", String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {})
                .register();

        final CommandManager runner = new CommandManager(plugin, adapter);
        final Actions.Type type = runner.actionTypes().stream()
                .filter(action -> action.id().equals("SingleLine"))
                .findFirst()
                .orElseThrow();
        final com.notquests.core.actions.Action data =
                Actions.parse(adapter, type, "hello there --enabled");

        assertEquals("hello there", data.text("message"));
        assertTrue(data.flag("enabled"));
    }

    @Test
    void savedConditionCreationPersistsNegationAndRejectsDuplicates() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds unless negated.")
                .check((condition, questPlayer) -> "")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(SavedConditionCommands.saveCondition(plugin, adapter, "BlockedBySuccess", "Static", "--negate").success());
        assertTrue(SavedConditionCommands.checkSavedCondition(plugin, new TestPlayer("player-1"), "BlockedBySuccess").success());
        assertEquals(
                "<error>Error! A condition with the name <highlight>BlockedBySuccess</highlight> already exists!",
                SavedConditionCommands.saveCondition(plugin, adapter, "BlockedBySuccess", "Static", "").message());
    }

    @Test
    void givingSavedItemsIsResolvedInCoreAndDeliveredThroughPlatformPlayer() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final TestPlayer player = new TestPlayer("player-1", "NoeX");
        final NotQuestsAdapter onlineAdapter = new OnlinePlayerAdapter(adapter, player);

        assertTrue(ItemCommands.createSavedItem(plugin, onlineAdapter, "DailyBook", "book", "default").success());
        assertTrue(ItemCommands.giveSavedItem(plugin, onlineAdapter, "DailyBook", player.playerName(), 4).success());

        assertEquals(List.of("book:4"), player.receivedItems);
    }

    @Test
    void conversationOptionSuggestionsComeFromActiveCoreConversation() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final TestPlayer player = new TestPlayer("player-1");
        plugin.saveConversation("intro", List.of("Option one", "Option two"));

        assertTrue(plugin.startConversation(player, "intro", true, ignored -> {}));
        assertEquals(List.of("1", "2"), ConversationCommands.conversationOptionIds(plugin, player));

        ConversationCommands.continueConversation(plugin, player, 1);
        assertEquals(List.of(), ConversationCommands.conversationOptionIds(plugin, player));
    }

    @Test
    void editCommandsDoNotCreateMissingQuestsOrCategories() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertFalse(QuestEditCommands.setQuestDisplayName(plugin, "MissingQuest", "Display").success());
        assertFalse(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "MissingQuest", "StaticObjective", "", "").success());
        assertFalse(QuestLifecycleCommands.createQuest(plugin, "Virus", "MissingCategory").success());
        assertFalse(CategoryCommands.setCategoryDisplayName(plugin, "MissingCategory", "Display").success());

        assertEquals(List.of(), plugin.questNames());
        assertFalse(plugin.categoryNames().contains("MissingCategory"));
    }

    @Test
    void removeTextCommandsUseRemovalMessages() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(SavedConditionCommands.saveCondition(plugin, adapter, "Requirement", "Static", "").success());
        assertTrue(SavedConditionCommands.setSavedConditionDescription(plugin, "Requirement", "Needs something.").success());
        assertEquals(
                "<success>Description successfully removed from condition <highlight>Requirement</highlight>! New description: <highlight2></highlight2>",
                SavedConditionCommands.removeSavedConditionDescription(plugin, "Requirement").message());

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());
        assertTrue(QuestObjectiveCommands.setObjectiveDescription(
                        plugin, "Virus", new int[] {1}, "Break blocks.")
                .success());
        final String objectiveMessage = QuestObjectiveCommands.removeObjectiveDescription(
                        plugin, "Virus", new int[] {1})
                .message();
        assertTrue(objectiveMessage.contains("removed"), objectiveMessage);
        assertFalse(objectiveMessage.contains("added"), objectiveMessage);
    }

    @Test
    void savedActionConditionEditMessagesMatchOldPaperOutput() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .check((condition, questPlayer) -> "")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(SavedActionCommands.saveAction(plugin, adapter, "Notify", "Echo", "").success());
        assertEquals(
                "<success>Static Condition successfully added to Action <highlight>Notify</highlight>!",
                SavedActionCommands.addSavedActionCondition(plugin, adapter, "Notify", "Static", "").message());
        assertEquals(
                "<success>Description successfully added to condition with ID <highlight>1</highlight> of action <highlight2>Notify</highlight2>! New description: <highlight2>Needs static</highlight2>",
                SavedActionCommands.setSavedActionConditionDescription(plugin, "Notify", 1, "Needs static").message());
        assertEquals(
                "<main>Description of condition with ID <highlight>1</highlight> of action <highlight2>Notify</highlight2>:\nNeeds static",
                SavedActionCommands.savedActionConditionDescription(plugin, "Notify", 1).message());
        assertEquals(
                "<success>Description successfully removed from condition with ID <highlight>1</highlight> of action <highlight2>Notify</highlight2>! New description: <highlight2></highlight2>",
                SavedActionCommands.removeSavedActionConditionDescription(plugin, "Notify", 1).message());
    }

    @Test
    void objectiveLocationMessagesMatchOldPaperOutput() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());
        assertEquals(
                "<error>This objective has no marker location yet. Use <highlight>location set here</highlight> in-game while editing this objective, or set exact coordinates from console.",
                QuestObjectiveCommands.setObjectiveLocationEnabled(plugin, "Virus", 1, true).message());
        assertEquals(
                "<success>Objective <highlight>1</highlight> now points to <highlight2>world 4 5 6</highlight2> using <highlight>exact coordinates</highlight>.",
                QuestObjectiveCommands.setObjectiveLocation(
                        plugin, "Virus", new int[] {1}, "world", 4, 5, 6, "exact coordinates").message());
        assertEquals(
                "<main>Objective <highlight>1</highlight> marker is <success>enabled</success><main> at <highlight2>world 4 5 6</highlight2>.",
                QuestObjectiveCommands.objectiveLocationStatus(plugin, "Virus", 1).message());
        assertEquals(
                "<success>Objective <highlight>1</highlight> keeps its saved marker location, but no longer shows it to players.",
                QuestObjectiveCommands.setObjectiveLocationEnabled(plugin, "Virus", 1, false).message());
        assertEquals(
                "<main>Objective <highlight>1</highlight> marker is <warn>disabled</warn><main> at <highlight2>world 4 5 6</highlight2>.",
                QuestObjectiveCommands.objectiveLocationStatus(plugin, "Virus", 1).message());
    }

    @Test
    void objectiveListMatchesOldPaperStructure() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .taskDescription((objective, questPlayer, activeObjective) -> "Do the static objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());

        assertEquals(
                List.of(
                        "",
                        "<highlight>Objectives for Quest <highlight2>Virus</highlight2>:</highlight>",
                        "<highlight>1.</highlight> <main>StaticObjective",
                        "   <highlight>Unlock Conditions:",
                        "      <unimportant>No unlock conditions found!",
                        "   <highlight>Progress Conditions:",
                        "      <unimportant>No progress conditions found!",
                        "   <highlight>Complete Conditions:",
                        "      <unimportant>No complete conditions found!",
                        "Do the static objective."),
                QuestObjectiveCommands.listQuestObjectives(plugin, "Virus").stream()
                        .map(CommandMessage::message)
                        .toList());
    }

    @Test
    void objectiveTextMessagesUseOldPaperIdWording() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());

        assertEquals(
                "<main>Description successfully added to objective with ID <highlight>1</highlight>! New description: <highlight2>Break blocks.</highlight2>",
                QuestObjectiveCommands.setObjectiveDescription(plugin, "Virus", new int[] {1}, "Break blocks.").message());
        assertEquals(
                "<main>Current description of objective with ID <highlight>1</highlight>: <highlight2>Break blocks.</highlight2>",
                QuestObjectiveCommands.objectiveDescription(plugin, "Virus", new int[] {1}).message());
        assertEquals(
                "<main>Description successfully removed from objective with ID <highlight>1</highlight>! New description: <highlight2></highlight2>",
                QuestObjectiveCommands.removeObjectiveDescription(plugin, "Virus", new int[] {1}).message());
    }

    @Test
    void completionNpcRightClickSelectStartsSelectionInsteadOfStoringLiteral() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter base = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final NotQuestsAdapter adapter = adapterWithNpcSelection(plugin, base);
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);
        final TestPlayer player = new TestPlayer("player-1");

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());

        final CommandMessage message =
                QuestObjectiveCommands.setObjectiveCompletionNpc(
                        plugin, adapter, player, "Virus", new int[] {1}, "rightClickSelect");

        assertTrue(message.success());
        assertEquals("", message.message());
        assertFalse("rightClickSelect".equals(plugin.getOrCreateQuest("Virus").getObjectiveFromID(1).getCompletionNPC()));
        final String selector = "armorstand:00000000-0000-0000-0000-000000000001";
        assertEquals(
                "<success>The completionArmorStandUUID of the objective with the ID <highlight>1</highlight> has been set to the NPC with the ID <highlight2>"
                        + selector
                        + "</highlight2> and name <highlight2>armorstand:00000000-0000-0000-0000-000000000001 (Dummy)</highlight2>!",
                player.messages.getFirst());
        assertEquals(selector, plugin.getOrCreateQuest("Virus").getObjectiveFromID(1).getCompletionNPC());
    }

    @Test
    void armorStandClearAndListUseCoreAttachmentState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        plugin.syncQuestNpcAttachment(
                "Virus",
                "armorstand",
                com.notquests.core.npc.NQNPCID.fromUUID(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                "Dummy",
                true);
        plugin.syncQuestNpcAttachment(
                "Virus",
                "citizens",
                com.notquests.core.npc.NQNPCID.fromInteger(12),
                "Guide",
                true);

        final List<CommandMessage> list = QuestNpcCommands.questArmorStands(plugin, "Virus");
        assertEquals("<highlight>Armor stands bound to quest <highlight2>Virus</highlight2>:", list.getFirst().message());
        assertTrue(list.get(1).message().contains("armorstand:00000000-0000-0000-0000-000000000001"));

        assertEquals(
                "<success>Cleared 1 armor-stand attachment(s) for quest <highlight>Virus</highlight>.",
                QuestNpcCommands.clearQuestArmorStands(plugin, adapter, "Virus").message());
        assertTrue(QuestNpcCommands.questArmorStands(plugin, "Virus").get(1).message().contains("No armor stands"));
        assertEquals(1, plugin.quest("Virus").getNpcAttachments().size());
        assertEquals("citizens", plugin.quest("Virus").getNpcAttachments().getFirst().npcType());
    }

    @Test
    void acceptCooldownCommandStoresOldPaperMinuteValue() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestEditCommands.setQuestAcceptCooldownComplete(plugin, "Virus", Duration.ofSeconds(90)).success());

        assertEquals(1, plugin.getOrCreateQuest("Virus").getAcceptCooldownComplete());
    }

    @Test
    void nestedProgressOrderEditsSelectedObjectiveHolderOnly() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", new int[] {1}, "StaticObjective", "", "").success());
        assertTrue(QuestObjectiveCommands.setObjectiveProgressOrder(
                plugin, "Virus", new int[] {1}, "lastToFirst").success());

        assertEquals("", plugin.getOrCreateQuest("Virus").getObjectiveProgressOrder());
        assertEquals("lastToFirst", plugin.getOrCreateQuest("Virus").getObjectiveFromID(1).getChildObjectiveProgressOrder());
    }

    @Test
    void emptyQuestEntryListsOnlyPrintOldPaperHeaders() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());

        assertEquals(List.of("<highlight>Requirements for Quest <highlight2>Virus</highlight2>:"),
                QuestRequirementCommands.listQuestRequirements(plugin, "Virus").stream()
                        .map(CommandMessage::message)
                        .toList());
        assertEquals(List.of("<highlight>Rewards for Quest <highlight2>Virus</highlight2>:"),
                QuestRewardCommands.listQuestRewards(plugin, "Virus").stream()
                        .map(CommandMessage::message)
                        .toList());
        assertEquals(List.of("<highlight>Triggers for Quest <highlight2>Virus</highlight2>:"),
                TriggerCommands.listQuestTriggers(plugin, "Virus").stream()
                        .map(CommandMessage::message)
                        .toList());
    }

    @Test
    void rewardDisplayNameShowMatchesOldPaperOutput() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestRewardCommands.addQuestReward(plugin, adapter, "Virus", "Echo", "").success());

        assertEquals("<main>This reward has no display name set.",
                QuestRewardCommands.questRewardDisplayName(plugin, "Virus", 1).message());
        assertTrue(QuestRewardCommands.setQuestRewardDisplayName(plugin, "Virus", 1, "Daily Reward")
                .success());
        assertEquals("<main>Reward display name: <highlight>Daily Reward</highlight>",
                QuestRewardCommands.questRewardDisplayName(plugin, "Virus", 1).message());
    }

    @Test
    void commandActionInputNormalizesLeadingSlashLikeOldPaperStorage() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("ConsoleCommand")
                .displayName("Console Command")
                .description("Runs a server command.")
                .field("command", adapter.fields().commandText(), "Command to run.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);
        final Actions.Type type = runner.actionTypes().stream()
                .filter(action -> action.id().equals("ConsoleCommand"))
                .findFirst()
                .orElseThrow();

        final com.notquests.core.actions.Action withoutSlash = Actions.parse(adapter, type, "say hello");
        final com.notquests.core.actions.Action withSlash = Actions.parse(adapter, type, "/say hello");

        assertEquals("/say hello", withoutSlash.text("command"));
        assertEquals("/say hello", withSlash.text("command"));
    }

    @Test
    void questPreviewDoesNotInventEmptyRequirementOrRewardLines() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        final List<String> messages = PlayerQuestCommands.questPreview(plugin, new TestPlayer("PlayerOne"), "Virus").stream()
                .map(CommandMessage::message)
                .toList();

        assertFalse(messages.contains("<unimportant>No requirements."));
        assertFalse(messages.contains("<unimportant>No rewards."));
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Click to accept the Quest <highlight>Virus")));
    }

    @Test
    void questPreviewKeepsOldPaperHiddenRequirementAndRewardRules() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Static")
                .displayName("Static")
                .description("Always succeeds.")
                .conditionDescription((condition, questPlayer, objects) -> "Static condition details.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.actions()
                .action("Echo")
                .displayName("Echo")
                .description("Records a message.")
                .field("message", adapter.fields().greedyText(), "Message.")
                .actionDescription((action, questPlayer, objects) -> "Echo action: " + action.text("message"))
                .execute((action, questPlayer, objects) -> {})
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestRequirementCommands.addQuestRequirement(plugin, adapter, "Virus", "Static", "").success());
        assertTrue(QuestRequirementCommands.setRequirementHidden(plugin, "Virus", 1, "true")
                .success());
        assertTrue(QuestRewardCommands.addQuestReward(plugin, adapter, "Virus", "Echo", "visible").success());
        assertTrue(QuestRewardCommands.setQuestRewardDisplayName(plugin, "Virus", 1, "Visible Reward")
                .success());
        assertTrue(QuestRewardCommands.addQuestReward(plugin, adapter, "Virus", "Echo", "hidden").success());

        final List<String> messages = PlayerQuestCommands.questPreview(plugin, new TestPlayer("PlayerOne"), "Virus").stream()
                .map(CommandMessage::message)
                .toList();

        assertFalse(messages.stream().anyMatch(message -> message.contains("Static")), messages.toString());
        assertTrue(messages.contains("<green>1. <blue>Visible Reward</green>"), messages.toString());
        assertTrue(messages.contains("<green>2. <blue>Reward hidden</blue></green>"), messages.toString());
        assertFalse(messages.stream().anyMatch(message -> message.contains("Echo action: hidden")), messages.toString());
    }

    @Test
    void conditionCommandsKeepOldPaperValidationRules() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.conditions()
                .condition("Date")
                .displayName("Date")
                .description("Checks real-world date/time.")
                .field("Date operation", adapter.fields().text(() -> List.of("after", "before")), "Date operation.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.conditions()
                .condition("CompletedObjective")
                .displayName("Completed Objective")
                .description("Checks that another objective is complete.")
                .field("dependingObjectiveId", adapter.fields().integer(-1), "Depending objective.")
                .check((condition, questPlayer) -> "")
                .register();
        adapter.objectives()
                .objective("StaticObjective")
                .displayName("Static Objective")
                .description("A test objective.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertFalse(SavedConditionCommands.saveCondition(plugin, adapter, "BadDate", "Date", "between").success());
        assertTrue(SavedConditionCommands.saveCondition(plugin, adapter, "GoodDate", "Date", "After").success());
        assertEquals("after", plugin.savedCondition("GoodDate").getData().text("Date operation"));

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "StaticObjective", "", "").success());
        final CommandMessage selfDependency =
                QuestObjectiveCommands.addObjectiveCondition(
                        plugin, adapter, "Virus", new int[] {1}, "unlock", "CompletedObjective", "1");
        assertFalse(selfDependency.success());
        assertEquals("<error>Error: You cannot set an objective to depend on itself!", selfDependency.message());
    }

    @Test
    void rootHelpIsGeneratedFromCommandTreeWithClickableUsageLines() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);

        final List<String> messages = runner.rootSummary("qa");

        assertTrue(messages.getFirst().contains("available admin commands"));
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("<click:suggest_command:'/qa edit '>")
                        && message.contains("<highlight>edit</highlight>")));
    }

    @Test
    void questProgressListsCompletedObjectiveIds() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);
        plugin.getOrCreateQuest("Virus");
        plugin.activeQuestPlayer("PlayerOne").addActiveQuest("Virus");
        plugin.activeQuestPlayer("PlayerOne").addCompletedObjectiveId("Virus", 2);

        final List<String> messages = QuestProgressCommands.questProgress(plugin, adapter, "PlayerOne", "Virus").stream()
                .map(CommandMessage::message)
                .toList();

        assertTrue(messages.stream().anyMatch(message -> message.contains("<highlight>2.</highlight> <success>completed")));
    }

    @Test
    void takeableQuestSuggestionsExcludeDisabledQuests() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);
        plugin.getOrCreateQuest("OpenQuest");
        plugin.getOrCreateQuest("HiddenQuest").setTakeEnabled(false);

        assertEquals(List.of("OpenQuest"), runner.takeableQuestNames());
    }

    @Test
    void variableCheckCommandUsesPlayerFlagAsTarget() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter baseAdapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final NotQuestsAdapter adapter = new OnlinePlayerAdapter(baseAdapter);
        adapter.variables().stringVariable("WhoAmI")
                .displayName("Who Am I")
                .description("Returns the target player identifier.")
                .singular("Who Am I")
                .plural("Who Am I")
                .get((questPlayer, objects) -> questPlayer.playerIdentifier())
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                root = NQCommandBuilder.root("qa", NQDescription.of("Admin commands."));
        final NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = RegistryCommands.adminCommands(root, runner).stream()
                        .filter(registration -> registration.steps().stream()
                                .map(NQCommandStep::name)
                                .toList()
                                .equals(List.of("qa", "variables", "check", "WhoAmI")))
                        .findFirst()
                        .orElseThrow();

        final List<CommandMessage> messages = command.handler().execute(new FlaggedPlayerContext());

        assertEquals(1, messages.size());
        assertEquals(
                "<main>WhoAmI variable (string) result for player Flagged:</main> <highlight>Flagged</highlight>",
                messages.getFirst().message());
    }

    @Test
    void triggerCreationStoresSharedActionApplyOnAndWorldState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.triggers()
                .trigger("BEGIN")
                .displayName("Begin")
                .description("Runs when the quest starts.")
                .register();
        adapter.objectives()
                .objective("TestObjective")
                .displayName("Test Objective")
                .description("A test objective used for trigger targeting.")
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);

        assertTrue(QuestLifecycleCommands.createQuest(plugin, "Virus", "").success());
        assertTrue(QuestObjectiveCommands.addQuestObjective(plugin, adapter, "Virus", "TestObjective", "", "").success());
        assertTrue(TriggerCommands.addQuestTrigger(plugin, "Virus", "BEGIN", "Notify", "O1", "world", "").success());

        final com.notquests.core.triggers.Trigger data = plugin.getOrCreateQuest("Virus").getTriggerFromID(1).data();
        assertEquals("Notify", data.text("action"));
        assertEquals(1, data.integer("applyOn"));
        assertEquals("world", data.text("worldName"));
        assertFalse(TriggerCommands.addQuestTrigger(plugin, "Virus", "BEGIN", "Notify", "O2", "world", "").success());
    }

    @Test
    void resetAndRemoveQuestMatchesOldPaperSemantics() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = new OnlinePlayerAdapter(
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)),
                new TestPlayer("PlayerOne", "NoeX"));
        final CommandManager runner = new CommandManager(plugin, adapter);
        plugin.getOrCreateQuest("Virus");
        plugin.questPlayer("PlayerOne", "default").addActiveQuest("Virus");
        plugin.questPlayer("PlayerOne", "default")
                .addCompletedQuest(new com.notquests.core.structs.QuestPlayer.CompletedQuest("Virus", "PlayerOne", 1L));

        final List<CommandMessage> messages =
                QuestLifecycleCommands.resetAndRemoveQuest(plugin, adapter, "PlayerOne", "Virus");

        assertTrue(messages.stream().anyMatch(message -> message.message().contains("active quest")));
        assertTrue(messages.stream().anyMatch(message -> message.message().contains("completed quest")));
        assertTrue(messages.stream().anyMatch(message -> message.message().contains("UUID <highlight>PlayerOne")));
        assertTrue(messages.stream().anyMatch(message -> message.message().contains("name <highlight2>NoeX")));
        assertEquals(List.of(), plugin.questPlayer("PlayerOne", "default").getActiveQuestIdentifiers().stream().toList());
        assertEquals(List.of(), plugin.questPlayer("PlayerOne", "default").getCompletedQuests());
    }

    @Test
    void resetAndFailAllMatchesOldPaperSemantics() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final CommandManager runner = new CommandManager(plugin, adapter);
        plugin.getOrCreateQuest("Virus");
        plugin.questPlayer("PlayerOne", "default").addActiveQuest("Virus");
        plugin.questPlayer("PlayerOne", "default")
                .addCompletedQuest(new com.notquests.core.structs.QuestPlayer.CompletedQuest("Virus", "PlayerOne", 1L));

        final List<CommandMessage> messages =
                QuestLifecycleCommands.failQuestForAllPlayers(plugin, adapter, "Virus");

        assertTrue(messages.stream().anyMatch(message -> message.message().contains("Failed the quest as an active quest")));
        assertTrue(messages.stream().anyMatch(message -> message.message().contains("completed quest")));
        assertEquals(List.of(), plugin.questPlayer("PlayerOne", "default").getActiveQuestIdentifiers().stream().toList());
        assertEquals(List.of("Virus"), plugin.questPlayer("PlayerOne", "default").getFailedQuests().stream()
                .map(com.notquests.core.structs.QuestPlayer.FailedQuest::questIdentifier)
                .toList());
        assertEquals(List.of(), plugin.questPlayer("PlayerOne", "default").getCompletedQuests());
    }

    private static NQCommandTree.Node<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            child(
                    final NQCommandTree.Node<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            node,
                    final String name) {
        return node.childNodes().stream()
                .filter(child -> child.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private record ArgumentContext(Map<String, String> arguments) implements NQCommandContext {
        @Override
        public String argument(final String name) {
            return arguments.getOrDefault(name, "");
        }

        @Override public Object rawArgument(final String name) { return argument(name); }
        @Override public boolean flagPresent(final String name) { return false; }
        @Override public String flag(final String name) { return ""; }
        @Override public Object rawFlag(final String name) { return ""; }
        @Override public Object platformSender() { return null; }
        @Override public String rawInput() { return ""; }

        @Override
        public String platformVersion() {
            return "test";
        }

        @Override
        public PlatformPlayer questPlayer() {
            return null;
        }
    }

    private static final class TestPlayer implements TestPlatformPlayer {
        private final String playerIdentifier;
        private final String playerName;
        private final List<String> messages = new ArrayList<>();
        private final List<String> receivedItems = new ArrayList<>();

        private TestPlayer(final String playerIdentifier) {
            this(playerIdentifier, playerIdentifier);
        }

        private TestPlayer(final String playerIdentifier, final String playerName) {
            this.playerIdentifier = playerIdentifier;
            this.playerName = playerName;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return playerIdentifier;
        }

        @Override
        public String playerName() {
            return playerName;
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
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }

        @Override
        public boolean giveItems(
                final List<com.notquests.core.items.SavedItems.ItemChoice> items) {
            final ItemSelection selection = items.getFirst().selection();
            receivedItems.add(selection.listedMaterials("") + ":" + selection.amount());
            return true;
        }
    }

    private static NotQuestsAdapter adapterWithNpcSelection(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter delegate) {
        return new NpcSelectingAdapter(plugin, delegate, null);
    }

    private static NotQuestsAdapter adapterWithDeferredNpcSelection(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter delegate,
            final AtomicInteger pendingSelectionId) {
        return new NpcSelectingAdapter(plugin, delegate, pendingSelectionId);
    }

    private record NpcSelectingAdapter(
            NotQuestsPlugin plugin,
            NotQuestsAdapter delegate,
            AtomicInteger pendingSelectionId) implements TestNotQuestsAdapter {
        @Override
        public FieldFactories fields() {
            return delegate.fields();
        }

        @Override
        public Actions.Registry actions() {
            return delegate.actions();
        }

        @Override
        public Conditions.Registry conditions() {
            return delegate.conditions();
        }

        @Override
        public Objectives.Registry objectives() {
            return delegate.objectives();
        }

        @Override
        public Triggers.Registry triggers() {
            return delegate.triggers();
        }

        @Override
        public Variables.Registry variables() {
            return delegate.variables();
        }

        @Override
        public List<String> variableNames(final VariableDataType type) {
            return delegate.variableNames(type);
        }

        @Override
        public VariableDataType variableType(final String variableName) {
            return delegate.variableType(variableName);
        }

        @Override
        public String variableSingular(final String variableName) {
            return delegate.variableSingular(variableName);
        }

        @Override
        public String variablePlural(final String variableName) {
            return delegate.variablePlural(variableName);
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            return delegate.variableFields(variableName);
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            return delegate.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public String serverBrand() {
            return delegate.serverBrand();
        }

        @Override
        public List<String> damageTypeIds() {
            return delegate.damageTypeIds();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return delegate.onlinePlayerNames();
        }

        @Override
        public PlatformPlayer onlineQuestPlayer(final String playerName) {
            return delegate.onlineQuestPlayer(playerName);
        }

        @Override
        public List<String> worldNames() {
            return delegate.worldNames();
        }

        @Override
        public List<String> itemSelectionOptions() {
            return delegate.itemSelectionOptions();
        }

        @Override
        public List<String> entityTypeIds() {
            return delegate.entityTypeIds();
        }

        @Override
        public List<String> particleTypeIds() {
            return delegate.particleTypeIds();
        }

        @Override
        public List<String> soundTypeIds() {
            return delegate.soundTypeIds();
        }

        @Override
        public List<String> soundCategoryIds() {
            return delegate.soundCategoryIds();
        }

        @Override
        public List<String> statisticIds() {
            return delegate.statisticIds();
        }

        @Override
        public List<String> advancementIds() {
            return delegate.advancementIds();
        }

        @Override
        public List<String> blockMaterialOptions() {
            return delegate.blockMaterialOptions();
        }

        @Override
        public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
            return delegate.playerStatistic(questPlayer, statisticId);
        }

        @Override
        public boolean setPlayerStatistic(
                final PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            return delegate.setPlayerStatistic(questPlayer, statisticId, value);
        }

        @Override
        public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
            return delegate.hasAdvancement(questPlayer, advancementId);
        }

        @Override
        public boolean setAdvancement(
                final PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            return delegate.setAdvancement(questPlayer, advancementId, completed);
        }

        @Override
        public String blockMaterial(final com.notquests.core.platform.NQLocation location) {
            return delegate.blockMaterial(location);
        }

        @Override
        public boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final com.notquests.core.platform.NQLocation location,
                final String materialOrKeyword) {
            return delegate.setBlockMaterial(questPlayer, location, materialOrKeyword);
        }

        @Override
        public List<ItemSelection> containerInventoryItems(
                final com.notquests.core.platform.NQLocation location) {
            return delegate.containerInventoryItems(location);
        }

        @Override
        public boolean addContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            return delegate.addContainerInventoryItems(location, items, dropOverflow);
        }

        @Override
        public boolean removeContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items) {
            return delegate.removeContainerInventoryItems(location, items);
        }

        @Override
        public boolean setContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items) {
            return delegate.setContainerInventoryItems(location, items);
        }

        @Override
        public List<String> enchantmentIds() {
            return delegate.enchantmentIds();
        }

        @Override
        public com.notquests.core.platform.NQLocation location(
                final String worldName,
                final double x,
                final double y,
                final double z) {
            return delegate.location(worldName, x, y, z);
        }

        @Override
        public ItemSelection parseItemSelection(final String input) {
            return delegate.parseItemSelection(input);
        }

        @Override
        public boolean supportsNpcAttachments() {
            return delegate.supportsNpcAttachments();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return delegate.supportsArmorStandAttachmentTools();
        }

        @Override
        public List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
            return delegate.npcSelectorOptions(allowNone, allowRightClickSelect);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return delegate.npcSelection(npcSelector);
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return delegate.setNpcQuestGiver(selection, enabled);
        }

        @Override
        public boolean giveArmorStandTool(
                final PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return delegate.giveArmorStandTool(actor, tool);
        }

        @Override
        public boolean giveNpcSelectionTool(
                final PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            if (pendingSelectionId != null) {
                pendingSelectionId.set(selectionId);
                return true;
            }
            final java.util.UUID uuid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
            plugin.completeNpcSelection(
                    selectionId,
                    "armorstand",
                    com.notquests.core.npc.NQNPCID.fromUUID(uuid),
                    "Dummy");
            return true;
        }

        @Override
        public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
            return delegate.hasPermission(questPlayer, permission);
        }

        @Override
        public boolean setPermission(final PlatformPlayer questPlayer, final String permission, final boolean value) {
            return delegate.setPermission(questPlayer, permission, value);
        }

        @Override
        public List<String> inventorySlotIds() {
            return delegate.inventorySlotIds();
        }

        @Override
        public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
            return delegate.inventoryItemEnchantments(questPlayer, slotId);
        }

        @Override
        public void warn(final String message) {
            delegate.warn(message);
        }

        @Override
        public void logInfo(final String message) {
            delegate.logInfo(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            delegate.broadcast(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            delegate.dispatchConsoleCommand(command);
        }

        @Override
        public void schedule(final Duration delay, final Runnable action) {
            delegate.schedule(delay, action);
        }

        @Override
        public boolean isServerThread() {
            return delegate.isServerThread();
        }

        @Override
        public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
            return delegate.callOnServerThread(action);
        }

        @Override
        public String resolveActionText(
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return delegate.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final ActiveObjective activeObjective,
                final Map<String, String> replacements) {
            return delegate.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }

    private record OnlinePlayerAdapter(NotQuestsAdapter delegate, PlatformPlayer player) implements TestNotQuestsAdapter {
        private OnlinePlayerAdapter(final NotQuestsAdapter delegate) {
            this(delegate, null);
        }

        @Override
        public FieldFactories fields() {
            return delegate.fields();
        }

        @Override
        public Actions.Registry actions() {
            return delegate.actions();
        }

        @Override
        public Conditions.Registry conditions() {
            return delegate.conditions();
        }

        @Override
        public Objectives.Registry objectives() {
            return delegate.objectives();
        }

        @Override
        public Triggers.Registry triggers() {
            return delegate.triggers();
        }

        @Override
        public Variables.Registry variables() {
            return delegate.variables();
        }

        @Override
        public List<String> variableNames(final VariableDataType type) {
            return delegate.variableNames(type);
        }

        @Override
        public VariableDataType variableType(final String variableName) {
            return delegate.variableType(variableName);
        }

        @Override
        public String variableSingular(final String variableName) {
            return delegate.variableSingular(variableName);
        }

        @Override
        public String variablePlural(final String variableName) {
            return delegate.variablePlural(variableName);
        }

        @Override
        public List<RegistryField.Definition> variableFields(final String variableName) {
            return delegate.variableFields(variableName);
        }

        @Override
        public Object variableValue(
                final String variableName,
                final PlatformPlayer questPlayer,
                final Object... objects) {
            return delegate.variableValue(variableName, questPlayer, objects);
        }

        @Override
        public String serverBrand() {
            return delegate.serverBrand();
        }

        @Override
        public List<String> damageTypeIds() {
            return delegate.damageTypeIds();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return delegate.onlinePlayerNames();
        }

        @Override
        public List<String> worldNames() {
            return delegate.worldNames();
        }

        @Override
        public List<String> itemSelectionOptions() {
            return delegate.itemSelectionOptions();
        }

        @Override
        public List<String> entityTypeIds() {
            return delegate.entityTypeIds();
        }

        @Override
        public List<String> particleTypeIds() {
            return delegate.particleTypeIds();
        }

        @Override
        public List<String> soundTypeIds() {
            return delegate.soundTypeIds();
        }

        @Override
        public List<String> soundCategoryIds() {
            return delegate.soundCategoryIds();
        }

        @Override
        public List<String> statisticIds() {
            return delegate.statisticIds();
        }

        @Override
        public List<String> advancementIds() {
            return delegate.advancementIds();
        }

        @Override
        public List<String> blockMaterialOptions() {
            return delegate.blockMaterialOptions();
        }

        @Override
        public int playerStatistic(final PlatformPlayer questPlayer, final String statisticId) {
            return delegate.playerStatistic(questPlayer, statisticId);
        }

        @Override
        public boolean setPlayerStatistic(
                final PlatformPlayer questPlayer,
                final String statisticId,
                final int value) {
            return delegate.setPlayerStatistic(questPlayer, statisticId, value);
        }

        @Override
        public boolean hasAdvancement(final PlatformPlayer questPlayer, final String advancementId) {
            return delegate.hasAdvancement(questPlayer, advancementId);
        }

        @Override
        public boolean setAdvancement(
                final PlatformPlayer questPlayer,
                final String advancementId,
                final boolean completed) {
            return delegate.setAdvancement(questPlayer, advancementId, completed);
        }

        @Override
        public String blockMaterial(final com.notquests.core.platform.NQLocation location) {
            return delegate.blockMaterial(location);
        }

        @Override
        public boolean setBlockMaterial(
                final PlatformPlayer questPlayer,
                final com.notquests.core.platform.NQLocation location,
                final String materialOrKeyword) {
            return delegate.setBlockMaterial(questPlayer, location, materialOrKeyword);
        }

        @Override
        public List<ItemSelection> containerInventoryItems(
                final com.notquests.core.platform.NQLocation location) {
            return delegate.containerInventoryItems(location);
        }

        @Override
        public boolean addContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items,
                final boolean dropOverflow) {
            return delegate.addContainerInventoryItems(location, items, dropOverflow);
        }

        @Override
        public boolean removeContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items) {
            return delegate.removeContainerInventoryItems(location, items);
        }

        @Override
        public boolean setContainerInventoryItems(
                final com.notquests.core.platform.NQLocation location,
                final List<com.notquests.core.items.SavedItems.ItemChoice> items) {
            return delegate.setContainerInventoryItems(location, items);
        }

        @Override
        public List<String> enchantmentIds() {
            return delegate.enchantmentIds();
        }

        @Override
        public com.notquests.core.platform.NQLocation location(
                final String worldName,
                final double x,
                final double y,
                final double z) {
            return delegate.location(worldName, x, y, z);
        }

        @Override
        public ItemSelection parseItemSelection(final String input) {
            return delegate.parseItemSelection(input);
        }

        @Override
        public boolean supportsNpcAttachments() {
            return delegate.supportsNpcAttachments();
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return delegate.supportsArmorStandAttachmentTools();
        }

        @Override
        public List<String> npcSelectorOptions(final boolean allowNone, final boolean allowRightClickSelect) {
            return delegate.npcSelectorOptions(allowNone, allowRightClickSelect);
        }

        @Override
        public NpcSelection npcSelection(final String npcSelector) {
            return delegate.npcSelection(npcSelector);
        }

        @Override
        public boolean setNpcQuestGiver(final NpcSelection selection, final boolean enabled) {
            return delegate.setNpcQuestGiver(selection, enabled);
        }

        @Override
        public boolean giveArmorStandTool(
                final PlatformPlayer actor,
                final ArmorStandToolItem tool) {
            return delegate.giveArmorStandTool(actor, tool);
        }

        @Override
        public boolean giveNpcSelectionTool(
                final PlatformPlayer actor,
                final int selectionId,
                final String displayName,
                final List<String> lore) {
            return delegate.giveNpcSelectionTool(actor, selectionId, displayName, lore);
        }

        @Override
        public boolean hasPermission(final PlatformPlayer questPlayer, final String permission) {
            return delegate.hasPermission(questPlayer, permission);
        }

        @Override
        public boolean setPermission(final PlatformPlayer questPlayer, final String permission, final boolean value) {
            return delegate.setPermission(questPlayer, permission, value);
        }

        @Override
        public List<String> inventorySlotIds() {
            return delegate.inventorySlotIds();
        }

        @Override
        public List<String> inventoryItemEnchantments(final PlatformPlayer questPlayer, final String slotId) {
            return delegate.inventoryItemEnchantments(questPlayer, slotId);
        }

        @Override
        public PlatformPlayer onlineQuestPlayer(final String playerName) {
            if (player != null
                    && (player.playerIdentifier().equalsIgnoreCase(playerName)
                            || player.playerName().equalsIgnoreCase(playerName))) {
                return player;
            }
            return new TestPlayer(playerName);
        }

        @Override
        public void warn(final String message) {
            delegate.warn(message);
        }

        @Override
        public void logInfo(final String message) {
            delegate.logInfo(message);
        }

        @Override
        public void broadcast(final String miniMessage) {
            delegate.broadcast(miniMessage);
        }

        @Override
        public void dispatchConsoleCommand(final String command) {
            delegate.dispatchConsoleCommand(command);
        }

        @Override
        public void schedule(final Duration delay, final Runnable action) {
            delegate.schedule(delay, action);
        }

        @Override
        public boolean isServerThread() {
            return delegate.isServerThread();
        }

        @Override
        public <T> T callOnServerThread(final java.util.concurrent.Callable<T> action) throws Exception {
            return delegate.callOnServerThread(action);
        }

        @Override
        public String resolveActionText(
                final Actions.Data action,
                final PlatformPlayer questPlayer,
                final String text,
                final Object... objects) {
            return delegate.resolveActionText(action, questPlayer, text, objects);
        }

        @Override
        public String objectiveTaskText(
                final String translationKey,
                final PlatformPlayer questPlayer,
                final ActiveObjective activeObjective,
                final Map<String, String> replacements) {
            return delegate.objectiveTaskText(translationKey, questPlayer, activeObjective, replacements);
        }
    }

    private static final class FlaggedPlayerContext implements NQCommandContext {
        @Override
        public String argument(final String name) {
            return "";
        }

        @Override public Object rawArgument(final String name) { return argument(name); }

        @Override public boolean flagPresent(final String name) {
            return NQFlags.VARIABLE_CHECK_PLAYER.name().equals(name);
        }

        @Override
        public String flag(final String name) {
            return NQFlags.VARIABLE_CHECK_PLAYER.name().equals(name) ? "Flagged" : "";
        }

        @Override public Object rawFlag(final String name) { return flag(name); }
        @Override public Object platformSender() { return null; }
        @Override public String rawInput() { return ""; }

        @Override
        public PlatformPlayer questPlayer() {
            return new TestPlayer("Sender");
        }

        @Override
        public String platformVersion() {
            return "test";
        }
    }
}
