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
import rocks.gravili.notquests.paper.managers.tags.Tag;
import rocks.gravili.notquests.paper.managers.tags.TagType;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

/*
 * This Argument is currently unused and might not be needed at all. It was meant for the tag system.
 */
public final class TagValueArgument<C> extends CommandArgument<C, Object> {

  private TagValueArgument(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main,
      String contextTagName) {
    super(
        required,
        name,
        new TagValueArgument.TagParser<>(main, contextTagName),
        defaultValue,
        Object.class,
        suggestionsProvider,
        defaultDescription);
  }

  /**
   * Create a new {@link TagValueArgument.Builder}.
   *
   * @param name Name of the argument
   * @param <C> Command sender type
   * @return Created builder
   */
  public static <C> TagValueArgument.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main, final String contextTagName) {
    return new TagValueArgument.Builder<>(name, main, contextTagName);
  }

  /**
   * @param name Argument name
   * @param <C> Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, Object> of(
      final @NonNull String name, final NotQuests main, final String contextTagName) {
    return TagValueArgument.<C>newBuilder(name, main, contextTagName).asRequired().build();
  }

  /**
   * @param name Argument name
   * @param <C> Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, Object> optional(
      final @NonNull String name, final NotQuests main, final String contextTagName) {
    return TagValueArgument.<C>newBuilder(name, main, contextTagName).asOptional().build();
  }

  /**
   * Create a new required {@link TagValueArgument} with the specified default value.
   *
   * @param name Argument name
   * @param defaultNum Default value
   * @param <C> Command sender type
   * @return Created argument
   */
  public static <C> @NonNull CommandArgument<C, Object> optional(
      final @NonNull String name,
      final int defaultNum,
      final NotQuests main,
      final String contextTagName) {
    return TagValueArgument.<C>newBuilder(name, main, contextTagName)
        .asOptionalWithDefault(defaultNum)
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Object> {

    private final NotQuests main;
    private final String contextTagName;

    private Builder(final @NonNull String name, final NotQuests main, final String contextTagName) {
      super(Object.class, name);
      this.main = main;
      this.contextTagName = contextTagName;
    }

    /**
     * Sets the command argument to be optional, with the specified default value.
     *
     * @param defaultValue default value
     * @return this builder
     * @see CommandArgument.Builder#asOptionalWithDefault(String)
     * @since 1.5.0
     */
    public TagValueArgument.Builder<C> asOptionalWithDefault(final int defaultValue) {
      return (TagValueArgument.Builder<C>) this.asOptionalWithDefault(defaultValue);
    }

    @NotNull
    @Override
    public TagValueArgument<C> build() {
      return new TagValueArgument<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          main,
          contextTagName);
    }
  }

  public static final class TagParser<C> implements ArgumentParser<C, Object> {
    private final NotQuests main;
    private final String contextTagName;

    /** Construct a new String parser */
    public TagParser(final NotQuests main, final String contextTagName) {
      this.main = main;
      this.contextTagName = contextTagName;
    }

    @Override
    public @NonNull ArgumentParseResult<Object> parse(
        final @NonNull CommandContext<C> context,
        final @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(TagValueArgument.TagParser.class, context));
      }
      final String input = inputQueue.peek();
      inputQueue.remove();

      Tag tag = main.getTagManager().getTag(context.get(contextTagName));
      if (tag.getTagType() == TagType.BOOLEAN) {
        if (input.equalsIgnoreCase("true")) {
          return ArgumentParseResult.success(true);
        } else if (input.equalsIgnoreCase("false")) {
          return ArgumentParseResult.success(false);
        } else {
          return ArgumentParseResult.failure(
              new IllegalArgumentException("This boolean tag needs a true or false value."));
        }
      }

      return ArgumentParseResult.success(input);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(
        final @NonNull CommandContext<C> context, final @NonNull String input) {

      List<String> completions = new java.util.ArrayList<>();

      Tag tag = main.getTagManager().getTag(context.get(contextTagName));
      if (tag.getTagType() == TagType.BOOLEAN) {
        completions.add("true");
        completions.add("false");
        for (String variableString : main.getVariablesManager().getVariableIdentifiers()) {
          Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
          if (variable == null || variable.getVariableDataType() != VariableDataType.BOOLEAN) {
            continue;
          }
          if (variable.getRequiredStrings().isEmpty()
              && variable.getRequiredNumbers().isEmpty()
              && variable.getRequiredBooleans().isEmpty()
              && variable.getRequiredBooleanFlags().isEmpty()) {
            completions.add(variableString);
          } else {
            completions.add(variableString + "(");
          }
        }
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Enter Value]",
              "[...]");

      return completions;
    }
  }
}
