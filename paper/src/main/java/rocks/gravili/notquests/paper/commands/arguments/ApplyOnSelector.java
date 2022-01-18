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
//
// MIT License
//
// Copyright (c) 2021 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package rocks.gravili.notquests.paper.commands.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.function.BiFunction;

public class ApplyOnSelector<C> extends CommandArgument<C, Integer> { //0 = Quest

    protected ApplyOnSelector(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main,
            String questContext
    ) {
        super(required, name, new ApplyOnSelector.ApplyOnsParser<>(main, questContext), defaultValue, Integer.class, suggestionsProvider);
    }


    public static <C> ApplyOnSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main, final String questContext) {
        return new ApplyOnSelector.Builder<>(name, main, questContext);
    }

    public static <C> @NonNull CommandArgument<C, Integer> of(final @NonNull String name, final NotQuests main, final String questContext) {
        return ApplyOnSelector.<C>newBuilder(name, main, questContext).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, Integer> optional(final @NonNull String name, final NotQuests main, final String questContext) {
        return ApplyOnSelector.<C>newBuilder(name, main, questContext).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, Integer> optional(
            final @NonNull String name,
            final @NonNull Integer applyOn,
            final NotQuests main,
            final String questContext
    ) {
        return ApplyOnSelector.<C>newBuilder(name, main, questContext).asOptionalWithDefault("" + applyOn).build();
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, Integer> {
        private final NotQuests main;
        private final String questContext;

        private Builder(final @NonNull String name, NotQuests main, String questContext) {
            super(Integer.class, name);
            this.main = main;
            this.questContext = questContext;
        }

        @Override
        public @NonNull CommandArgument<C, Integer> build() {
            return new ApplyOnSelector<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main,
                    this.questContext
            );
        }
    }


    public static final class ApplyOnsParser<C> implements ArgumentParser<C, Integer> {

        private final NotQuests main;
        private final String questContext;

        /**
         * Constructs a new PluginsParser.
         */
        public ApplyOnsParser(
                NotQuests main,
                String questContext
        ) {
            this.main = main;
            this.questContext = questContext;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> completions = new java.util.ArrayList<>();

            final Quest quest = context.get(questContext);

            completions.add("Quest");
            for(Objective objective : quest.getObjectives()){
                completions.add("O" + objective.getObjectiveID());
            }


            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Apply On]", "[...]");


            return completions;
        }

        @Override
        public @NonNull ArgumentParseResult<Integer> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(ApplyOnsParser.class, context));
            }
            final String input = inputQueue.peek();
            inputQueue.remove();

            final Quest quest = context.get(questContext);

            if (input.equalsIgnoreCase("Quest")) {
                return ArgumentParseResult.success(0);
            } else {
                try {
                    int objectiveID = Integer.parseInt(input.toLowerCase(Locale.ROOT).replace("o", ""));
                    if (quest.getObjectiveFromID(objectiveID) != null) {
                        return ArgumentParseResult.success(objectiveID);
                    } else {
                        return ArgumentParseResult.failure(new IllegalArgumentException("ApplyOn Objective '" + input + "' is not an objective of the Quest!"
                        ));
                    }
                } catch (Exception e) {
                    return ArgumentParseResult.failure(new IllegalArgumentException("ApplyOn Objective '" + input + "' is not a valid applyOn objective!"
                    ));
                }
            }


        }

        @Override
        public boolean isContextFree() {
            return true;
        }

    }
}