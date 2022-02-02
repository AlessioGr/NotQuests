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
// LIABILITY, WHETHER IN AN List<Action> OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
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
import io.leangen.geantyref.TypeToken;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public class MultipleActionsSelector<C> extends CommandArgument<C, List<Action>> {

    protected MultipleActionsSelector(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(required, name, new ListActionsParser<>(main), defaultValue, new TypeToken<List<Action>>(){}, suggestionsProvider);
    }

    public static <C> MultipleActionsSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new MultipleActionsSelector.Builder<>(name, main);
    }

    public static <C> @NonNull CommandArgument<C, List<Action>> of(final @NonNull String name, final NotQuests main) {
        return MultipleActionsSelector.<C>newBuilder(name, main).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, List<Action>> optional(final @NonNull String name, final NotQuests main) {
        return MultipleActionsSelector.<C>newBuilder(name, main).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, List<Action>> optional(
            final @NonNull String name,
            final @NonNull List<Action> actions,
            final NotQuests main
    ) {
        return MultipleActionsSelector.<C>newBuilder(name, main).asOptionalWithDefault("actions").build();
    }


    public static final class Builder<C> extends CommandArgument.Builder<C, List<Action>> {
        private final NotQuests main;

        private Builder(final @NonNull String name, NotQuests main) {
            super(new TypeToken<List<Action>>(){}, name);
            this.main = main;
        }

        @Override
        public @NonNull CommandArgument<C, List<Action>> build() {
            return new MultipleActionsSelector<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main
            );
        }
    }


    public static final class ListActionsParser<C> implements ArgumentParser<C, List<Action>> {

        private final NotQuests main;


        /**
         * Constructs a new PluginsParser.
         */
        public ListActionsParser(
                NotQuests main
        ) {
            this.main = main;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> suggestions = new ArrayList<>();
            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Action Names, separated by a comma]", "[...]");

            if(input.endsWith(",")){
                for(String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()){
                    suggestions.add(input + actionName);
                }
            }else{
                if(!input.contains(",")){
                    for(String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()){
                        suggestions.add(actionName);
                        if(input.endsWith(actionName)){
                            suggestions.add(actionName + ",");
                        }
                    }
                }else{
                    for(String actionName : main.getActionsYMLManager().getActionsAndIdentifiers().keySet()){
                        suggestions.add(input.substring(0, input.lastIndexOf(",")+1) + actionName);
                        if(input.endsWith(actionName)){
                            suggestions.add(input + ",");
                        }
                    }
                }
            }


            return suggestions;
        }

        @Override
        public @NonNull ArgumentParseResult<List<Action>> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(ListActionsParser.class, context));
            }
            final String input = inputQueue.peek();
            final List<Action> actions = new ArrayList<>();
            for(final String inputAction : input.split(",")){
                final Action action = main.getActionsYMLManager().getAction(inputAction);
                if (action == null) {
                    return ArgumentParseResult.failure(new IllegalArgumentException("Action '" + inputAction + "' does not exist!"
                    ));
                }
                actions.add(action);
            }
            inputQueue.remove();

            if (actions.isEmpty()) {
                return ArgumentParseResult.failure(new IllegalArgumentException("No valid action found!"
                ));
            }

            return ArgumentParseResult.success(actions);

        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}