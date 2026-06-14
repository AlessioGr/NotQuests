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
import rocks.gravili.notquests.paper.commands.framework.NQCommandContext;
import rocks.gravili.notquests.paper.commands.framework.NQSuggestionProvider;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Native-framework port of {@code StringVariableValueParser}: a generic "variable value" argument
 * whose value is the raw string. Backed by a <b>non-greedy</b> {@code string()} (mirroring the Cloud
 * mapping in {@code CommandManager.preSetupCommands()}), so it reads a single token and can precede
 * other arguments.
 */
public final class StringVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    public StringVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static StringVariableArgument stringVariableArgument(final String identifier, final Variable<?> variable) {
        return new StringVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> completions = new ArrayList<>();

        // A variable's required-string argument (e.g. the IntegerTag "TagName") carries its own
        // suggestion provider on the matching StringVariableValueParser — that is where the real
        // values live (existing integer tags, block materials, ...). Such variables typically leave
        // getPossibleValues() null, so prefer the parser's provider and only fall back to
        // getPossibleValues() when there is none.
        final NQSuggestionProvider provider = parserSuggestionProvider();
        if (provider != null) {
            final NQCommandContext nqContext = context.getSource() instanceof CommandSourceStack
                    ? new NQCommandContext(
                            (CommandContext<CommandSourceStack>) context, Map.of(), Set.of(),
                            ((CommandContext<CommandSourceStack>) context).getInput())
                    : null;
            final List<String> provided = provider.suggest(nqContext, remaining);
            if (provided != null) {
                completions.addAll(provided);
            }
        } else {
            QuestPlayer questPlayer = null;
            if (context.getSource() instanceof CommandSourceStack source
                    && source.getSender() instanceof Player player) {
                questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
            }
            final List<String> possibleValues = variable.getPossibleValues(questPlayer);
            if (possibleValues != null) {
                completions.addAll(possibleValues);
            }
        }

        // Only show the "type free text" placeholder when there are no concrete values to choose
        // from, so real suggestions (blocks, tags, ...) aren't cluttered by an unselectable entry.
        if (completions.isEmpty()) {
            completions.add("<Enter String>");
        }
        return completions;
    }

    /** The suggestion provider declared on this variable's matching required-string parser, if any. */
    private NQSuggestionProvider parserSuggestionProvider() {
        if (variable.getRequiredStrings() == null) {
            return null;
        }
        for (final var parser : variable.getRequiredStrings()) {
            if (identifier.equals(parser.getIdentifier())) {
                return parser.getSuggestionProvider();
            }
        }
        return null;
    }
}
