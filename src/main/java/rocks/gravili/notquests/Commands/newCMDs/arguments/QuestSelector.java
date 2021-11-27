/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.Commands.newCMDs.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public class QuestSelector<C> extends CommandArgument<C, Quest> {

    protected QuestSelector(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(required, name, new QuestsParser<>(main), defaultValue, Quest.class, suggestionsProvider);
    }


    public static <C> QuestSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new QuestSelector.Builder<>(name, main);
    }

    public static <C> @NonNull CommandArgument<C, Quest> of(final @NonNull String name, final NotQuests main) {
        return QuestSelector.<C>newBuilder(name, main).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, Quest> optional(final @NonNull String name, final NotQuests main) {
        return QuestSelector.<C>newBuilder(name, main).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, Quest> optional(
            final @NonNull String name,
            final @NonNull Quest quest,
            final NotQuests main
    ) {
        return QuestSelector.<C>newBuilder(name, main).asOptionalWithDefault(quest.getQuestName()).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, Quest> {
        private final NotQuests main;

        private Builder(final @NonNull String name, NotQuests main) {
            super(Quest.class, name);
            this.main = main;
        }

        @Override
        public @NonNull CommandArgument<C, Quest> build() {
            return new QuestSelector<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main
            );
        }
    }


    public static final class QuestsParser<C> implements ArgumentParser<C, Quest> {

        private final NotQuests main;


        /**
         * Constructs a new PluginsParser.
         */
        public QuestsParser(
                NotQuests main
        ) {
            this.main = main;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> questNames = new java.util.ArrayList<>();
            for (Quest quest : main.getQuestManager().getAllQuests()) {
                questNames.add(quest.getQuestName());
            }

            final Audience audience = main.adventure().sender((CommandSender) context.getSender());
            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Quest Name]", "[...]");

            return questNames;
        }

        @Override
        public @NonNull ArgumentParseResult<Quest> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(QuestsParser.class, context));
            }
            final String input = inputQueue.peek();
            final Quest foundQuest = main.getQuestManager().getQuest(input);
            if (foundQuest == null) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Quest '" + inputQueue.peek() + "' does not exist!"
                ));
            }

            inputQueue.remove();
            return ArgumentParseResult.success(foundQuest);

        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}