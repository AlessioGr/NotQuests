package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.actions.Action;
import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.List;

final class QuestRewardCommands {
    private QuestRewardCommands() {}

    static CommandMessage addQuestReward(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String actionTypeId,
            final String rawArguments) {
        return addQuestReward(plugin, adapter, questName, actionTypeId, rawArguments, null);
    }

    static CommandMessage addQuestReward(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName,
            final String actionTypeId,
            final String rawArguments,
            final PlatformPlayer questPlayer) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        final Actions.Type type = actionType(plugin, actionTypeId);
        if (type == null) {
            return CommandMessage.error("Unknown NotQuests action type: " + highlight(actionTypeId) + ".");
        }
        try {
            quest.addReward(type.id(), Actions.parse(adapter, type, rawArguments, questPlayer));
            plugin.saveConfiguredData();
            return CommandMessage.success("<success>" + type.id()
                    + " Reward successfully added to Quest " + highlight(questName) + "!");
        } catch (final RuntimeException exception) {
            return CommandMessage.error("Cannot add NotQuests reward: " + exception.getMessage());
        }
    }

    static List<CommandMessage> listQuestRewards(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return List.of(missingQuest(questName));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<highlight>Rewards for Quest " + highlight2(questName) + ":"));
        for (final Action reward : quest.getRewards()) {
            final Actions.Type type = actionType(plugin, reward.typeId());
            messages.add(CommandMessage.success("<highlight>" + reward.id() + ".</highlight> <main>"
                    + reward.typeId()));
            messages.add(CommandMessage.success("<unimportant>--</unimportant> <main>"
                    + actionDescription(reward, type, null)));
        }
        return List.copyOf(messages);
    }

    static CommandMessage clearQuestRewards(final NotQuestsPlugin plugin, final String questName) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        quest.clearRewards();
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>All rewards of Quest " + highlight(questName)
                + " have been removed!");
    }

    static CommandMessage removeQuestReward(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId) {
        final Quest quest = plugin.questManager().getQuest(questName);
        if (quest == null) {
            return missingQuest(questName);
        }
        if (!quest.removeReward(rewardId)) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        plugin.saveConfiguredData();
        return CommandMessage.success("<main>The reward with the ID "
                + highlight(rewardId) + " of Quest " + highlight2(questName) + " has been removed!");
    }

    static CommandMessage questRewardInfo(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId) {
        final Action reward = reward(plugin, questName, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        final Actions.Type type = actionType(plugin, reward.typeId());
        return CommandMessage.success("<main>Reward " + highlight(rewardId) + " for Quest "
                + highlight2(questName) + ":\n"
                + "<unimportant>--</unimportant> <main>" + actionDescription(reward, type, null));
    }

    static CommandMessage questRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId) {
        final Action reward = reward(plugin, questName, rewardId);
        if (reward == null) {
            return CommandMessage.error("<error>Invalid reward.");
        }
        if (reward.getDisplayName() == null || reward.getDisplayName().isBlank()) {
            return CommandMessage.success("<main>This reward has no display name set.");
        }
        return CommandMessage.success("<main>Reward display name: <highlight>" + reward.getDisplayName() + "</highlight>");
    }

    static CommandMessage setQuestRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId,
            final String displayName) {
        final Action reward = reward(plugin, questName, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        reward.setDisplayName(displayName);
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Display name successfully added to reward with ID "
                + highlight(rewardId) + "! New display name: " + highlight2(reward.getDisplayName()));
    }

    static CommandMessage removeQuestRewardDisplayName(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId) {
        final Action reward = reward(plugin, questName, rewardId);
        if (reward == null) {
            return CommandMessage.error("Reward with the ID " + highlight(rewardId) + " was not found!");
        }
        reward.setDisplayName("");
        plugin.saveConfiguredData();
        return CommandMessage.success("<success>Display name successfully removed from reward with ID "
                + highlight(rewardId) + "!");
    }

    private static Action reward(
            final NotQuestsPlugin plugin,
            final String questName,
            final int rewardId) {
        final Quest quest = plugin.questManager().getQuest(questName);
        return quest == null ? null : quest.getRewardFromID(rewardId);
    }

    private static Actions.Type actionType(final NotQuestsPlugin plugin, final String id) {
        return plugin.registry().actions().stream()
                .filter(action -> action.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
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

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
