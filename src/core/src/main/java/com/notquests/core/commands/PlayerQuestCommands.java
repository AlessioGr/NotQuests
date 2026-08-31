package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.commands.framework.*;
import com.notquests.core.conditions.Condition;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PlayerQuestCommands {
    private PlayerQuestCommands() {}

    static List<CommandMessage> takeQuest(
            final NotQuestsPlugin plugin,
            final PlatformPlayer target,
            final String questName) {
        if (target == null || !target.hasPlayer()) {
            return List.of(CommandMessage.error("<error>This command can only be used by a Player."));
        }
        if (plugin.questManager().getQuest(questName) == null) {
            return List.of(missingQuest(questName));
        }
        plugin.giveQuest(target, questName, false, ignored -> {});
        return List.of();
    }

    static CommandMessage takeDisabledMessage(
            final NotQuestsPlugin plugin,
            final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null || quest.isTakeEnabled()) {
            return null;
        }
        return CommandMessage.error("<error>Accepting or previewing the quest <highlight>"
                + quest.getIdentifier() + "</highlight> is disabled.");
    }

    static List<CommandMessage> questPreview(
            final NotQuestsPlugin plugin,
            final PlatformPlayer target,
            final String questName) {
        if (target == null || !target.hasPlayer()) {
            return List.of(CommandMessage.error("<error>This command can only be used by a player."));
        }
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(missingQuest(questName));
        }
        final CommandMessage disabled = takeDisabledMessage(plugin, questName);
        if (disabled != null) {
            return List.of(disabled);
        }

        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.emptyLine());
        messages.add(CommandMessage.success("<gray>-----------------------------------"));
        messages.add(CommandMessage.success("<blue>Quest Preview for Quest <highlight>"
                + quest.getDisplayNameOrIdentifier() + "</highlight>:"));
        if (quest.getDescription() == null || quest.getDescription().isBlank()) {
            messages.add(CommandMessage.success(plugin.translate(target,
                    "chat.missing-quest-description",
                    Map.of(),
                    "<unimportant>This quest has no quest description.")));
        } else {
            messages.add(CommandMessage.success("<yellow>Quest description: <gray>" + quest.getDescription()));
        }
        messages.add(CommandMessage.success("<blue>Quest Requirements:"));
        messages.addAll(questPreviewRequirements(plugin, quest, target));
        messages.add(CommandMessage.success("<blue>Quest Rewards:"));
        messages.addAll(questPreviewRewards(plugin, quest, target));
        messages.add(CommandMessage.emptyLine());
        messages.add(CommandMessage.success("<click:run_command:'/nquests take " + quest.getIdentifier()
                + "'><hover:show_text:'<green>Click to accept the Quest <highlight>"
                + quest.getDisplayNameOrIdentifier()
                + "'><green>**[ACCEPT THIS QUEST]</hover></click>"));
        messages.add(CommandMessage.success("<gray>-----------------------------------"));
        return List.copyOf(messages);
    }

    static CommandMessage abortQuest(
            final NotQuestsPlugin plugin,
            final PlatformPlayer target,
            final String questName) {
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("<error>This command can only be used by a player.");
        }
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest != null && !quest.isAbortEnabled()) {
            return CommandMessage.error(plugin.translate(target,
                    "chat.abort-disabled",
                    Map.of("%QUESTNAME%", quest.getIdentifier()),
                    "<error>Aborting the quest <highlight>" + quest.getIdentifier() + "</highlight> is disabled."));
        }
        final ArrayList<String> warnings = new ArrayList<>();
        if (!plugin.failQuest(target, questName, warnings::add)) {
            return warnings.isEmpty()
                    ? CommandMessage.error("<error>Cannot abort quest " + highlight(questName) + ".")
                    : CommandMessage.error(warnings.getLast());
        }
        return CommandMessage.success(plugin.translate(target, "chat.quest-aborted", Map.of(), ""));
    }

    private static List<CommandMessage> questPreviewRequirements(
            final NotQuestsPlugin plugin,
            final Quest quest,
            final PlatformPlayer target) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        int counter = 1;
        for (final Condition requirement : quest.getRequirements()) {
            if (plugin.conditionHidden(target, requirement)) {
                continue;
            }
            final Conditions.Type type = conditionType(plugin, requirement.typeId());
            messages.add(CommandMessage.success("<green>" + counter + ". <yellow>"
                    + (type == null ? requirement.typeId() : type.id())));
            messages.add(CommandMessage.success(conditionDescription(requirement, type, target)));
            counter++;
        }
        return List.copyOf(messages);
    }

    private static List<CommandMessage> questPreviewRewards(
            final NotQuestsPlugin plugin,
            final Quest quest,
            final PlatformPlayer target) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        int counter = 1;
        for (final Action reward : quest.getRewards()) {
            final Actions.Type type = actionType(plugin, reward.typeId());
            final String displayName = reward.getDisplayName() == null ? "" : reward.getDisplayName();
            if (!displayName.isBlank()) {
                messages.add(CommandMessage.success("<green>" + counter + ". <blue>" + displayName + "</green>"));
            } else if (plugin.configuration().hideRewardsWithoutName()) {
                messages.add(CommandMessage.success("<green>" + counter
                        + plugin.translate(target,
                                "gui.reward-hidden-text",
                                Map.of(),
                                ". <blue>Reward hidden</blue>")
                        + "</green>"));
            } else {
                messages.add(CommandMessage.success("<green>" + counter + ". <blue>"
                        + actionDescription(reward, type, target) + "</green>"));
            }
            counter++;
        }
        return List.copyOf(messages);
    }

    private static Conditions.Type conditionType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().conditions().stream()
                .filter(condition -> condition.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static Actions.Type actionType(
            final NotQuestsPlugin plugin,
            final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }

    private static String conditionDescription(
            final Condition condition,
            final Conditions.Type type,
            final PlatformPlayer target) {
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
            return type.descriptionRenderer().render(condition.data(), target);
        } catch (final RuntimeException exception) {
            return type.description();
        }
    }

    private static String actionDescription(
            final Action action,
            final Actions.Type type,
            final PlatformPlayer target) {
        if (action.getDescription() != null && !action.getDescription().isBlank()) {
            return action.getDescription();
        }
        if (type == null) {
            return action.typeId();
        }
        if (type.descriptionRenderer() == null) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
        try {
            return type.descriptionRenderer().render(action.data(), target);
        } catch (final RuntimeException exception) {
            return type.displayName() == null || type.displayName().isBlank()
                    ? type.description()
                    : type.displayName();
        }
    }

    private static CommandMessage missingQuest(final String questName) {
        return CommandMessage.error("<error>Quest " + highlight(questName) + " does not exist!");
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }
}
