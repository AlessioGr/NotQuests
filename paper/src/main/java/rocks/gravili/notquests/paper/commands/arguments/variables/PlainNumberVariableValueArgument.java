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
package rocks.gravili.notquests.paper.commands.arguments.variables;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.NumberParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public final class PlainNumberVariableValueArgument<C> extends CommandArgument<C, Long> {

    private static final long MAX_SUGGESTIONS_INCREMENT = 10;
    private static final long NUMBER_SHIFT_MULTIPLIER = 10;

    private final long min;
    private final long max;

    private PlainNumberVariableValueArgument(
            final boolean required,
            final @NonNull String name,
            final long min,
            final long max,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main
    ) {
        super(
                required,
                name,
                new PlainNumberVariableValueArgument.LongParser<>(min, max, main),
                defaultValue,
                Long.class,
                suggestionsProvider,
                defaultDescription
        );
        this.min = min;
        this.max = max;
    }

    /**
     * Create a new {@link PlainNumberVariableValueArgument.Builder}.
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> PlainNumberVariableValueArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main) {
        return new PlainNumberVariableValueArgument.Builder<>(name, main);
    }

    /**
     * Create a new required {@link PlainNumberVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Long> of(final @NonNull String name, final NotQuests main) {
        return PlainNumberVariableValueArgument.<C>newBuilder(name, main).asRequired().build();
    }

    /**
     * Create a new optional {@link PlainNumberVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Long> optional(final @NonNull String name, final NotQuests main) {
        return PlainNumberVariableValueArgument.<C>newBuilder(name, main).asOptional().build();
    }

    /**
     * Create a new required {@link PlainNumberVariableValueArgument} with the specified default value.
     *
     * @param name       Argument name
     * @param defaultNum Default value
     * @param <C>        Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, Long> optional(final @NonNull String name, final long defaultNum , final NotQuests main) {
        return PlainNumberVariableValueArgument.<C>newBuilder(name, main).asOptionalWithDefault(defaultNum).build();
    }

    /**
     * Get the minimum accepted Long that could have been parsed
     *
     * @return Minimum Long
     */
    public long getMin() {
        return this.min;
    }

    /**
     * Get the maximum accepted Long that could have been parsed
     *
     * @return Maximum Long
     */
    public long getMax() {
        return this.max;
    }

    public static final class Builder<C> extends CommandArgument.Builder<C, Long> {

        private final NotQuests main;
        private long min = PlainNumberVariableValueArgument.LongParser.DEFAULT_MINIMUM;
        private long max = PlainNumberVariableValueArgument.LongParser.DEFAULT_MAXIMUM;

        private Builder(final @NonNull String name, final NotQuests main) {
            super(Long.class, name);
            this.main = main;
        }

        /**
         * Set a minimum value
         *
         * @param min Minimum value
         * @return Builder instance
         */
        public PlainNumberVariableValueArgument.Builder<C> withMin(final long min) {
            this.min = min;
            return this;
        }

        /**
         * Set a maximum value
         *
         * @param max Maximum value
         * @return Builder instance
         */
        public PlainNumberVariableValueArgument.Builder<C> withMax(final long max) {
            this.max = max;
            return this;
        }

        /**
         * Sets the command argument to be optional, with the specified default value.
         *
         * @param defaultValue default value
         * @return this builder
         * @see CommandArgument.Builder#asOptionalWithDefault(String)
         * @since 1.5.0
         */
        public PlainNumberVariableValueArgument.Builder<C> asOptionalWithDefault(final long defaultValue) {
            return (PlainNumberVariableValueArgument.Builder<C>) this.asOptionalWithDefault(Long.toString(defaultValue));
        }

        @Override
        public PlainNumberVariableValueArgument<C> build() {
            return new PlainNumberVariableValueArgument<>(this.isRequired(), this.getName(), this.min, this.max,
                    this.getDefaultValue(), this.getSuggestionsProvider(), this.getDefaultDescription(), main
            );
        }

    }

    public static final class LongParser<C> implements ArgumentParser<C, Long> {
        /**
         * Constant for the default/unset minimum value.
         *
         * @since 1.5.0
         */
        public static final long DEFAULT_MINIMUM = Long.MIN_VALUE;
        /**
         * Constant for the default/unset maximum value.
         *
         * @since 1.5.0
         */
        public static final long DEFAULT_MAXIMUM = Long.MAX_VALUE;
        private final NotQuests main;
        private final long min;
        private final long max;

        /**
         * Construct a new Long parser
         *
         * @param min Minimum acceptable value
         * @param max Maximum acceptable value
         */
        public LongParser(final long min, final long max, final NotQuests main) {
            this.min = min;
            this.max = max;
            this.main = main;
        }

        /**
         * Get Long suggestions. This supports both positive and negative numbers
         *
         * @param min   Minimum value
         * @param max   Maximum value
         * @param input Input
         * @return List of suggestions
         */
        @SuppressWarnings("MixedMutabilityReturnType")
        public static @NonNull List<@NonNull String> getSuggestions(
                final long min,
                final long max,
                final @NonNull String input
        ) {
            final Set<Long> numbers = new TreeSet<>();

            try {
                final long inputNum = Long.parseLong(input.equals("-") ? "-0" : input.isEmpty() ? "0" : input);
                final long inputNumAbsolute = Math.abs(inputNum);

                numbers.add(inputNumAbsolute); /* It's a valid number, so we suggest it */
                for (long i = 0; i < MAX_SUGGESTIONS_INCREMENT
                        && (inputNum * NUMBER_SHIFT_MULTIPLIER) + i <= max; i++) {
                    numbers.add((inputNumAbsolute * NUMBER_SHIFT_MULTIPLIER) + i);
                }

                final List<String> suggestions = new LinkedList<>();
                for (long number : numbers) {
                    if (input.startsWith("-")) {
                        number = -number; /* Preserve sign */
                    }
                    if (number < min || number > max) {
                        continue;
                    }
                    suggestions.add(String.valueOf(number));
                }

                return suggestions;
            } catch (final Exception ignored) {
                return Collections.emptyList();
            }
        }

        @Override
        public @NonNull ArgumentParseResult<Long> parse(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            final String input = inputQueue.peek();
            if (input == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(PlainNumberVariableValueArgument.LongParser.class, commandContext));
            }
            try {
                final long value = Long.parseLong(input);
                if (value < this.min || value > this.max) {
                    return ArgumentParseResult.failure(new PlainNumberVariableValueArgument.LongParseException(input, this, commandContext));
                }
                inputQueue.remove();
                return ArgumentParseResult.success(value);
            } catch (final Exception e) {
                return ArgumentParseResult.failure(new PlainNumberVariableValueArgument.LongParseException(input, this, commandContext));
            }
        }

        /**
         * Get the minimum value accepted by this parser
         *
         * @return Min value
         */
        public long getMin() {
            return this.min;
        }

        /**
         * Get the maximum value accepted by this parser
         *
         * @return Max value
         */
        public long getMax() {
            return this.max;
        }

        /**
         * Get whether this parser has a maximum set.
         * This will compare the parser's maximum to {@link #DEFAULT_MAXIMUM}.
         *
         * @return whether the parser has a maximum set
         * @since 1.5.0
         */
        public boolean hasMax() {
            return this.max != DEFAULT_MAXIMUM;
        }

        /**
         * Get whether this parser has a minimum set.
         * This will compare the parser's minimum to {@link #DEFAULT_MINIMUM}.
         *
         * @return whether the parser has a maximum set
         * @since 1.5.0
         */
        public boolean hasMin() {
            return this.min != DEFAULT_MINIMUM;
        }

        @Override
        public boolean isContextFree() {
            return true;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(
                final @NonNull CommandContext<C> commandContext,
                final @NonNull String input
        ) {
            if(!commandContext.contains("variable")){
                return getSuggestions(this.min, this.max, input);
            }


            Variable<?> variable = commandContext.get("variable");
            if(variable == null) {
                return getSuggestions(this.min, this.max, input);
            }





            if(commandContext.getSender() instanceof Player player) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                if (variable.getPossibleValues(questPlayer) == null) {
                    return getSuggestions(this.min, this.max, input);
                }

                return variable.getPossibleValues(questPlayer);
            }else{
                if(variable.getPossibleValues(null) == null){
                    return getSuggestions(this.min, this.max, input);
                }

                return variable.getPossibleValues(null);
            }

        }

    }


    public static final class LongParseException extends NumberParseException {

        private static final long serialVersionUID = -6933923056628373853L;

        private final PlainNumberVariableValueArgument.LongParser<?> parser;

        /**
         * Construct a new Long parse exception
         *
         * @param input          String input
         * @param min            Minimum value
         * @param max            Maximum value
         * @param commandContext Command context
         * @deprecated use {@link #LongParseException(String, PlainNumberVariableValueArgument.LongParser, CommandContext)} instead
         */
        @Deprecated
        public LongParseException(
                final @NonNull String input,
                final long min,
                final long max,
                final @NonNull CommandContext<?> commandContext,
                final NotQuests main
        ) {
            this(input, new PlainNumberVariableValueArgument.LongParser<>(min, max, main), commandContext);
        }

        /**
         * Create a new {@link PlainNumberVariableValueArgument.LongParseException}.
         *
         * @param input          input string
         * @param parser         Long parser
         * @param commandContext command context
         * @since 1.5.0
         */
        public LongParseException(
                final @NonNull String input,
                final PlainNumberVariableValueArgument.LongParser<?> parser,
                final @NonNull CommandContext<?> commandContext
        ) {
            super(input, parser.min, parser.max, PlainNumberVariableValueArgument.LongParser.class, commandContext);
            this.parser = parser;
        }

        @Override
        public boolean hasMin() {
            return this.parser.hasMin();
        }

        @Override
        public boolean hasMax() {
            return this.parser.hasMax();
        }

        @Override
        public @NonNull String getNumberType() {
            return "Long";
        }

    }

}