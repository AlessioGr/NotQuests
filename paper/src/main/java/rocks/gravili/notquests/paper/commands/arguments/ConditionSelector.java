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
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ConditionSelector<C> extends CommandArgument<C, Condition> {

  protected ConditionSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main) {
    super(
        required,
        name,
        new ConditionsParser<>(main),
        defaultValue,
        Condition.class,
        suggestionsProvider);
  }

  public static <C> ConditionSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new ConditionSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, Condition> of(
      final @NonNull String name, final NotQuests main) {
    return ConditionSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Condition> optional(
      final @NonNull String name, final NotQuests main) {
    return ConditionSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Condition> optional(
      final @NonNull String name, final @NonNull Condition Condition, final NotQuests main) {
    return ConditionSelector.<C>newBuilder(name, main)
        .asOptionalWithDefault(Condition.getConditionName())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Condition> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(Condition.class, name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, Condition> build() {
      return new ConditionSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class ConditionsParser<C> implements ArgumentParser<C, Condition> {

    private final NotQuests main;

    /** Constructs a new PluginsParser. */
    public ConditionsParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> conditionNames =
          new java.util.ArrayList<>(
              main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet());
      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Condition Name]",
              "[...]");

      return conditionNames;
    }

    @Override
    public @NonNull ArgumentParseResult<Condition> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(ConditionsParser.class, context));
      }
      final String input = inputQueue.peek();
      final Condition foundCondition = main.getConditionsYMLManager().getCondition(input);
      inputQueue.remove();

      if (foundCondition == null) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException("Condition '" + input + "' does not exist!"));
      }

      return ArgumentParseResult.success(foundCondition);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
