package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.gui.GuiContext;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NotQuestsCommands {
    public static final String TYPE = "type";
    public static final String ARGUMENTS = "arguments";
    public static final String NAME = "name";
    public static final String VARIABLE = "variable";
    public static final String VALUE = "value";
    public static final String CONVERSATION = "conversation";

    private static final NQArgumentType OBJECTIVE_TYPE =
            NQArgumentType.word("objective type", NQArgumentType.SuggestionSource.OBJECTIVE_TYPES);
    private static final NQArgumentType ACTION_TYPE =
            NQArgumentType.word("action type", NQArgumentType.SuggestionSource.ACTION_TYPES);
    private static final NQArgumentType SAVED_ACTION =
            NQArgumentType.word("saved action", NQArgumentType.SuggestionSource.SAVED_ACTIONS);
    private static final NQArgumentType CONDITION_TYPE =
            NQArgumentType.word("condition type", NQArgumentType.SuggestionSource.CONDITION_TYPES);
    private static final NQArgumentType VARIABLE_NAME =
            NQArgumentType.word("variable", NQArgumentType.SuggestionSource.VARIABLE_NAMES);
    private static final NQArgumentType WORD =
            NQArgumentType.word("text");
    private static final NQArgumentType RAW_ARGUMENTS =
            NQArgumentType.greedyString("arguments");
    private static final NQArgumentType BOOLEAN_OR_RAW_VALUE =
            NQArgumentType.greedyString("value", NQArgumentType.SuggestionSource.BOOLEAN_VALUES);

    private final CommandManager commandManager;
    private final Supplier<String> version;
    private final Supplier<String> minecraftVersion;
    private final Supplier<Path> dataFolder;

    NotQuestsCommands(
            final CommandManager commandManager,
            final Supplier<String> version,
            final Supplier<String> minecraftVersion,
            final Supplier<Path> dataFolder) {
        this.commandManager = commandManager;
        this.version = version;
        this.minecraftVersion = minecraftVersion;
        this.dataFolder = dataFolder;
    }

    public CommandManager commandManager() {
        return commandManager;
    }

    public List<String> userRootNames() {
        return NQCommandSchema.USER.names();
    }

    public List<String> adminRootNames() {
        return NQCommandSchema.ADMIN.names();
    }

    public List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            userCommandsWithAliases() {
        return userCommands(
                NQCommandSchema.USER.name(),
                NQCommandSchema.USER.description(),
                commandManager,
                NQCommandSchema.USER.aliases().toArray(String[]::new));
    }

    public List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommandsWithAliases() {
        return adminCommands(
                NQCommandSchema.ADMIN.name(),
                NQCommandSchema.ADMIN.description(),
                commandManager,
                version,
                minecraftVersion,
                dataFolder,
                NQCommandSchema.ADMIN.aliases().toArray(String[]::new));
    }

    public List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            userCommands(final String rootName) {
        return userCommands(rootName, NQCommandSchema.USER.description(), commandManager);
    }

    public List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommands(final String rootName) {
        return adminCommands(
                rootName,
                NQCommandSchema.ADMIN.description(),
                commandManager,
                version,
                minecraftVersion,
                dataFolder);
    }

    public List<CommandMessage> exportGeneratedMetadata() {
        return exportGeneratedMetadata(commandManager, version.get(), minecraftVersion.get(), dataFolder.get());
    }

    public List<String> suggestions(
            final NQArgumentType argument,
            final NQSuggestionProvider<NQCommandContext> override,
            final NQCommandContext context,
            final String input) {
        return NQCommandBuilder.resolveSuggestions(
                argument,
                override,
                context,
                input,
                fallbackArgument -> rawSuggestions(fallbackArgument, context));
    }

    public CommandSuggestions suggestions(
            final NQCommandTree.Node<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    node,
            final NQCommandContext context,
            final String input) {
        if (node == null || node.argument() == null) {
            return new CommandSuggestions(0, List.of(), "");
        }
        final List<String> values = suggestions(node.argument(), node.suggestionOverride(), context, input);
        final CommandSuggestions suggestions = new CommandSuggestions(
                0,
                values.isEmpty() && node.suggestionOverride() != null
                        ? List.of()
                        : NQCommandBuilder.matchingSuggestions(node.argument(), values, input, node.name()),
                CommandHintRenderer.hintLabel(node));
        showHint(context, suggestions.hint());
        return suggestions;
    }

    public CommandSuggestions flagSuggestions(
            final NQCommandTree.Node<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    node,
            final NQCommandContext context,
            final String input) {
        final String raw = input == null ? "" : input;
        final int tokenStart = DoubleDashFlagParser.currentTokenStart(raw);
        final NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>> awaitingValue =
                DoubleDashFlagParser.awaitingValue(raw, node.commandFlags(), NQFlag::name, NQFlag::isPresence);
        if (awaitingValue != null) {
            final NQArgumentType argument = awaitingValue.valueArgument();
            final String currentToken = raw.substring(tokenStart);
            final List<String> values = suggestions(
                    argument,
                    awaitingValue.valueSuggestions(),
                    context,
                    currentToken);
            final CommandSuggestions suggestions = new CommandSuggestions(
                    tokenStart,
                    values.isEmpty() && awaitingValue.valueSuggestions() != null
                            ? List.of()
                            : NQCommandBuilder.matchingSuggestions(
                                    argument,
                                    values,
                                    currentToken,
                                    argument == null ? awaitingValue.name() : argument.valueTypeName()),
                    CommandHintRenderer.hintLabel(awaitingValue.description(), awaitingValue.name()));
            showHint(context, suggestions.hint());
            return suggestions;
        }

        final Set<String> present = NQCommandContext.flags(raw, node.commandFlags()).present();
        final List<String> flags = node.commandFlags().stream()
                .filter(flag -> !present.contains(flag.name()))
                .map(flag -> "--" + flag.name())
                .toList();
        final CommandSuggestions suggestions = new CommandSuggestions(
                tokenStart,
                NQCommandBuilder.prefixMatches(flags, raw.substring(tokenStart)),
                "[Optional command flags]");
        showHint(context, suggestions.hint());
        return suggestions;
    }

    private void showHint(final NQCommandContext context, final String hint) {
        if (context == null || !commandManager.plugin().configuration().commandHintActionbarEnabled()) {
            return;
        }
        CommandHintRenderer.sendActionBarHint(
                context.questPlayer(),
                context.rawInput(),
                hint,
                commandManager.plugin().configuration().commandHintMaxPreviousArguments());
    }

    public boolean execute(
            final NQCommandTree.Node<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    node,
            final NQCommandContext context,
            final boolean permissionGranted,
            final boolean senderMatches,
            final Consumer<CommandMessage> output) {
        final List<CommandMessage> messages;
        if (!permissionGranted) {
            messages = List.of(CommandBranchHelp.noPermission());
        } else if (!senderMatches) {
            messages = List.of(CommandBranchHelp.wrongSender(
                    node.senderType() == null ? "player" : node.senderType().getSimpleName()));
        } else {
            messages = node.handler().execute(context);
        }
        boolean success = true;
        for (final CommandMessage message : messages) {
            success &= message.success();
            output.accept(message);
        }
        return success;
    }

    public record CommandSuggestions(int tokenStart, List<String> values, String hint) {
        public CommandSuggestions {
            values = List.copyOf(values == null ? List.of() : values);
            hint = hint == null ? "" : hint;
        }
    }

    private List<String> rawSuggestions(
            final NQArgumentType argument,
            final NQCommandContext context) {
        if (argument == null) {
            return List.of();
        }
        return switch (argument.suggestions()) {
            case OBJECTIVE_TYPES -> commandManager.objectiveTypeIds();
            case ACTION_TYPES -> commandManager.actionTypeIds();
            case SAVED_ACTIONS -> commandManager.savedActionNames();
            case SAVED_CONDITIONS -> commandManager.savedConditionNames();
            case CONDITION_TYPES -> commandManager.conditionTypeIds();
            case VARIABLE_NAMES -> commandManager.variableIds();
            case QUEST_NAMES -> commandManager.questNames();
            case TAKEABLE_QUEST_NAMES -> commandManager.takeableQuestNames();
            case CATEGORY_NAMES -> commandManager.categoryNames();
            case CONVERSATION_NAMES -> commandManager.conversationNames();
            case ACTIVE_QUEST_NAMES -> commandManager.activeQuestNames(context == null ? null : context.questPlayer());
            case PROFILE_NAMES -> ProfileCommands.profileNames(
                    commandManager.plugin(),
                    context == null ? null : context.questPlayer());
            case ITEM_NAMES -> commandManager.savedItemNames();
            case TAG_TYPES -> Arrays.stream(TagType.values())
                    .map(Enum::name)
                    .toList();
            case TAG_NAMES -> commandManager.tagNames();
            case TRIGGER_OBJECTIVE_NAMES -> commandManager.triggerCommandNames();
            case CONVERSATION_SPEAKER_NAMES -> commandManager.conversationSpeakerNames(
                    context == null ? "" : context.argument(CONVERSATION));
            case BOOLEAN_VALUES -> argument.kind() == NQArgumentType.Kind.BOOLEAN
                    || argument.kind() == NQArgumentType.Kind.BOOLEAN_EXPRESSION
                    || "boolean".equals(commandManager.variableValueType(
                            context == null ? "" : context.argument(VARIABLE)))
                            ? List.of("true", "false")
                            : List.of();
            case NONE -> kindSuggestions(argument);
        };
    }

    private List<String> kindSuggestions(final NQArgumentType argument) {
        return switch (argument.kind()) {
            case PLAYER -> commandManager.onlinePlayerNames();
            case WORLD -> commandManager.worldNames();
            case ITEM_SELECTION -> commandManager.itemSelectionOptions();
            case ACTION_LIST -> commandManager.savedActionNames();
            case ENTITY_TYPE -> commandManager.entityTypeIds();
            case ENCHANTMENT -> commandManager.enchantmentIds();
            case NPC_SELECTOR -> commandManager.npcSelectorOptions(false, true);
            case NPC_SELECTOR_OR_NONE -> commandManager.npcSelectorOptions(true, true);
            case DURATION -> List.of("250ms", "500ms", "1s", "5s", "10s", "30s", "1m", "5m", "1h");
            case INTEGER -> List.of("1", "2", "3", "4", "5", "10", "16", "32", "64");
            case DOUBLE -> List.of("0", "1", "2", "3", "4", "5", "10", "16", "32", "64", "100");
            case NUMBER_EXPRESSION, NUMBER_EXPRESSION_TOKEN -> List.of("1", "2", "3", "5", "10", "25", "50", "100");
            case BOOLEAN, BOOLEAN_EXPRESSION -> List.of("true", "false");
            default -> List.of();
        };
    }

    public static NotQuestsCommands create(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final Supplier<String> version,
            final Supplier<String> minecraftVersion,
            final Supplier<Path> dataFolder) {
        return new NotQuestsCommands(plugin.commandManager(adapter, dataFolder), version, minecraftVersion, dataFolder);
    }

    public static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            userCommands(
                    final String rootName,
                    final NQDescription rootDescription,
                    final CommandManager commandManager,
                    final String... rootAliases) {
        final List<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                root = NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root(rootName, rootDescription, rootAliases)
                        .permission("notquests.use");

        commands.add(root.commandDescription(NQDescription.of("Opens the NotQuests GUI."))
                .handler(context -> playerOnly(context, () -> userHome(commandManager, context)))
                .registration());
        commands.add(root.literal("help", NQDescription.of("Shows command help."))
                .required("query", NQArgumentType.greedyString("help query"), NQDescription.of("Command name, topic, or search text to show help for."))
                .handler(context -> success(commandManager.rootSummary(rootName)))
                .registration());

        commands.addAll(ProfileCommands.userCommands(root, commandManager.plugin()));

        commands.add(root.literal("take", NQDescription.of("Takes or accepts a quest."))
                .commandDescription(NQDescription.of("Starts a quest."))
                .handler(context -> openGuiOrText(
                        commandManager,
                        context,
                        "main-take",
                        "",
                        "",
                        List.of(CommandMessage.error("<red>Please specify the <highlight>name of the quest</highlight> you wish to take.\n"
                                + "<yellow>/nquests <gold>take <dark_aqua>[Quest Name]"))))
                .registration());
        commands.add(root.literal("take", NQDescription.of("Takes or accepts a quest."))
                .required(
                        "questName",
                        NQArgumentType.takeableQuest(),
                        NQDescription.of("Identifier of the quest you want to start."))
                .commandDescription(NQDescription.of("Starts a quest."))
                .handler(context -> {
                    if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
                        return List.of(CommandMessage.error("This command can only be used by a player."));
                    }
                    return PlayerQuestCommands.takeQuest(
                            commandManager.plugin(),
                            context.questPlayer(),
                            context.argument("questName"));
                })
                .registration());

        commands.add(root.literal("questPoints", NQDescription.of("Shows the command sender's quest points."))
                .commandDescription(NQDescription.of("Shows your quest points."))
                .handler(context -> playerOnly(context, () -> List.of(QuestPointCommands.userQuestPoints(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        playerIdentifier(context)))))
                .registration());
        commands.addAll(ConversationCommands.userCommands(root, commandManager.plugin()));
        commands.add(root.literal("activeQuests", NQDescription.of("Shows active quests for a player."))
                .commandDescription(NQDescription.of("Shows your active quests."))
                .handler(context -> openGuiOrText(
                        commandManager,
                        context,
                        "main-active",
                        "",
                        "",
                        QuestProgressCommands.userActiveQuests(commandManager.plugin(), context.questPlayer())))
                .registration());

        commands.add(root.literal("abort", NQDescription.of("Aborts an active quest."))
                .commandDescription(NQDescription.of("Aborts an active quest."))
                .handler(context -> openGuiOrText(
                        commandManager,
                        context,
                        "main-active",
                        "",
                        "",
                        abortNoQuestMessages(commandManager, context)))
                .registration());
        commands.add(root.literal("abort", NQDescription.of("Aborts an active quest."))
                .required(
                        "Active Quest",
                        NQArgumentType.activeQuest(),
                        NQDescription.of("Name of the active quest which should be aborted or inspected."))
                .commandDescription(NQDescription.of("Aborts an active quest."))
                .handler(context -> abortActiveQuest(commandManager, context))
                .registration());

        commands.add(root.literal("preview", NQDescription.of("Previews a quest without accepting it."))
                .commandDescription(NQDescription.of("Previews a quest."))
                .handler(context -> openGuiOrText(
                        commandManager,
                        context,
                        "main-take",
                        "",
                        "",
                        List.of(CommandMessage.error("<red>Please specify the <highlight>name of the quest</highlight> you wish to preview.\n"
                                + "<yellow>/nquests <gold>preview <dark_aqua>[Quest Name]"))))
                .registration());
        commands.add(root.literal("preview", NQDescription.of("Previews a quest without accepting it."))
                .required(
                        "questName",
                        NQArgumentType.takeableQuest(),
                        NQDescription.of("Identifier of the quest preview to open."))
                .commandDescription(NQDescription.of("Previews a quest."))
                .handler(context -> previewQuest(commandManager, context))
                .registration());
        commands.add(root.literal("progress", NQDescription.of("Shows progress for an active quest."))
                .required(
                        "Active Quest",
                        NQArgumentType.activeQuest(),
                        NQDescription.of("Name of the active quest which should be aborted or inspected."))
                .commandDescription(NQDescription.of("Shows progress for an active quest."))
                .handler(context -> activeQuestProgress(commandManager, context))
                .registration());
        if (commandManager.userCommandGuiEnabled()) {
            commands.add(root.literal("category", NQDescription.of("Opens the quest browser for a specific category."))
                    .required(
                            "Category",
                            NQArgumentType.category(),
                            NQDescription.of("Quest category to browse in the quest GUI."))
                    .commandDescription(NQDescription.of("Opens the selected quest category."))
                    .handler(context -> playerOnly(context, () ->
                            openGui(commandManager, context, "category-take", "", context.argument("Category"))))
                    .registration());
        }

        return List.copyOf(commands);
    }

    public static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            portableUserCommands(
                    final String rootName,
                    final NQDescription rootDescription,
                    final CommandManager commandManager,
                    final String... rootAliases) {
        final List<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                root = NQCommandBuilder.root(rootName, rootDescription, rootAliases);

        commands.add(root.handler(context -> success(commandManager.rootSummary(rootName))).registration());
        commands.add(root.literal("version", NQDescription.of("Shows the loaded NotQuests version."))
                .handler(NQCommandHandler.message(context -> commandManager.versionMessage(context.platformVersion())))
                .registration());
        commands.add(root.literal("registry", NQDescription.of("Lists how many objectives, actions, conditions, triggers, and variables are registered."))
                .handler(context -> success(commandManager.registrySummary()))
                .registration());
        commands.add(root.literal("objectives", NQDescription.of("Lists and activates registered NotQuests objective types."))
                .handler(NQCommandHandler.message(context -> commandManager.objectivesMessage()))
                .registration());
        commands.add(root.literal("objectives", NQDescription.of("Lists and activates registered NotQuests objective types."))
                .literal("activate", NQDescription.of("Activates one objective type for the executing player."))
                .required(TYPE, OBJECTIVE_TYPE, NQDescription.of("Objective type to activate."))
                .required(ARGUMENTS, RAW_ARGUMENTS, NQDescription.of("Objective arguments in the same order shown by the objective type."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.activateObjective(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(),
                        context.argument(TYPE),
                        context.argument(ARGUMENTS))))
                .registration());
        commands.add(root.literal("actions", NQDescription.of("Lists, executes, saves, and runs NotQuests action types."))
                .handler(NQCommandHandler.message(context -> commandManager.actionsMessage()))
                .registration());
        commands.add(root.literal("actions", NQDescription.of("Lists, executes, saves, and runs NotQuests action types."))
                .literal("execute", NQDescription.of("Executes one action type immediately for the executing player."))
                .required(TYPE, ACTION_TYPE, NQDescription.of("Action type to execute."))
                .required(ARGUMENTS, RAW_ARGUMENTS, NQDescription.of("Action arguments in the same order shown by the action type."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.executeAction(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(),
                        context.argument(TYPE),
                        context.argument(ARGUMENTS),
                        null)))
                .registration());
        commands.add(root.literal("actions", NQDescription.of("Lists, executes, saves, and runs NotQuests action types."))
                .literal("save", NQDescription.of("Saves one configured action for later reuse."))
                .required(NAME, WORD, NQDescription.of("Name to save this configured action under."))
                .required(TYPE, ACTION_TYPE, NQDescription.of("Action type to configure."))
                .required(ARGUMENTS, RAW_ARGUMENTS, NQDescription.of("Action arguments in the same order shown by the action type."))
                .handler(NQCommandHandler.commandMessage(context -> SavedActionCommands.saveAction(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.argument(NAME), context.argument(TYPE), context.argument(ARGUMENTS))))
                .registration());
        commands.add(root.literal("actions", NQDescription.of("Lists, executes, saves, and runs NotQuests action types."))
                .literal("executeSaved", NQDescription.of("Executes a saved action by name for the executing player."))
                .required(NAME, SAVED_ACTION, NQDescription.of("Saved action name to execute."))
                .handler(NQCommandHandler.commandMessage(context -> SavedActionCommands.executeSavedAction(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(), context.argument(NAME))))
                .registration());
        commands.add(root.literal("conditions", NQDescription.of("Lists and checks registered NotQuests condition types."))
                .handler(NQCommandHandler.message(context -> commandManager.conditionsMessage()))
                .registration());
        commands.add(root.literal("conditions", NQDescription.of("Lists and checks registered NotQuests condition types."))
                .literal("check", NQDescription.of("Checks one condition type immediately for the executing player."))
                .required(TYPE, CONDITION_TYPE, NQDescription.of("Condition type to check."))
                .required(ARGUMENTS, RAW_ARGUMENTS, NQDescription.of("Condition arguments in the same order shown by the condition type."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.checkCondition(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(),
                        context.argument(TYPE),
                        context.argument(ARGUMENTS))))
                .registration());
        commands.addAll(ConversationCommands.rootCommands(root, commandManager.plugin()));
        commands.add(root.literal("variables", NQDescription.of("Lists, checks, and changes registered NotQuests variables."))
                .handler(NQCommandHandler.message(context -> commandManager.variablesMessage()))
                .registration());
        commands.add(root.literal("variables", NQDescription.of("Lists, checks, and changes registered NotQuests variables."))
                .literal("check", NQDescription.of("Shows the current value of one variable for the executing player."))
                .required(VARIABLE, VARIABLE_NAME, NQDescription.of("Variable to check."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.checkVariable(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(),
                        context.argument(VARIABLE),
                        "")))
                .registration());
        commands.add(root.literal("variables", NQDescription.of("Lists, checks, and changes registered NotQuests variables."))
                .literal("check", NQDescription.of("Shows the current value of one variable for the executing player."))
                .required(VARIABLE, VARIABLE_NAME, NQDescription.of("Variable to check."))
                .required(ARGUMENTS, RAW_ARGUMENTS, NQDescription.of("Additional variable arguments, when this variable requires them."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.checkVariable(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.questPlayer(),
                        context.argument(VARIABLE),
                        context.argument(ARGUMENTS))))
                .registration());
        commands.add(root.literal("variables", NQDescription.of("Lists, checks, and changes registered NotQuests variables."))
                .literal("set", NQDescription.of("Changes one writable variable for the executing player."))
                .required(VARIABLE, VARIABLE_NAME, NQDescription.of("Writable variable to change."))
                .required(VALUE, BOOLEAN_OR_RAW_VALUE, NQDescription.of("New value for the selected variable."))
                .handler(NQCommandHandler.commandMessage(context -> RegistryCommands.setVariable(
                        commandManager.plugin(),
                        context.questPlayer(),
                        context.argument(VARIABLE),
                        context.argument(VALUE))))
                .registration());
        commands.add(root.literal("triggers", NQDescription.of("Lists registered NotQuests trigger types."))
                .handler(NQCommandHandler.message(context -> commandManager.triggersMessage()))
                .registration());
        return List.copyOf(commands);
    }

    public static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommands(
                    final String rootName,
                    final NQDescription rootDescription,
                    final CommandManager commandManager,
                    final Supplier<String> version,
                    final Supplier<String> minecraftVersion,
                    final Supplier<Path> dataFolder,
                    final String... rootAliases) {
        final List<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                root = NQCommandBuilder.<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        root(rootName, rootDescription, rootAliases)
                        .permission("notquests.admin");

        commands.add(root.commandDescription(NQDescription.of("Opens the help menu"))
                .handler(context -> success(commandManager.rootSummary(rootName)))
                .registration());
        commands.add(root.literal("help", NQDescription.of("Shows command help."))
                .commandDescription(NQDescription.of("Opens the help menu"))
                .handler(context -> success(commandManager.rootSummary(rootName)))
                .registration());
        commands.add(root.literal("help", NQDescription.of("Shows command help."))
                .required("query", NQArgumentType.greedyString("help query"), NQDescription.of("Command name, topic, or search text to show admin help for."))
                .commandDescription(NQDescription.of("Opens the help menu"))
                .handler(context -> success(commandManager.rootSummary(rootName)))
                .registration());
        commands.add(root.literal("version", NQDescription.of("Shows the installed NotQuests version and server environment."))
                .commandDescription(NQDescription.of("Displays the NotQuests version and platform details."))
                .handler(ignored -> List.of(CommandMessage.success(
                        commandManager.versionMessage(version.get(), minecraftVersion.get()))))
                .registration());
        commands.addAll(DebugCommands.saveCommands(root, commandManager.plugin()));
        commands.addAll(ConversationCommands.adminCommands(
                root,
                commandManager.plugin(),
                commandManager.adapter(),
                dataFolder));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                items = root.literal(
                        "items",
                        NQDescription.of("Manages custom NotQuests items."));
        addItemCommands(
                commands,
                items.literal(
                        "items",
                        NQDescription.of("Manages custom NotQuests items.")),
                commandManager);
        commands.addAll(DebugCommands.debugCommands(
                root,
                commandManager.plugin(),
                commandManager.adapter(),
                commandManager,
                version,
                minecraftVersion,
                dataFolder));
        commands.addAll(CategoryCommands.adminCommands(root, commandManager.plugin(), commandManager.adapter()));

        commands.addAll(SavedConditionCommands.adminCommands(root, commandManager.plugin(), commandManager.adapter()));

        commands.addAll(SavedActionCommands.adminCommands(root, commandManager.plugin(), commandManager.adapter()));

        commands.addAll(TagCommands.adminCommands(root, commandManager.plugin(), commandManager.adapter()));

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                editQuest = root.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific quest."))
                        .required(
                                "quest",
                                NQArgumentType.quest(),
                                NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."));
        commands.addAll(QuestEditCommands.adminCommands(editQuest, commandManager.plugin(), commandManager.adapter()));
        commands.addAll(QuestNpcCommands.adminCommands(editQuest, commandManager.plugin(), commandManager.adapter()));
        commands.addAll(AdminEditCommands.commands(editQuest, commandManager));
        commands.addAll(RegistryCommands.adminCommands(root, commandManager));
        commands.addAll(QuestLifecycleCommands.adminCommandsBeforeProgress(root, commandManager.plugin(), commandManager.adapter()));
        commands.add(QuestProgressCommands.adminCommand(root, commandManager.plugin(), commandManager.adapter()));
        commands.addAll(QuestLifecycleCommands.adminCommandsAfterProgress(root, commandManager.plugin(), commandManager.adapter()));
        commands.addAll(QuestPointCommands.adminCommands(root, commandManager.plugin(), commandManager.adapter()));
        commands.addAll(DebugCommands.reloadCommands(root, commandManager.plugin()));

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                list = root.literal("list", NQDescription.of("Lists NotQuests data, type registries, and useful placeholders."));
        commands.add(list.literal("ObjectiveTypes", NQDescription.of("Lists every available objective type."))
                .commandDescription(NQDescription.of("Shows all available objective types."))
                .handler(NQCommandHandler.message(ignored -> commandManager.objectivesMessage()))
                .registration());
        commands.add(list.literal("RequirementTypes", NQDescription.of("Lists every available requirement and condition type."))
                .commandDescription(NQDescription.of("Shows all available requirement and condition types."))
                .handler(NQCommandHandler.message(ignored -> commandManager.conditionsMessage()))
                .registration());
        commands.add(list.literal("ActionTypes", NQDescription.of("Lists every available action and reward type."))
                .commandDescription(NQDescription.of("Shows all available action and reward types."))
                .handler(NQCommandHandler.message(ignored -> commandManager.actionsMessage()))
                .registration());
        commands.add(list.literal("TriggerTypes", NQDescription.of("Lists every available trigger type."))
                .commandDescription(NQDescription.of("Shows all available trigger types."))
                .handler(NQCommandHandler.message(ignored -> commandManager.triggersMessage()))
                .registration());
        commands.add(list.literal("AllQuests", NQDescription.of("Lists every loaded quest across all categories."))
                .commandDescription(NQDescription.of("Shows all created quests."))
                .handler(ignored -> success(commandManager.questsMessages()))
                .registration());
        commands.add(list.literal("Placeholders", NQDescription.of("Lists placeholders that can be used in trigger and action commands."))
                .commandDescription(NQDescription.of("Shows placeholders available in commands."))
                .handler(ignored -> success(commandManager.placeholderMessages()))
                .registration());
        return List.copyOf(commands);
    }

    private static void addItemCommands(
            final List<NQCommandRegistration<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>>
                    commands,
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    items,
            final CommandManager commandManager) {
        commands.addAll(ItemCommands.adminCommands(items, commandManager.plugin(), commandManager.adapter()));
    }

    private static List<CommandMessage> success(final List<String> messages) {
        return messages.stream().map(CommandMessage::success).toList();
    }

    public static List<CommandMessage> exportGeneratedMetadata(
            final CommandManager commandManager,
            final String pluginVersion,
            final String minecraftVersion,
            final Path dataFolder) {
        final List<CommandMessage> messages = new ArrayList<>();
        messages.addAll(exportCommandSchema(commandManager, pluginVersion, dataFolder));
        messages.addAll(exportMetadata(commandManager, pluginVersion, minecraftVersion, dataFolder));
        return List.copyOf(messages);
    }

    private static List<CommandMessage> exportCommandSchema(
            final CommandManager commandManager,
            final String pluginVersion,
            final Path dataFolder) {
        try {
            final Path output = generatedDir(dataFolder).resolve("commands.json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, commandManager.commandIndex(pluginVersion).toJson(), StandardCharsets.UTF_8);
            return List.of(CommandMessage.success(
                    "Exported NotQuests command schema to " + output + "."));
        } catch (final Exception exception) {
            return List.of(CommandMessage.error(
                    "Failed to export NotQuests command schema: " + exception.getMessage()));
        }
    }

    private static List<CommandMessage> exportMetadata(
            final CommandManager commandManager,
            final String pluginVersion,
            final String minecraftVersion,
            final Path dataFolder) {
        try {
            final Path output = generatedDir(dataFolder).resolve("metadata.json");
            Files.createDirectories(output.getParent());
            Files.writeString(
                    output,
                    commandManager.metadataIndex(pluginVersion, minecraftVersion).toJson(),
                    StandardCharsets.UTF_8);
            return List.of(CommandMessage.success(
                    "Exported NotQuests metadata to " + output + "."));
        } catch (final Exception exception) {
            return List.of(CommandMessage.error(
                    "Failed to export NotQuests metadata: " + exception.getMessage()));
        }
    }

    private static Path generatedDir(final Path dataFolder) {
        return dataFolder.resolve("generated");
    }

    private static List<CommandMessage> userCommandMenu() {
        return List.of(
                CommandMessage.success("<blue><bold>NotQuests Player Commands:"),
                userCommandMenuLine("/nquests take ", "<yellow>/nquests <gold>take <dark_aqua>[Quest Name]", "Takes/Starts a Quest"),
                userCommandMenuLine("/nquests abort ", "<yellow>/nquests <gold>abort <dark_aqua>[Quest Name]", "Fails a Quest"),
                userCommandMenuLine("/nquests preview ", "<yellow>/nquests <gold>preview <dark_aqua>[Quest Name]", "Shows more information about a Quest"),
                userCommandMenuLine("/nquests activeQuests", "<yellow>/nquests <gold>activeQuests", "Shows all your active Quests"),
                userCommandMenuLine("/nquests progress ", "<yellow>/nquests <gold>progress <dark_aqua>[Quest Name]", "Shows the progress of an active Quest"),
                userCommandMenuLine("/nquests questPoints", "<yellow>/nquests <gold>questPoints", "Shows how many Quest Points you have"));
    }

    private static CommandMessage userCommandMenuLine(
            final String command,
            final String line,
            final String hover) {
        return CommandMessage.success("<click:suggest_command:'" + command
                + "'><hover:show_text:'<green>" + hover + "'>" + line + "</hover></click>");
    }

    private static List<CommandMessage> userHome(
            final CommandManager commandManager,
            final NQCommandContext context) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return playerOnlyMessage();
        }
        if (!commandManager.userCommandGuiEnabled()) {
            return userCommandMenu();
        }
        if (commandManager.openGui(context.questPlayer(), "main-base", "", GuiContext.EMPTY)) {
            return List.of();
        }
        return userCommandMenu();
    }

    private static List<CommandMessage> openGui(
            final CommandManager commandManager,
            final NQCommandContext context,
        final String guiName,
        final String questName,
        final String categoryName) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return playerOnlyMessage();
        }
        if (commandManager.openGui(
                context.questPlayer(),
                guiName,
                "",
                GuiContext.of(questName, categoryName))) {
            return List.of();
        }
        return List.of(CommandMessage.error("Could not open the NotQuests GUI."));
    }

    private static List<CommandMessage> openGuiOrText(
            final CommandManager commandManager,
            final NQCommandContext context,
            final String guiName,
            final String questName,
            final String categoryName,
            final List<CommandMessage> textFallback) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return playerOnlyMessage();
        }
        if (!commandManager.userCommandGuiEnabled()) {
            return textFallback;
        }
        return openGui(commandManager, context, guiName, questName, categoryName);
    }

    private static List<CommandMessage> abortNoQuestMessages(
            final CommandManager commandManager,
            final NQCommandContext context) {
        if (commandManager.activeQuestNames(context.questPlayer()).isEmpty()) {
            return List.of(CommandMessage.error("<error>Seems like you don't have any active quests!"));
        }
        return List.of(CommandMessage.error("<red>Please specify the <highlight>name of the quest</highlight> you wish to abort (fail).\n"
                + "<yellow>/nquests <gold>abort <dark_aqua>[Quest Name]"));
    }

    private static List<CommandMessage> abortActiveQuest(
            final CommandManager commandManager,
            final NQCommandContext context) {
        return playerOnly(context, () -> {
            final String activeQuest = activeQuestName(commandManager, context, context.argument("Active Quest"));
            if (activeQuest == null) {
                return notActiveQuestMessages(commandManager, context);
            }
            return commandManager.userCommandGuiEnabled()
                    ? openGui(commandManager, context, "active-quest-abort-confirm", activeQuest, "")
                    : List.of(PlayerQuestCommands.abortQuest(commandManager.plugin(), context.questPlayer(), activeQuest));
        });
    }

    private static List<CommandMessage> activeQuestProgress(
            final CommandManager commandManager,
            final NQCommandContext context) {
        return playerOnly(context, () -> {
            final String activeQuest = activeQuestName(commandManager, context, context.argument("Active Quest"));
            if (activeQuest == null) {
                return notActiveQuestMessages(commandManager, context);
            }
            return QuestProgressCommands.userQuestProgress(commandManager.plugin(), context.questPlayer(), activeQuest);
        });
    }

    private static List<CommandMessage> notActiveQuestMessages(
            final CommandManager commandManager,
            final NQCommandContext context) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.error("<error>Quest was not found or active!"));
        messages.addAll(QuestProgressCommands.userActiveQuests(commandManager.plugin(), context.questPlayer()));
        return List.copyOf(messages);
    }

    private static String activeQuestName(
            final CommandManager commandManager,
            final NQCommandContext context,
            final String requestedQuest) {
        if (requestedQuest == null || requestedQuest.isBlank()) {
            return null;
        }
        return commandManager.activeQuestNames(context.questPlayer()).stream()
                .filter(activeQuest -> activeQuest.equalsIgnoreCase(requestedQuest))
                .findFirst()
                .orElse(null);
    }

    private static List<CommandMessage> previewQuest(
            final CommandManager commandManager,
            final NQCommandContext context) {
        final String questName = context.argument("questName");
        final CommandMessage disabled = PlayerQuestCommands.takeDisabledMessage(commandManager.plugin(), questName);
        if (disabled != null) {
            return List.of(disabled);
        }
        if (!commandManager.userCommandGuiEnabled()) {
            return PlayerQuestCommands.questPreview(commandManager.plugin(), context.questPlayer(), questName);
        }
        return openGui(commandManager, context, "quest-preview", questName, "");
    }

    private static List<CommandMessage> playerOnly(
            final NQCommandContext context,
            final Supplier<List<CommandMessage>> messages) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return playerOnlyMessage();
        }
        return messages.get();
    }

    private static List<CommandMessage> playerOnlyMessage() {
        return List.of(CommandMessage.error("<error>This command can only be used by a Player."));
    }

    private static List<String> withPlaceholder(final List<String> suggestions, final String placeholder) {
        final ArrayList<String> values = new ArrayList<>(suggestions == null ? List.of() : suggestions);
        if (placeholder != null && !placeholder.isBlank()) {
            values.add(placeholder);
        }
        return List.copyOf(values);
    }

    private static boolean bool(final String value) {
        return Boolean.parseBoolean(value);
    }

    private static String playerIdentifier(final NQCommandContext context) {
        return context.questPlayer() == null || context.questPlayer().playerIdentifier().isBlank()
                ? "unknown"
                : context.questPlayer().playerIdentifier();
    }

    private static String playerNameOrFlag(final NQCommandContext context, final String flagName) {
        final String value = context.flag(flagName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return context.questPlayer() == null ? "" : context.questPlayer().playerName();
    }

    private static Duration duration(final Object value) {
        if (value instanceof final Duration duration) {
            return duration;
        }
        if (!(value instanceof final String raw) || raw.isBlank()) {
            return null;
        }
        final String input = raw.trim().toLowerCase(Locale.ROOT);
        try {
            if (input.endsWith("ms")) {
                return Duration.ofMillis(Long.parseLong(input.substring(0, input.length() - 2)));
            }
            if (input.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            if (input.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(input.substring(0, input.length() - 1)));
            }
            return Duration.ofSeconds(Long.parseLong(input));
        } catch (final NumberFormatException exception) {
            return null;
        }
    }
}
