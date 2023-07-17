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

public class EntityTypeSelector<C> extends CommandArgument<C, String> {

  protected EntityTypeSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main,
      boolean mythicMobsFactions) {
    super(
        required,
        name,
        new EntityTypeParser<>(main, mythicMobsFactions),
        defaultValue,
        String.class,
        suggestionsProvider);
  }

  public static <C> EntityTypeSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main, boolean mythicMobsFactions) {
    return new EntityTypeSelector.Builder<>(name, main, mythicMobsFactions);
  }

  public static <C> @NonNull CommandArgument<C, String> of(
      final @NonNull String name, final NotQuests main, final boolean mythicMobsFactions) {
    return EntityTypeSelector.<C>newBuilder(name, main, mythicMobsFactions).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, String> optional(
      final @NonNull String name, final NotQuests main, boolean mythicMobsFactions) {
    return EntityTypeSelector.<C>newBuilder(name, main, mythicMobsFactions).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, String> optional(
      final @NonNull String name, final @NonNull String entityType, final NotQuests main, boolean mythicMobsFactions) {
    return EntityTypeSelector.<C>newBuilder(name, main, mythicMobsFactions).asOptionalWithDefault(entityType).build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, String> {
    private final NotQuests main;
    private final boolean mythicMobsFactions;

    private Builder(final @NonNull String name, NotQuests main, boolean mythicMobsFactions) {
      super(String.class, name);
      this.main = main;
      this.mythicMobsFactions = mythicMobsFactions;
    }

    @Override
    public @NonNull CommandArgument<C, String> build() {
      return new EntityTypeSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main,
              this.mythicMobsFactions);
    }
  }

  public static final class EntityTypeParser<C> implements ArgumentParser<C, String> {

    private final NotQuests main;
    private final boolean mythicMobsFactions;

    /** Constructs a new EntityTypePare. */
    public EntityTypeParser(NotQuests main, boolean mythicMobsFactions) {
      this.main = main;
      this.mythicMobsFactions = mythicMobsFactions;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> completions =
          new java.util.ArrayList<>(main.getDataManager().standardEntityTypeCompletions);
      completions.add("any");

      //Add extra Mythic Mobs completions, if enabled
      if (mythicMobsFactions && main.getIntegrationsManager().isMythicMobsEnabled() && main.getIntegrationsManager().getMythicMobsManager() != null) {
        completions.addAll(main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:"));
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Mob Name / 'ANY']",
              "[...]");

      return completions;
    }

    @Override
    public @NonNull ArgumentParseResult<String> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(EntityTypeParser.class, context));
      }
      final String input = inputQueue.peek();
      inputQueue.remove();

      if (mythicMobsFactions && main.getIntegrationsManager().isMythicMobsEnabled() && main.getIntegrationsManager().getMythicMobsManager() != null) {
        if(main.getIntegrationsManager().getMythicMobsManager().getFactionNames("mmfaction:").contains(input)) {
          return ArgumentParseResult.success(input);
        }
      }

      if (!main.getDataManager().standardEntityTypeCompletions.contains(input)
          && !input.equalsIgnoreCase("ANY")) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException("Entity type '" + input + "' does not exist!"));
      }

      return ArgumentParseResult.success(input);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
