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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code ActiveQuestParser}: resolves a currently-{@link ActiveQuest} for a
 * target player. The target is the explicit {@code player} argument (admin commands) when present,
 * otherwise the command sender (user commands like {@code /nq abort}).
 *
 * <p>NOTE: the Cloud parser read the target player from the command context / sender inside
 * {@code parse}. Brigadier does not hand {@code convert} a context, so resolution is delegated to
 * {@link #convert(CommandContext, String)}.
 */
public final class ActiveQuestArgument extends NQArgumentType<ActiveQuest> {
    private final NotQuests main;

    public ActiveQuestArgument(final NotQuests main) {
        this.main = main;
    }

    public static ActiveQuestArgument activeQuestArgument(final NotQuests main) {
        return new ActiveQuestArgument(main);
    }

    /**
     * Resolves the target player for this argument: an explicit "player" argument (admin commands) if
     * present, otherwise the command sender (user commands like /nq abort, where there is no "player"
     * argument and the sender is the player).
     */
    private @Nullable OfflinePlayer resolveTargetPlayer(final @NonNull CommandContext<?> context) {
        try {
            return (OfflinePlayer) context.getArgument("player", Object.class);
        } catch (final IllegalArgumentException notAnArgument) {
            if (context.getSource() instanceof CommandSourceStack sourceStack
                    && sourceStack.getSender() instanceof Player player) {
                return player;
            }
            return null;
        }
    }

    @Override
    public ActiveQuest convert(final String input) throws CommandSyntaxException {
        return convert(input, null);
    }

    // Paper hands CustomArgumentType the command source, so we can resolve the sender's active quest
    // here (covers /nq abort, /nq progress and admin-on-self). The explicit "player" target (a prior
    // positional arg) isn't reachable at parse time and falls back to the sender.
    @Override
    public <S> ActiveQuest convert(final String input, final S source) throws CommandSyntaxException {
        OfflinePlayer offlinePlayer = null;
        if (source instanceof CommandSourceStack sourceStack
                && sourceStack.getSender() instanceof Player player) {
            offlinePlayer = player;
        }
        final QuestPlayer activeQuestPlayer =
                offlinePlayer == null
                        ? null
                        : main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
        final ActiveQuest activeQuest =
                activeQuestPlayer == null
                        ? null
                        : activeQuestPlayer.getActiveQuest(main.getQuestManager().getQuest(input));
        if (activeQuest == null) {
            throw fail(
                    main.getLanguageManager()
                            .getString("chat.quest-does-not-exist", (QuestPlayer) null)
                            .replace("%QUESTNAME%", input));
        }
        return activeQuest;
    }

    public ActiveQuest convert(final CommandContext<?> context, final String input) throws CommandSyntaxException {
        final OfflinePlayer offlinePlayer = resolveTargetPlayer(context);
        final QuestPlayer activeQuestPlayer =
                offlinePlayer == null
                        ? null
                        : main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
        final ActiveQuest activeQuest =
                activeQuestPlayer == null
                        ? null
                        : activeQuestPlayer.getActiveQuest(main.getQuestManager().getQuest(input));
        if (activeQuest == null) {
            throw fail(
                    main.getLanguageManager()
                            .getString("chat.quest-does-not-exist", (QuestPlayer) null)
                            .replace("%QUESTNAME%", input));
        }

        // NOTE: ActiveQuestParser resolves an already-active quest for abort/fail/complete/progress.
        // It must NOT enforce the take/preview "take-disabled" rule (that belongs to QuestParser),
        // otherwise /nq abort, /nq progress, /qa failQuest, /qa completeQuest get blocked whenever
        // quest-preview-GUI is enabled or the sender is the console.
        return activeQuest;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> questNames = new ArrayList<>();
        final OfflinePlayer offlinePlayer = resolveTargetPlayer(context);
        final QuestPlayer activeQuestPlayer =
                offlinePlayer == null
                        ? null
                        : main.getQuestPlayerManager().getActiveQuestPlayer(offlinePlayer.getUniqueId());
        if (activeQuestPlayer != null) {
            for (final ActiveQuest quest : activeQuestPlayer.getActiveQuests()) {
                questNames.add(quest.getQuestIdentifier());
            }
        }
        return questNames;
    }
}
