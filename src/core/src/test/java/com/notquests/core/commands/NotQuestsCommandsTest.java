package com.notquests.core.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.managers.CommandManager;
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
import java.util.List;
import java.util.Map;

class NotQuestsCommandsTest {
    @Test
    void ownsPortableUserCommandShape() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final CommandManager runner = new CommandManager(
                plugin, plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.portableUserCommands("notquests", NQDescription.of("Player commands."), runner)
                .forEach(tree::register);

        assertEquals(
                List.of("version", "registry", "objectives", "actions", "conditions", "conversations", "variables", "triggers"),
                tree.root("notquests").childNodes().stream().map(NQCommandTree.Node::name).toList());
        assertEquals(
                List.of("execute", "save", "executeSaved"),
                tree.root("notquests").childNodes().stream()
                        .filter(node -> node.name().equals("actions"))
                        .findFirst()
                        .orElseThrow()
                        .childNodes()
                        .stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
    }

    @Test
    void ownsPlayerCommandShape() {
        final CommandManager runner = testRunner();
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.userCommands("notquests", NQDescription.of("Player commands."), runner)
                .forEach(tree::register);

        assertEquals(
                List.of(
                        "help",
                        "profiles",
                        "take",
                        "questPoints",
                        "continueConversation",
                        "activeQuests",
                        "abort",
                        "preview",
                        "progress",
                        "category"),
                tree.root("notquests").childNodes().stream().map(NQCommandTree.Node::name).toList());
        assertEquals(
                List.of("<enter new profile-name (no spaces!)"),
                child(child(child(tree.root("notquests"), "profiles"), "create"), "profile-name")
                        .suggestionOverride()
                        .suggest(null, ""));
    }

    @Test
    void rootPlayerCommandFallsBackToTextMenuWhenGuiCannotOpen() {
        final CommandManager runner = testRunner();
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.userCommands("notquests", NQDescription.of("Player commands."), runner)
                .forEach(tree::register);

        final List<CommandMessage> messages =
                tree.root("notquests").handler().execute(new TestCommandContext(new GuiUnavailablePlayer()));

        assertTrue(messages.stream().anyMatch(message -> message.message().contains("NotQuests Player Commands")));
    }

    @Test
    void activeQuestCommandsValidateBeforeOpeningGui() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final CommandManager runner = new CommandManager(
                plugin, plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        NotQuestsCommands.userCommands("notquests", NQDescription.of("Player commands."), runner)
                .forEach(tree::register);
        QuestLifecycleCommands.createQuest(plugin, "Virus", "");
        final GuiTrackingPlayer player = new GuiTrackingPlayer();
        plugin.giveQuest(player, "Virus", true, ignored -> {});
        final NQCommandTree.Node<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                activeQuestAbort = child(child(tree.root("notquests"), "abort"), "Active Quest");

        final List<CommandMessage> invalidMessages = activeQuestAbort.handler()
                .execute(new TestCommandContext(player, Map.of("Active Quest", "NotActive")));

        assertTrue(invalidMessages.stream().anyMatch(message -> message.message().contains("Quest was not found or active")));
        assertEquals("", player.openedQuest);

        final List<CommandMessage> validMessages = activeQuestAbort.handler()
                .execute(new TestCommandContext(player, Map.of("Active Quest", "virus")));

        assertEquals(List.of(), validMessages);
        assertEquals("active-quest-abort-confirm", player.openedQuest);
    }

    @Test
    void ownsBasicAdminCommandShape() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final CommandManager runner = new CommandManager(
                plugin, plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.adminCommands(
                        "notquestsadmin",
                        NQDescription.of("Admin commands."),
                        runner,
                        () -> "test",
                        () -> "test",
                        () -> Path.of("build/test-notquests"))
                .forEach(tree::register);

        assertEquals(
                List.of(
                        "help",
                        "version",
                        "save",
                        "conversations",
                        "items",
                        "debug",
                        "categories",
                        "conditions",
                        "actions",
                        "tags",
                        "edit",
                        "create",
                        "delete",
                        "clone",
                        "give",
                        "completeQuest",
                        "failQuest",
                        "activeQuests",
                        "completedQuests",
                        "progress",
                        "triggerObjective",
                        "resetAndFailQuestForAllPlayers",
                        "resetAndRemoveQuest",
                        "questpoints",
                        "reload",
                        "list"),
                tree.root("notquestsadmin").childNodes().stream().map(NQCommandTree.Node::name).toList());
        plugin.getOrCreateQuest("Tutorial");
        final var cloneSource = child(child(tree.root("notquestsadmin"), "clone"), "sourceQuest");
        assertEquals(NQArgumentType.quest(), cloneSource.argument());
        final var cloneTarget = child(cloneSource, "newQuestName");
        final var cloned = cloneTarget.handler().execute(new TestCommandContext(null, Map.of(
                "sourceQuest", "Tutorial", "newQuestName", "TutorialHard")));
        assertTrue(cloned.getFirst().success());
        assertEquals(List.of("Tutorial", "TutorialHard"), plugin.questNames());
        assertEquals(
                List.of("general.yml", "languages", "conversations"),
                tree.root("notquestsadmin").childNodes().stream()
                        .filter(node -> node.name().equals("reload"))
                        .findFirst()
                        .orElseThrow()
                        .childNodes()
                        .stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
        assertEquals(
                List.of("create", "list", "edit"),
                child(tree.root("notquestsadmin"), "items").childNodes().stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
        assertEquals(
                List.of("ObjectiveTypes", "RequirementTypes", "ActionTypes", "TriggerTypes", "AllQuests", "Placeholders"),
                tree.root("notquestsadmin").childNodes().stream()
                        .filter(node -> node.name().equals("list"))
                        .findFirst()
                        .orElseThrow()
                        .childNodes()
                        .stream()
                        .map(NQCommandTree.Node::name)
                .toList());
        assertEquals(
                List.of("default", "<Enter new category name>"),
                child(child(child(tree.root("notquestsadmin"), "categories"), "create"), "categoryName")
                        .suggestionOverride()
                        .suggest(null, ""));
        assertEquals(
                List.of("<Enter new Quest Name>"),
                child(child(tree.root("notquestsadmin"), "create"), "questName")
                        .suggestionOverride()
                        .suggest(null, ""));
        final NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>> speakerColor =
                child(child(child(child(child(child(tree.root("notquestsadmin"), "conversations"), "edit"), "conversation"), "speakers"), "add"), "speaker-name")
                        .commandFlags()
                        .stream()
                        .filter(flag -> flag.name().equals("speakerColor"))
                        .findFirst()
                        .orElseThrow();
        assertTrue(speakerColor.valueSuggestions().suggest(null, "").contains("<dark_aqua>"));
        assertEquals(
                NQArgumentType.SuggestionSource.CONVERSATION_SPEAKER_NAMES,
                child(child(child(child(child(child(tree.root("notquestsadmin"), "conversations"), "edit"), "conversation"), "speakers"), "remove"), "speaker")
                        .argument()
                        .suggestions());
    }

    @Test
    void objectiveIdSuggestionsAreEmptyWhenTheQuestHasNoObjectives() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter =
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter,
                () -> "test",
                () -> "test",
                () -> Path.of("build/test-notquests"));
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        commands.adminCommands("notquestsadmin").forEach(tree::register);
        assertTrue(plugin.createQuest("TestQuest").success());
        final NQCommandTree.Node<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                objectiveId = child(
                        child(
                                child(
                                        child(tree.root("notquestsadmin"), "edit"),
                                        "quest"),
                                "objectives"),
                        "edit").childNodes().stream()
                        .filter(node -> node.name().equals("objectiveId"))
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                List.of(),
                commands.suggestions(
                                objectiveId,
                                new TestCommandContext(
                                        new GuiUnavailablePlayer(),
                                        Map.of("quest", "TestQuest")),
                                "")
                        .values());
    }

    @Test
    void keepsOldPaperActionBranchesInCoreCommandGraph() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        adapter.actions()
                .action("ShowTitle")
                .displayName("Show Title")
                .description("Shows a title overlay in the center of the target player's screen.")
                .field("title", adapter.fields().greedyText(), "Title text.")
                .flag("fadeIn", adapter.fields().duration(Duration.ofMillis(500)), "Fade-in duration.")
                .flag("stay", adapter.fields().duration(Duration.ofSeconds(3)), "Stay duration.")
                .flag("fadeOut", adapter.fields().duration(Duration.ofMillis(500)), "Fade-out duration.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("Beam")
                .displayName("Beam")
                .description("Shows or removes a guiding beam marker for the target player.")
                .field("beamName", adapter.fields().text(), "Beam identifier.")
                .flag("remove", adapter.fields().presenceFlag(), "Remove the beam.")
                .flag("location", adapter.fields().storedLocation(), "Beam location.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("SpawnMob")
                .displayName("Spawn Mob")
                .description("Spawns entities at a player or fixed location.")
                .field("entityType", adapter.fields().entityType(), "Entity type.")
                .field("amount", adapter.fields().integer(1), "Amount.")
                .flag("spawnRadiusX", adapter.fields().integer(0), "X radius.")
                .flag("spawnRadiusY", adapter.fields().integer(0), "Y radius.")
                .flag("spawnRadiusZ", adapter.fields().integer(0), "Z radius.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        adapter.actions()
                .action("SpawnParticle")
                .displayName("Spawn Particle")
                .description("Spawns a particle effect at a player or fixed location.")
                .field("particle", adapter.fields().text(() -> List.of("happy_villager")), "Particle.")
                .field("count", adapter.fields().integer(1), "Count.")
                .flag("offsetX", adapter.fields().doubleNumber(0), "X offset.")
                .flag("offsetY", adapter.fields().doubleNumber(0), "Y offset.")
                .flag("offsetZ", adapter.fields().doubleNumber(0), "Z offset.")
                .flag("speed", adapter.fields().doubleNumber(0), "Speed.")
                .flag("forEveryone", adapter.fields().presenceFlag(), "Show to everyone.")
                .execute((action, questPlayer, objects) -> {})
                .register();
        final CommandManager runner = new CommandManager(plugin, adapter);
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.adminCommands(
                        "notquestsadmin",
                        NQDescription.of("Admin commands."),
                        runner,
                        () -> "test",
                        () -> "test",
                        () -> Path.of("build/test-notquests"))
                .forEach(tree::register);
        final List<String> syntaxes = tree.commandInfos().stream()
                .map(NQCommandSchema.CommandInfo::syntax)
                .toList();

        assertEquals(1, syntaxes.stream()
                .filter(syntax -> syntax.startsWith("/notquestsadmin actions add <actionName> ShowTitle timed "))
                .count());
        assertEquals(1, syntaxes.stream()
                .filter(syntax -> syntax.startsWith("/notquestsadmin actions add <actionName> Beam <beamName> spawn "))
                .count());
        assertEquals(1, syntaxes.stream()
                .filter(syntax -> syntax.startsWith("/notquestsadmin actions add <actionName> SpawnMob <entityType> <amount> Location "))
                .count());
        assertEquals(1, syntaxes.stream()
                .filter(syntax -> syntax.startsWith("/notquestsadmin actions add <actionName> SpawnParticle <particle> <count> PlayerLocation"))
                .count());
    }

    @Test
    void ownsNpcCommandShapeWhenAdapterProvidesNpcCapability() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final NotQuestsAdapter adapter = new NpcCapableAdapter(
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
        final CommandManager runner = new CommandManager(plugin, adapter);
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        NotQuestsCommands.adminCommands(
                        "notquestsadmin",
                        NQDescription.of("Admin commands."),
                        runner,
                        () -> "test",
                        () -> "test",
                        () -> Path.of("build/test-notquests"))
                .forEach(tree::register);

        assertEquals(
                List.of("add", "clear", "list"),
                child(child(child(tree.root("notquestsadmin"), "edit"), "quest"), "npcs").childNodes().stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
        assertEquals(
                List.of("check", "add", "clear", "list", "remove"),
                child(child(child(tree.root("notquestsadmin"), "edit"), "quest"), "armorstands").childNodes().stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
        assertEquals(
                List.of("add"),
                child(
                                child(
                                        child(child(tree.root("notquestsadmin"), "conversations"), "edit"),
                                        "conversation"),
                                "npcs")
                        .childNodes()
                        .stream()
                        .map(NQCommandTree.Node::name)
                        .toList());
    }

    private static CommandManager testRunner() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        return new CommandManager(
                plugin,
                plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null)));
    }

    private static NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> child(
            final NQCommandTree.Node<NQArgumentType, NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>, NQSuggestionProvider<NQCommandContext>, NQCommandHandler> node,
            final String name) {
        return node.childNodes().stream()
                .filter(child -> child.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private record TestCommandContext(PlatformPlayer questPlayer, Map<String, String> arguments)
            implements NQCommandContext {
        private TestCommandContext(final PlatformPlayer questPlayer) {
            this(questPlayer, Map.of());
        }

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
    }

    private static class GuiUnavailablePlayer implements TestPlatformPlayer {
        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String playerIdentifier() {
            return "test-player";
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
        public void closeInventory() {}

        @Override
        public com.notquests.core.platform.NQLocation lookingAtBlock(final double maxDistance) {
            return null;
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }

    private static final class GuiTrackingPlayer extends GuiUnavailablePlayer {
        private String openedQuest = "";

        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
            openedQuest = gui.id();
            return true;
        }
    }

    private record NpcCapableAdapter(NotQuestsAdapter delegate) implements TestNotQuestsAdapter {
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
        public List<com.notquests.core.items.ItemSelection> containerInventoryItems(
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
        public com.notquests.core.items.ItemSelection parseItemSelection(final String input) {
            return delegate.parseItemSelection(input);
        }

        @Override
        public boolean supportsNpcAttachments() {
            return true;
        }

        @Override
        public boolean supportsArmorStandAttachmentTools() {
            return true;
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
        public void warn(final String message) {}

        @Override
        public void logInfo(final String message) {}

        @Override
        public void broadcast(final String miniMessage) {}

        @Override
        public void dispatchConsoleCommand(final String command) {}

        @Override
        public void schedule(final java.time.Duration delay, final Runnable action) {
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
}
