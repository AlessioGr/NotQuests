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
package rocks.gravili.notquests.paper.commands.arguments.variables;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public final class ListVariableValueArgument<C> extends CommandArgument<C, String> {


    private ListVariableValueArgument(
            final boolean required,
            final @NonNull String name,
            final @NonNull String defaultValue,
            final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String,
                    @NonNull List<@NonNull String>> suggestionsProvider,
            final @NonNull ArgumentDescription defaultDescription,
            NotQuests main,
            Variable<?> variable
    ) {
        super(
                required,
                name,
                new ListVariableValueArgument.StringParser<>(main, variable),
                defaultValue,
                String.class,
                suggestionsProvider,
                defaultDescription
        );
    }

    /**
     * Create a new {@link ListVariableValueArgument.Builder}.
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> ListVariableValueArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return new ListVariableValueArgument.Builder<>(name, main, variable);
    }

    /**
     * Create a new required {@link ListVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> of(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return ListVariableValueArgument.<C>newBuilder(name, main, variable).asRequired().build();
    }

    /**
     * Create a new optional {@link ListVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return ListVariableValueArgument.<C>newBuilder(name, main, variable).asOptional().build();
    }

    /**
     * Create a new required {@link ListVariableValueArgument} with the specified default value.
     *
     * @param name       Argument name
     * @param defaultNum Default value
     * @param <C>        Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final int defaultNum, final NotQuests main, final Variable<?> variable) {
        return ListVariableValueArgument.<C>newBuilder(name, main, variable).asOptionalWithDefault(defaultNum).build();
    }



    public static final class Builder<C> extends CommandArgument.Builder<C, String> {

        private final NotQuests main;
        private final Variable<?> variable;

        private Builder(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
            super(String.class, name);
            this.main = main;
            this.variable = variable;
        }


        /**
         * Sets the command argument to be optional, with the specified default value.
         *
         * @param defaultValue default value
         * @return this builder
         * @see CommandArgument.Builder#asOptionalWithDefault(String)
         * @since 1.5.0
         */
        public ListVariableValueArgument.Builder<C> asOptionalWithDefault(final int defaultValue) {
            return (ListVariableValueArgument.Builder<C>) this.asOptionalWithDefault(defaultValue);
        }

        @NotNull
        @Override
        public ListVariableValueArgument<C> build() {
            return new ListVariableValueArgument<>(this.isRequired(), this.getName(),
                    this.getDefaultValue(), this.getSuggestionsProvider(), this.getDefaultDescription(), main, variable
            );
        }

    }

    public static final class StringParser<C> implements ArgumentParser<C, String> {
        private final NotQuests main;
        private final Variable<?> variable;


        /**
         * Construct a new String parser
         */
        public StringParser(final NotQuests main, final Variable<?> variable) {
            this.main = main;
            this.variable = variable;
        }


        @Override
        public @NonNull ArgumentParseResult<String> parse(
                final @NonNull CommandContext<C> context,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(ListVariableValueArgument.StringParser.class, context));
            }
            final String input = inputQueue.peek();
            inputQueue.remove();

            return ArgumentParseResult.success(input);

        }



        @Override
        public boolean isContextFree() {
            return true;
        }

        @Override
        public @NonNull List<@NonNull String> suggestions(
                final @NonNull CommandContext<C> context,
                final @NonNull String input
        ) {

            List<String> completions = new java.util.ArrayList<>();
            completions.add("<Enter Variables>");


            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Enter Variables (separated wit a comma)]", "[...]");

            if(context.getSender() instanceof Player player){
                if(variable.getPossibleValues(player) == null){
                    return completions;
                }
                completions.addAll(variable.getPossibleValues(player));
            }else{
                if(variable.getPossibleValues(null) == null){
                    return completions;
                }
                completions.addAll(variable.getPossibleValues(null));
            }

            return completions;
        }

    }

}