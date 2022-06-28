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
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

public final class NumberVariableValueArgument<C> extends CommandArgument<C, String> {
    private static final int MAX_SUGGESTIONS_INCREMENT = 10;
    private static final int NUMBER_SHIFT_MULTIPLIER = 10;

    private NumberVariableValueArgument(
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
                new NumberVariableValueArgument.StringParser<>(main, variable),
                defaultValue,
                String.class,
                suggestionsProvider,
                defaultDescription
        );
    }

    /**
     * Create a new {@link NumberVariableValueArgument.Builder}.
     *
     * @param name Name of the argument
     * @param <C>  Command sender type
     * @return Created builder
     */
    public static <C> NumberVariableValueArgument.@NonNull Builder<C> newBuilder(final @NonNull String name, final NotQuests main, @Nullable final Variable<?> variable) {
        return new NumberVariableValueArgument.Builder<>(name, main, variable);
    }

    /**
     * Create a new required {@link NumberVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> of(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return NumberVariableValueArgument.<C>newBuilder(name, main, variable).asRequired().build();
    }

    /**
     * Create a new optional {@link NumberVariableValueArgument}.
     *
     * @param name Argument name
     * @param <C>  Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final NotQuests main, final Variable<?> variable) {
        return NumberVariableValueArgument.<C>newBuilder(name, main, variable).asOptional().build();
    }

    /**
     * Create a new required {@link NumberVariableValueArgument} with the specified default value.
     *
     * @param name       Argument name
     * @param defaultNum Default value
     * @param <C>        Command sender type
     * @return Created argument
     */
    public static <C> @NonNull CommandArgument<C, String> optional(final @NonNull String name, final int defaultNum , final NotQuests main, final Variable<?> variable) {
        return NumberVariableValueArgument.<C>newBuilder(name, main, variable).asOptionalWithDefault(defaultNum).build();
    }



    public static final class Builder<C> extends CommandArgument.Builder<C, String> {

        private final NotQuests main;
        private final @Nullable Variable<?> variable;

        private Builder(final @NonNull String name, final NotQuests main, @Nullable final Variable<?> variable) {
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
        public NumberVariableValueArgument.Builder<C> asOptionalWithDefault(final int defaultValue) {
            return (NumberVariableValueArgument.Builder<C>) this.asOptionalWithDefault(defaultValue);
        }

        @NotNull
        @Override
        public NumberVariableValueArgument<C> build() {
            return new NumberVariableValueArgument<>(this.isRequired(), this.getName(),
                    this.getDefaultValue(), this.getSuggestionsProvider(), this.getDefaultDescription(), main, variable
            );
        }

    }

    public static final class StringParser<C> implements ArgumentParser<C, String> {
        private final NotQuests main;
        private final @Nullable Variable<?> variable;

        /**
         * Construct a new String parser
         */
        public StringParser(final NotQuests main, final @Nullable Variable<?> variable) {
            this.main = main;
            this.variable = variable;
        }


        @Override
        public @NonNull ArgumentParseResult<String> parse(
                final @NonNull CommandContext<C> context,
                final @NonNull Queue<@NonNull String> inputQueue
        ) {
            if (inputQueue.isEmpty()) {
                return ArgumentParseResult.failure(new NoInputProvidedException(NumberVariableValueArgument.StringParser.class, context));
            }
            final String input = inputQueue.peek();
            inputQueue.remove();

            try{
                final NumberExpression numberExpression = new NumberExpression(main, input);

                if (context.getSender() instanceof Player player) {
                    try {
                        numberExpression.calculateValue(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
                    } catch (Exception e) {
                        if (main.getConfiguration().isDebug()) {
                            e.printStackTrace();
                        }
                        return ArgumentParseResult.failure(new IllegalArgumentException("Invalid Expression: " + input + ". Error: " + e.toString()));
                    }
                }

            } catch (Exception e){
                return ArgumentParseResult.failure(new IllegalArgumentException("Erroring Expression: " + input + ". Error: " + e.toString()));
            }


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
            final CommandContext<CommandSender> commandSenderCommandContext = (CommandContext<CommandSender>) context;
            final CommandSender sender = commandSenderCommandContext.getSender();

            final List<String> completions = new java.util.ArrayList<>();
            completions.add("<Enter Variable or Number>");

            for(final String variableString : main.getVariablesManager().getVariableIdentifiers()) {
                final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
                if (variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN )) {
                    continue;
                }
                if(variable.getRequiredStrings().isEmpty() && variable.getRequiredNumbers().isEmpty() && variable.getRequiredBooleans().isEmpty() && variable.getRequiredBooleanFlags().isEmpty()){
                    completions.add(variableString);
                }else{
                    if(!input.endsWith(variableString+"(")){
                        if(input.endsWith(",") && input.contains(variableString+"(")){
                            for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                                if(!input.contains(stringArgument.getName())){
                                    completions.add(input + stringArgument.getName() + ":");
                                }
                            }
                            for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                                if(!input.contains(numberVariableValueArgument.getName())){
                                    completions.add(input + numberVariableValueArgument.getName() + ":");
                                }
                            }
                            for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                                if(!input.contains(booleanArgument.getName())){
                                    completions.add(input + booleanArgument.getName() + ":");
                                }
                            }
                            for(CommandFlag<Void> flag : variable.getRequiredBooleanFlags()){
                                if(!input.contains(flag.getName())){
                                    completions.add(input + "--" + flag.getName() + "");
                                }
                            }
                        }else if(!input.endsWith(")")){
                            if(input.contains(variableString+"(") && (!input.contains(")") || (input.lastIndexOf("(") < input.lastIndexOf(")"))) ){
                                final String subStringAfter = input.substring(input.indexOf(variableString+"("));

                                for(final StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                                    if(subStringAfter.contains(":")){
                                        if(subStringAfter.endsWith(":")){
                                            final List<String> suggestions = stringArgument.getSuggestionsProvider().apply(commandSenderCommandContext, "");

                                            for(final String suggestion : suggestions){
                                                completions.add(input + suggestion);
                                            }
                                        }else{
                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];

                                            final List<String> suggestions = stringArgument.getSuggestionsProvider().apply(commandSenderCommandContext, stringAfterLastDoubleDot);


                                            for(final String suggestion : suggestions){

                                                completions.add(input.substring(0, input.length()-stringAfterLastDoubleDot.length()-1) + ":" + suggestion);
                                            }
                                        }
                                    }else{
                                        completions.add(variableString+"(" + stringArgument.getName() + ":");
                                    }
                                }
                                for(final NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()) {
                                    if (subStringAfter.contains(":")) {
                                        if(subStringAfter.endsWith(":")){
                                            final List<String> suggestions = numberVariableValueArgument.getSuggestionsProvider().apply(commandSenderCommandContext, "");

                                            for(final String suggestion : suggestions){
                                                completions.add(input + suggestion);
                                            }
                                        }else{

                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];

                                            final List<String> suggestions = numberVariableValueArgument.getSuggestionsProvider().apply(commandSenderCommandContext, stringAfterLastDoubleDot);


                                            for(final String suggestion : suggestions){

                                                completions.add(input.substring(0, input.length()-stringAfterLastDoubleDot.length()-1) + ":" + suggestion);
                                            }
                                        }
                                    } else {
                                        completions.add(variableString+"(" + numberVariableValueArgument.getName() + ":");
                                    }
                                }for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()) {
                                    if (subStringAfter.contains(":")) {
                                        if(subStringAfter.endsWith(":")){
                                            final List<String> suggestions = booleanArgument.getSuggestionsProvider().apply(commandSenderCommandContext, "");

                                            for(final String suggestion : suggestions){
                                                completions.add(input + suggestion);
                                            }
                                        }else{

                                            final String[] splitDoubleDots = subStringAfter.split(":");
                                            final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];

                                            final List<String> suggestions = booleanArgument.getSuggestionsProvider().apply(commandSenderCommandContext, stringAfterLastDoubleDot);


                                            for(final String suggestion : suggestions){

                                                completions.add(input.substring(0, input.length()-stringAfterLastDoubleDot.length()-1) + ":" + suggestion);
                                            }
                                        }
                                    } else {
                                        completions.add(variableString+"(" + booleanArgument.getName() + ":");
                                    }
                                }for(CommandFlag<Void> flag : variable.getRequiredBooleanFlags()){
                                    completions.add(variableString+"(--" + flag.getName() + "");
                                }
                            }else{
                                completions.add(variableString+"(");
                            }
                        }
                    }else{//Moree completionss
                        for(StringArgument<CommandSender> stringArgument : variable.getRequiredStrings()){
                            completions.add(variableString+"(" + stringArgument.getName() + ":");
                        }
                        for(NumberVariableValueArgument<CommandSender> numberVariableValueArgument : variable.getRequiredNumbers()){
                            completions.add(variableString+"(" + numberVariableValueArgument.getName() + ":");
                        }
                        for(BooleanVariableValueArgument<CommandSender> booleanArgument : variable.getRequiredBooleans()){
                            completions.add(variableString+"(" + booleanArgument.getName() + ":");
                        }
                        for(CommandFlag<Void> flag : variable.getRequiredBooleanFlags()){
                            completions.add(variableString+"(--" + flag.getName() + "");
                        }

                    }
                }
            }

            //Now the number completions
            final Set<Double> numbers = new TreeSet<>();
            double min = -Double.MAX_VALUE;
            double max = Double.MAX_VALUE;

            try {
                final double inputNum = Long.parseLong(input.equals("-") ? "-0" : input.isEmpty() ? "0" : input);
                final double inputNumAbsolute = Math.abs(inputNum);

                numbers.add(inputNumAbsolute); /* It's a valid number, so we suggest it */
                for (double i = 0; i < 1
                        && (inputNum * NUMBER_SHIFT_MULTIPLIER) + i <= max; i++) {
                    numbers.add((inputNumAbsolute * NUMBER_SHIFT_MULTIPLIER) + i);
                }

                for (double number : numbers) {
                    if (input.startsWith("-")) {
                        number = -number; /* Preserve sign */
                    }
                    if (number < min || number > max) {
                        continue;
                    }
                    completions.add(String.valueOf(number));
                }

                //return suggestions;
            } catch (final Exception ignored) {
                //return Collections.emptyList();
            }

            for(int i=0; i<10; i++){
                completions.add(i + ".0");
            }









            final List<String> allArgs = context.getRawInput();

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.getSender(), allArgs.toArray(new String[0]), "[Enter Variable / Mathematical Expression]", "[...]");



            if(context.getSender() instanceof Player player){
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                if (variable == null || variable.getPossibleValues(questPlayer) == null) {
                    return completions;
                }
                completions.addAll(variable.getPossibleValues(questPlayer));
            }else{
                if(variable == null || variable.getPossibleValues(null) == null){
                    return completions;
                }
                completions.addAll(variable.getPossibleValues(null));
            }
            return completions;
        }

    }

}