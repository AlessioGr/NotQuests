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

public class ConversationSelector<C> extends CommandArgument<C, Conversation> {

  protected ConversationSelector(
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
        new ConversationsParser<>(main),
        defaultValue,
        Conversation.class,
        suggestionsProvider);
  }

  public static <C> ConversationSelector.@NonNull Builder<C> newBuilder(
      final @NonNull String name, final NotQuests main) {
    return new ConversationSelector.Builder<>(name, main);
  }

  public static <C> @NonNull CommandArgument<C, Conversation> of(
      final @NonNull String name, final NotQuests main) {
    return ConversationSelector.<C>newBuilder(name, main).asRequired().build();
  }

  public static <C> @NonNull CommandArgument<C, Conversation> optional(
      final @NonNull String name, final NotQuests main) {
    return ConversationSelector.<C>newBuilder(name, main).asOptional().build();
  }

  public static <C> @NonNull CommandArgument<C, Conversation> optional(
      final @NonNull String name, final @NonNull Conversation conversation, final NotQuests main) {
    return ConversationSelector.<C>newBuilder(name, main)
        .asOptionalWithDefault(conversation.getIdentifier())
        .build();
  }

  public static final class Builder<C> extends CommandArgument.Builder<C, Conversation> {
    private final NotQuests main;

    private Builder(final @NonNull String name, NotQuests main) {
      super(Conversation.class, name);
      this.main = main;
    }

    @Override
    public @NonNull CommandArgument<C, Conversation> build() {
      return new ConversationSelector<>(
          this.isRequired(),
          this.getName(),
          this.getDefaultValue(),
          this.getSuggestionsProvider(),
          this.getDefaultDescription(),
          this.main);
    }
  }

  public static final class ConversationsParser<C> implements ArgumentParser<C, Conversation> {

    private final NotQuests main;

    /** Constructs a new PluginsParser. */
    public ConversationsParser(NotQuests main) {
      this.main = main;
    }

    @NotNull
    @Override
    public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
      final List<String> questNames = new java.util.ArrayList<>();
      if(main.getConversationManager() != null){
        for (final Conversation conversation : main.getConversationManager().getAllConversations()) {
          questNames.add(conversation.getIdentifier());
        }
      }

      final List<String> allArgs = context.getRawInput();

      main.getUtilManager()
          .sendFancyCommandCompletion(
              (CommandSender) context.getSender(),
              allArgs.toArray(new String[0]),
              "[Conversation Name]",
              "[...]");

      return questNames;
    }

    @Override
    public @NonNull ArgumentParseResult<Conversation> parse(
        @NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
      if (inputQueue.isEmpty()) {
        return ArgumentParseResult.failure(
            new NoInputProvidedException(ConversationsParser.class, context));
      }

      if(main.getConversationManager() == null){
        return ArgumentParseResult.failure(
            new Exception(
                "ConversationManager is null. Check your console for previous NotQuests startup errors."));
      }

      final String conversationIdentifierInput = inputQueue.peek();
      final Conversation foundConversation =
          main.getConversationManager().getConversation(conversationIdentifierInput);
      inputQueue.remove();

      if (foundConversation == null) {
        return ArgumentParseResult.failure(
            new IllegalArgumentException(
                "Conversation '" + conversationIdentifierInput + "' does not exist!"));
      }

      return ArgumentParseResult.success(foundConversation);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}
