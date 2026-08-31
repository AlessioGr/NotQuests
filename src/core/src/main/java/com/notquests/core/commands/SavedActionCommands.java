package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.SavedActions.Chain;
import com.notquests.core.actions.SavedActions;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SavedActionCommands {
    private SavedActionCommands() {}

    static List<NQCommandRegistration<
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
                    final NotQuestsPlugin plugin,
                    final NotQuestsAdapter adapter) {
        final ArrayList<NQCommandRegistration<
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
                actions = root.literal(
                        "actions",
                        NQDescription.of("Manages saved actions, inline actions, and action execution."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                actionsEdit = actions.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a saved action."))
                        .required(
                                "action",
                                NQArgumentType.action(),
                                NQDescription.of("Identifier of the saved action to edit; use /qa actions to list saved actions."));
        commands.add(actions.literal("list", NQDescription.of("Lists every saved action."))
                .commandDescription(NQDescription.of("Lists all saved actions."))
                .handler(ignored -> listSavedActions(plugin))
                .registration());
        commands.add(actionsEdit.literal("delete", NQDescription.of("Deletes the selected saved action."), "remove")
                .commandDescription(NQDescription.of("Deletes a saved action."))
                .handler(context -> List.of(deleteSavedAction(plugin, context.argument("action"))))
                .registration());
        commands.add(actionsEdit.literal("execute", NQDescription.of("Executes the selected action or command."), "run")
                .optional(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player who should be used as the target when executing the selected action; defaults to the sender when possible."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.IGNORE_ACTION_CONDITIONS.name(), NQFlags.IGNORE_ACTION_CONDITIONS.description()))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.SILENT_ACTION_EXECUTION.name(), NQFlags.SILENT_ACTION_EXECUTION.description()))
                .commandDescription(NQDescription.of("Executes a saved action."))
                .handler(context -> executeSavedAction(
                        plugin,
                        adapter,
                        CommandSupport.targetPlatformPlayer(adapter, context.argument("player"), context.questPlayer()),
                        context.argument("action"),
                        context.flagPresent(NQFlags.IGNORE_ACTION_CONDITIONS.name()),
                        context.flagPresent(NQFlags.SILENT_ACTION_EXECUTION.name())))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                actionConditions = actionsEdit.literal(
                        "conditions",
                        NQDescription.of("Manages conditions required before the selected action can run."));
        commands.add(actionConditions.literal("clear", NQDescription.of("Removes every condition from the selected saved action."))
                .commandDescription(NQDescription.of("Removes all conditions from the selected saved action."))
                .handler(context -> List.of(clearSavedActionConditions(plugin, context.argument("action"))))
                .registration());
        commands.add(actionConditions.literal("list", NQDescription.of("Lists every condition on the selected saved action."), "show")
                .commandDescription(NQDescription.of("Lists all conditions attached to the selected saved action."))
                .handler(context -> savedActionConditions(plugin, context.argument("action")))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                editActionCondition = actionConditions.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a specific action condition."))
                        .required(
                                "condition-id",
                                NQArgumentType.integer("condition id"),
                                NQDescription.of("Condition ID shown by this saved action's condition list."),
                                (context, input) -> savedActionConditionIds(plugin, context.argument("action")));
        commands.add(editActionCondition.literal("delete", NQDescription.of("Removes the selected condition from the saved action."), "remove")
                .commandDescription(NQDescription.of("Removes a condition from the selected saved action."))
                .handler(context -> List.of(deleteSavedActionCondition(
                        plugin,
                        context.argument("action"),
                        integer(context.argument("condition-id")))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                actionConditionDescription = editActionCondition.literal(
                        "description",
                        NQDescription.of("Saved action condition description shown when listing this action's conditions. Supports MiniMessage formatting."));
        commands.add(actionConditionDescription.literal("set", NQDescription.of("Sets the selected action condition's description text."))
                .required(
                        "description",
                        NQArgumentType.greedyString("condition description"),
                        NQDescription.of("Action condition description shown when listing this saved action's conditions."))
                .commandDescription(NQDescription.of("Sets the selected action condition's description."))
                .handler(context -> List.of(setSavedActionConditionDescription(
                        plugin,
                        context.argument("action"),
                        integer(context.argument("condition-id")),
                        context.argument("description"))))
                .registration());
        commands.add(actionConditionDescription.literal("remove", NQDescription.of("Removes the selected action condition's custom description text."), "delete")
                .commandDescription(NQDescription.of("Removes the selected action condition's description."))
                .handler(context -> List.of(removeSavedActionConditionDescription(
                        plugin,
                        context.argument("action"),
                        integer(context.argument("condition-id")))))
                .registration());
        commands.add(actionConditionDescription.literal("show", NQDescription.of("Shows the selected action condition's description text."), "check")
                .commandDescription(NQDescription.of("Shows the selected action condition's description."))
                .handler(context -> List.of(savedActionConditionDescription(
                        plugin,
                        context.argument("action"),
                        integer(context.argument("condition-id")))))
                .registration());
        commands.add(editActionCondition.literal("hidden", NQDescription.of("Controls whether the selected action condition is hidden from players."))
                .literal("set", NQDescription.of("Changes whether the selected action condition is hidden from players."))
                .required(
                        "hiddenStatusExpression",
                        NQArgumentType.word("hidden status expression"),
                        NQDescription.of("Boolean expression that decides whether this action condition is hidden from players."))
                .commandDescription(NQDescription.of("Sets the selected action condition's hidden status."))
                .handler(context -> List.of(setSavedActionConditionHidden(
                        plugin,
                        context.argument("action"),
                        integer(context.argument("condition-id")),
                        context.argument("hiddenStatusExpression"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                actionCategory = actionsEdit.literal(
                        "category",
                        NQDescription.of("Shows or changes the category assigned to the selected action."));
        commands.add(actionCategory.literal("show", NQDescription.of("Shows the category currently assigned to the selected action."))
                .commandDescription(NQDescription.of("Shows the selected action's category."))
                .handler(context -> List.of(savedActionCategory(plugin, context.argument("action"))))
                .registration());
        commands.add(actionCategory.literal("set", NQDescription.of("Moves the selected action into another category."))
                .required(
                        "category",
                        NQArgumentType.category(),
                        NQDescription.of("New category for this saved action."))
                .commandDescription(NQDescription.of("Changes the selected action's category."))
                .handler(context -> List.of(setSavedActionCategory(
                        plugin,
                        context.argument("action"),
                        context.argument("category"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage saveAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String actionName,
            final String actionTypeId,
            final String rawArguments) {
        return saveAction(plugin, adapter, actionName, actionTypeId, rawArguments, "", null);
    }

    static CommandMessage saveAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String actionName,
            final String actionTypeId,
            final String rawArguments,
            final String categoryName,
            final Duration executionDelay) {
        return saveAction(
                plugin,
                adapter,
                actionName,
                actionTypeId,
                rawArguments,
                categoryName,
                executionDelay,
                null);
    }

    static CommandMessage saveAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String actionName,
            final String actionTypeId,
            final String rawArguments,
            final String categoryName,
            final Duration executionDelay,
            final PlatformPlayer questPlayer) {
        if (actionName == null || actionName.isBlank()) {
            return CommandMessage.error("Action name cannot be blank.");
        }
        if (actionName.contains(".")) {
            return CommandMessage.error("<error>Action " + highlight(actionName) + " cannot contain a dot in its name!");
        }
        if (plugin.savedActions().action(actionName) != null) {
            return CommandMessage.error("<error>Error! An action with the name " + highlight(actionName) + " already exists!");
        }
        final Actions.Type actionType = actionType(plugin, actionTypeId);
        if (actionType == null) {
            return CommandMessage.error("Unknown NotQuests action type: " + highlight(actionTypeId) + ".");
        }
        try {
            plugin.saveAction(
                    actionName,
                    actionType,
                    Actions.parse(adapter, actionType, rawArguments, questPlayer),
                    executionDelay);
            final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
            if (action != null && categoryName != null && !categoryName.isBlank()) {
                if (plugin.questManager().getCategory(categoryName) == null) {
                    return missingCategory(categoryName);
                }
                action.setCategory(categoryName);
            }
            return saveAndReturn(plugin, CommandMessage.success("<success>" + actionType.id() + " Action with the name "
                    + highlight(actionName) + " has been created successfully!"));
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot save NotQuests action: " + exception.getMessage());
        }
    }

    static CommandMessage executeSavedAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String actionName) {
        final List<CommandMessage> messages = executeSavedAction(plugin, adapter, questPlayer, actionName, true, false);
        return messages.isEmpty()
                ? CommandMessage.none()
                : messages.getFirst();
    }

    static List<CommandMessage> executeSavedAction(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final PlatformPlayer questPlayer,
            final String actionName,
            final boolean ignoreConditions,
            final boolean silent) {
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return List.of(CommandMessage.error("<error>Execute in game or specify a player"));
        }
        final SavedActions.Execution result = plugin.executeSavedActions(
                Chain.builder()
                        .actionNames(actionName)
                        .amount(1)
                        .ignoreConditions(ignoreConditions)
                        .randomRange(-1, -1)
                        .delay(Duration.ofMillis(-1))
                        .build(),
                adapter::schedule,
                questPlayer,
                ignored -> {});
        if (silent) {
            return List.of();
        }
        if (result == null || !result.executedAny()) {
            final List<String> warnings = result == null ? List.of() : result.warnings();
            if (!warnings.isEmpty()) {
                return warnings.stream().map(CommandMessage::error).toList();
            }
            return List.of(CommandMessage.error("<error>Action with the name "
                    + highlight(actionName) + " could not be executed."));
        }
        return List.of(CommandMessage.success("<success>Action with the name "
                + highlight(actionName) + " has been executed!"));
    }

    static CommandMessage deleteSavedAction(final NotQuestsPlugin plugin, final String actionName) {
        if (!plugin.deleteSavedAction(actionName)) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        return saveAndReturn(plugin, CommandMessage.success("<success>Action with the name "
                + highlight2(actionName) + " has been deleted."));
    }

    static List<CommandMessage> listSavedActions(final NotQuestsPlugin plugin) {
        final List<String> names = plugin.savedActionNames();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All Actions:"));
        int counter = 1;
        for (final String actionName : names) {
            final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>" + actionName));
            messages.add(CommandMessage.success("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type:</unimportant> <highlight2>"
                    + (action == null ? "unknown" : action.getType().id())));
            counter++;
        }
        return List.copyOf(messages);
    }

    static CommandMessage savedActionCategory(final NotQuestsPlugin plugin, final String actionName) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        final String category = blankDefault(action.getCategory(), "default");
        return CommandMessage.success("<main>Category for action " + highlight(actionName)
                + ": " + highlight2(category) + ".");
    }

    static CommandMessage setSavedActionCategory(
            final NotQuestsPlugin plugin,
            final String actionName,
            final String categoryName) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        if (plugin.questManager().getCategory(categoryName) == null) {
            return missingCategory(categoryName);
        }
        final String oldCategory = blankDefault(action.getCategory(), "default");
        if (oldCategory.equalsIgnoreCase(categoryName)) {
            return CommandMessage.error("<error> Error: The action " + highlight(actionName)
                    + " already has the category " + highlight2(oldCategory) + ".");
        }
        action.setCategory(categoryName);
        return saveAndReturn(plugin, CommandMessage.success("<success>Category for action " + highlight(actionName)
                + " has successfully been changed from " + highlight2(oldCategory)
                + " to " + highlight2(categoryName) + "!"));
    }

    static CommandMessage addSavedActionCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String actionName,
            final String conditionTypeId,
            final String rawArguments) {
        return addSavedActionCondition(plugin, adapter, actionName, conditionTypeId, rawArguments, null);
    }

    static CommandMessage addSavedActionCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String actionName,
            final String conditionTypeId,
            final String rawArguments,
            final PlatformPlayer questPlayer) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        final Conditions.Type type = conditionType(plugin, conditionTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests condition type: " + highlight(conditionTypeId) + ".");
        }
        try {
            final Condition data = Conditions.parse(
                    adapter, type, rawArguments, questPlayer);
            applySharedConditionFlags(data, rawArguments);
            final CommandMessage validation = validateConditionData(type, data);
            if (validation != null) {
                return validation;
            }
            action.addCondition(type, data);
            return saveAndReturn(plugin, CommandMessage.success("<success>" + type.id()
                    + " Condition successfully added to Action " + highlight(actionName) + "!"));
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add condition to action " + highlight(actionName)
                    + ": " + exception.getMessage());
        }
    }

    static List<CommandMessage> savedActionConditions(final NotQuestsPlugin plugin, final String actionName) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return List.of(CommandMessage.error("Action " + highlight(actionName) + " does not exist."));
        }
        final List<SavedActions.SavedCondition> conditions = action.getConditions();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Conditions of action with identifier "
                + highlight2(actionName) + ":</highlight>"));
        if (conditions.isEmpty()) {
            messages.add(CommandMessage.success("<warn>This action has no conditions!"));
            return List.copyOf(messages);
        }
        int counter = 1;
        for (final SavedActions.SavedCondition condition : conditions) {
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>"
                    + condition.getType().id()));
            messages.add(CommandMessage.success("<main>" + conditionDescription(condition)));
            counter++;
        }
        return List.copyOf(messages);
    }

    static List<String> savedActionConditionIds(final NotQuestsPlugin plugin, final String actionName) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return List.of();
        }
        return action.getConditions().stream()
                .map(condition -> String.valueOf(condition.getID()))
                .toList();
    }

    static CommandMessage clearSavedActionConditions(final NotQuestsPlugin plugin, final String actionName) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        action.clearConditions();
        return saveAndReturn(plugin, CommandMessage.success("<success>All conditions of action with identifier "
                + highlight(actionName) + " have been removed!"));
    }

    static CommandMessage deleteSavedActionCondition(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        if (action == null) {
            return CommandMessage.error("Action " + highlight(actionName) + " does not exist.");
        }
        if (!action.removeCondition(conditionId)) {
            return CommandMessage.error("Action condition " + highlight(conditionId) + " does not exist.");
        }
        return saveAndReturn(plugin, CommandMessage.success("<success>Condition with the ID " + highlight(conditionId)
                + " of action " + highlight2(actionName) + " has been removed!"));
    }

    static CommandMessage setSavedActionConditionDescription(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId,
            final String description) {
        final SavedActions.SavedCondition condition = savedActionCondition(plugin, actionName, conditionId);
        if (condition == null) {
            return CommandMessage.error("Action condition " + highlight(conditionId) + " does not exist.");
        }
        condition.setDescription(description);
        return saveAndReturn(plugin, CommandMessage.success("<success>Description successfully added to condition with ID "
                + highlight(conditionId)
                + " of action " + highlight2(actionName) + "! New description: " + highlight2(condition.getDescription())));
    }

    static CommandMessage removeSavedActionConditionDescription(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId) {
        final SavedActions.SavedCondition condition = savedActionCondition(plugin, actionName, conditionId);
        if (condition == null) {
            return CommandMessage.error("Action condition " + highlight(conditionId) + " does not exist.");
        }
        condition.setDescription("");
        return saveAndReturn(plugin, CommandMessage.success("<success>Description successfully removed from condition with ID "
                + highlight(conditionId)
                + " of action " + highlight2(actionName) + "! New description: " + highlight2(condition.getDescription())));
    }

    static CommandMessage savedActionConditionDescription(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId) {
        final SavedActions.SavedCondition condition = savedActionCondition(plugin, actionName, conditionId);
        if (condition == null) {
            return CommandMessage.error("Action condition " + highlight(conditionId) + " does not exist.");
        }
        return CommandMessage.success("<main>Description of condition with ID " + highlight(conditionId)
                + " of action " + highlight2(actionName) + ":\n"
                + blankDefault(condition.getDescription(), ""));
    }

    static CommandMessage setSavedActionConditionHidden(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId,
            final String hiddenExpression) {
        final SavedActions.SavedCondition condition = savedActionCondition(plugin, actionName, conditionId);
        if (condition == null) {
            return CommandMessage.error("Action condition " + highlight(conditionId) + " does not exist.");
        }
        condition.setHiddenExpression(hiddenExpression);
        return saveAndReturn(plugin, CommandMessage.success("<success>Hidden status successfully added to condition with ID "
                + highlight(conditionId)
                + " of action " + highlight2(actionName) + "! New hidden status: " + highlight2(condition.getHiddenExpression())));
    }

    private static SavedActions.SavedCondition savedActionCondition(
            final NotQuestsPlugin plugin,
            final String actionName,
            final int conditionId) {
        final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
        return action == null ? null : action.getConditionFromID(conditionId);
    }

    private static String conditionDescription(final SavedActions.SavedCondition condition) {
        if (condition.getDescription() != null && !condition.getDescription().isBlank()) {
            return condition.getDescription();
        }
        if (condition.getType().descriptionRenderer() == null) {
            return condition.getType().description();
        }
        try {
            return condition.getType().descriptionRenderer().render(condition.getData(), null);
        } catch (final RuntimeException exception) {
            return condition.getType().description();
        }
    }

    private static Actions.Type actionType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Conditions.Type conditionType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static CommandMessage validateConditionData(
            final Conditions.Type type,
            final Condition data) {
        if ("Date".equalsIgnoreCase(type.id())) {
            final String operation = data.text("Date operation").toLowerCase(Locale.ROOT);
            if (!operation.equals("after") && !operation.equals("before")) {
                return CommandMessage.error(
                        "<error>Error: The date operation can only be <highlight>after</highlight> or <highlight>before</highlight>.");
            }
            data.setValue("Date operation", operation);
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

    private static CommandMessage saveAndReturn(final NotQuestsPlugin plugin, final CommandMessage message) {
        if (message != null && message.success()) {
            plugin.saveData();
        }
        return message;
    }

    private static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static CommandMessage missingCategory(final String categoryName) {
        return CommandMessage.error("<error>No Category found: " + categoryName);
    }

    private static int integer(final String value) {
        return Integer.parseInt(value);
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
