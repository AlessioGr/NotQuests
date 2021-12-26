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
import io.leangen.geantyref.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public class MiniMessageSelector<C> extends CommandArgument<C, String[]> {

    private boolean withPlaceholders = false;

    protected MiniMessageSelector(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @NonNull BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main,
            boolean withPlaceholders
    ) {
        super(required, name, new MiniMessageParser<>(main, withPlaceholders), defaultValue, TypeToken.get(String[].class), suggestionsProvider);
        this.withPlaceholders = withPlaceholders;
    }

    public static <C> MiniMessageSelector.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new MiniMessageSelector.Builder<>(name, main);
    }

    public static <C> @NonNull CommandArgument<C, String[]> of(final @NonNull String name, final NotQuests main) {
        return MiniMessageSelector.<C>newBuilder(name, main).asRequired().build();
    }

    public static <C> @NonNull CommandArgument<C, String[]> optional(final @NonNull String name, final NotQuests main) {
        return MiniMessageSelector.<C>newBuilder(name, main).asOptional().build();
    }

    public static <C> @NonNull CommandArgument<C, String[]> optional(
            final @NonNull String name,
            final @NonNull String command,
            final NotQuests main
    ) {
        return MiniMessageSelector.<C>newBuilder(name, main).asOptionalWithDefault(command).build();
    }

    public @NonNull boolean isWithPlaceholders() {
        return this.withPlaceholders;
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, String[]> {
        private final NotQuests main;
        private boolean withPlaceholders = false;

        private Builder(final @NonNull String name, NotQuests main) {
            super(TypeToken.get(String[].class), name);
            this.main = main;
        }

        public CommandArgument.Builder<C, String[]> withPlaceholders() {
            this.withPlaceholders = true;
            return this;
        }

        @Override
        public @NonNull CommandArgument<C, String[]> build() {
            return new MiniMessageSelector<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription(),
                    this.main,
                    this.withPlaceholders
            );
        }
    }


    public static final class MiniMessageParser<C> implements ArgumentParser<C, String[]> {

        private final NotQuests main;
        private final boolean withPlaceholders;


        /**
         * Constructs a new MiniMessageParser.
         */
        public MiniMessageParser(
                NotQuests main,
                boolean withPlaceholders
        ) {
            this.main = main;
            this.withPlaceholders = withPlaceholders;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> completions = new java.util.ArrayList<>();

            String rawInput = context.getRawInputJoined();
            if (input.startsWith("{")) {
                completions.addAll(main.getCommandManager().getAdminCommands().placeholders);
            } else {
                if(input.startsWith("<")){
                    for(String color : main.getUtilManager().getMiniMessageTokens()){
                        completions.add("<"+ color +">");
                        //Now the closings. First we search IF it contains an opening and IF it doesnt contain more closings than the opening
                        if(rawInput.contains("<"+color+">")){
                            if(StringUtils.countMatches(rawInput, "<"+color+">") > StringUtils.countMatches(rawInput, "</"+color+">")){
                                completions.add("</"+ color +">");
                            }
                        }
                    }
                }else{
                    completions.add("<Enter Message>");
                }
            }

            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "<Enter Message>", "");


            return completions;
        }

        @Override
        public @NonNull ArgumentParseResult<String[]> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
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