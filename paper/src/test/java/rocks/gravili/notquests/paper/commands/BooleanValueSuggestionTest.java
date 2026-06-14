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

package rocks.gravili.notquests.paper.commands;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import org.junit.jupiter.api.Test;
import rocks.gravili.notquests.paper.commands.arguments.variables.BooleanVariableArgument;

/**
 * Drives the REAL Brigadier dispatcher through the REAL {@link BooleanVariableArgument} and asserts
 * the completions a player would see while tab-completing a boolean value (e.g. the trailing
 * argument of {@code /qa conditions check <BooleanVariable> equals <value>}).
 *
 * <p>This exercises the same server-side suggestion path Brigadier runs for a player; the only thing
 * it skips is the network round-trip (suggestions for these custom arguments are computed on the
 * server, so nothing client-side is involved in producing the list).
 */
class BooleanValueSuggestionTest {

    private static List<String> completionsFor(final String input) throws Exception {
        // The boolean-value argument's suggestions are static (true/false); it does not need a
        // variable or the plugin instance to produce them.
        final BooleanVariableArgument argument = BooleanVariableArgument.booleanVariableArgument("expression", null);
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("test").then(argument("value", argument)));

        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }

    @Test
    void booleanValueArgumentSuggestsTrueAndFalse() throws Exception {
        final List<String> completions = completionsFor("test ");
        assertTrue(completions.contains("true"),
                "boolean value argument should suggest 'true' but suggested: " + completions);
        assertTrue(completions.contains("false"),
                "boolean value argument should suggest 'false' but suggested: " + completions);
    }
}
