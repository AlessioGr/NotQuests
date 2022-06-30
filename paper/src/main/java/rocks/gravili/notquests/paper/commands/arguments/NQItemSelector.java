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
import rocks.gravili.notquests.paper.managers.items.NQItem;

public class NQItemSelector<C> extends CommandArgument<C, NQItem> {

  protected NQItemSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main) {
    super(
        required, name, new NQItemsParser<>(main), defaultValue, NQItem.class, suggestionsProvider);
  }

  public static <C> NQItemSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new NQItemSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, NQItem> of(
      final @NonNull String name, final NotQuests main) {
    return NQItemSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, NQItem> optional(
      final @NonNull String name, final NotQuests main) {
    return NQItemSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, NQItem> optional(
      final @NonNull String name, final @NonNull NQItem NQItem, final NotQuests main) {
    return NQItemSelector.<C>newBuilder(name, main)
        .asOptionalWithDefault(NQItem.getItemName())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, NQItem> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(NQItem.class, name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, NQItem> build() {
      return new NQItemSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class NQItemsParser<C> implements ArgumentParser<C, NQItem> {

    private final NotQuests main;

    /** Constructs a new PluginsParser. */
    public NQItemsParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> NQItemNames =
          new java.util.ArrayList<>(main.getItemsManager().getItemsIdentifiers());
      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[NQItem Name]",
              "[...]");

      return NQItemNames;
    }

    @Override
    public @NonNull ArgumentParseResult<NQItem> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(NQItemsParser.class, context));
      }
      final String input = inputQueue.peek();
      final NQItem foundNQItem = main.getItemsManager().getItem(input);
      inputQueue.remove();

      if (foundNQItem == null) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException("NQItem '" + input + "' does not exist!"));
      }

      return ArgumentParseResult.success(foundNQItem);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
