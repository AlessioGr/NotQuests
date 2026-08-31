package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.SavedActions;
import com.notquests.core.commands.framework.*;
import com.notquests.core.managers.UtilManager;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.registry.fields.RegistryField;
import com.notquests.core.structs.Quest;
import com.notquests.core.triggers.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class TriggerCommands {
    private TriggerCommands() {}

    static CommandMessage addQuestTrigger(
            final NotQuestsPlugin plugin,
            final String questName,
            final String triggerTypeId,
            final String actionName,
            final String applyOnTarget,
            final String worldName,
            final String rawArguments) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Triggers.Type type = triggerType(plugin, triggerTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests trigger type: " + highlight(triggerTypeId) + ".");
        }
        final Trigger data = new Trigger(0, type.id(), null);
        final List<String> tokens = Actions.tokenize(rawArguments);
        for (int i = 0; i < type.fields().size() && i < tokens.size(); i++) {
            final RegistryField.Definition field = type.fields().get(i);
            data.setValue(field.name(), parseTriggerFieldValue(field, tokens.get(i)));
        }
        final Integer applyOn = parseApplyOn(applyOnTarget);
        if (applyOn == null) {
            return CommandMessage.error("ApplyOn Objective '" + applyOnTarget + "' is not a valid applyOn objective!");
        }
        if (applyOn > 0 && quest.getObjectiveFromID(applyOn) == null) {
            return CommandMessage.error("ApplyOn Objective '" + applyOnTarget + "' is not an objective of the Quest!");
        }
        data.setValue("action", actionName == null ? "" : actionName);
        data.setValue("applyOn", applyOn);
        data.setValue("worldName", worldName == null || worldName.isBlank() ? "ALL" : worldName);
        quest.addTrigger(type.id(), data);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>" + type.id()
                + " Trigger successfully added to Quest " + highlight(questName) + "!");
    }

    static List<CommandMessage> listQuestTriggers(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(missingQuest(questName));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Triggers for Quest " + highlight2(questName) + ":"));
        for (final Trigger trigger : quest.getTriggers()) {
            final Triggers.Type type = triggerType(plugin, trigger.typeId());
            final String actionName = trigger.data().text("action");
            final SavedActions.SavedAction action = plugin.savedActions().action(actionName);
            final String actionDescription = action == null
                    ? "none"
                    : actionDescription(action);
            final int applyOn = trigger.data().integer("applyOn", 0);
            final String world = blankDefault(trigger.data().text("worldName"), "ALL");
            messages.add(CommandMessage.success("<highlight>" + trigger.id() + ".</highlight> Type: <main>"
                    + trigger.typeId()));
            messages.add(CommandMessage.success("<unimportant>--</unimportant> <main>"
                    + triggerDescription(trigger, type)));
            messages.add(CommandMessage.success("<unimportant>--- Action Name:</unimportant> <main>"
                    + blankDefault(actionName, "none")));
            messages.add(CommandMessage.success("<unimportant>------ Description:</unimportant> <main>"
                    + actionDescription));
            messages.add(CommandMessage.success("<unimportant>--- Amount of triggers needed for first execution:</unimportant> <main>"
                    + trigger.data().integer("amount", 1)));
            messages.add(CommandMessage.success("<unimportant>--- Apply on:</unimportant> <main>"
                    + (applyOn <= 0 ? "Quest" : "Objective " + applyOn)));
            messages.add(CommandMessage.success("<unimportant>--- In World:</unimportant> <main>"
                    + ("ALL".equalsIgnoreCase(world) ? "Any World" : world)));
        }
        return List.copyOf(messages);
    }

    static CommandMessage clearQuestTriggers(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.clearTriggers();
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>All Triggers of Quest " + highlight(questName)
                + " have been removed!");
    }

    static CommandMessage removeQuestTrigger(final NotQuestsPlugin plugin, final String questName, final int triggerId) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        if (!quest.removeTrigger(triggerId)) {
            return CommandMessage.error("<error> Error: Trigger with the ID " + highlight(triggerId) + " was not found!");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<main>The trigger with the ID "
                + highlight(triggerId) + " of Quest " + highlight2(questName) + " has been removed!");
    }

    private static Triggers.Type triggerType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().triggers().stream()
                .filter(trigger -> trigger.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String triggerDescription(
            final Trigger trigger,
            final Triggers.Type type) {
        if (type == null) {
            return trigger.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.description();
        }
        try {
            return type.descriptionRenderer().apply(new Triggers.Description() {
                @Override
                public String text(final String name) {
                    return trigger.data().text(name);
                }

                @Override
                public int integer(final String name, final int fallback) {
                    return trigger.data().integer(name, fallback);
                }
            });
        } catch (final RuntimeException exception) {
            return type.description();
        }
    }

    private static String actionDescription(final SavedActions.SavedAction action) {
        if (action == null || action.getType() == null) {
            return "none";
        }
        if (action.getType().descriptionRenderer() == null) {
            return action.getType().displayName() == null || action.getType().displayName().isBlank()
                    ? action.getType().description()
                    : action.getType().displayName();
        }
        try {
            return action.getType().descriptionRenderer().render(action.getData(), null);
        } catch (final RuntimeException exception) {
            return action.getType().displayName() == null || action.getType().displayName().isBlank()
                    ? action.getType().description()
                    : action.getType().displayName();
        }
    }

    private static Object parseTriggerFieldValue(final RegistryField.Definition field, final String rawValue) {
        final String type = normalizeValueType(field.valueType());
        if (type.contains("integer") || type.contains("wholenumber")) {
            return Integer.parseInt(rawValue);
        }
        if (type.equals("number") || type.contains("optionalnumber")) {
            return Double.parseDouble(rawValue);
        }
        if (type.contains("duration")) {
            return UtilManager.parseDuration(rawValue);
        }
        return rawValue;
    }

    private static String normalizeValueType(final String valueType) {
        return valueType == null
                ? ""
                : valueType.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
    }

    private static Integer parseApplyOn(final String applyOnTarget) {
        if (applyOnTarget == null || applyOnTarget.isBlank() || applyOnTarget.equalsIgnoreCase("Quest")) {
            return 0;
        }
        final String normalized = applyOnTarget.trim().toLowerCase(Locale.ROOT).replace("o", "");
        try {
            final int applyOn = Integer.parseInt(normalized);
            return applyOn < 0 ? null : applyOn;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static String blankDefault(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
