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
import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class VariableSelector<C> extends CommandArgument<C, Variable<?>> { // 0 = Quest

  protected VariableSelector(
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
        new VariableSelector.VariableParser<>(main),
        defaultValue,
        new TypeToken<Variable<?>>() {},
        suggestionsProvider);
  }

  public static <C> VariableSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new VariableSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, Variable<?>> of(
      final @NonNull String name, final NotQuests main) {
    return VariableSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Variable<?>> optional(
      final @NonNull String name, final NotQuests main) {
    return VariableSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Variable<?>> optional(
      final @NonNull String name, final @NonNull Variable<?> Variable, final NotQuests main) {
    return VariableSelector.<C>newBuilder(name, main).asOptionalWithDefault("").build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Variable<?>> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(new TypeToken<Variable<?>>() {}, name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, Variable<?>> build() {
      return new VariableSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class VariableParser<C> implements ArgumentParser<C, Variable<?>> {

    private final NotQuests main;

    /** Constructs a new PluginsParser. */
    public VariableParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {

      List<String> completions =
          new java.util.ArrayList<>(main.getVariablesManager().getVariableIdentifiers());

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Variable Name]",
              "[...]");

      return completions;
    }

    @Override
    public @NonNull ArgumentParseResult<Variable<?>> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(VariableParser.class, context));
      }
      final String input = inputQueue.peek();
      inputQueue.remove();

      // ((Player)context.getSender()).sendMessage("Q: " +  inputQueue.peek());

      for (final String variableString : main.getVariablesManager().getVariableIdentifiers()) {
        if (input.equalsIgnoreCase(variableString)) {
          Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
          if (variable == null) {
            return ArgumentParseResult.failure(
                new IllegalArgumentException("Variable '" + input + "' was not found."));
          }
          return ArgumentParseResult.success(variable);
        }
      }
      return ArgumentParseResult.failure(
          new IllegalArgumentException("Variable '" + input + "' was not found."));
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
