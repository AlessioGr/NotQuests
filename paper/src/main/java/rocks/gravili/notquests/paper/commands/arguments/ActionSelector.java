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
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.Action;

public class ActionSelector<C> extends CommandArgument<C, Action> {

  protected ActionSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main) {
    super(
        required, name, new ActionsParser<>(main), defaultValue, Action.class, suggestionsProvider);
  }

  public static <C> ActionSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new ActionSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, Action> of(
      final @NonNull String name, final NotQuests main) {
    return ActionSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Action> optional(
      final @NonNull String name, final NotQuests main) {
    return ActionSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Action> optional(
      final @NonNull String name, final @NonNull Action action, final NotQuests main) {
    return ActionSelector.<C>newBuilder(name, main)
        .asOptionalWithDefault(action.getActionName())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Action> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(Action.class, name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, Action> build() {
      return new ActionSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class ActionsParser<C> implements ArgumentParser<C, Action> {

    private final NotQuests main;

    /** Constructs a new PluginsParser. */
    public ActionsParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> actionNames =
          new java.util.ArrayList<>(
              main.getActionsYMLManager().getActionsAndIdentifiers().keySet());
      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Action Name]",
              "[...]");

      return actionNames;
    }

    @Override
    public @NonNull ArgumentParseResult<Action> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(ActionsParser.class, context));
      }
      final String input = inputQueue.peek();
      final Action foundAction = main.getActionsYMLManager().getAction(input);
      inputQueue.remove();

      if (foundAction == null) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException("Action '" + input + "' does not exist!"));
      }

      return ArgumentParseResult.success(foundAction);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
