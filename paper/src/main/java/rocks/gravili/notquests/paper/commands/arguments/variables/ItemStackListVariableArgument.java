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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArgumentType;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Native-framework port of {@code ItemStackListVariableValueParser}: a generic "variable value"
 * argument whose value is the raw string. Like the Cloud parser, {@code parse} reads a single token,
 * so this keeps the default <b>non-greedy</b> {@code string()} native type.
 */
public final class ItemStackListVariableArgument extends NQArgumentType<String> {
    private final NotQuests main;

    private final String identifier;
    private final Variable<?> variable;

    public ItemStackListVariableArgument(final String identifier, final Variable<?> variable) {
        this.main = NotQuests.getInstance();
        this.identifier = identifier;
        this.variable = variable;
    }

    public static ItemStackListVariableArgument itemStackListVariableArgument(final String identifier, final Variable<?> variable) {
        return new ItemStackListVariableArgument(identifier, variable);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String convert(final String input) throws CommandSyntaxException {
        return input;
    }

    // Material is a static enum; suggestions run on every keystroke — build the name list once.
    private static volatile List<String> cachedMaterialNames;

    private static List<String> materialNames() {
        List<String> cached = cachedMaterialNames;
        if (cached == null) {
            final List<String> list = new ArrayList<>(Material.values().length + 2);
            for (final Material value : Material.values()) {
                list.add(value.name().toLowerCase(Locale.ROOT));
            }
            list.add("hand");
            list.add("any");
            cached = List.copyOf(list);
            cachedMaterialNames = cached; // benign race: computation is idempotent
        }
        return cached;
    }

    @Override
    protected List<String> suggest(final CommandContext<?> context, final String remaining) {
        final List<String> completions = new ArrayList<>(materialNames());

        QuestPlayer questPlayer = null;
        if (context.getSource() instanceof CommandSourceStack source
                && source.getSender() instanceof Player player) {
            questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());
        }
        final List<String> possibleValues = variable.getPossibleValues(questPlayer);
        if (possibleValues != null) {
            completions.addAll(possibleValues);
        }

        // Only show the "type free text" placeholder when there is nothing concrete to choose from,
        // so the material list isn't cluttered by an unselectable entry.
        if (completions.isEmpty()) {
            completions.add("<Enter String>");
        }
        return completions;
    }
}
