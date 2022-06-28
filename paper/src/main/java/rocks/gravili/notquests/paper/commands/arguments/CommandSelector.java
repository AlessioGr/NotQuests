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
import io.leangen.geantyref.TypeToken;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;

public class CommandSelector<C> extends CommandArgument<C, String[]> {

  protected CommandSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @NonNull BiFunction<
                  @NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
              suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main) {
    super(
        required,
        name,
        new CommandParser<>(main),
        defaultValue,
        TypeToken.get(String[].class),
        suggestionsProvider);
  }

  public static <C> CommandSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new CommandSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, String[]> of(
      final @NonNull String name, final NotQuests main) {
    return CommandSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, String[]> optional(
      final @NonNull String name, final NotQuests main) {
    return CommandSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, String[]> optional(
      final @NonNull String name, final @NonNull String command, final NotQuests main) {
    return CommandSelector.<C>newBuilder(name, main).asOptionalWithDefault(command).build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, String[]> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(TypeToken.get(String[].class), name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, String[]> build() {
      return new CommandSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class CommandParser<C> implements ArgumentParser<C, String[]> {

    private final NotQuests main;

    /** Constructs a new CommandParser. */
    public CommandParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      String cmd =
          context
              .getRawInputJoined()
              .substring(context.getRawInputJoined().indexOf("ConsoleCommand ") + 15);

      List<String> completions = new java.util.ArrayList<>();

      // audience.sendMessage(main.parse(
      //        "Input: " + cmd
      // ));

      if (main.getCommandManager().getCommandMap() != null) {
        List<String> compl =
            main.getCommandManager()
                .getCommandMap()
                .tabComplete(main.getMain().getServer().getConsoleSender(), cmd);
        if (compl != null) {
          for (String cmd1 : compl) {
            completions.add(cmd1);
          }
        }
      }

      if (input.startsWith("{")) {
        completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
      } else {
        // completions.add("<Enter Console Command>");
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "<Enter Console Command>",
              "");

      return completions;
    }

    @Override
    public @NonNull ArgumentParseResult<String[]> parse(
        final @NonNull CommandContext<C> commandContext,
        final @NonNull Queue<@NonNull String> inputQueue) {
      final String[] result = new String[inputQueue.size()];
      for (int i = 0; i < result.length; i++) {
        result[i] = inputQueue.remove();
      }
      return ArgumentParseResult.success(result);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
