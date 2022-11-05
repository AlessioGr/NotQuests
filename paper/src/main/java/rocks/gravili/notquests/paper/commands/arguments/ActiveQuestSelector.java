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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiFunction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ActiveQuestSelector<C> extends CommandArgument<C, ActiveQuest> {

  protected ActiveQuestSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main,
      String playerContext) {
    super(
        required,
        name,
        new ActiveQuestsParser<>(main, playerContext),
        defaultValue,
        ActiveQuest.class,
        suggestionsProvider);
  }

  public static <C> ActiveQuestSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main, final String playerContext) {
    return new ActiveQuestSelector.Builder<>(name, main, playerContext);
  }

  public static <C> @NonNull CommandArgument<C, ActiveQuest> of(
      final @NonNull String name, final NotQuests main, final String playerContext) {
    return ActiveQuestSelector.<C>newBuilder(name, main, playerContext).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, ActiveQuest> optional(
      final @NonNull String name, final NotQuests main, final String playerContext) {
    return ActiveQuestSelector.<C>newBuilder(name, main, playerContext).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, ActiveQuest> optional(
      final @NonNull String name,
      final @NonNull ActiveQuest activeQuest,
      final NotQuests main,
      final String questContext) {
    return ActiveQuestSelector.<C>newBuilder(name, main, questContext)
        .asOptionalWithDefault(activeQuest.getQuest().getIdentifier())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, ActiveQuest> {
    private final NotQuests main;
    private final String playerContext;

    private Builder(final @NonNull String name, NotQuests main, String playerContext) {
      super(ActiveQuest.class, name);
      this.main = main;
      this.playerContext = playerContext;
    }

    @Override
    public @NonNull CommandArgument<C, ActiveQuest> build() {
      return new ActiveQuestSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main,
          this.playerContext);
    }
  }

  public static final class ActiveQuestsParser<C> implements ArgumentParser<C, ActiveQuest> {

    private final NotQuests main;
    private final String playerContext;

    /** Constructs a new PluginsParser. */
    public ActiveQuestsParser(NotQuests main, String playerContext) {
      this.main = main;
      this.playerContext = playerContext;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> questNames = new java.util.ArrayList<>();

      UUID uuid;
      if (playerContext != null) {
        SinglePlayerSelector singlePlayerSelector = context.get(playerContext);
        if (singlePlayerSelector.getPlayer() != null) {
          uuid = singlePlayerSelector.getPlayer().getUniqueId();
        } else {
          uuid = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector()).getUniqueId();
        }
      } else {
        uuid = ((Player) context.getSender()).getUniqueId();
      }

      final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(uuid);
      if (questPlayer != null) {
        for (Quest quest : main.getQuestManager().getAllQuests()) {
          if (questPlayer.hasAcceptedQuest(quest)) {
            questNames.add(quest.getIdentifier() );
          }
        }
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Active Quest Name]",
              "[...]");

      return questNames;
    }

    @Override
    public @NonNull ArgumentParseResult<ActiveQuest> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(ActiveQuestsParser.class, context));
      }
      final String input = inputQueue.peek();

      UUID uuid;
      Player player = null;
      if (playerContext != null) {
        SinglePlayerSelector singlePlayerSelector = context.get(playerContext);
        if (singlePlayerSelector.getPlayer() != null) {
          player = singlePlayerSelector.getPlayer();
          uuid = player.getUniqueId();
        } else {
          uuid = Bukkit.getOfflinePlayer(singlePlayerSelector.getSelector()).getUniqueId();
        }
      } else {
        player = ((Player) context.getSender());
        uuid = player.getUniqueId();
      }

      final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(uuid);
      if (questPlayer == null) {

        return ArgumentParseResult.failure(
            new IllegalArgumentException(
                main.getLanguageManager()
                    .getString("chat.quest-not-active-error", player)
                    .replace("%QUESTNAME%", input)));
      }

      final Quest foundQuest = main.getQuestManager().getQuest(input);
      if (foundQuest == null) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException(
                main.getLanguageManager()
                    .getString("chat.quest-not-active-error", player, questPlayer)
                    .replace("%QUESTNAME%", input)));
      }
      inputQueue.remove();

      for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
        if (activeQuest.getQuest().equals(foundQuest)) {
          return ArgumentParseResult.success(activeQuest);
        }
      }

      return ArgumentParseResult.failure(
          new IllegalArgumentException(
              main.getLanguageManager()
                  .getString("chat.quest-not-active-error", player, questPlayer)
                  .replace("%QUESTNAME%", input)));
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
