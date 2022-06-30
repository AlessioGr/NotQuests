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
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.Speaker;

public class SpeakerSelector<C> extends CommandArgument<C, Speaker> { // 0 = Quest

  protected SpeakerSelector(
      final boolean required,
      final @NonNull String name,
      final @NonNull String defaultValue,
      final @Nullable
          BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>>
          suggestionsProvider,
      final @NonNull ArgumentDescription defaultDescription,
      NotQuests main,
      String conversationContext) {
    super(
        required,
        name,
        new SpeakerSelector.SpeakerParser<>(main, conversationContext),
        defaultValue,
        Speaker.class,
        suggestionsProvider);
  }

  public static <C> SpeakerSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main, final String conversationContext) {
    return new SpeakerSelector.Builder<>(name, main, conversationContext);
  }

  public static <C> @NonNull CommandArgument<C, Speaker> of(
      final @NonNull String name, final NotQuests main, final String conversationContext) {
    return SpeakerSelector.<C>newBuilder(name, main, conversationContext).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Speaker> optional(
      final @NonNull String name, final NotQuests main, final String conversationContext) {
    return SpeakerSelector.<C>newBuilder(name, main, conversationContext).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Speaker> optional(
      final @NonNull String name,
      final @NonNull Speaker speaker,
      final NotQuests main,
      final String conversationContext) {
    return SpeakerSelector.<C>newBuilder(name, main, conversationContext)
        .asOptionalWithDefault("" + speaker.getSpeakerName())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Speaker> {
    private final NotQuests main;
    private final String conversationContext;

    private Builder(final @NonNull String name, NotQuests main, String conversationContext) {
      super(Speaker.class, name);
      this.main = main;
      this.conversationContext = conversationContext;
    }

    @Override
    public @NonNull CommandArgument<C, Speaker> build() {
      return new SpeakerSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main,
          this.conversationContext);
    }
  }

  public static final class SpeakerParser<C> implements ArgumentParser<C, Speaker> {

    private final NotQuests main;
    private final String conversationContext;

    /** Constructs a new PluginsParser. */
    public SpeakerParser(NotQuests main, String conversationContext) {
      this.main = main;
      this.conversationContext = conversationContext;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      List<String> completions = new java.util.ArrayList<>();

      final Conversation conversation = context.get(conversationContext);

      if (conversation.getSpeakers() != null && conversation.getSpeakers().size() > 0) {
        final int speakerCount = conversation.getSpeakers().size();
        for (int i = 0; i < speakerCount; i++) {
          completions.add(conversation.getSpeakers().get(i).getSpeakerName());
        }
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Speaker Name]",
              "[...]");

      return completions;
    }

    @Override
    public @NonNull ArgumentParseResult<Speaker> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(SpeakerParser.class, context));
      }
      final String input = inputQueue.peek();
      inputQueue.remove();

      final Conversation conversation = context.get(conversationContext);

      for (final Speaker speaker : conversation.getSpeakers()) {
        if (speaker.getSpeakerName().equalsIgnoreCase(input)) {
          return ArgumentParseResult.success(speaker);
        }
      }
      return ArgumentParseResult.failure(
          new IllegalArgumentException(
              "Speaker '"
                  + input
                  + "' was not found in conversation "
                  + conversation.getIdentifier()
                  + "!"));
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
