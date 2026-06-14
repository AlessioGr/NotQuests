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
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument;

/**
 * Drives Brigadier through the real {@link NumberVariableArgument}. This covers the server-side
 * suggestion path used by commands such as
 * {@code /qa edit <quest> objectives add BreakBlocks grass_block <amount>}.
 */
class NumberValueSuggestionTest {

    private static List<String> completionsFor(final String input) throws Exception {
        final NumberVariableArgument argument = NumberVariableArgument.numberVariableArgument("amount", null);
        final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("test").then(argument("amount", argument)));

        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new Object()))
                .get()
                .getList()
                .stream()
                .map(Suggestion::getText)
                .toList();
    }

    @Test
    void numberValueArgumentSuggestsUsefulNumberExamples() throws Exception {
        final List<String> completions = completionsFor("test ");
        assertTrue(completions.contains("1"), "number value argument should suggest '1' but suggested: " + completions);
        assertTrue(completions.contains("10"), "number value argument should suggest '10' but suggested: " + completions);
        assertTrue(completions.contains("100"), "number value argument should suggest '100' but suggested: " + completions);
    }
}
