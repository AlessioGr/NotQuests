package com.notquests.neoforge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.notquests.builtin.BuiltInPack;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.AdminEditCommands;
import com.notquests.core.commands.NotQuestsCommands;
import com.notquests.core.commands.framework.CommandMessage;
import com.notquests.core.commands.framework.NQArgumentType;
import com.notquests.core.commands.framework.NQCommandContext;
import com.notquests.core.commands.framework.NQCommandHandler;
import com.notquests.core.commands.framework.NQCommandTree;
import com.notquests.core.commands.framework.NQFlag;
import com.notquests.core.commands.framework.NQSuggestionProvider;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.List;
import java.util.Map;

class NeoForgeRegistryCommandRoutingTest {
    @Test
    void registryBackedObjectiveCommandsRouteThroughCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter, () -> "test", () -> "26.1.2", () -> java.nio.file.Path.of("build/test-notquests"));
        final CommandManager runner = commands.commandManager();
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();

        commands.adminCommands("qa").forEach(tree::register);
        assertTrue(plugin.createQuest("TestQuest").success());

        final var command = child(
                child(
                        child(
                                child(
                                        child(
                                                child(tree.root("qa"), "edit"),
                                                "quest"),
                                        "objectives"),
                                "add"),
                        "BreakBlocks"),
                "materials");
        assertEquals(NQArgumentType.Kind.ITEM_SELECTION, command.argument().kind());
        final var leaf = child(command, "amount");
        final var messages = leaf.handler().execute(new TestContext(Map.of(
                "quest", "TestQuest",
                "materials", "diamond_ore",
                "amount", "5")));

        assertTrue(messages.get(0).success(), messages.get(0).message());
        assertEquals(
                "<success>BreakBlocks Objective successfully added to Quest <highlight>TestQuest</highlight>!",
                messages.get(0).message());
        assertEquals("BreakBlocks", plugin.getOrCreateQuest("TestQuest").getObjectives().get(0).typeId());
    }

    @Test
    void coreCommandsSuggestObjectiveIdsFromCoreQuestState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter, () -> "test", () -> "26.1.2", () -> java.nio.file.Path.of("build/test-notquests"));
        final CommandManager runner = commands.commandManager();
        assertTrue(plugin.createQuest("TestQuest").success());
        plugin.getOrCreateQuest("TestQuest")
                .addObjective("BreakBlocks", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()), "");

        assertEquals(
                List.of("1"),
                objectiveIdSuggestions(commands, new TestContext(Map.of("quest", "TestQuest"))));
    }

    @Test
    void coreCommandsSuggestQuestEntryIdsFromCoreQuestState() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter, () -> "test", () -> "26.1.2", () -> java.nio.file.Path.of("build/test-notquests"));
        final CommandManager runner = commands.commandManager();
        assertTrue(plugin.createQuest("TestQuest").success());
        plugin.getOrCreateQuest("TestQuest")
                .addObjective("BreakBlocks", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()), "");
        plugin.getOrCreateQuest("TestQuest")
                .addRequirement("WorldTime", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()));
        plugin.getOrCreateQuest("TestQuest")
                .getObjectiveFromID(1)
                .addCondition("unlock", "WorldTime", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()));
        plugin.getOrCreateQuest("TestQuest")
                .addReward("SendMessage", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()));
        plugin.getOrCreateQuest("TestQuest")
                .addTrigger("BEGIN", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()));

        final TestContext context = new TestContext(Map.of("quest", "TestQuest", "objectiveId", "1"));
        assertEquals(List.of("1"), requirementIdSuggestions(commands, context));
        assertEquals(List.of("1"), objectiveConditionIdSuggestions(commands, context, AdminEditCommands.ConditionGroup.UNLOCK));
        assertEquals(List.of("1"), rewardIdSuggestions(commands, context, AdminEditCommands.RewardTarget.QUEST));
        assertEquals(List.of("1"), triggerIdSuggestions(commands, context));
    }

    @Test
    void exactLocationEditStoresAndEnablesCoreObjectiveMarker() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter, () -> "test", () -> "26.1.2", () -> java.nio.file.Path.of("build/test-notquests"));
        final CommandManager runner = commands.commandManager();
        assertTrue(plugin.createQuest("TestQuest").success());
        plugin.getOrCreateQuest("TestQuest")
                .addObjective("BreakBlocks", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()), "");

        final var messages = execute(
                commands,
                List.of("qa", "edit", "quest", "objectives", "edit", "objectiveId", "location", "set", "world", "x", "y", "z"),
                new TestContext(Map.of(
                        "quest", "TestQuest",
                        "objectiveId", "1",
                        "world", "world",
                        "x", "4",
                        "y", "5",
                        "z", "6")));

        assertTrue(messages.get(0).success(), messages.get(0).message());
        final var objective = plugin.getOrCreateQuest("TestQuest").getObjectiveFromID(1);
        assertTrue(objective.isLocationEnabled());
        assertEquals("world", objective.getLocation().worldName());
        assertEquals(4.0d, objective.getLocation().x());
        assertEquals(5.0d, objective.getLocation().y());
        assertEquals(6.0d, objective.getLocation().z());
    }

    @Test
    void lookingLocationEditRoutesThroughCore() {
        final NotQuestsPlugin plugin = NotQuestsPlugin.create();
        final var adapter = plugin.createRegistryAdapter(new NotQuestsRegistry.PlatformHooks(null, null, null));
        BuiltInPack.register(adapter);
        final NotQuestsCommands commands = plugin.commandSurface(
                adapter, () -> "test", () -> "26.1.2", () -> java.nio.file.Path.of("build/test-notquests"));
        final CommandManager runner = commands.commandManager();
        assertTrue(plugin.createQuest("TestQuest").success());
        plugin.getOrCreateQuest("TestQuest")
                .addObjective("BreakBlocks", new com.notquests.neoforge.TestData(new java.util.LinkedHashMap<>()), "");

        final var messages = execute(
                commands,
                List.of("qa", "edit", "quest", "objectives", "edit", "objectiveId", "location", "set", "looking"),
                new TestContext(
                        Map.of("quest", "TestQuest", "objectiveId", "1"),
                        new LookingPlayer("world", 4, 5, 6)));

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).success(), messages.get(0).message());
        final var objective = plugin.getOrCreateQuest("TestQuest").getObjectiveFromID(1);
        assertEquals("world", objective.getLocation().worldName());
        assertEquals(4.0d, objective.getLocation().x());
        assertEquals(5.0d, objective.getLocation().y());
        assertEquals(6.0d, objective.getLocation().z());
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
                            parent,
                    final String name) {
        return parent.childNodes().stream()
                .filter(node -> node.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing command node: " + name));
    }

    private static List<CommandMessage> execute(
            final NotQuestsCommands commands,
            final List<String> path,
            final TestContext context) {
        return node(commands, path).handler().execute(context);
    }

    private static List<String> objectiveIdSuggestions(final NotQuestsCommands commands, final TestContext context) {
        return suggestions(commands, List.of("qa", "edit", "quest", "objectives", "edit", "objectiveId"), context);
    }

    private static List<String> requirementIdSuggestions(final NotQuestsCommands commands, final TestContext context) {
        return suggestions(
                commands,
                List.of("qa", "edit", "quest", "requirements", "edit", AdminEditCommands.REQUIREMENT_ID),
                context);
    }

    private static List<String> objectiveConditionIdSuggestions(
            final NotQuestsCommands commands,
            final TestContext context,
            final AdminEditCommands.ConditionGroup group) {
        return suggestions(
                commands,
                List.of(
                        "qa",
                        "edit",
                        "quest",
                        "objectives",
                        "edit",
                        "objectiveId",
                        "conditions",
                        conditionGroupName(group),
                        "edit",
                        AdminEditCommands.CONDITION_ID),
                context);
    }

    private static List<String> rewardIdSuggestions(
            final NotQuestsCommands commands,
            final TestContext context,
            final AdminEditCommands.RewardTarget target) {
        return suggestions(commands, rewardIdPath(target), context);
    }

    private static List<String> triggerIdSuggestions(final NotQuestsCommands commands, final TestContext context) {
        return suggestions(
                commands,
                List.of("qa", "edit", "quest", "triggers", "remove", AdminEditCommands.TRIGGER_ID),
                context);
    }

    private static List<String> suggestions(
            final NotQuestsCommands commands,
            final List<String> path,
            final TestContext context) {
        return node(commands, path).suggestionOverride().suggest(context, "");
    }

    private static String conditionGroupName(final AdminEditCommands.ConditionGroup group) {
        return switch (group) {
            case UNLOCK -> "unlock";
            case PROGRESS -> "progress";
            case COMPLETE -> "complete";
        };
    }

    private static List<String> rewardIdPath(final AdminEditCommands.RewardTarget target) {
        return switch (target) {
            case QUEST -> List.of("qa", "edit", "quest", "rewards", "edit", AdminEditCommands.REWARD_ID);
            case OBJECTIVE -> List.of(
                    "qa",
                    "edit",
                    "quest",
                    "objectives",
                    "edit",
                    "objectiveId",
                    "rewards",
                    "edit",
                    AdminEditCommands.REWARD_ID);
        };
    }

    private static NQCommandTree.Node<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            node(final NotQuestsCommands commands, final List<String> path) {
        final NQCommandTree<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                tree = new NQCommandTree<>();
        commands.adminCommands("qa").forEach(tree::register);
        NQCommandTree.Node<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                node = tree.root(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            node = child(node, path.get(i));
        }
        return node;
    }

    private record TestContext(Map<String, String> arguments, PlatformPlayer player) implements NQCommandContext {
        private TestContext(final Map<String, String> arguments) {
            this(arguments, null);
        }

        @Override
        public String argument(final String name) {
            return arguments.getOrDefault(name, "");
        }

        @Override
        public Object rawArgument(final String name) {
            return arguments.get(name);
        }

        @Override
        public boolean flagPresent(final String name) {
            return false;
        }

        @Override
        public String flag(final String name) {
            return "";
        }

        @Override
        public Object rawFlag(final String name) {
            return null;
        }

        @Override
        public PlatformPlayer questPlayer() {
            return player;
        }

        @Override
        public Object platformSender() {
            return null;
        }

        @Override
        public String rawInput() {
            return "";
        }

        @Override
        public String platformVersion() {
            return "test";
        }
    }

    private record LookingPlayer(String worldName, double x, double y, double z) implements TestPlatformPlayer {
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
        public NQLocation lookingAtBlock(final double maxDistance) {
            return new NQLocation() {
                @Override
                public String worldName() {
                    return worldName;
                }

                @Override
                public double x() {
                    return x;
                }

                @Override
                public double y() {
                    return y;
                }

                @Override
                public double z() {
                    return z;
                }

                @Override
                public float yaw() {
                    return 0;
                }

                @Override
                public float pitch() {
                    return 0;
                }
            };
        }
        @Override
        public boolean showGui(final com.notquests.core.gui.GuiService.ResolvedGui gui) {
        return false;
    }
    }
}
