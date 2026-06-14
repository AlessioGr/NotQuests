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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Native-framework port of {@code ListVariableValueParser}: a generic "variable value" argument whose
 * value is the raw string. Like the Cloud parser, {@code parse} reads a single token, so this keeps
 * the default <b>non-greedy</b> {@code string()} native type.
 */
public final class ListVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    public ListVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static ListVariableArgument listVariableArgument(final String identifier, final Variable<?> variable) {
        return new ListVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> completions = new ArrayList<>();
        completions.add("<Enter Variables>");

        QuestPlayer questPlayer = null;
        if (context.getSource() instanceof CommandSourceStack source
                && source.getSender() instanceof Player player) {
            questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        }
        final List<String> possibleValues = variable.getPossibleValues(questPlayer);
        if (possibleValues != null) {
            for (final String suggestion : possibleValues) {
                completions.add(suggestion);
            }
        }
        return completions;
    }
}
