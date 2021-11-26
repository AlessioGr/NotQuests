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

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.NotQuests;

import java.util.List;
import java.util.Queue;

public class EntityTypeSelector<C> extends CommandArgument<C, String> {

    public EntityTypeSelector(
            boolean required,
            String name,
            NotQuests main
    ) {
        super(
                required,
                name,
                new EntityTypeParser<>(main),
                "",
                String.class,
                null
        );
    }


    public static final class EntityTypeParser<C> implements ArgumentParser<C, String> {

        private final NotQuests main;


        /**
         * Constructs a new PluginsParser.
         */
        public EntityTypeParser(
                NotQuests main
        ) {
            this.main = main;
        }


        @NotNull
        @Override
        public List<String> suggestions(@NotNull CommandContext<C> context, @NotNull String input) {
            List<String> completions = new java.util.ArrayList<>(main.getDataManager().standardEntityTypeCompletions);
            completions.add("ANY");

            return completions;
        }

        @Override
        public @NonNull ArgumentParseResult<String> parse(@NonNull CommandContext<@NonNull C> context, @NonNull Queue<@NonNull String> inputQueue) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(EntityTypeParser.class, context));
            }
            final String input = inputQueue.peek();
            inputQueue.remove();

            if (!main.getDataManager().standardEntityTypeCompletions.contains(input) && !input.equalsIgnoreCase("ANY")) {
                return ArgumentParseResult.failure(new IllegalArgumentException("Entity type '" + inputQueue.peek() + "' does not exist!"));
            }

            return ArgumentParseResult.success(input);

        }

        @Override
        public boolean isContextFree() {
            return true;
        }
    }
}