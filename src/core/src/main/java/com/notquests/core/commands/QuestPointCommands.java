package com.notquests.core.commands;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.commands.framework.*;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class QuestPointCommands {
    private QuestPointCommands() {}

    static CommandMessage userQuestPoints(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final QuestPlayer questPlayer = target.questPlayer();
        if (questPlayer == null) {
            return CommandMessage.error(plugin.translate(target.platformPlayer(),
                    "chat.questpoints.none",
                    Map.of(),
                    "<error>Seems like you don't have any quest points!"));
        }
        return CommandMessage.success(plugin.translate(target.platformPlayer(),
                "chat.questpoints.query",
                Map.of("%QUESTPOINTS%", String.valueOf(questPlayer.getQuestPoints())),
                "<main>You currently have <highlight>%QUESTPOINTS%</highlight> quest points."));
    }

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
                questPoints = root.literal(
                        "questpoints",
                        NQDescription.of("Shows or changes a player's quest points."))
                        .required(
                                "player",
                                NQArgumentType.player(),
                                NQDescription.of("Player whose quest points should be shown or changed."));
        commands.add(questPoints.literal(
                        "show",
                        NQDescription.of("Shows the selected player's current quest points."))
                .commandDescription(NQDescription.of("Shows a player's quest points."))
                .handler(context -> List.of(questPoints(plugin, adapter, context.argument("player"))))
                .registration());
        commands.add(questPoints.literal(
                        "add",
                        NQDescription.of("Adds quest points to the selected player."))
                .required(
                        "amount",
                        NQArgumentType.integer("quest point amount"),
                        NQDescription.of("Quest point amount used by this command."))
                .commandDescription(NQDescription.of("Adds quest points to a player."))
                .handler(context -> addQuestPointsMessages(
                        plugin,
                        adapter,
                        context.argument("player"),
                        integer(context.argument("amount"))))
                .registration());
        commands.add(questPoints.literal(
                        "remove",
                        NQDescription.of("Removes quest points from the selected player."))
                .required(
                        "amount",
                        NQArgumentType.integer("quest point amount"),
                        NQDescription.of("Quest point amount used by this command."))
                .commandDescription(NQDescription.of("Removes quest points from a player."))
                .handler(context -> removeQuestPointsMessages(
                        plugin,
                        adapter,
                        context.argument("player"),
                        integer(context.argument("amount"))))
                .registration());
        commands.add(questPoints.literal(
                        "set",
                        NQDescription.of("Sets the selected player's quest points to an exact amount."))
                .required(
                        "amount",
                        NQArgumentType.integer("quest point amount"),
                        NQDescription.of("Quest point amount used by this command."))
                .commandDescription(NQDescription.of("Sets a player's quest points."))
                .handler(context -> setQuestPointsMessages(
                        plugin,
                        adapter,
                        context.argument("player"),
                        integer(context.argument("amount"))))
                .registration());
        return List.copyOf(commands);
    }

    static CommandMessage questPoints(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final QuestPlayer questPlayer = target.questPlayer();
        if (questPlayer == null) {
            return CommandMessage.error("<error>Seems like the player "
                    + CommandSupport.highlight(playerName)
                    + " "
                    + CommandSupport.playerOnlineStatus(plugin, adapter, playerName)
                    + " does not have any quest points!");
        }
        return CommandMessage.success("<main>Quest points for player "
                + CommandSupport.highlight(playerName)
                + " "
                + CommandSupport.playerOnlineStatus(plugin, adapter, playerName)
                + ": "
                + CommandSupport.highlight2(questPlayer.getQuestPoints()));
    }

    static CommandMessage setQuestPoints(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        return setQuestPointsMessages(plugin, adapter, playerName, amount).getFirst();
    }

    private static List<CommandMessage> setQuestPointsMessages(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        return questPointsMutationMessages(plugin, adapter, playerName, amount);
    }

    static CommandMessage addQuestPoints(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        return addQuestPointsMessages(plugin, adapter, playerName, amount).getFirst();
    }

    private static List<CommandMessage> addQuestPointsMessages(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final long next = target.questPlayer().getQuestPoints() + amount;
        return questPointsMutationMessages(plugin, adapter, playerName, next);
    }

    static CommandMessage removeQuestPoints(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        return removeQuestPointsMessages(plugin, adapter, playerName, amount).getFirst();
    }

    private static List<CommandMessage> removeQuestPointsMessages(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final long old = target.questPlayer().getQuestPoints();
        return questPointsMutationMessages(plugin, adapter, playerName, old - amount);
    }

    private static List<CommandMessage> questPointsMutationMessages(
            final NotQuestsPlugin plugin,
            final NotQuestsAdapter adapter,
            final String playerName,
            final long amount) {
        final PlayerTarget target = CommandSupport.playerTarget(plugin, adapter, playerName);
        final QuestPlayer questPlayer = target.questPlayer();
        final long oldQuestPoints = questPlayer.getQuestPoints();
        if (!plugin.setQuestPoints(questPlayer, target.platformPlayer(), amount)) {
            return List.of(CommandMessage.error("<error>Quest points for player "
                    + CommandSupport.highlight(playerName)
                    + " "
                    + CommandSupport.playerOnlineStatus(plugin, adapter, playerName)
                    + " could not be changed."));
        }
        final long storedQuestPoints = questPlayer.getQuestPoints();
        final ArrayList<CommandMessage> messages = new ArrayList<>();
        messages.add(CommandMessage.success("<main>Quest points for player "
                + CommandSupport.highlight(playerName)
                + " "
                + CommandSupport.playerOnlineStatus(plugin, adapter, playerName)
                + " have been set from "
                + CommandSupport.unimportant(oldQuestPoints)
                + " to "
                + CommandSupport.highlight2(storedQuestPoints)
                + "."));
        if (adapter.onlineQuestPlayer(playerName) == null) {
            messages.add(questPoints(plugin, adapter, playerName));
        }
        plugin.saveData();
        return List.copyOf(messages);
    }

    private static int integer(final String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ignored) {
            return 0;
        }
    }
}
