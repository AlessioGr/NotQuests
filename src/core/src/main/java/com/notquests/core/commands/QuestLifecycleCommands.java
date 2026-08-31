package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.QuestPlayer.CompletedQuest;
import com.notquests.core.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

final class QuestLifecycleCommands {
    private QuestLifecycleCommands() {}

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommandsBeforeProgress(
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
        commands.add(root.literal("create", NQDescription.of("Creates a new quest."))
                .required(
                        "questName",
                        NQArgumentType.word("quest name"),
                        NQDescription.of("Unique quest identifier."),
                        (context, input) -> List.of("<Enter new Quest Name>"))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>builder(
                                NQFlags.CATEGORY.name(), NQFlags.CATEGORY.description())
                        .withArgument(NQArgumentType.category())
                        .build())
                .commandDescription(NQDescription.of("Creates a new quest."))
                .handler(context -> List.of(createQuest(
                        plugin,
                        context.argument("questName"),
                        context.flag(NQFlags.CATEGORY.name()))))
                .registration());
        commands.add(root.literal("delete", NQDescription.of("Deletes an existing quest."))
                .required(
                        "questName",
                        NQArgumentType.quest(),
                        NQDescription.of("Unique quest identifier."))
                .commandDescription(NQDescription.of("Deletes an existing quest."))
                .handler(context -> List.of(deleteQuest(plugin, context.argument("questName"))))
                .registration());
        commands.add(root.literal("give", NQDescription.of("Gives a quest to a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .required(
                        "quest",
                        NQArgumentType.quest(),
                        NQDescription.of("Quest the player should start."))
                .flag(NQFlag.<NQArgumentType, NQSuggestionProvider<NQCommandContext>>presence(
                        NQFlags.FORCE.name(), NQFlags.FORCE.description()))
                .commandDescription(NQDescription.of("Gives a quest to a player."))
                .handler(context -> List.of(giveQuest(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("quest"),
                        context.flagPresent(NQFlags.FORCE.name()))))
                .registration());
        commands.add(root.literal(
                        "completeQuest",
                        NQDescription.of("Completes one active quest for a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .required(
                        "activeQuest",
                        NQArgumentType.activeQuest(),
                        NQDescription.of("Active quest on the selected player."))
                .commandDescription(NQDescription.of("Completes an active quest for a player."))
                .handler(context -> List.of(completeQuest(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("activeQuest"))))
                .registration());
        commands.add(root.literal("failQuest", NQDescription.of("Fails one active quest for a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .required(
                        "activeQuest",
                        NQArgumentType.activeQuest(),
                        NQDescription.of("Active quest on the selected player."))
                .commandDescription(NQDescription.of("Fails an active quest for a player."))
                .handler(context -> List.of(failQuest(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("activeQuest"))))
                .registration());
        commands.add(root.literal("activeQuests", NQDescription.of("Shows active quests for a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .commandDescription(NQDescription.of("Shows active quests for a player."))
                .handler(context -> activeQuests(plugin, adapter, context.argument("player")))
                .registration());
        commands.add(root.literal(
                        "completedQuests",
                        NQDescription.of("Shows completed quests for a player."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .commandDescription(NQDescription.of("Shows completed quests for a player."))
                .handler(context -> completedQuests(plugin, adapter, context.argument("player")))
                .registration());
        return List.copyOf(commands);
    }

    static List<NQCommandRegistration<
                    NQArgumentType,
                    NQFlag<NQArgumentType, NQSuggestionProvider<NQCommandContext>>,
                    NQSuggestionProvider<NQCommandContext>,
                    NQCommandHandler>>
            adminCommandsAfterProgress(
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
        commands.add(root.literal("triggerObjective", NQDescription.of("Adds progress to matching TriggerCommand objectives for a player."))
                .required(
                        "trigger-name",
                        NQArgumentType.triggerObjectiveName(),
                        NQDescription.of("Name configured on the TriggerCommand objective."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .commandDescription(NQDescription.of("Manually adds one progress point to unlocked TriggerCommand objectives with the selected trigger name."))
                .handler(context -> triggerObjective(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("trigger-name")))
                .registration());
        commands.add(root.literal(
                        "resetAndFailQuestForAllPlayers",
                        NQDescription.of("Fails a quest for every loaded player and removes completed progress."))
                .required(
                        "quest",
                        NQArgumentType.quest(),
                        NQDescription.of("Quest the player should start."))
                .commandDescription(NQDescription.of("Fails the quest for all loaded players, removes completed entries, and resets progress."))
                .handler(context -> failQuestForAllPlayers(plugin, adapter, context.argument("quest")))
                .registration());
        commands.add(root.literal(
                        "resetAndRemoveQuest",
                        NQDescription.of("Removes a quest from player progress data."))
                .required(
                        "player",
                        NQArgumentType.player(),
                        NQDescription.of("Player affected by this quest command."))
                .required(
                        "quest",
                        NQArgumentType.quest(),
                        NQDescription.of("Quest the player should start."))
                .commandDescription(NQDescription.of("Removes a quest from a player or all loaded players."))
                .handler(context -> resetAndRemoveQuest(
                        plugin,
                        adapter,
                        context.argument("player"),
                        context.argument("quest")))
                .registration());
        commands.add(root.literal(
                        "resetAndRemoveQuest",
                        NQDescription.of("Removes a quest from player progress data."))
                .literal("all", NQDescription.of("Applies this operation to all matching players or entries."))
                .required(
                        "quest",
                        NQArgumentType.quest(),
                        NQDescription.of("Quest the player should start."))
                .commandDescription(NQDescription.of("Removes a quest from a player or all loaded players."))
                .handler(context -> removeQuestForAllPlayers(plugin, adapter, context.argument("quest")))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage createQuest(
            final NotQuestsPlugin plugin,
            final String questName,
            final String categoryName) {
        return categoryName == null || categoryName.isBlank()
                ? plugin.createQuest(questName)
                : plugin.createQuest(questName, categoryName);
    }

    static CommandMessage deleteQuest(final NotQuestsPlugin plugin, final String questName) {
        return plugin.deleteQuest(questName);
    }

    static CommandMessage giveQuest(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String questName,
            final boolean forceGive) {
        final PlatformPlayer target = targetPlayer(adapter, playerName);
        return giveQuest(plugin, target, questName, forceGive);
    }

    static CommandMessage giveQuest(
            final NotQuestsPlugin plugin,
            final PlatformPlayer target,
            final String questName,
            final boolean forceGive) {
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("<error>This command can only be used by a player.");
        }
        final ArrayList<String> warnings = new ArrayList<>();
        if (plugin.giveQuest(target, questName, forceGive, warnings::add)) {
            if (forceGive) {
                return CommandMessage.success(plugin.translate(target,
                        "chat.force-add-active-quest-accepted",
                        Map.of(),
                        "<success>Successfully accepted the quest (Forced)."));
            }
            return CommandMessage.success("<main>accepted");
        }
        if (!warnings.isEmpty()) {
            return CommandMessage.error(warnings.getLast());
        }
        return CommandMessage.error("<error>Quest " + highlight(questName)
                + " could not be given to player " + highlight2(target.playerIdentifier()) + ".");
    }

    static CommandMessage completeQuest(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String questName) {
        final PlatformPlayer target = targetPlayer(adapter, playerName);
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("Player " + highlight(playerName) + " is not online.");
        }
        if (!plugin.completeQuest(target, questName, ignored -> {})) {
            return CommandMessage.error("<error>Player " + highlight(playerDisplayName(target))
                    + " seems to not have accepted any quests!");
        }
        return CommandMessage.success("<success>The active quest " + highlight(questName)
                + " has been completed for player " + highlight2(playerDisplayName(target)) + "!");
    }

    static CommandMessage failQuest(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String questName) {
        final PlatformPlayer target = targetPlayer(adapter, playerName);
        if (target == null || !target.hasPlayer()) {
            return CommandMessage.error("Player " + highlight(playerName) + " is not online.");
        }
        if (!plugin.failQuest(target, questName, ignored -> {})) {
            return CommandMessage.error("<error>Player " + highlight(playerDisplayName(target))
                    + " seems to not have accepted any quests!");
        }
        return CommandMessage.success("<main>The active quest " + highlight(questName)
                + " has been failed for player " + highlight2(playerDisplayName(target)) + "!");
    }

    static List<CommandMessage> activeQuests(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final QuestPlayer questPlayer = plugin.activeQuestPlayer(target.identifier());
        final List<String> names = questPlayer.getActiveQuestIdentifiers().stream().sorted().toList();
        if (names.isEmpty()) {
            return List.of(CommandMessage.error("<error>Seems like the player "
                    + highlight(target.displayName()) + " " + target.onlineStatus()
                    + " did not accept any active quests."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<main>Active quests of player "
                + highlight(target.displayName()) + " " + target.onlineStatus() + ":"));
        int counter = 1;
        for (final String name : names) {
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <main>" + name));
            counter++;
        }
        messages.add(CommandMessage.success("<unimportant>Total active quests: <highlight2>"
                + names.size() + "</highlight2>."));
        return List.copyOf(messages);
    }

    static List<CommandMessage> completedQuests(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final List<CompletedQuest> completedQuests =
                plugin.activeQuestPlayer(target.identifier())
                        .getCompletedQuests()
                        .stream()
                        .sorted(Comparator.comparing(CompletedQuest::questIdentifier))
                        .toList();
        if (completedQuests.isEmpty()) {
            return List.of(CommandMessage.error("<error>Seems like the player "
                    + highlight(target.displayName()) + " " + target.onlineStatus()
                    + " never completed any quests."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<main>Completed quests of player "
                + highlight(target.displayName()) + " " + target.onlineStatus() + ":"));
        int counter = 1;
        for (final CompletedQuest completedQuest : completedQuests) {
            messages.add(CommandMessage.success("<highlight>" + counter + ".</highlight> <highlight2>"
                    + completedQuest.questIdentifier()
                    + "</highlight2> <main>Completed: </main><highlight2>"
                    + new Date(completedQuest.timeCompleted())
                    + "</highlight2>"));
            counter++;
        }
        messages.add(CommandMessage.success("<unimportant>Total completed quests: <highlight2>"
                + completedQuests.size() + "</highlight2>."));
        return List.copyOf(messages);
    }

    static List<CommandMessage> resetAndRemoveQuest(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String questName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.addAll(resetAndRemoveQuestMessages(
                withPlayerName(adapter, plugin.resetAndRemoveQuest(target.identifier(), questName), target.displayName()),
                questName));
        messages.add(CommandMessage.success("<success>Finished resetting/removing Quest "
                + highlight(questName) + " for player " + highlight2(target.displayName()) + "."));
        return List.copyOf(messages);
    }

    static List<CommandMessage> failQuestForAllPlayers(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        for (final NotQuestsPlugin.QuestReset result : plugin.resetAndFailQuestForAllPlayers(questName)) {
            messages.addAll(resetAndFailQuestMessages(withPlayerName(adapter, result, result.playerIdentifier()), questName));
        }
        messages.add(CommandMessage.success("<success>Finished failing Quest "
                + highlight(questName) + " for all loaded players."));
        return List.copyOf(messages);
    }

    static List<CommandMessage> removeQuestForAllPlayers(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String questName) {
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        for (final NotQuestsPlugin.QuestReset result : plugin.resetAndRemoveQuestForAllPlayers(questName)) {
            messages.addAll(resetAndRemoveQuestMessages(withPlayerName(adapter, result, result.playerIdentifier()), questName));
        }
        messages.add(CommandMessage.success("<success>Finished resetting/removing Quest "
                + highlight(questName) + " for all loaded players."));
        return List.copyOf(messages);
    }

    static List<CommandMessage> triggerObjective(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final String triggerName) {
        final PlatformPlayer target = adapter.onlineQuestPlayer(playerName);
        if (target == null || !target.hasPlayer()) {
            return List.of(CommandMessage.error("Player " + highlight(playerName) + " is not online."));
        }
        plugin.triggerCommandObjectiveProgress(target, triggerName);
        return List.of();
    }

    private static List<CommandMessage> resetAndRemoveQuestMessages(
            final NotQuestsPlugin.QuestReset result,
            final String questName) {
        if (!result.playerFound()) {
            return List.of(CommandMessage.error("<error>Error: PlatformPlayer of Player "
                    + highlight(result.playerIdentifier()) + " not found."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        if (result.activeRemoved()) {
            messages.add(CommandMessage.success("<success>Removed the quest as an active quest for the player with the UUID "
                    + highlight(result.playerIdentifier()) + " and name " + highlight2(result.playerName()) + "."));
        }
        for (int i = 0; i < result.completedRemoved(); i++) {
            messages.add(CommandMessage.success("<success>Removed the quest as a completed quest for the player with the UUID "
                    + highlight(result.playerIdentifier()) + " and name " + highlight2(result.playerName()) + "."));
        }
        return List.copyOf(messages);
    }

    private static List<CommandMessage> resetAndFailQuestMessages(
            final NotQuestsPlugin.QuestReset result,
            final String questName) {
        if (!result.playerFound()) {
            return List.of(CommandMessage.error("<error>Error: PlatformPlayer of Player "
                    + highlight(result.playerIdentifier()) + " not found."));
        }
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        if (result.activeFailed()) {
            messages.add(CommandMessage.success("<success>Failed the quest as an active quest for the player with the UUID "
                    + highlight(result.playerIdentifier()) + " and name " + highlight2(result.playerName()) + "."));
        }
        for (int i = 0; i < result.completedRemoved(); i++) {
            messages.add(CommandMessage.success("<success>Removed the quest as a completed quest for the player with the UUID "
                    + highlight(result.playerIdentifier()) + " and name " + highlight2(result.playerName()) + "."));
        }
        return List.copyOf(messages);
    }

    private static NotQuestsPlugin.QuestReset withPlayerName(
            final NotQuestsAdapter adapter,
            final NotQuestsPlugin.QuestReset result,
            final String lookupName) {
        if (result == null || !result.playerFound()) {
            return result;
        }
        final PlatformPlayer onlinePlayer = targetPlayer(adapter, lookupName);
        final String displayName = playerDisplayName(onlinePlayer);
        if (displayName.isBlank() || displayName.equals(result.playerName())) {
            return result;
        }
        return NotQuestsPlugin.QuestReset.changed(
                result.playerIdentifier(),
                displayName,
                result.activeRemoved(),
                result.activeFailed(),
                result.completedRemoved());
    }

    private static PlatformPlayer targetPlayer(final NotQuestsAdapter adapter, final String playerName) {
        if (adapter == null || playerName == null || playerName.isBlank()) {
            return null;
        }
        final PlatformPlayer player = adapter.onlineQuestPlayer(playerName);
        return player != null && player.hasPlayer() ? player : null;
    }

    private static String playerDisplayName(final PlatformPlayer questPlayer) {
        if (questPlayer == null) {
            return "";
        }
        return questPlayer.playerName() == null || questPlayer.playerName().isBlank()
                ? questPlayer.playerIdentifier()
                : questPlayer.playerName();
    }

    private static String highlight(final Object value) {
        return "<highlight>" + value + "</highlight>";
    }

    private static String highlight2(final Object value) {
        return "<highlight2>" + value + "</highlight2>";
    }
}
