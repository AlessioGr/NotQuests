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
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
public class ListVariableValueParser<C> implements ArgumentParser<C, String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    protected ListVariableValueParser(String identifier, Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static <C> @NonNull ParserDescriptor<C, String> listVariableParser(String identifier, Variable<?> variable) {
        return ParserDescriptor.of(new ListVariableValueParser<>(identifier, variable), String.class);
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(@NonNull CommandContext<@NonNull C> commandContext, @NonNull CommandInput commandInput) {
        if (commandInput.isEmpty()) {
            return ArgumentParseResult.failure(new IllegalArgumentException("No input provided"));
        }
        return ArgumentParseResult.success(commandInput.readString());
    }


    @Override
    public @NonNull SuggestionProvider<C> suggestionProvider() {
        return ((context, input) ->  {
            List<Suggestion> completions = new ArrayList<>();
            completions.add(Suggestion.suggestion("<Enter Variables>"));

            main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), input.input().split(" "), "[Enter String]", "[...]");


            if(context.sender() instanceof Player player) {
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
}