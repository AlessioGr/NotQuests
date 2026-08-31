package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.conditions.ConditionCheck;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class SavedConditionCommands {
    private SavedConditionCommands() {}

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
                conditions = root.literal(
                        "conditions",
                        NQDescription.of("Manages saved conditions that can be reused by quests, objectives, and actions."));
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conditionsEdit = conditions.literal(
                                "edit",
                                NQDescription.of("Opens subcommands for editing a saved condition."))
                        .required(
                                "condition",
                                NQArgumentType.condition(),
                                NQDescription.of("Identifier of the saved condition to edit; use /qa conditions list to see saved conditions."));
        commands.add(conditions.literal("list", NQDescription.of("Lists every saved condition."))
                .commandDescription(NQDescription.of("Lists all saved conditions."))
                .handler(ignored -> listSavedConditions(plugin))
                .registration());
        commands.add(conditionsEdit.literal("delete", NQDescription.of("Deletes the selected saved condition."))
                .commandDescription(NQDescription.of("Deletes a saved condition."))
                .handler(context -> List.of(deleteSavedCondition(plugin, context.argument("condition"))))
                .registration());
        commands.add(conditionsEdit.literal("check", NQDescription.of("Checks the selected saved condition for a player."))
                .optional(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player for which the saved condition should be checked; defaults to the sender when possible."))
                .commandDescription(NQDescription.of("Checks a saved condition for a player."))
                .handler(context -> List.of(checkSavedCondition(
                        plugin,
                        CommandSupport.targetPlatformPlayer(adapter, context.argument("player"), context.questPlayer()),
                        context.argument("condition"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conditionCategory = conditionsEdit.literal(
                        "category",
                        NQDescription.of("Shows or changes the category assigned to the selected condition."));
        commands.add(conditionCategory.literal("show", NQDescription.of("Shows the category currently assigned to the selected condition."))
                .commandDescription(NQDescription.of("Shows the selected condition's category."))
                .handler(context -> List.of(savedConditionCategory(plugin, context.argument("condition"))))
                .registration());
        commands.add(conditionCategory.literal("set", NQDescription.of("Moves the selected condition into another category."))
                .required(
                        "category",
                        NQArgumentType.category(),
                        NQDescription.of("New category for this saved condition."))
                .commandDescription(NQDescription.of("Changes the selected condition's category."))
                .handler(context -> List.of(setSavedConditionCategory(
                        plugin,
                        context.argument("condition"),
                        context.argument("category"))))
                .registration());
        final NQCommandBuilder<
                        NQArgumentType,
                        NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                        NQSuggestionProvider<NQCommandContext>,
                        NQCommandHandler>
                conditionDescription = conditionsEdit.literal(
                        "description",
                        NQDescription.of("Shows or changes the selected condition's description."));
        commands.add(conditionDescription.literal("set", NQDescription.of("Sets the selected condition's description text."))
                .required(
                        "description",
                        NQArgumentType.greedyString("condition description"),
                        NQDescription.of("Condition description shown when listing or previewing the saved condition."))
                .commandDescription(NQDescription.of("Sets the selected condition's description."))
                .handler(context -> List.of(setSavedConditionDescription(
                        plugin,
                        context.argument("condition"),
                        context.argument("description"))))
                .registration());
        commands.add(conditionDescription.literal("remove", NQDescription.of("Removes the selected condition's custom description text."))
                .commandDescription(NQDescription.of("Removes the selected condition's description."))
                .handler(context -> List.of(removeSavedConditionDescription(plugin, context.argument("condition"))))
                .registration());
        commands.add(conditionDescription.literal("show", NQDescription.of("Shows the selected condition's description text."))
                .commandDescription(NQDescription.of("Shows the selected condition's description."))
                .handler(context -> List.of(savedConditionDescription(plugin, context.argument("condition"))))
                .registration());
        commands.add(conditionsEdit.literal("hidden", NQDescription.of("Controls whether the selected saved condition is hidden from players."))
                .literal("set", NQDescription.of("Changes whether the selected saved condition is hidden from players."))
                .required(
                        "hiddenStatusExpression",
                        NQArgumentType.word("hidden status expression"),
                        NQDescription.of("Boolean expression that decides whether this saved condition is hidden from players."))
                .commandDescription(NQDescription.of("Sets the selected condition's hidden status."))
                .handler(context -> List.of(setSavedConditionHidden(
                        plugin,
                        context.argument("condition"),
                        context.argument("hiddenStatusExpression"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage saveCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String conditionName,
            final String conditionTypeId,
            final String rawArguments) {
        return saveCondition(plugin, adapter, conditionName, conditionTypeId, rawArguments, "");
    }

    static CommandMessage saveCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String conditionName,
            final String conditionTypeId,
            final String rawArguments,
            final String categoryName) {
        return saveCondition(
                plugin, adapter, conditionName, conditionTypeId, rawArguments, categoryName, null);
    }

    static CommandMessage saveCondition(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String conditionName,
            final String conditionTypeId,
            final String rawArguments,
            final String categoryName,
            final PlatformPlayer questPlayer) {
        if (conditionName == null || conditionName.isBlank()) {
            return CommandMessage.error("Condition name cannot be blank.");
        }
        if (plugin.savedCondition(conditionName) != null) {
            return CommandMessage.error("<error>Error! A condition with the name " + highlight(conditionName) + " already exists!");
        }
        final Conditions.Type conditionType = conditionType(plugin, conditionTypeId);
        if (conditionType == null) {
            return CommandMessage.error("Unknown NotQuests condition type: " + highlight(conditionTypeId) + ".");
        }
        try {
            final Condition data = Conditions.parse(
                    adapter, conditionType, rawArguments, questPlayer);
            applySharedConditionFlags(data, rawArguments);
            final CommandMessage validation = validateConditionData(conditionType, data);
            if (validation != null) {
                return validation;
            }
            final NotQuestsPlugin.StoredCondition condition =
                    plugin.saveConditionAndReturn(conditionName, conditionType, data);
            if (categoryName != null && !categoryName.isBlank()) {
                if (plugin.questManager().getCategory(categoryName) == null) {
                    return missingCategory(categoryName);
                }
                condition.setCategory(categoryName);
            }
            return saveAndReturn(plugin, CommandMessage.success("<success>" + conditionType.id() + " Condition with the name "
                    + highlight(conditionName) + " has been created successfully!"));
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot save NotQuests condition: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listSavedConditions(final NotQuestsPlugin plugin) {
        final List<String> names = plugin.savedConditionNames();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>All Conditions:"));
        int counter = 1;
        for (final String conditionName : names) {
            final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>" + conditionName));
            messages.add(CommandMessage.success("  <veryUnimportant>└─</veryUnimportant> <unimportant>Type: <highlight2>"
                    + (condition == null ? "unknown" : condition.getType().id())));
            counter++;
        }
        return List.copyOf(messages);
    }

    static CommandMessage deleteSavedCondition(final NotQuestsPlugin plugin, final String conditionName) {
        if (!plugin.deleteSavedCondition(conditionName)) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        return saveAndReturn(plugin, CommandMessage.success("<success>Condition with the name "
                + highlight(conditionName) + " has been deleted."));
    }

    static CommandMessage checkSavedCondition(
            final NotQuestsPlugin plugin,
            final PlatformPlayer questPlayer,
            final String conditionName) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        if (questPlayer == null || !questPlayer.hasPlayer()) {
            return CommandMessage.error("<error>Execute in game or specify player");
        }
        final String result = ConditionCheck.check(condition.getType(), condition.getData(), questPlayer);
        return result == null || result.isBlank()
                ? CommandMessage.success("<success>Condition with the name " + highlight(condition.getName())
                        + " has been checked! Result:</success>\n<success>Condition fulfilled!")
                : CommandMessage.success("<success>Condition with the name " + highlight(condition.getName())
                        + " has been checked! Result:</success>\n" + result);
    }

    static CommandMessage savedConditionCategory(final NotQuestsPlugin plugin, final String conditionName) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        return CommandMessage.success("<main>Category for condition " + highlight(condition.getName())
                + ": " + highlight2(blankDefault(condition.getCategory(), "default")) + ".");
    }

    static CommandMessage setSavedConditionCategory(
            final NotQuestsPlugin plugin,
            final String conditionName,
            final String categoryName) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        if (plugin.questManager().getCategory(categoryName) == null) {
            return missingCategory(categoryName);
        }
        final String oldCategory = blankDefault(condition.getCategory(), "default");
        if (oldCategory.equalsIgnoreCase(categoryName)) {
            return CommandMessage.error("<error> Error: The condition " + highlight(condition.getName())
                    + " already has the category " + highlight2(oldCategory) + ".");
        }
        condition.setCategory(categoryName);
        return saveAndReturn(plugin, CommandMessage.success("<success>Category for condition " + highlight(condition.getName())
                + " has successfully been changed from " + highlight2(oldCategory)
                + " to " + highlight2(categoryName) + "!"));
    }

    static CommandMessage setSavedConditionDescription(
            final NotQuestsPlugin plugin,
            final String conditionName,
            final String description) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        condition.setDescription(description);
        return saveAndReturn(plugin, CommandMessage.success("<success>Description successfully added to condition "
                + highlight(condition.getName()) + "! New description: " + highlight2(condition.getDescription())));
    }

    static CommandMessage removeSavedConditionDescription(
            final NotQuestsPlugin plugin,
            final String conditionName) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        condition.setDescription("");
        return saveAndReturn(plugin, CommandMessage.success("<success>Description successfully removed from condition "
                + highlight(condition.getName()) + "! New description: " + highlight2(condition.getDescription())));
    }

    static CommandMessage savedConditionDescription(final NotQuestsPlugin plugin, final String conditionName) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        return CommandMessage.success("<main>Description of condition " + highlight(condition.getName())
                + ":\n" + blankDefault(condition.getDescription(), ""));
    }

    static CommandMessage setSavedConditionHidden(
            final NotQuestsPlugin plugin,
            final String conditionName,
            final String hiddenExpression) {
        final NotQuestsPlugin.StoredCondition condition = plugin.savedCondition(conditionName);
        if (condition == null) {
            return CommandMessage.error("Condition " + highlight(conditionName) + " does not exist.");
        }
        condition.setHiddenExpression(hiddenExpression);
        return saveAndReturn(plugin, CommandMessage.success("<success>Hidden status successfully added to condition "
                + highlight(condition.getName()) + "! New hidden status: " + highlight2(condition.getHiddenExpression())));
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

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
