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

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;
import rocks.gravili.notquests.paper.structs.variables.VariableDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
public class BooleanVariableValueParser<C> implements ArgumentParser<C, String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    private SuggestionProvider<C> suggestionProvider;

    protected BooleanVariableValueParser(String identifier, Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static <C> @NonNull BooleanVariableValueParser<C> of(String identifier, Variable<?> variable, SuggestionProvider<C> suggestionProvider) {
        BooleanVariableValueParser<C> parser = new BooleanVariableValueParser<>(identifier, variable);
        parser.suggestionProvider = suggestionProvider;
        return parser;
    }

    public static <C> @NonNull BooleanVariableValueParser<C> of(String identifier, Variable<?> variable) {
        return new BooleanVariableValueParser<>(identifier, variable);
    }

    public static <C> @NonNull ParserDescriptor<C, String> booleanVariableParser(String identifier, Variable<?> variable) {
        return ParserDescriptor.of(new BooleanVariableValueParser<>(identifier, variable), String.class);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        if (commandInput.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("No input provided"));
        }
        final String input = commandInput.readString();
        try {
            final NumberExpression numberExpression = new NumberExpression(main, input);

            if (commandContext.sender() instanceof Player player) {
                try {
                    numberExpression.calculateBooleanValue(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()));
                } catch (Exception e) {
                    if (main.getConfiguration().isDebug()) {
                        e.printStackTrace();
                    }
                    return ArgumentParseResult.failure(new IllegalArgumentException("Invalid Expression: " + input + ". Error: " + e));
                }
            }
        } catch (Exception e) {
            return ArgumentParseResult.failure(new IllegalArgumentException("Erroring Expression: " + input + ". Error: " + e));
        }
        return ArgumentParseResult.success(input);
    }


    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        if(suggestionProvider != null) return suggestionProvider;

        return ((context, input) -> {
            final List<Suggestion> completions = new ArrayList<>();
            completions.add(Suggestion.suggestion("true"));
            completions.add(Suggestion.suggestion("false"));

            String rawInput = input.input();
            for(final String variableString : main.getVariablesManager().getVariableIdentifiers()) {
                final Variable<?> variable = main.getVariablesManager().getVariableFromString(variableString);
                if (variable == null || (variable.getVariableDataType() != VariableDataType.NUMBER && variable.getVariableDataType() != VariableDataType.BOOLEAN )) {
                    continue;
                }
                if(variable.getRequiredStrings().isEmpty() && variable.getRequiredNumbers().isEmpty() && variable.getRequiredBooleans().isEmpty() && variable.getRequiredBooleanFlags().isEmpty()){
                    completions.add(Suggestion.suggestion(variableString));
                }else{
                    if (!rawInput.endsWith(variableString + "(")) {
                        if (rawInput.endsWith(",") && rawInput.contains(variableString + "(")) {
                            for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                                if (!rawInput.contains(stringParser.getIdentifier())) {
                                    completions.add(Suggestion.suggestion(rawInput + stringParser.getIdentifier() + ":"));
                                }
                            }
                            for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                                if (!rawInput.contains(numberParser.getIdentifier())) {
                                    completions.add(Suggestion.suggestion(rawInput + numberParser.getIdentifier() + ":"));
                                }
                            }
                            for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                                if (!rawInput.contains(booleanParser.getIdentifier())) {
                                    completions.add(Suggestion.suggestion(rawInput + booleanParser.getIdentifier() + ":"));
                                }
                            }
                            for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                                if (!rawInput.contains(flag.name())) {
                                    completions.add(Suggestion.suggestion(rawInput + "--" + flag.name() + ""));
                                }
                            }
                        } else if (!rawInput.endsWith(")")) {
                            if (rawInput.contains(variableString + "(") && (!rawInput.contains(")") || (rawInput.lastIndexOf("(") < rawInput.lastIndexOf(")")))) {
                                final String subStringAfter = rawInput.substring(rawInput.indexOf(variableString + "("));

                                try {
                                    for (final StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                                        if (subStringAfter.contains(":")) {
                                            Iterable<Suggestion> suggestions = (Iterable<Suggestion>) stringParser.suggestionProvider().suggestionsFuture(context.get(identifier), input).get();
                                            if (subStringAfter.endsWith(":")) {
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput + suggestion.suggestion())));
                                            } else {
                                                final String[] splitDoubleDots = subStringAfter.split(":");
                                                final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion())));
                                            }
                                        } else {
                                            completions.add(Suggestion.suggestion(variableString + "(" + stringParser.getIdentifier() + ":"));
                                        }
                                    }
                                    for (final NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                                        if (subStringAfter.contains(":")) {
                                            Iterable<Suggestion> suggestions = (Iterable<Suggestion>) numberParser.suggestionProvider().suggestionsFuture(context.get(identifier), input).get();
                                            if (subStringAfter.endsWith(":")) {
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput + suggestion.suggestion())));
                                            } else {
                                                final String[] splitDoubleDots = subStringAfter.split(":");
                                                final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion())));
                                            }
                                        } else {
                                            completions.add(Suggestion.suggestion(variableString + "(" + numberParser.getIdentifier() + ":"));
                                        }
                                    }
                                    for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                                        if (subStringAfter.contains(":")) {
                                            Iterable<Suggestion> suggestions = (Iterable<Suggestion>) booleanParser.suggestionProvider().suggestionsFuture(context.get(identifier), input).get();
                                            if (subStringAfter.endsWith(":")) {
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput + suggestion.suggestion())));
                                            } else {

                                                final String[] splitDoubleDots = subStringAfter.split(":");
                                                final String stringAfterLastDoubleDot = splitDoubleDots[splitDoubleDots.length - 1];
                                                suggestions.forEach(suggestion -> completions.add(Suggestion.suggestion(rawInput.substring(0, rawInput.length() - stringAfterLastDoubleDot.length() - 1) + ":" + suggestion.suggestion())));
                                            }
                                        } else {
                                            completions.add(Suggestion.suggestion(variableString + "(" + booleanParser.getIdentifier() + ":"));
                                        }
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new RuntimeException(e);
                                }

                                for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                                    completions.add(Suggestion.suggestion(variableString + "(--" + flag.name() + ""));
                                }
                            } else {
                                completions.add(Suggestion.suggestion(variableString + "("));
                            }
                        }
                    } else {
                        for (StringVariableValueParser<CommandSender> stringParser : variable.getRequiredStrings()) {
                            completions.add(Suggestion.suggestion(variableString + "(" + stringParser.getIdentifier() + ":"));
                        }
                        for (NumberVariableValueParser<CommandSender> numberParser : variable.getRequiredNumbers()) {
                            completions.add(Suggestion.suggestion(variableString + "(" + numberParser.getIdentifier() + ":"));
                        }
                        for (BooleanVariableValueParser<CommandSender> booleanParser : variable.getRequiredBooleans()) {
                            completions.add(Suggestion.suggestion(variableString + "(" + booleanParser.getIdentifier() + ":"));
                        }
                        for (CommandFlag<Void> flag : variable.getRequiredBooleanFlags()) {
                            completions.add(Suggestion.suggestion(variableString + "(--" + flag.name() + ""));
                        }
                    }
                }
            }

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), rawInput.split(" "), "[Enter Variable]", "[...]");

            if(context.sender() instanceof final Player player) {
                final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
                if (variable.getPossibleValues(questPlayer) == null) {
                    return CompletableFuture.completedFuture(completions);
                }
                completions.addAll(variable.getPossibleValues(questPlayer));
            }else{
                if(variable.getPossibleValues(null) == null){
                    return CompletableFuture.completedFuture(completions);
                }
                completions.addAll(variable.getPossibleValues(null));
            }
            return CompletableFuture.completedFuture(completions);
        });
    }

    public ParserDescriptor<C, String> getParserDescriptor() {
        return ParserDescriptor.of(this, String.class);
    }

    public String getIdentifier() {
        return identifier;
    }
}