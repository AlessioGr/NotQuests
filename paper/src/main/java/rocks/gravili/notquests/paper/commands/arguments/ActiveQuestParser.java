/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.commands.arguments;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.CaptionVariable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.exception.parsing.ParserException;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActiveQuestParser<C> implements ArgumentParser<C, ActiveQuest> {
    private final NotQuests main;

    protected ActiveQuestParser(NotQuests main) {
        this.main = main;
    }

    public static <C> @NonNull ParserDescriptor<C, ActiveQuest> activeQuestParser(final NotQuests main) {
        return ParserDescriptor.of(new ActiveQuestParser<>(main), ActiveQuest.class);
    }

    /**
     * Resolves the target player for this argument: an explicit "player" argument (admin commands)
     * if present, otherwise the command sender (user commands like /nq abort, where there is no
     * "player" argument and the sender is the player).
     */
    private @Nullable OfflinePlayer resolveTargetPlayer(final @NonNull CommandContext<C> context) {
        if (context.contains("player")) {
            return context.get("player");
        }
        if (context.sender() instanceof Player player) {
            return player;
        }
        return null;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull ActiveQuest> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        final OfflinePlayer offlinePlayer = resolveTargetPlayer(commandContext);
        if (offlinePlayer == null || commandInput.isEmpty()) {
            return ArgumentParseResult.failure(new QuestParseException(commandContext));
        }
        final String input = commandInput.readString();
        final QuestPlayer activeQuestPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
        final ActiveQuest activeQuest = activeQuestPlayer == null ? null : activeQuestPlayer.getActiveQuest(main.getQuestManager().getQuest(input));
        if (activeQuest == null) {
            if (commandContext.sender() instanceof Player player) {
                return ArgumentParseResult.failure(new IllegalArgumentException(main.getLanguageManager().getString("chat.quest-does-not-exist", player).replace("%QUESTNAME%", input)));
            } else {
                return ArgumentParseResult.failure(new IllegalArgumentException(main.getLanguageManager().getString("chat.quest-does-not-exist", (QuestPlayer) null).replace("%QUESTNAME%", input)));
            }
        }

        // NOTE: ActiveQuestParser resolves an already-active quest for abort/fail/complete/progress.
        // It must NOT enforce the take/preview "take-disabled" rule (that belongs to QuestParser),
        // otherwise /nq abort, /nq progress, /qa failQuest, /qa completeQuest get blocked whenever
        // quest-preview-GUI is enabled or the sender is the console.
        return ArgumentParseResult.success(activeQuest);
    }


    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return (context, input) -> {
            final List<Suggestion> questNames = new ArrayList<>();
            final OfflinePlayer offlinePlayer = resolveTargetPlayer(context);
            final QuestPlayer activeQuestPlayer = offlinePlayer == null
                    ? null
                    : main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
            if (activeQuestPlayer != null) {
                for (ActiveQuest quest : activeQuestPlayer.getActiveQuests()) {
                    questNames.add(Suggestion.suggestion(quest.getQuestIdentifier()));
                }
            }

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), context.rawInput().input().split(" "), "[Quest Name]", "[...]");
            return CompletableFuture.completedFuture(questNames);
        };
    }

    public static final class QuestParseException extends ParserException {

        public QuestParseException(@Nullable Throwable cause, @NonNull CommandContext<?> context, @NonNull CaptionVariable... captionVariables) {
            super(cause, ActiveQuestParser.class, context, Caption.of(""), captionVariables);
        }

        public QuestParseException(@NonNull CommandContext<?> context, @NonNull CaptionVariable... captionVariables) {
            super(ActiveQuestParser.class, context, Caption.of(""), captionVariables);
        }
    }
}