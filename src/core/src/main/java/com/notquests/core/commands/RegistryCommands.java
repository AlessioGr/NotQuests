package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.managers.CommandManager;
import com.notquests.core.managers.UtilManager;
import com.notquests.core.objectives.Objective;
import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry.Variables.BooleanVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ItemStackListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.ListVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.NumberVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables.StringVariableHandler;
import com.notquests.core.registry.NotQuestsRegistry.Variables;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.variables.VariableDataType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class RegistryCommands {
    private RegistryCommands() {}

    @FunctionalInterface
    private interface ActionCommand {
        CommandMessage run(
                NQCommandContext context,
                String actionTypeId,
                String rawArguments);
    }

    @FunctionalInterface
    private interface ConditionCommand {
        CommandMessage run(
                NQCommandContext context,
                String conditionTypeId,
                String rawArguments);
    }

    public static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommands(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final CommandManager commandManager) {
        final List<NQCommandRegistration<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>>
                commands = new ArrayList<>();

        addObjectiveCommands(commands, root, commandManager);
        addActionCommands(commands, root, commandManager);
        addConditionCommands(commands, root, commandManager);
        addTriggerCommands(commands, root, commandManager);
        addVariableCommands(commands, root, commandManager);
        return List.copyOf(commands);
    }

    static CommandMessage activateObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String objectiveTypeId,
            final String rawArguments) {
        if (questPlayer == null || !questPlayer.hasPlayer() || questPlayer.playerIdentifier().isBlank()) {
            return CommandMessage.error("<error>Execute in game or specify player.");
        }
        final Objectives.Type type = objectiveType(plugin, objectiveTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests objective type: " + highlight(objectiveTypeId) + ".");
        }
        try {
            final Objective data = Objectives.parse(
                    adapter, type, rawArguments, questPlayer);
            final var progress = plugin.activateManualObjective(questPlayer, type, data);
            if (progress == null) {
                return CommandMessage.error("Cannot activate NotQuests objective " + highlight(type.id()) + ".");
            }
            plugin.saveData();
            return CommandMessage.success("<success>Activated NotQuests objective "
                    + highlight(type.id())
                    + " with required progress "
                    + highlight2(formatProgress(progress.progressNeeded()))
                    + ".");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot activate NotQuests objective "
                    + highlight(type.id())
                    + ": "
                    + exception.getMessage());
        }
    }

    static CommandMessage executeAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String actionTypeId,
            final String rawArguments,
            final Duration delay) {
        final Actions.Type actionType = actionType(plugin, actionTypeId);
        if (actionType == null) {
            return CommandMessage.error("Unknown NotQuests action type: " + highlight(actionTypeId) + ".");
        }
        if (actionType.executor() == null) {
            return CommandMessage.error("NotQuests action type cannot be executed from one command: "
                    + highlight(actionTypeId) + ".");
        }
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.none();
        }
        try {
            final Action action = Actions.parse(
                    adapter, actionType, rawArguments, questPlayer);
            final Runnable run = () -> actionType.executor().execute(action, questPlayer);
            if (delay == null || delay.isNegative() || delay.isZero()) {
                run.run();
            } else {
                adapter.schedule(delay, run);
            }
            return CommandMessage.none();
        } catch (final RuntimeException exception) {
            return CommandMessage.error("NotQuests action failed: " + exception.getMessage());
        }
    }

    static CommandMessage checkCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String conditionTypeId,
            final String rawArguments) {
        final Conditions.Type conditionType = conditionType(plugin, conditionTypeId);
        if (conditionType == null) {
            return CommandMessage.error("Unknown NotQuests condition type: " + highlight(conditionTypeId) + ".");
        }
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.error("This condition needs an online player.");
        }
        final PlatformPlayer target = questPlayer;
        try {
            final Condition data = Conditions.parse(
                    adapter, conditionType, rawArguments, target);
            applySharedConditionFlags(data, rawArguments);
            final CommandMessage validation = validateConditionData(conditionType, data, null);
            if (validation != null) {
                return validation;
            }
            final String result = ConditionCheck.check(conditionType, data, target);
            final boolean fulfilled = result == null || result.isBlank();
            return CommandMessage.success("<main>" + conditionType.id()
                    + " condition result for player "
                    + playerDisplayName(target)
                    + ":</main> <highlight>"
                    + (fulfilled ? "" : result)
                    + (fulfilled ? "<positive>fulfilled" : " <negative>(not fulfilled)"));
        } catch (final RuntimeException exception) {
            return CommandMessage.error("NotQuests condition check failed: " + exception.getMessage());
        }
    }

    static CommandMessage checkVariable(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String variableName,
            final String rawArguments) {
        final Variables.Type variable = variable(plugin, variableName);
        if (variable == null) {
            return CommandMessage.error("Unknown NotQuests variable: " + highlight(variableName) + ".");
        }
        final Object[] arguments;
        try {
            arguments = Variables.parse(adapter, variable, rawArguments, questPlayer);
        } catch (final RuntimeException exception) {
            return CommandMessage.error(
                    "Cannot check NotQuests variable " + highlight(variable.id()) + ": " + exception.getMessage());
        }
        final Object value = switch (variable.valueType()) {
            case "boolean" -> ((Variables.BooleanVariableHandler) variable.handler()).getValue(questPlayer, arguments);
            case "number" -> ((Variables.NumberVariableHandler) variable.handler()).getValue(questPlayer, arguments);
            case "string" -> ((Variables.StringVariableHandler) variable.handler()).getValue(questPlayer, arguments);
            case "list" -> ((Variables.ListVariableHandler) variable.handler()).getValue(questPlayer, arguments);
            case "itemStackList" -> ((Variables.ItemStackListVariableHandler) variable.handler()).getValue(questPlayer, arguments);
            default -> "";
        };
        return CommandMessage.success("<main>" + variable.id() + " variable (" + variable.valueType()
                + ") result for player " + playerDisplayName(questPlayer)
                + ":</main> " + highlight(formatVariableValue(value)));
    }

    static CommandMessage setVariable(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String variableName,
            final String rawValue) {
        final Variables.Type variable = variable(plugin, variableName);
        if (variable == null) {
            return CommandMessage.error("Unknown NotQuests variable: " + highlight(variableName) + ".");
        }
        try {
            final boolean changed = switch (variable.valueType()) {
                case "boolean" -> setBoolean(variable, questPlayer, rawValue);
                case "number" -> setNumber(variable, questPlayer, rawValue);
                case "string" -> setString(variable, questPlayer, rawValue);
                default -> false;
            };
            if (!changed) {
                return CommandMessage.error("NotQuests variable " + highlight(variable.id()) + " cannot be changed.");
            }
            plugin.saveData();
            return CommandMessage.success("<success>" + variable.id() + " set to " + highlight(rawValue));
        } catch (final RuntimeException exception) {
            return CommandMessage.error(
                    "Cannot set NotQuests variable " + highlight(variable.id()) + ": " + exception.getMessage());
        }
    }

    private static void addObjectiveCommands(
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
                    root,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                editQuest = root.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific quest."), "e")
                        .required(
                                "quest",
                                NQArgumentType.quest(),
                                NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."));
        addObjectiveLevel(commands, editQuest, commandManager, 0);

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                level1 = editQuest.literal(
                                "objectives",
                                NQDescription.of("Manages objectives on the selected quest."), "o")
                        .literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific objective on the selected quest."))
                        .required(
                                "objectiveId",
                                NQArgumentType.integer("objective id"),
                                NQDescription.of("Objective ID shown by this quest's objectives list."))
                        .literal(
                                "objectives",
                                NQDescription.of("Manages child objectives inside the selected objective."), "o");
        addObjectiveLevel(commands, level1, commandManager, 1);

        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                level2 = level1.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a child objective inside the selected objective."))
                        .required(
                                "objectiveId2",
                                NQArgumentType.integer("child objective id"),
                                NQDescription.of("Child objective ID shown inside the selected parent objective."))
                        .literal(
                                "objectives",
                                NQDescription.of("Manages child objectives inside the selected nested objective."), "o");
        addObjectiveLevel(commands, level2, commandManager, 2);
    }

    private static void addObjectiveLevel(
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
                    objectivesBase,
            final CommandManager commandManager,
            final int level) {
        final NQDescription addDescription = switch (level) {
            case 0 -> NQDescription.of("Adds a new objective to the selected quest.");
            case 1 -> NQDescription.of("Adds a child objective to the selected objective.");
            default -> NQDescription.of("Adds a child objective to the selected nested objective.");
        };
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                addBase = level == 0
                        ? objectivesBase.literal(
                                "objectives",
                                NQDescription.of("Manages objectives on the selected quest."), "o")
                        : objectivesBase;
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                add = addBase.literal("add", addDescription);
        for (final Objectives.Type objectiveType : commandManager.objectiveTypes()) {
            if ("ReachLocation".equalsIgnoreCase(objectiveType.id())) {
                addReachLocationObjectiveCommands(commands, add, objectiveType, commandManager, level);
                continue;
            }
            if ("ShootArrow".equalsIgnoreCase(objectiveType.id())) {
                addShootArrowObjectiveCommands(commands, add, objectiveType, commandManager, level);
                continue;
            }
            if (objectiveType.variableCommand() != null) {
                addVariableObjectiveCommands(commands, add, objectiveType, commandManager, level);
                continue;
            }
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    typed = fields(add.literal(objectiveType.id(), NQDescription.of(objectiveType.description())), objectiveType.fields())
                            .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                            NQFlags.TASK_DESCRIPTION.name(),
                                            NQFlags.TASK_DESCRIPTION.description())
                                    .withArgument(NQArgumentType.greedyString("task description"))
                                    .build());
            commands.add(flags(typed, objectiveType.flags())
                    .commandDescription(NQDescription.of("Adds a " + objectiveType.displayName() + " objective."))
                    .handler(context -> List.of(QuestObjectiveCommands.addQuestObjective(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            objectiveParentPath(context, level),
                            objectiveType.id(),
                            rawArguments(context, objectiveType.fields(), objectiveType.flags()),
                            context.flag(NQFlags.TASK_DESCRIPTION.name()),
                            context.questPlayer())))
                    .registration());
            if ("Interact".equalsIgnoreCase(objectiveType.id())) {
                addInteractLookingObjectiveCommand(commands, add, objectiveType, commandManager, level);
            }
        }
    }

    private static void addInteractLookingObjectiveCommand(
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
                    add,
            final Objectives.Type objectiveType,
            final CommandManager commandManager,
            final int level) {
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                        .required(
                                "amount",
                                NQArgumentType.numberExpressionToken("number expression"),
                                NQDescription.of("Number of matching interactions required."))
                        .literal("looking", NQDescription.of("Uses the block you are looking at as the interaction location."));
        command = flags(command, objectiveType.flags())
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.TASK_DESCRIPTION.name(),
                                NQFlags.TASK_DESCRIPTION.description())
                        .withArgument(NQArgumentType.greedyString("task description"))
                        .build());
        commands.add(command.commandDescription(NQDescription.of("Adds an Interact objective using the block you are looking at."))
                .handler(context -> {
                    if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
                        return List.of(CommandMessage.error(
                                "<error>This shortcut can only be used by a player. Use the coordinate form from console."));
                    }
                    final NQLocation target =
                            context.questPlayer().lookingAtBlock(120);
                    if (target == null) {
                        return List.of(CommandMessage.error("<error>No block found in your line of sight."));
                    }
                    return List.of(QuestObjectiveCommands.addQuestObjective(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            objectiveParentPath(context, level),
                            objectiveType.id(),
                            interactLookingRawArguments(context, objectiveType, target),
                            context.flag(NQFlags.TASK_DESCRIPTION.name()),
                            context.questPlayer()));
                })
                .registration());
    }

    private static void addReachLocationObjectiveCommands(
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
                    add,
            final Objectives.Type objectiveType,
            final CommandManager commandManager,
            final int level) {
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                centerRadius = add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                        .required(
                                "world",
                                NQArgumentType.world(),
                                NQDescription.of("World containing the center of the reach-location region."))
                        .required(
                                "x",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center X coordinate of the reach-location region."))
                        .required(
                                "y",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center Y coordinate of the reach-location region."))
                        .required(
                                "z",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center Z coordinate of the reach-location region."))
                        .required(
                                "radius",
                                NQArgumentType.number("number"),
                                NQDescription.of("Radius in blocks around the center that counts as reaching this location."))
                        .required(
                                "locationName",
                                NQArgumentType.greedyString("location name"),
                                NQDescription.of("Name shown to players for this location in objective task text."));
        centerRadius = withTaskDescriptionFlag(centerRadius);
        commands.add(centerRadius.commandDescription(NQDescription.of("Adds a Reach Location objective."))
                .handler(context -> List.of(QuestObjectiveCommands.addQuestObjective(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.argument("quest"),
                        objectiveParentPath(context, level),
                        objectiveType.id(),
                        reachLocationCenterRadiusRawArguments(context),
                        context.flag(NQFlags.TASK_DESCRIPTION.name()),
                        context.questPlayer())))
                .registration());

        if (!commandManager.supportsWorldEditSelection()) {
            return;
        }
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                worldEdit = add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                        .literal(
                                "worldeditselection",
                                NQDescription.of("Uses your current WorldEdit selection as the target region."))
                        .required(
                                "locationName",
                                NQArgumentType.greedyString("location name"),
                                NQDescription.of("Name shown to players for this location in objective task text."));
        worldEdit = withTaskDescriptionFlag(worldEdit);
        commands.add(worldEdit.commandDescription(NQDescription.of("Adds a Reach Location objective from your WorldEdit selection."))
                .handler(context -> {
                    final CommandMessage error = selectedRegionError(context, commandManager);
                    if (error != null) {
                        return List.of(error);
                    }
                    return List.of(QuestObjectiveCommands.addQuestObjective(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            objectiveParentPath(context, level),
                            objectiveType.id(),
                            reachLocationRegionRawArguments(
                                    commandManager.worldEditSelection(context.questPlayer()),
                                    context.argument("locationName")),
                            context.flag(NQFlags.TASK_DESCRIPTION.name()),
                            context.questPlayer()));
                })
                .registration());
    }

    private static void addShootArrowObjectiveCommands(
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
                    add,
            final Objectives.Type objectiveType,
            final CommandManager commandManager,
            final int level) {
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                centerRadius = add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                        .required(
                                "amount",
                                NQArgumentType.numberExpressionToken("number expression"),
                                NQDescription.of("Number of arrows the player must land inside the target region."))
                        .required(
                                "world",
                                NQArgumentType.world(),
                                NQDescription.of("World containing the center of the target arrow region."))
                        .required(
                                "x",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center X coordinate of the target arrow region."))
                        .required(
                                "y",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center Y coordinate of the target arrow region."))
                        .required(
                                "z",
                                NQArgumentType.number("number"),
                                NQDescription.of("Center Z coordinate of the target arrow region."))
                        .required(
                                "radius",
                                NQArgumentType.number("number"),
                                NQDescription.of("Radius in blocks around the target center where arrows count."));
        centerRadius = withTaskDescriptionFlag(centerRadius);
        commands.add(centerRadius.commandDescription(NQDescription.of("Adds a Shoot Arrow objective."))
                .handler(context -> List.of(QuestObjectiveCommands.addQuestObjective(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.argument("quest"),
                        objectiveParentPath(context, level),
                        objectiveType.id(),
                        shootArrowCenterRadiusRawArguments(context),
                        context.flag(NQFlags.TASK_DESCRIPTION.name()),
                        context.questPlayer())))
                .registration());

        if (!commandManager.supportsWorldEditSelection()) {
            return;
        }
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                worldEdit = add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                        .required(
                                "amount",
                                NQArgumentType.numberExpressionToken("number expression"),
                                NQDescription.of("Number of arrows the player must land inside the target region."))
                        .literal(
                                "worldeditselection",
                                NQDescription.of("Uses your current WorldEdit selection as the arrow target region."));
        worldEdit = withTaskDescriptionFlag(worldEdit);
        commands.add(worldEdit.commandDescription(NQDescription.of("Adds a Shoot Arrow objective from your WorldEdit selection."))
                .handler(context -> {
                    final CommandMessage error = selectedRegionError(context, commandManager);
                    if (error != null) {
                        return List.of(error);
                    }
                    return List.of(QuestObjectiveCommands.addQuestObjective(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            objectiveParentPath(context, level),
                            objectiveType.id(),
                            shootArrowRegionRawArguments(
                                    context.argument("amount"),
                                    commandManager.worldEditSelection(context.questPlayer())),
                            context.flag(NQFlags.TASK_DESCRIPTION.name()),
                            context.questPlayer()));
                })
                .registration());
    }

    private static void addVariableObjectiveCommands(
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
                    add,
            final Objectives.Type objectiveType,
            final CommandManager commandManager,
            final int level) {
        final Variables.Command variableCommand = objectiveType.variableCommand();
        for (final Variables.Type variable : variableTypes(commandManager, variableCommand, true)) {
            final List<NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>>
                    starts = List.of(
                            add.literal(variable.id(), NQDescription.of(variable.description())),
                            add.literal(objectiveType.id(), NQDescription.of(objectiveType.description()))
                                    .literal(variable.id(), NQDescription.of(variable.description())));
            for (final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    start : starts) {
                NQCommandBuilder<
                                NQArgumentType,
                                NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                NQSuggestionProvider<NQCommandContext>,
                                NQCommandHandler>
                        command = fields(start, positionalFields(variable.fields()));
                command = command.required(
                        variableCommand.operatorField(),
                        NQArgumentType.word("comparison operator"),
                        NQDescription.of(variableCommand.operatorDescription()),
                        (context, input) -> variableCommand.operators());
                command = command.required(
                        variableCommand.expressionField(),
                        argument(variableCommand.expressionType(), true),
                        NQDescription.of(variableCommand.expressionDescription()),
                        suggestions(variableCommand.expressionType()));
                command = flags(flags(command, variableFlags(variable.fields())), objectiveType.flags())
                        .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                        NQFlags.TASK_DESCRIPTION.name(),
                                        NQFlags.TASK_DESCRIPTION.description())
                                .withArgument(NQArgumentType.greedyString("task description"))
                                .build());
                commands.add(command.commandDescription(NQDescription.of("Adds a " + objectiveType.displayName() + " objective."))
                        .handler(context -> List.of(QuestObjectiveCommands.addQuestObjective(
                                commandManager.plugin(),
                                commandManager.adapter(),
                                context.argument("quest"),
                                objectiveParentPath(context, level),
                                objectiveType.id(),
                                rawVariableCommandArguments(
                                        context,
                                        variable,
                                        variableCommand,
                                        variableFlags(variable.fields()),
                                        objectiveType.flags()),
                                context.flag(NQFlags.TASK_DESCRIPTION.name()),
                                context.questPlayer())))
                        .registration());
            }
        }
    }

    private static void addActionCommands(
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
                    root,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                actions = root.literal("actions", NQDescription.of("Manages saved actions, inline actions, and action execution."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                saveBase = actions.literal("add", NQDescription.of("Creates a new saved action."))
                        .required("actionName", NQArgumentType.word("action name"), NQDescription.of("Name used to save this action."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                executeBase = actions.literal("execute", NQDescription.of("Executes the selected action or command."));

        for (final Actions.Type actionType : commandManager.actionTypes()) {
            addActionCommand(
                    commands,
                    saveBase,
                    actionType,
                    commandManager,
                    (context, type, raw) -> SavedActionCommands.saveAction(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("actionName"),
                            type,
                            raw,
                            context.flag(NQFlags.CATEGORY.name()),
                            duration(context.rawFlag(NQFlags.DELAY.name())),
                            context.questPlayer()),
                    true,
                    false);
            addActionCommand(
                    commands,
                    executeBase,
                    actionType,
                    commandManager,
                    (context, type, raw) -> RegistryCommands.executeAction(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            CommandSupport.targetPlatformPlayer(commandManager.adapter(),
                                    context.flag(NQFlags.ACTION_TARGET_PLAYER.name()),
                                    context.questPlayer()),
                            type,
                            raw,
                            duration(context.rawFlag(NQFlags.DELAY.name()))),
                    false,
                    true);
            addRewardActionCommands(commands, root, actionType, commandManager);
        }
    }

    private static void addActionCommand(
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
                    base,
            final Actions.Type actionType,
            final CommandManager commandManager,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        if (actionType.variableCommand() != null) {
            addVariableActionCommands(commands, base, actionType, commandManager, handler, savedAction, inlineAction);
            return;
        }
        if (addSpecialActionCommand(commands, base, actionType, handler, savedAction, inlineAction)) {
            return;
        }
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = actionType.typeLiteral()
                        ? base.literal(actionType.id(), NQDescription.of(actionType.description()))
                        : base;
        command = flags(fields(command, actionType.fields()), actionType.flags());
        command = actionGlobalFlags(command, savedAction, inlineAction, true);
        commands.add(command.commandDescription(savedAction
                        ? createActionDescription(actionType.displayName())
                        : executeInlineActionDescription(actionType.displayName()))
                .handler(context -> List.of(handler.run(
                        context,
                        actionType.id(),
                        rawArguments(context, actionType.fields(), actionType.flags()))))
                .registration());
    }

    private static boolean addSpecialActionCommand(
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
                    base,
            final Actions.Type actionType,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                root = actionType.typeLiteral()
                        ? base.literal(actionType.id(), NQDescription.of(actionType.description()))
                        : base;
        return switch (actionType.id()) {
            case "ShowTitle" -> {
                addShowTitleActionCommands(commands, root, actionType, handler, savedAction, inlineAction);
                yield true;
            }
            case "Beam" -> {
                addBeamActionCommands(commands, root, actionType, handler, savedAction, inlineAction);
                yield true;
            }
            case "SpawnMob" -> {
                addSpawnMobActionCommands(commands, root, actionType, handler, savedAction, inlineAction);
                yield true;
            }
            case "SpawnParticle" -> {
                addSpawnParticleActionCommands(commands, root, actionType, handler, savedAction, inlineAction);
                yield true;
            }
            default -> false;
        };
    }

    private static void addShowTitleActionCommands(
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
                    root,
            final Actions.Type actionType,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        registerActionCommand(
                commands,
                actionGlobalFlags(root.required(
                                "title",
                                NQArgumentType.greedyString("title"),
                                NQDescription.of("Title text shown in the center of the target player's screen. Use `|` to add a subtitle.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> context.argument("title"),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
        registerActionCommand(
                commands,
                actionGlobalFlags(root.literal(
                                "timed",
                                NQDescription.of("Creates a title action with custom fade-in, stay, and fade-out durations."))
                        .required(
                                "fadeIn",
                                NQArgumentType.duration(),
                                NQDescription.of("How long the title should fade in. Examples: 250ms, 1s."))
                        .required(
                                "stay",
                                NQArgumentType.duration(),
                                NQDescription.of("How long the title should stay fully visible. Examples: 3s, 1500ms."))
                        .required(
                                "fadeOut",
                                NQArgumentType.duration(),
                                NQDescription.of("How long the title should fade out. Examples: 500ms, 1s."))
                        .required(
                                "title",
                                NQArgumentType.greedyString("title"),
                                NQDescription.of("Title text shown in the center of the target player's screen. Use `|` to add a subtitle.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> String.join(
                        " ",
                        "timed",
                        context.argument("fadeIn"),
                        context.argument("stay"),
                        context.argument("fadeOut"),
                        context.argument("title")),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
    }

    private static void addBeamActionCommands(
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
                    root,
            final Actions.Type actionType,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                namedBeam = root.required(
                        "beamName",
                        NQArgumentType.word("beam name"),
                        NQDescription.of("Beam identifier to show, replace, or remove for the target player."));
        registerActionCommand(
                commands,
                actionGlobalFlags(namedBeam.literal(
                                "remove",
                                NQDescription.of("Removes the named beam from the target player's screen.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> context.argument("beamName") + " remove",
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
        registerActionCommand(
                commands,
                actionGlobalFlags(namedBeam.literal(
                                "spawn",
                                NQDescription.of("Shows the named beam at a fixed world location."))
                        .required("world", NQArgumentType.world(), NQDescription.of("World where the beam should be shown."))
                        .required("x", NQArgumentType.integer("x"), NQDescription.of("X coordinate where the beam should be shown."))
                        .required("y", NQArgumentType.integer("y"), NQDescription.of("Y coordinate where the beam should be shown."))
                        .required("z", NQArgumentType.integer("z"), NQDescription.of("Z coordinate where the beam should be shown.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> String.join(
                        " ",
                        context.argument("beamName"),
                        "spawn",
                        context.argument("world"),
                        context.argument("x"),
                        context.argument("y"),
                        context.argument("z")),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
    }

    private static void addSpawnMobActionCommands(
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
                    root,
            final Actions.Type actionType,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        final List<RegistryField.Definition> radiusFlags =
                namedFields(actionType.flags(), "spawnRadiusX", "spawnRadiusY", "spawnRadiusZ");
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                common = flags(root
                        .required(
                                "entityType",
                                NQArgumentType.entityType(),
                                NQDescription.of("Entity type or custom mob id to spawn."))
                        .required(
                                "amount",
                                NQArgumentType.integer("amount"),
                                NQDescription.of("Number of mobs to spawn.")), radiusFlags);
        registerActionCommand(
                commands,
                actionGlobalFlags(common.literal(
                                "PlayerLocation",
                                NQDescription.of("Spawns mobs at the target player's current location.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> joinRaw(
                        context.argument("entityType"),
                        context.argument("amount"),
                        "PlayerLocation",
                        rawArguments(context, List.of(), radiusFlags)),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
        registerActionCommand(
                commands,
                actionGlobalFlags(common.literal(
                                "Location",
                                NQDescription.of("Spawns mobs at a fixed world location."))
                        .required("world", NQArgumentType.world(), NQDescription.of("World where the mob should be spawned."))
                        .required("x", NQArgumentType.integer("x"), NQDescription.of("X coordinate where the mob should be spawned."))
                        .required("y", NQArgumentType.integer("y"), NQDescription.of("Y coordinate where the mob should be spawned."))
                        .required("z", NQArgumentType.integer("z"), NQDescription.of("Z coordinate where the mob should be spawned.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> joinRaw(
                        context.argument("entityType"),
                        context.argument("amount"),
                        "Location",
                        context.argument("world"),
                        context.argument("x"),
                        context.argument("y"),
                        context.argument("z"),
                        rawArguments(context, List.of(), radiusFlags)),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
    }

    private static void addSpawnParticleActionCommands(
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
                    root,
            final Actions.Type actionType,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        final List<RegistryField.Definition> particleFlags =
                namedFields(actionType.flags(), "offsetX", "offsetY", "offsetZ", "speed", "forEveryone");
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                common = flags(root
                        .required(
                                "particle",
                                NQArgumentType.word("particle"),
                                NQDescription.of("Particle effect to spawn. Only particles that do not require extra data are supported."),
                                suggestions(field(actionType.fields(), "particle")))
                        .required(
                                "count",
                                NQArgumentType.integer("count"),
                                NQDescription.of("Number of particles to spawn.")), particleFlags);
        registerActionCommand(
                commands,
                actionGlobalFlags(common.literal(
                                "PlayerLocation",
                                NQDescription.of("Spawns the particles at the target player's current location.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> joinRaw(
                        context.argument("particle"),
                        context.argument("count"),
                        "PlayerLocation",
                        rawArguments(context, List.of(), particleFlags)),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
        registerActionCommand(
                commands,
                actionGlobalFlags(common.literal(
                                "Location",
                                NQDescription.of("Spawns the particles at a fixed world location."))
                        .required("world", NQArgumentType.world(), NQDescription.of("World where the particles should be spawned."))
                        .required("x", NQArgumentType.number("x"), NQDescription.of("X coordinate where the particles should be spawned."))
                        .required("y", NQArgumentType.number("y"), NQDescription.of("Y coordinate where the particles should be spawned."))
                        .required("z", NQArgumentType.number("z"), NQDescription.of("Z coordinate where the particles should be spawned.")),
                        savedAction,
                        inlineAction,
                        true),
                actionType,
                handler,
                context -> joinRaw(
                        context.argument("particle"),
                        context.argument("count"),
                        "Location",
                        context.argument("world"),
                        context.argument("x"),
                        context.argument("y"),
                        context.argument("z"),
                        rawArguments(context, List.of(), particleFlags)),
                savedAction ? createActionDescription(actionType.displayName()) : executeInlineActionDescription(actionType.displayName()));
    }

    private static void registerActionCommand(
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
                    command,
            final Actions.Type actionType,
            final ActionCommand handler,
            final Function<NQCommandContext, String> rawArguments,
            final NQDescription commandDescription) {
        commands.add(command.commandDescription(commandDescription)
                .handler(context -> List.of(handler.run(context, actionType.id(), rawArguments.apply(context))))
                .registration());
    }

    private static void addRewardActionCommands(
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
                    root,
            final Actions.Type actionType,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                editQuest = root.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific quest."), "e")
                        .required(
                                "quest",
                                NQArgumentType.quest(),
                                NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                questRewards = editQuest.literal("rewards", NQDescription.of("Manages rewards granted by the selected quest."), "rew")
                        .literal("add", NQDescription.of("Adds a reward granted by the selected quest."));
        addRewardActionCommand(
                commands,
                questRewards,
                actionType,
                commandManager,
                (context, type, raw) -> QuestRewardCommands.addQuestReward(
                        commandManager.plugin(),
                        commandManager.adapter(),
                        context.argument("quest"),
                        type,
                        raw,
                        context.questPlayer()));

        for (int level = 0; level <= 2; level++) {
            final int objectiveLevel = level;
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    objectiveRewards = objectiveEditBase(root, objectiveLevel)
                            .literal("rewards", NQDescription.of("Manages rewards granted by the selected objective."), "rew")
                            .literal("add", NQDescription.of("Adds a reward granted when the selected objective completes."));
            addRewardActionCommand(
                    commands,
                    objectiveRewards,
                    actionType,
                    commandManager,
                    (context, type, raw) -> QuestObjectiveCommands.addObjectiveReward(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            objectivePath(context, objectiveLevel),
                            type,
                            raw,
                            context.questPlayer()));
        }
    }

    private static void addRewardActionCommand(
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
                    base,
            final Actions.Type actionType,
            final CommandManager commandManager,
            final ActionCommand handler) {
        if (actionType.variableCommand() != null) {
            addVariableActionCommands(commands, base, actionType, commandManager, handler, false, false);
            return;
        }
        if (addSpecialActionCommand(commands, base, actionType, handler, false, false)) {
            return;
        }
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = actionGlobalFlags(
                        flags(fields(base.literal(actionType.id(), NQDescription.of(actionType.description())), actionType.fields()), actionType.flags()),
                        false,
                        false,
                        true);
        commands.add(command.commandDescription(createActionDescription(actionType.displayName()))
                .handler(context -> List.of(handler.run(
                        context,
                        actionType.id(),
                        rawArguments(context, actionType.fields(), actionType.flags()))))
                .registration());
    }

    private static void addVariableActionCommands(
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
                    base,
            final Actions.Type actionType,
            final CommandManager commandManager,
            final ActionCommand handler,
            final boolean savedAction,
            final boolean inlineAction) {
        final Variables.Command variableCommand = actionType.variableCommand();
        for (final Variables.Type variable : variableTypes(commandManager, variableCommand, true)) {
            NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    command = fields(base.literal(variable.id(), NQDescription.of(variable.description())), positionalFields(variable.fields()));
            command = command.required(
                    variableCommand.operatorField(),
                    NQArgumentType.word("action operator"),
                    NQDescription.of(variableCommand.operatorDescription()),
                    (context, input) -> variableCommand.operators());
            command = command.required(
                    variableCommand.expressionField(),
                    argument(variableCommand.expressionType(), true),
                    NQDescription.of(variableCommand.expressionDescription()),
                    suggestions(variableCommand.expressionType()));
            if (variableCommand.variableType() == VariableDataType.ITEMSTACKLIST) {
                command = command.required(
                        "amount",
                        NQArgumentType.integer("stack amount"),
                        NQDescription.of("Stack amount to apply for each item in this item-list action."));
            }
            command = flags(flags(command, variableFlags(variable.fields())), actionType.flags());
            command = actionGlobalFlags(command, savedAction, inlineAction, true);
            commands.add(command.commandDescription(savedAction
                            ? createActionDescription(actionType.displayName())
                            : executeInlineActionDescription(actionType.displayName()))
                    .handler(context -> List.of(handler.run(
                            context,
                            actionType.id(),
                            rawVariableCommandArguments(
                                    context,
                                    variable,
                                    variableCommand,
                                    variableFlags(variable.fields()),
                                    actionType.flags()))))
                    .registration());
        }
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            actionGlobalFlags(
                    NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            command,
                    final boolean category,
                    final boolean player,
                    final boolean delay) {
        if (category) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.CATEGORY.name(), NQFlags.CATEGORY.description())
                    .withArgument(NQArgumentType.category())
                    .build());
        }
        if (player) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.ACTION_TARGET_PLAYER.name(), NQFlags.ACTION_TARGET_PLAYER.description())
                    .withArgument(NQArgumentType.player())
                    .build());
        }
        if (delay) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.DELAY.name(), NQFlags.DELAY.description())
                    .withArgument(NQArgumentType.duration())
                    .build());
        }
        return command;
    }

    private static void addConditionCommands(
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
                    root,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                saved = root.literal("conditions", NQDescription.of("Manages saved conditions that can be reused by quests, objectives, and actions."));
        for (final Conditions.Type conditionType : commandManager.conditionTypes()) {
            addConditionCommand(commands, saved.literal("add", NQDescription.of("Creates a saved condition."))
                            .required("conditionName", NQArgumentType.word("condition name"), NQDescription.of("Name used to save this condition.")),
                    conditionType,
                    commandManager,
                    Conditions.Target.SAVED_CONDITION,
                    (context, type, raw) -> SavedConditionCommands.saveCondition(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("conditionName"),
                            type,
                            raw,
                            context.flag(NQFlags.CATEGORY.name()),
                            context.questPlayer()));
            addConditionCommand(commands, saved.literal("check", NQDescription.of("Checks a condition without saving it.")),
                    conditionType,
                    commandManager,
                    Conditions.Target.INLINE,
                    (context, type, raw) -> RegistryCommands.checkCondition(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            CommandSupport.targetPlatformPlayer(commandManager.adapter(),
                                    context.flag(NQFlags.PLAYER.name()),
                                    context.questPlayer()),
                            type,
                            raw));
            addConditionCommand(
                    commands,
                    questRequirements(root),
                    conditionType,
                    commandManager,
                    Conditions.Target.QUEST,
                    (context, type, raw) -> QuestRequirementCommands.addQuestRequirement(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("quest"),
                            type,
                            raw,
                            context.questPlayer()));
            for (int level = 0; level <= 2; level++) {
                final int objectiveLevel = level;
                addConditionCommand(
                        commands,
                        objectiveConditions(root, AdminEditCommands.ConditionGroup.UNLOCK, objectiveLevel),
                        conditionType,
                        commandManager,
                        Conditions.Target.OBJECTIVE_UNLOCK,
                        (context, type, raw) -> QuestObjectiveCommands.addObjectiveCondition(
                                commandManager.plugin(),
                                commandManager.adapter(),
                                context.argument("quest"), objectivePath(context, objectiveLevel), "unlock", type, raw,
                                context.questPlayer()));
                addConditionCommand(
                        commands,
                        objectiveConditions(root, AdminEditCommands.ConditionGroup.PROGRESS, objectiveLevel),
                        conditionType,
                        commandManager,
                        Conditions.Target.OBJECTIVE_PROGRESS,
                        (context, type, raw) -> QuestObjectiveCommands.addObjectiveCondition(
                                commandManager.plugin(),
                                commandManager.adapter(),
                                context.argument("quest"), objectivePath(context, objectiveLevel), "progress", type, raw,
                                context.questPlayer()));
                addConditionCommand(
                        commands,
                        objectiveConditions(root, AdminEditCommands.ConditionGroup.COMPLETE, objectiveLevel),
                        conditionType,
                        commandManager,
                        Conditions.Target.OBJECTIVE_COMPLETE,
                        (context, type, raw) -> QuestObjectiveCommands.addObjectiveCondition(
                                commandManager.plugin(),
                                commandManager.adapter(),
                                context.argument("quest"), objectivePath(context, objectiveLevel), "complete", type, raw,
                                context.questPlayer()));
            }
            addConditionCommand(
                    commands,
                    actionConditions(root),
                    conditionType,
                    commandManager,
                    Conditions.Target.ACTION,
                    (context, type, raw) -> SavedActionCommands.addSavedActionCondition(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            context.argument("action"),
                            type,
                            raw,
                            context.questPlayer()));
        }
    }

    private static void addConditionCommand(
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
                    base,
            final Conditions.Type conditionType,
            final CommandManager commandManager,
            final Conditions.Target conditionFor,
            final ConditionCommand handler) {
        if (!conditionType.validTargets().contains(conditionFor)) {
            return;
        }
        if (conditionType.variableCommand() != null) {
            addVariableConditionCommands(commands, base, conditionType, commandManager, conditionFor, handler);
            return;
        }
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = flags(fields(base.literal(conditionType.id(), NQDescription.of(conditionType.description())), conditionType.fields()), conditionType.flags());
        command = conditionGlobalFlags(command, conditionFor);
        commands.add(command.commandDescription(createConditionDescription(conditionType.displayName(), targetName(conditionFor)))
                .handler(context -> List.of(handler.run(
                        context,
                        conditionType.id(),
                        withConditionFlags(
                                context,
                                conditionFor,
                                rawArguments(context, conditionType.fields(), conditionType.flags())))))
                .registration());
    }

    private static void addVariableConditionCommands(
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
                    base,
            final Conditions.Type conditionType,
            final CommandManager commandManager,
            final Conditions.Target conditionFor,
            final ConditionCommand handler) {
        final Variables.Command variableCommand = conditionType.variableCommand();
        for (final Variables.Type variable : variableTypes(commandManager, variableCommand, false)) {
            NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    command = fields(
                            base.literal(variable.id(), NQDescription.of(variable.description())),
                            positionalFields(variable.fields()));
            command = command.required(
                    variableCommand.operatorField(),
                    NQArgumentType.word("comparison operator"),
                    NQDescription.of(variableCommand.operatorDescription()),
                    (context, input) -> variableCommand.operators());
            command = command.required(
                    variableCommand.expressionField(),
                    argument(variableCommand.expressionType(), true),
                    NQDescription.of(variableCommand.expressionDescription()),
                    suggestions(variableCommand.expressionType()));
            if (variableCommand.variableType() == VariableDataType.ITEMSTACKLIST) {
                command = command.required(
                        "amount",
                        NQArgumentType.integer("stack amount"),
                        NQDescription.of("Required stack amount for each item in this item-list condition."));
            }
            command = flags(flags(command, variableFlags(variable.fields())), conditionType.flags());
            command = conditionGlobalFlags(command, conditionFor);
            commands.add(command.commandDescription(createConditionDescription(conditionType.displayName(), targetName(conditionFor)))
                    .handler(context -> List.of(handler.run(
                            context,
                            conditionType.id(),
                            withConditionFlags(
                                    context,
                                    conditionFor,
                                    rawVariableConditionArguments(
                                            context,
                                            variable,
                                            variableCommand,
                                            variableFlags(variable.fields()),
                                            conditionType.flags())))))
                    .registration());
        }
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            conditionGlobalFlags(
                    NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            command,
                    final Conditions.Target conditionFor) {
        command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                        NQFlags.NEGATE.name(), NQFlags.NEGATE.description())
                .build());
        if (conditionFor == Conditions.Target.OBJECTIVE_PROGRESS) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.name(),
                            NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.description())
                    .build());
        }
        if (conditionFor == Conditions.Target.INLINE) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.PLAYER.name(), NQFlags.PLAYER.description())
                    .withArgument(NQArgumentType.player())
                    .build());
        }
        if (conditionFor == Conditions.Target.SAVED_CONDITION) {
            command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                            NQFlags.CATEGORY.name(), NQFlags.CATEGORY.description())
                    .withArgument(NQArgumentType.category())
                    .build());
        }
        return command;
    }

    private static String withConditionFlags(
            final NQCommandContext context,
            final Conditions.Target conditionFor,
            final String rawArguments) {
        final ArrayList<String> parts = new ArrayList<>();
        if (rawArguments != null && !rawArguments.isBlank()) {
            parts.add(rawArguments);
        }
        if (context.flagPresent(NQFlags.NEGATE.name())) {
            parts.add("--" + NQFlags.NEGATE.name());
        }
        if (conditionFor == Conditions.Target.OBJECTIVE_PROGRESS
                && context.flagPresent(NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.name())) {
            parts.add("--" + NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.name());
        }
        return String.join(" ", parts);
    }

    private static List<Variables.Type> variableTypes(
            final CommandManager commandManager,
            final Variables.Command command) {
        return variableTypes(commandManager, command, false);
    }

    private static List<Variables.Type> variableTypes(
            final CommandManager commandManager,
            final Variables.Command command,
            final boolean settableOnly) {
        if (command == null) {
            return List.of();
        }
        return commandManager.variableTypes().stream()
                .filter(variable -> variableType(variable.valueType()) == command.variableType())
                .filter(variable -> !settableOnly || canSet(variable.handler()))
                .toList();
    }

    private static boolean canSet(final Object handler) {
        if (handler instanceof final Variables.BooleanVariableHandler booleanHandler) {
            return booleanHandler.canSet();
        }
        if (handler instanceof final Variables.NumberVariableHandler numberHandler) {
            return numberHandler.canSet();
        }
        if (handler instanceof final Variables.StringVariableHandler stringHandler) {
            return stringHandler.canSet();
        }
        if (handler instanceof final Variables.ListVariableHandler listHandler) {
            return listHandler.canSet();
        }
        return handler instanceof final Variables.ItemStackListVariableHandler itemHandler && itemHandler.canSet();
    }

    private static List<RegistryField.Definition> positionalFields(final List<RegistryField.Definition> fields) {
        return fields.stream().filter(field -> !field.flag() && !field.presenceFlag()).toList();
    }

    private static List<RegistryField.Definition> variableFlags(final List<RegistryField.Definition> fields) {
        return fields.stream().filter(field -> field.flag() || field.presenceFlag()).toList();
    }

    private static VariableDataType variableType(final String valueType) {
        return switch (normalize(valueType)) {
            case "number" -> VariableDataType.NUMBER;
            case "string" -> VariableDataType.STRING;
            case "boolean" -> VariableDataType.BOOLEAN;
            case "list" -> VariableDataType.LIST;
            case "itemstacklist" -> VariableDataType.ITEMSTACKLIST;
            default -> null;
        };
    }

    private static void addTriggerCommands(
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
                    root,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                base = root.literal("edit", NQDescription.of("Opens subcommands for editing a specific quest."), "e")
                        .required("quest", NQArgumentType.quest(), NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."))
                        .literal("triggers", NQDescription.of("Manages triggers attached to this quest."), "t")
                        .literal("add", NQDescription.of("Adds a trigger that runs an action when the selected quest changes state."))
                        .required("action", NQArgumentType.action(), NQDescription.of("Action which will be executed when the trigger runs."));
        for (final Triggers.Type triggerType : commandManager.triggerTypes()) {
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    command = fields(base.literal(triggerType.id(), NQDescription.of(triggerType.description())), triggerType.fields())
                            .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                            NQFlags.APPLY_ON.name(), NQFlags.APPLY_ON.description())
                                    .withArgument(NQArgumentType.integer("quest or objective target"))
                                    .withSuggestions((context, input) -> List.of("0", "1", "2"))
                                    .build())
                            .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                            NQFlags.TRIGGER_WORLD.name(), NQFlags.TRIGGER_WORLD.description())
                                    .withArgument(NQArgumentType.word("world name"))
                                    .withSuggestions((context, input) -> {
                                        final List<String> worlds = new ArrayList<>();
                                        worlds.add("ALL");
                                        worlds.addAll(commandManager.worldNames());
                                        return worlds;
                                    })
                                    .build());
            commands.add(command.commandDescription(NQDescription.of("Adds a " + triggerType.displayName() + " trigger to the selected quest."))
                    .handler(context -> List.of(TriggerCommands.addQuestTrigger(
                            commandManager.plugin(),
                            context.argument("quest"),
                            triggerType.id(),
                            context.argument("action"),
                            context.flag(NQFlags.APPLY_ON.name()),
                            context.flag(NQFlags.TRIGGER_WORLD.name()),
                            rawArguments(context, triggerType.fields(), List.of()))))
                    .registration());
        }
    }

    private static void addVariableCommands(
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
                    root,
            final CommandManager commandManager) {
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                base = root.literal(
                                "variables",
                                NQDescription.of("Evaluates NotQuests variables for a player or the command sender."), "variable")
                        .literal(
                                "check",
                                NQDescription.of("Displays a NotQuests variable's value for a player or the command sender."));
        for (final Variables.Type variable : commandManager.variableTypes()) {
            final List<RegistryField.Definition> variableFields = positionalFields(variable.fields());
            final List<RegistryField.Definition> variableFlags = variableFlags(variable.fields());
            final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    command = flags(fields(base.literal(variable.id(), NQDescription.of(variable.description())), variableFields), variableFlags)
                            .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                            NQFlags.VARIABLE_CHECK_PLAYER.name(),
                                            NQFlags.VARIABLE_CHECK_PLAYER.description())
                                    .withArgument(NQArgumentType.player())
                                    .build());
            commands.add(command.commandDescription(NQDescription.of("Checks the " + variable.displayName() + " variable."))
                    .handler(context -> List.of(RegistryCommands.checkVariable(
                            commandManager.plugin(),
                            commandManager.adapter(),
                            CommandSupport.targetPlatformPlayer(commandManager.adapter(),
                                    context.flag(NQFlags.VARIABLE_CHECK_PLAYER.name()),
                                    context.questPlayer()),
                            variable.id(),
                            rawArguments(context, variableFields, variableFlags))))
                    .registration());
        }
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            questRequirements(final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    root) {
        return root.literal("edit", NQDescription.of("Opens subcommands for editing a specific quest."), "e")
                .required("quest", NQArgumentType.quest(), NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."))
                .literal("requirements", NQDescription.of("Manages requirements that must pass before the selected quest can be taken."), "req")
                .literal("add", NQDescription.of("Adds a requirement that must pass before players can take the selected quest."));
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            objectiveConditions(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final AdminEditCommands.ConditionGroup group,
                    final int level) {
        final String groupName = switch (group) {
            case UNLOCK -> "unlock";
            case PROGRESS -> "progress";
            case COMPLETE -> "complete";
        };
        final NQDescription groupDescription = switch (group) {
            case UNLOCK -> NQDescription.of("Configures conditions required before the objective can unlock.");
            case PROGRESS -> NQDescription.of("Configures conditions required while the objective is progressing.");
            case COMPLETE -> NQDescription.of("Configures conditions required before the objective can complete.");
        };
        return objectiveEditBase(root, level)
                .literal("conditions", NQDescription.of("Manages conditions attached to the selected objective."))
                .literal(groupName, groupDescription)
                .literal("add", NQDescription.of("Adds a condition to this objective condition group."));
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            objectiveEditBase(
                    final NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            root,
                    final int level) {
        NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                command = root.literal("edit", NQDescription.of("Opens subcommands for editing a specific quest."), "e")
                        .required("quest", NQArgumentType.quest(), NQDescription.of("Identifier of the quest to edit; use /qa list to see available quests."))
                        .literal("objectives", NQDescription.of("Manages objectives on the selected quest."), "o")
                        .literal("edit", NQDescription.of("Opens subcommands for editing a specific objective on the selected quest."))
                        .required("objectiveId", NQArgumentType.integer("objective id"), NQDescription.of("Objective ID shown by this quest's objectives list."));
        for (int i = 1; i <= level; i++) {
            command = command.literal("objectives", NQDescription.of("Manages child objectives inside the selected objective."), "o")
                    .literal("edit", NQDescription.of("Opens subcommands for editing a child objective inside the selected objective."))
                    .required(
                            "objectiveId" + (i + 1),
                            NQArgumentType.integer("child objective id"),
                            NQDescription.of("Child objective ID shown inside the selected parent objective."));
        }
        return command;
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            actionConditions(final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    root) {
        return root.literal("actions", NQDescription.of("Manages saved actions, inline actions, and action execution."))
                .literal("edit", NQDescription.of("Opens subcommands for editing a saved action."))
                .required("action", NQArgumentType.action(), NQDescription.of("Identifier of the saved action to edit; use /qa actions to list saved actions."))
                .literal("conditions", NQDescription.of("Manages conditions required before the selected action can run."))
                .literal("add", NQDescription.of("Adds a condition to the selected saved action."));
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            fields(
                    NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            command,
                    final List<RegistryField.Definition> fields) {
        for (int i = 0; i < fields.size(); i++) {
            command = field(command, fields.get(i), i == fields.size() - 1);
        }
        return command;
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            field(
                    NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            command,
                    final RegistryField.Definition field,
                    final boolean last) {
        if (isLocation(field)) {
            return command.required(locationPart(field, "world"), NQArgumentType.world(), NQDescription.of(field.description() + " world."))
                    .required(locationPart(field, "x"), NQArgumentType.number("x coordinate"), NQDescription.of(field.description() + " X coordinate."))
                    .required(locationPart(field, "y"), NQArgumentType.number("y coordinate"), NQDescription.of(field.description() + " Y coordinate."))
                    .required(locationPart(field, "z"), NQArgumentType.number("z coordinate"), NQDescription.of(field.description() + " Z coordinate."));
        }
        return command.required(field.name(), argument(field, last), NQDescription.of(field.description()), suggestions(field));
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            flags(
                    NQCommandBuilder<
                                    NQArgumentType,
                                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                                    NQSuggestionProvider<NQCommandContext>,
                                    NQCommandHandler>
                            command,
                    final List<RegistryField.Definition> flags) {
        for (final RegistryField.Definition flag : flags) {
            if (flag.presenceFlag()) {
                command = command.flag(NQFlag.presence(flag.name(), NQDescription.of(flag.description())));
            } else {
                command = command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                flag.name(), NQDescription.of(flag.description()))
                        .withArgument(argument(flag, true))
                        .withSuggestions(suggestions(flag))
                        .build());
            }
        }
        return command;
    }

    private static NQArgumentType argument(final RegistryField.Definition field, final boolean last) {
        final String type = normalize(field.valueType());
        if (type.contains("booleanexpression")) {
            return NQArgumentType.booleanExpression(field.valueType());
        }
        if (type.contains("npcselector")) {
            return NQArgumentType.npcSelector();
        }
        if (type.contains("itemselection") || type.contains("item selection")) {
            return NQArgumentType.itemSelection();
        }
        if (type.contains("actionnames") || type.contains("savedactionnames")) {
            return NQArgumentType.actionList();
        }
        if (type.equals("condition") || type.equals("savedconditionname")) {
            return NQArgumentType.condition();
        }
        if (type.contains("entity")) {
            return NQArgumentType.entityType();
        }
        if (type.contains("enchantment")) {
            return NQArgumentType.enchantment();
        }
        if (type.contains("integer") || type.contains("whole number")) {
            return NQArgumentType.integer(field.valueType());
        }
        if (type.equals("number") || type.contains("optionalnumber")) {
            return NQArgumentType.number(field.valueType());
        }
        if (type.contains("duration")) {
            return NQArgumentType.duration();
        }
        if (type.contains("numberexpressiontoken")) {
            return NQArgumentType.numberExpressionToken(field.valueType());
        }
        if (type.contains("numberexpression") || type.contains("number expression")) {
            return last
                    ? NQArgumentType.numberExpression(field.valueType())
                    : NQArgumentType.numberExpressionToken(field.valueType());
        }
        if (type.contains("greedy") || type.contains("command")) {
            return last ? NQArgumentType.greedyString(field.valueType()) : NQArgumentType.word(field.valueType());
        }
        return NQArgumentType.word(field.valueType());
    }

    private static NQSuggestionProvider<NQCommandContext> suggestions(final RegistryField.Definition field) {
        if (field == null || field.suggestions() == null) {
            return null;
        }
        return (context, input) -> field.suggestions().get();
    }

    private static RegistryField.Definition field(final List<RegistryField.Definition> fields, final String name) {
        for (final RegistryField.Definition field : fields) {
            if (field.name().equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private static List<RegistryField.Definition> namedFields(
            final List<RegistryField.Definition> fields,
            final String... names) {
        final List<RegistryField.Definition> selected = new ArrayList<>();
        for (final String name : names) {
            final RegistryField.Definition field = field(fields, name);
            if (field != null) {
                selected.add(field);
            }
        }
        return List.copyOf(selected);
    }

    private static String joinRaw(final String... parts) {
        final List<String> values = new ArrayList<>();
        for (final String part : parts) {
            if (part != null && !part.isBlank()) {
                values.add(part);
            }
        }
        return String.join(" ", values);
    }

    private static String rawArguments(
            final NQCommandContext context,
            final List<RegistryField.Definition> fields,
            final List<RegistryField.Definition> flags) {
        final List<String> values = new ArrayList<>();
        for (final RegistryField.Definition field : fields) {
            appendRawValue(values, context, field);
        }
        for (final RegistryField.Definition flag : flags) {
            if (flag.presenceFlag()) {
                if (context.flagPresent(flag.name())) {
                    values.add("--" + flag.name());
                }
                continue;
            }
            final String value = context.flag(flag.name());
            if (value != null && !value.isBlank()) {
                values.add("--" + flag.name());
                values.add(value);
            }
        }
        return String.join(" ", values);
    }

    private static NQCommandBuilder<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>
            withTaskDescriptionFlag(final NQCommandBuilder<
                            NQArgumentType,
                            NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                            NQSuggestionProvider<NQCommandContext>,
                            NQCommandHandler>
                    command) {
        return command.flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                        NQFlags.TASK_DESCRIPTION.name(),
                        NQFlags.TASK_DESCRIPTION.description())
                .withArgument(NQArgumentType.greedyString("task description"))
                .build());
    }

    private static CommandMessage selectedRegionError(
            final NQCommandContext context,
            final CommandManager commandManager) {
        if (context.questPlayer() == null || !context.questPlayer().hasPlayer()) {
            return CommandMessage.error(
                    "<error>This shortcut can only be used by a player. Use the coordinate form from console.");
        }
        if (commandManager.worldEditSelection(context.questPlayer()) == null) {
            return CommandMessage.error(
                    "<error>Please make a region selection using WorldEdit first.");
        }
        return null;
    }

    private static String reachLocationCenterRadiusRawArguments(final NQCommandContext context) {
        final String world = context.argument("world");
        final double x = doubleArgument(context, "x");
        final double y = doubleArgument(context, "y");
        final double z = doubleArgument(context, "z");
        final double radius = Math.max(0, doubleArgument(context, "radius"));
        return String.join(
                " ",
                locationRaw(world, x - radius, y - radius, z - radius),
                locationRaw(world, x + radius, y + radius, z + radius),
                context.argument("locationName"));
    }

    private static String reachLocationRegionRawArguments(
            final LocationRegion region,
            final String locationName) {
        return String.join(" ", locationRaw(region.min()), locationRaw(region.max()), locationName);
    }

    private static String shootArrowCenterRadiusRawArguments(final NQCommandContext context) {
        return String.join(
                " ",
                context.argument("amount"),
                locationRaw(
                        context.argument("world"),
                        doubleArgument(context, "x"),
                        doubleArgument(context, "y"),
                        doubleArgument(context, "z")),
                "none",
                "none",
                context.argument("radius"));
    }

    private static String shootArrowRegionRawArguments(
            final String amount,
            final LocationRegion region) {
        return String.join(
                " ",
                amount,
                locationRaw(region.center()),
                locationRaw(region.min()),
                locationRaw(region.max()),
                String.valueOf(region.enclosingRadius()));
    }

    private static String locationRaw(final NQLocation location) {
        return location == null ? "none" : locationRaw(location.worldName(), location.x(), location.y(), location.z());
    }

    private static String locationRaw(final String worldName, final double x, final double y, final double z) {
        return worldName + " " + x + " " + y + " " + z;
    }

    private static double doubleArgument(final NQCommandContext context, final String name) {
        try {
            return Double.parseDouble(context.argument(name));
        } catch (final NumberFormatException exception) {
            return 0;
        }
    }

    private static String interactLookingRawArguments(
            final NQCommandContext context,
            final Objectives.Type objectiveType,
            final NQLocation target) {
        final List<String> values = new ArrayList<>();
        values.add(context.argument("amount"));
        values.add(target.worldName());
        values.add(String.valueOf((int) Math.floor(target.x())));
        values.add(String.valueOf((int) Math.floor(target.y())));
        values.add(String.valueOf((int) Math.floor(target.z())));
        for (final RegistryField.Definition flag : objectiveType.flags()) {
            if (flag.presenceFlag()) {
                if (context.flagPresent(flag.name())) {
                    values.add("--" + flag.name());
                }
                continue;
            }
            final String value = context.flag(flag.name());
            if (value != null && !value.isBlank()) {
                values.add("--" + flag.name());
                values.add(value);
            }
        }
        return String.join(" ", values);
    }

    private static String rawVariableConditionArguments(
            final NQCommandContext context,
            final Variables.Type variable,
            final Variables.Command command,
            final List<RegistryField.Definition> variableFlags,
            final List<RegistryField.Definition> flags) {
        final List<String> values = new ArrayList<>();
        values.add(variable.id());
        for (final RegistryField.Definition field : variable.fields()) {
            if (field.flag() || field.presenceFlag()) {
                continue;
            }
            appendRawValue(values, context, field);
        }
        values.add(context.argument(command.operatorField()));
        appendRawValue(values, context, command.expressionType());
        if (command.variableType() == VariableDataType.ITEMSTACKLIST) {
            values.add(context.argument("amount"));
        }
        appendFlags(values, context, variableFlags);
        appendFlags(values, context, flags);
        return String.join(" ", values);
    }

    private static String rawVariableCommandArguments(
            final NQCommandContext context,
            final Variables.Type variable,
            final Variables.Command command,
            final List<RegistryField.Definition> variableFlags,
            final List<RegistryField.Definition> flags) {
        final List<String> values = new ArrayList<>();
        values.add(variable.id());
        for (final RegistryField.Definition field : variable.fields()) {
            if (field.flag() || field.presenceFlag()) {
                continue;
            }
            appendRawValue(values, context, field);
        }
        values.add(context.argument(command.operatorField()));
        appendRawValue(values, context, command.expressionType());
        if (command.variableType() == VariableDataType.ITEMSTACKLIST) {
            values.add(context.argument("amount"));
        }
        appendFlags(values, context, variableFlags);
        appendFlags(values, context, flags);
        return String.join(" ", values);
    }

    private static void appendFlags(
            final List<String> values,
            final NQCommandContext context,
            final List<RegistryField.Definition> flags) {
        for (final RegistryField.Definition flag : flags) {
            if (flag.presenceFlag()) {
                if (context.flagPresent(flag.name())) {
                    values.add("--" + flag.name());
                }
            } else {
                final String value = context.flag(flag.name());
                if (value != null && !value.isBlank()) {
                    values.add("--" + flag.name());
                    values.add(value);
                }
            }
        }
    }

    private static void appendRawValue(
            final List<String> values,
            final NQCommandContext context,
            final RegistryField.Definition field) {
        if (isLocation(field)) {
            values.add(context.argument(locationPart(field, "world")));
            values.add(context.argument(locationPart(field, "x")));
            values.add(context.argument(locationPart(field, "y")));
            values.add(context.argument(locationPart(field, "z")));
            return;
        }
        values.add(context.argument(field.name()));
    }

    private static boolean isLocation(final RegistryField.Definition field) {
        return normalize(field.valueType()).equals("location");
    }

    private static String locationPart(final RegistryField.Definition field, final String part) {
        return field.name() + "_" + part;
    }

    private static String normalize(final String valueType) {
        return valueType == null ? "" : valueType.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
    }

    private static String targetName(final Conditions.Target conditionFor) {
        return switch (conditionFor) {
            case QUEST -> "quest requirement";
            case OBJECTIVE_UNLOCK -> "objective unlock condition";
            case OBJECTIVE_PROGRESS -> "objective progress condition";
            case OBJECTIVE_COMPLETE -> "objective completion condition";
            case SAVED_CONDITION -> "saved condition";
            case INLINE -> "inline condition check";
            case ACTION -> "saved action condition";
        };
    }

    private static int[] objectiveParentPath(final NQCommandContext context, final int level) {
        if (level <= 0) {
            return new int[0];
        }
        final int[] path = new int[level];
        for (int i = 0; i < level; i++) {
            path[i] = integer(context.argument(i == 0 ? "objectiveId" : "objectiveId" + (i + 1)));
        }
        return path;
    }

    private static int[] objectivePath(final NQCommandContext context, final int level) {
        final int[] path = new int[level + 1];
        for (int i = 0; i <= level; i++) {
            path[i] = integer(context.argument(i == 0 ? "objectiveId" : "objectiveId" + (i + 1)));
        }
        return path;
    }

    private static int integer(final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException exception) {
            return 1;
        }
    }

    private static Duration duration(final Object raw) {
        if (raw instanceof final Duration duration) {
            return duration;
        }
        final String input = raw == null ? "" : raw.toString();
        if (input.isBlank()) {
            return null;
        }
        return UtilManager.parseDuration(input);
    }

    private static Actions.Type actionType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Conditions.Type conditionType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Objectives.Type objectiveType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().objectives().stream()
                .filter(objective -> objective.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Variables.Type variable(final NotQuestsPlugin plugin, final String variableName) {
        return plugin.registry().variables().stream()
                .filter(variable -> variable.id().equalsIgnoreCase(variableName))
                .findFirst()
                .orElse(null);
    }

    private static boolean setBoolean(
            final Variables.Type variable,
            final PlatformPlayer questPlayer,
            final String rawValue) {
        if (!(variable.handler() instanceof final Variables.BooleanVariableHandler handler) || !handler.canSet()) {
            return false;
        }
        return handler.setValue(Boolean.parseBoolean(rawValue), questPlayer);
    }

    private static boolean setNumber(
            final Variables.Type variable,
            final PlatformPlayer questPlayer,
            final String rawValue) {
        if (!(variable.handler() instanceof final Variables.NumberVariableHandler handler) || !handler.canSet()) {
            return false;
        }
        return handler.setValue(Double.parseDouble(rawValue), questPlayer);
    }

    private static boolean setString(
            final Variables.Type variable,
            final PlatformPlayer questPlayer,
            final String rawValue) {
        if (!(variable.handler() instanceof final Variables.StringVariableHandler handler) || !handler.canSet()) {
            return false;
        }
        return handler.setValue(rawValue, questPlayer);
    }

    private static String playerDisplayName(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return "";
        }
        return questPlayer.playerName() == null || questPlayer.playerName().isBlank()
                ? questPlayer.playerIdentifier()
                : questPlayer.playerName();
    }

    private static String formatVariableValue(final Object value) {
        if (value instanceof final ItemSelection selection) {
            return selection.listedMaterials("highlight");
        }
        if (value instanceof final Collection<?> values) {
            return values.stream()
                    .map(RegistryCommands::formatVariableValue)
                    .toList()
                    .toString();
        }
        return String.valueOf(value);
    }

    private static CommandMessage validateConditionData(
            final Conditions.Type type,
            final Condition data,
            final int[] objectivePath) {
        if ("Date".equalsIgnoreCase(type.id())) {
            final String operation = data.text("Date operation").toLowerCase(Locale.ROOT);
            if (!operation.equals("after") && !operation.equals("before")) {
                return CommandMessage.error(
                        "<error>Error: The date operation can only be <highlight>after</highlight> or <highlight>before</highlight>.");
            }
            data.setValue("Date operation", operation);
        }
        if ("CompletedObjective".equalsIgnoreCase(type.id())
                && objectivePath != null
                && objectivePath.length > 0
                && data.integer("dependingObjectiveId", -1) == objectivePath[objectivePath.length - 1]) {
            return CommandMessage.error("<error>Error: You cannot set an objective to depend on itself!");
        }
        return null;
    }

    private static void applySharedConditionFlags(
            final Condition data,
            final String rawArguments) {
        final List<String> tokens = Actions.tokenize(rawArguments);
        if (tokens.contains("--" + NQFlags.NEGATE.name())) {
            data.setValue("negated", true);
        }
        if (tokens.contains("--" + NQFlags.ALLOW_PROGRESS_DECREASE_IF_NOT_FULFILLED.name())) {
            data.setValue("allowProgressDecreaseIfNotFulfilled", true);
        }
    }

    private static String formatProgress(final double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }

    private static NQDescription createActionDescription(final String actionDisplayName) {
        return NQDescription.of("Creates a new " + actionDisplayName + " action.");
    }

    private static NQDescription executeInlineActionDescription(final String actionDisplayName) {
        return NQDescription.of("Executes a " + actionDisplayName + " action immediately without saving it.");
    }

    private static NQDescription createConditionDescription(final String conditionDisplayName, final String target) {
        return NQDescription.of("Creates a new " + conditionDisplayName + " " + target + ".");
    }
}
