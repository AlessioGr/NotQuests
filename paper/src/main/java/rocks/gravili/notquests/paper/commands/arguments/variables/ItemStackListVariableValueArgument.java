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
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public final class ItemStackListVariableValueArgument<C> extends CommandArgument<C, String> {


    private ItemStackListVariableValueArgument(
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
                new ItemStackListVariableValueArgument.StringParser<>(main, variable),
                defaultValue,
                String.class,
                suggestionsProvider,
                defaultDescription
        );
    }

    /**
     * Create a new {@link ItemStackListVariableValueArgument.Builder}.
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> ItemStackListVariableValueArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return new ItemStackListVariableValueArgument.Builder<>(name, main, variable);
    }

    /**
     * Create a new required {@link ItemStackListVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> of(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return ItemStackListVariableValueArgument.<C>newBuilder(name, main, variable).asRequired().build();
    }

    /**
     * Create a new optional {@link ItemStackListVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return ItemStackListVariableValueArgument.<C>newBuilder(name, main, variable).asOptional().build();
    }

    /**
     * Create a new required {@link ItemStackListVariableValueArgument} with the specified default value.
     *
     * @param name       Argument name
     * @param defaultNum Default value
     * @param <C>        Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final int defaultNum, final NotQuests main, final Variable<?> variable) {
        return ItemStackListVariableValueArgument.<C>newBuilder(name, main, variable).asOptionalWithDefault(defaultNum).build();
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
        public ItemStackListVariableValueArgument.Builder<C> asOptionalWithDefault(final int defaultValue) {
            return (ItemStackListVariableValueArgument.Builder<C>) this.asOptionalWithDefault(defaultValue);
        }

        @NotNull
        @Override
        public ItemStackListVariableValueArgument<C> build() {
            return new ItemStackListVariableValueArgument<>(this.isRequired(), this.getName(),
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
                return ArgumentParseResult.failure(new NoInputProvidedException(ItemStackListVariableValueArgument.StringParser.class, context));
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
            for (Material value : Material.values()) {
                completions.add(value.name().toLowerCase());
            }
            completions.add("hand");
            completions.add("any");


            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Enter Variables (separated wit a comma)]", "[...]");

            if(context.getSender() instanceof Player player) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                if (variable.getPossibleValues(questPlayer) == null) {
                    return completions;
                }
                completions.addAll(variable.getPossibleValues(questPlayer));
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