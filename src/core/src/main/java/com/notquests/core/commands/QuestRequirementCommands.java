package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QuestRequirementCommands {
    private QuestRequirementCommands() {}

    static CommandMessage addQuestRequirement(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String conditionTypeId,
            final String rawArguments) {
        return addQuestRequirement(plugin, adapter, questName, conditionTypeId, rawArguments, null);
    }

    static CommandMessage addQuestRequirement(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String conditionTypeId,
            final String rawArguments,
            final PlatformPlayer questPlayer) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
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
            quest.addRequirement(type.id(), data);
            plugin.saveData();
            return CommandMessage.success("<success>" + type.id()
                    + " Requirement successfully added to Quest " + highlight(questName) + "!");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add NotQuests requirement: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listQuestRequirements(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(missingQuest(questName));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Requirements for Quest " + highlight2(questName) + ":"));
        for (final Condition requirement : quest.getRequirements()) {
            final Conditions.Type type = conditionType(plugin, requirement.typeId());
            messages.add(CommandMessage.success("<highlight>" + requirement.id() + ".</highlight> <main>"
                    + requirement.typeId()));
            messages.add(CommandMessage.success("<main>" + conditionDescription(requirement, type)));
        }
        return List.copyOf(messages);
    }

    static CommandMessage clearQuestRequirements(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.clearRequirements();
        plugin.saveData();
        return CommandMessage.success("<main>All requirements of Quest " + highlight(questName)
                + " have been removed!");
    }

    static CommandMessage removeQuestRequirement(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        if (!quest.removeRequirement(requirementId)) {
            return CommandMessage.error("Requirement with the ID " + highlight(requirementId) + " was not found!");
        }
        plugin.saveData();
        return CommandMessage.success("<main>The requirement with the ID "
                + highlight(requirementId) + " of Quest " + highlight2(questName) + " has been removed!");
    }

    static CommandMessage requirementDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId) {
        final Condition requirement = requirement(plugin, questName, requirementId);
        if (requirement == null) {
            return CommandMessage.error("Requirement with the ID " + highlight(requirementId) + " was not found!");
        }
        return CommandMessage.success("<main>Current description of requirement with ID "
                + highlight(requirementId) + ": " + highlight2(requirement.getDescription()));
    }

    static CommandMessage setRequirementDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId,
            final String description) {
        final Condition requirement = requirement(plugin, questName, requirementId);
        if (requirement == null) {
            return CommandMessage.error("Requirement with the ID " + highlight(requirementId) + " was not found!");
        }
        requirement.setDescription(description);
        plugin.saveData();
        return CommandMessage.success("<success>Description successfully added to requirement with ID "
                + highlight(requirementId) + "! New description: " + highlight2(requirement.getDescription()));
    }

    static CommandMessage removeRequirementDescription(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId) {
        final Condition requirement = requirement(plugin, questName, requirementId);
        if (requirement == null) {
            return CommandMessage.error("Requirement with the ID " + highlight(requirementId) + " was not found!");
        }
        requirement.setDescription("");
        plugin.saveData();
        return CommandMessage.success("<success>Description successfully removed from requirement with ID "
                + highlight(requirementId) + "!");
    }

    static CommandMessage setRequirementHidden(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId,
            final String hiddenExpression) {
        final Condition requirement = requirement(plugin, questName, requirementId);
        if (requirement == null) {
            return CommandMessage.error("Requirement with the ID " + highlight(requirementId) + " was not found!");
        }
        requirement.setHiddenExpression(hiddenExpression);
        plugin.saveData();
        return CommandMessage.success("<success>Hidden status successfully added to requirement with ID "
                + highlight(requirementId) + "! New hidden status: " + highlight2(requirement.getHiddenExpression()));
    }

    private static Condition requirement(
            final NotQuestsPlugin plugin,
            final String questName,
            final int requirementId) {
        final Quest quest = plugin.questManager().getQuest(questName);
        return quest == null ? null : quest.getRequirementFromID(requirementId);
    }

    private static Conditions.Type conditionType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String conditionDescription(
            final Condition condition,
            final Conditions.Type type) {
        if (condition.getDescription() != null && !condition.getDescription().isBlank()) {
            return condition.getDescription();
        }
        if (type == null) {
            return condition.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.description();
        }
        try {
            return type.descriptionRenderer().render(condition.data(), null);
        } catch (final RuntimeException exception) {
            return type.description();
        }
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

    private static CommandMessage missingQuest(final String questName) {
        return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist!");
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
